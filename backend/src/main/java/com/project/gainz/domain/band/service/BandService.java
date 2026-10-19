package com.project.gainz.domain.band.service;

import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.band.dto.request.BandCreateRequest;
import com.project.gainz.domain.band.dto.request.BandJoinRequest;
import com.project.gainz.domain.band.dto.request.BandLeaderRequest;
import com.project.gainz.domain.band.dto.request.BandMemberOrderRequest;
import com.project.gainz.domain.band.dto.request.BandMemberPartRequest;
import com.project.gainz.domain.band.dto.request.BandStageRequest;
import com.project.gainz.domain.band.dto.request.BandUpdateRequest;
import com.project.gainz.domain.band.dto.response.BandMemberResponse;
import com.project.gainz.domain.band.dto.response.BandResponse;
import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.BandRepository;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.domain.record.event.RecordCheckEvent;
import com.project.gainz.domain.record.enums.RecordMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BandService {

    private static final String HOME_URL = "/home";

    public static final int MAX_MEMBERS = 8;
    public static final int MAX_JOINED_BANDS = 5;
    public static final int INVITE_CODE_LENGTH = 8;
    // 헷갈리는 글자(0·O, 1·I·L)는 뺀다
    static final String INVITE_CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    // 무대에 선 순서 (순서가 없는 예전 멤버는 오른쪽에 들어온 순서로)
    private static final Comparator<BandMember> STAGE_ORDER = Comparator
            .comparing((BandMember m) -> m.getStageOrder() != null ? m.getStageOrder() : Integer.MAX_VALUE)
            .thenComparing(BandMember::getId);

    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /* ---------- 조회 ---------- */

    // 내가 들어간 밴드 (들어간 순서). 멤버와 캐릭터는 한 번에 모아서 불러온다
    @Transactional(readOnly = true)
    public List<BandResponse> getMyBands(Long userId) {
        List<BandMember> mine = bandMemberRepository.findAllByUserIdOrderByIdAsc(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BandMember>> membersByBand = bandMemberRepository
                .findAllByBandIdInOrderByIdAsc(mine.stream().map(m -> m.getBand().getId()).toList()).stream()
                .collect(Collectors.groupingBy(m -> m.getBand().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, Character> characters = charactersOf(membersByBand.values().stream().flatMap(List::stream).toList());
        return mine.stream()
                .map(me -> toResponse(me.getBand(), me, membersByBand.getOrDefault(me.getBand().getId(), List.of()), characters, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public BandResponse getBand(Long userId, Long bandId) {
        BandMember me = requireMember(userId, bandId);
        return respond(me.getBand(), me, userId);
    }

    /* ---------- 만들기·고치기·없애기 ---------- */

    public BandResponse createBand(Long userId, BandCreateRequest request) {
        checkJoinLimit(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Band band = bandRepository.save(Band.builder()
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .leader(user)
                .inviteCode(newInviteCode())
                .build());
        BandMember leader = bandMemberRepository.save(BandMember.builder()
                .band(band)
                .user(user)
                .position(Position.LEADER)
                .part(request.part())
                .stageOrder(0)
                .build());
        eventPublisher.publishEvent(RecordCheckEvent.of(userId, RecordMetric.BANDS));
        return toResponse(band, leader, List.of(leader), charactersOf(List.of(leader)), userId);
    }

    public BandResponse updateBand(Long userId, Long bandId, BandUpdateRequest request) {
        BandMember me = requireLeader(userId, bandId);
        me.getBand().update(request.name().trim(), blankToNull(request.description()));
        return respond(me.getBand(), me, userId);
    }

    public BandResponse changeStage(Long userId, Long bandId, BandStageRequest request) {
        BandMember me = requireLeader(userId, bandId);
        me.getBand().changeStage(request.stage());
        return respond(me.getBand(), me, userId);
    }

    // 무대 자리 순서: 지금 멤버 전부를 왼쪽부터 한 번씩 보내야 한다
    public BandResponse reorderMembers(Long userId, Long bandId, BandMemberOrderRequest request) {
        BandMember me = requireLeader(userId, bandId);
        Map<Long, BandMember> members = activeMembers(bandMemberRepository.findAllByBandIdOrderByIdAsc(bandId)).stream()
                .collect(Collectors.toMap(BandMember::getId, Function.identity()));
        List<Long> order = request.memberIds();
        if (order.size() != members.size() || new HashSet<>(order).size() != order.size()
                || !members.keySet().containsAll(order)) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_ORDER);
        }
        for (int i = 0; i < order.size(); i++) {
            members.get(order.get(i)).changeStageOrder(i);
        }
        return respond(me.getBand(), me, userId);
    }

    // 모든 멤버를 빼고 밴드를 소프트 삭제
    public void deleteBand(Long userId, Long bandId) {
        BandMember me = requireLeader(userId, bandId);
        Band band = me.getBand();
        List<User> others = usersOf(activeMembers(bandMemberRepository.findAllByBandIdOrderByIdAsc(bandId)));
        disband(band);
        notificationService.notifyAll(others, userId, NotificationType.BAND_DISBAND, band.getName(),
                "리더가 밴드를 없앴어요", HOME_URL);
    }

    /* ---------- 초대 코드 ---------- */

    // 새 코드로 바꾼다 (예전 코드로는 더 이상 못 들어온다)
    public BandResponse regenerateInviteCode(Long userId, Long bandId) {
        BandMember me = requireLeader(userId, bandId);
        me.getBand().changeInviteCode(newInviteCode());
        return respond(me.getBand(), me, userId);
    }

    public BandResponse joinBand(Long userId, BandJoinRequest request) {
        String code = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        Band band = bandRepository.findByInviteCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
        if (bandMemberRepository.findByBandIdAndUserId(band.getId(), userId).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_BAND_MEMBER);
        }
        checkJoinLimit(userId);
        List<BandMember> members = activeMembers(bandMemberRepository.findAllByBandIdOrderByIdAsc(band.getId()));
        if (members.size() >= MAX_MEMBERS) {
            throw new BusinessException(ErrorCode.BAND_FULL);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 새 멤버는 무대 맨 오른쪽
        int nextOrder = members.stream()
                .map(BandMember::getStageOrder)
                .filter(o -> o != null)
                .max(Integer::compare)
                .map(o -> o + 1)
                .orElse(members.size());
        BandMember me = bandMemberRepository.save(BandMember.builder()
                .band(band)
                .user(user)
                .position(Position.MEMBER)
                .part(request.part())
                .stageOrder(nextOrder)
                .build());
        notificationService.notifyAll(usersOf(members), userId, NotificationType.BAND_JOIN, band.getName(),
                notificationService.nicknameOf(user) + " 님이 밴드에 들어왔어요", settingsUrl(band));
        eventPublisher.publishEvent(RecordCheckEvent.of(userId, RecordMetric.BANDS));
        return respond(band, me, userId);
    }

    /* ---------- 멤버 관리 ---------- */

    // 내 파트는 내가, 다른 멤버 파트는 리더가 바꾼다
    public BandResponse changePart(Long userId, Long bandId, Long memberId, BandMemberPartRequest request) {
        BandMember me = requireMember(userId, bandId);
        BandMember target = findMember(bandId, memberId);
        if (!target.getId().equals(me.getId()) && !me.isLeader()) {
            throw new BusinessException(ErrorCode.BAND_PART_CHANGE_DENIED);
        }
        target.changePart(request.part());
        return respond(me.getBand(), me, userId);
    }

    public BandResponse removeMember(Long userId, Long bandId, Long memberId) {
        BandMember me = requireLeader(userId, bandId);
        BandMember target = findMember(bandId, memberId);
        if (target.getId().equals(me.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_SELF);
        }
        target.delete();
        notificationService.notify(target.getUser(), NotificationType.BAND_KICKED, me.getBand().getName(),
                "밴드에서 내보내졌어요", HOME_URL);
        return respond(me.getBand(), me, userId);
    }

    // 리더는 리더를 넘긴 뒤에만 나갈 수 있다 (혼자 남은 리더는 밴드 없애기)
    public void leaveBand(Long userId, Long bandId) {
        BandMember me = requireMember(userId, bandId);
        if (me.isLeader()) {
            throw new BusinessException(ErrorCode.BAND_LEADER_CANNOT_LEAVE);
        }
        me.delete();
        Band band = me.getBand();
        notificationService.notify(band.getLeader(), NotificationType.BAND_LEAVE, band.getName(),
                notificationService.nicknameOf(me.getUser()) + " 님이 밴드를 나갔어요", settingsUrl(band));
    }

    public BandResponse transferLeader(Long userId, Long bandId, BandLeaderRequest request) {
        BandMember me = requireLeader(userId, bandId);
        BandMember target = findMember(bandId, request.memberId());
        if (target.getId().equals(me.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_BAND_LEADER);
        }
        me.changePosition(Position.MEMBER);
        target.changePosition(Position.LEADER);
        me.getBand().changeLeader(target.getUser());
        notificationService.notify(target.getUser(), NotificationType.BAND_LEADER, me.getBand().getName(),
                notificationService.nicknameOf(me.getUser()) + " 님이 리더를 넘겨줬어요. 이제 내가 리더예요",
                settingsUrl(me.getBand()));
        return respond(me.getBand(), me, userId);
    }

    // 회원 탈퇴: 모든 밴드에서 빠진다. 리더였던 밴드는 가장 먼저 들어온 멤버에게 넘기고, 혼자였으면 없앤다
    public void leaveAllBands(Long userId) {
        for (BandMember me : bandMemberRepository.findAllByUserIdOrderByIdAsc(userId)) {
            Band band = me.getBand();
            if (me.isLeader()) {
                BandMember next = activeMembers(bandMemberRepository.findAllByBandIdOrderByIdAsc(band.getId())).stream()
                        .filter(m -> !m.getId().equals(me.getId()))
                        .findFirst()
                        .orElse(null);
                if (next == null) {
                    disband(band);
                    continue;
                }
                next.changePosition(Position.LEADER);
                band.changeLeader(next.getUser());
            }
            me.delete();
        }
    }

    /* ---------- 내부 ---------- */

    private BandMember requireMember(Long userId, Long bandId) {
        return bandMemberRepository.findByBandIdAndUserId(bandId, userId)
                .orElseThrow(() -> new BusinessException(
                        bandRepository.existsById(bandId) ? ErrorCode.NOT_BAND_MEMBER : ErrorCode.BAND_NOT_FOUND));
    }

    private BandMember requireLeader(Long userId, Long bandId) {
        BandMember me = requireMember(userId, bandId);
        if (!me.isLeader()) {
            throw new BusinessException(ErrorCode.BAND_LEADER_ONLY);
        }
        return me;
    }

    private BandMember findMember(Long bandId, Long memberId) {
        return bandMemberRepository.findByIdAndBandId(memberId, bandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAND_MEMBER_NOT_FOUND));
    }

    private void checkJoinLimit(Long userId) {
        if (bandMemberRepository.countByUserId(userId) >= MAX_JOINED_BANDS) {
            throw new BusinessException(ErrorCode.BAND_JOIN_LIMIT_EXCEEDED);
        }
    }

    private void disband(Band band) {
        bandMemberRepository.findAllByBandIdOrderByIdAsc(band.getId()).forEach(BandMember::delete);
        band.disband();
    }

    String newInviteCode() {
        for (int attempt = 0; attempt < INVITE_CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
            }
            if (!bandRepository.existsByInviteCode(code.toString())) {
                return code.toString();
            }
        }
        throw new BusinessException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    private BandResponse respond(Band band, BandMember me, Long userId) {
        List<BandMember> members = bandMemberRepository.findAllByBandIdOrderByIdAsc(band.getId());
        return toResponse(band, me, members, charactersOf(members), userId);
    }

    // 같은 트랜잭션에서 방금 뺀 멤버는 아직 목록에 남아 있을 수 있어서 한 번 더 거른다
    private static List<User> usersOf(List<BandMember> members) {
        return members.stream().map(BandMember::getUser).toList();
    }

    private static String settingsUrl(Band band) {
        return "/bands/" + band.getId() + "/settings";
    }

    private static List<BandMember> activeMembers(List<BandMember> members) {
        return members.stream().filter(m -> !Boolean.TRUE.equals(m.getIsDeleted())).toList();
    }

    private BandResponse toResponse(Band band, BandMember me, List<BandMember> members,
                                    Map<Long, Character> characters, Long userId) {
        List<BandMemberResponse> memberResponses = activeMembers(members).stream()
                .sorted(STAGE_ORDER)
                .map(m -> BandMemberResponse.of(m, characters.get(m.getUser().getId()), userId))
                .toList();
        return BandResponse.of(band, me.getPosition(), memberResponses);
    }

    private Map<Long, Character> charactersOf(List<BandMember> members) {
        List<Long> userIds = members.stream().map(m -> m.getUser().getId()).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return characterRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(c -> c.getUser().getId(), Function.identity(), (a, b) -> a));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
