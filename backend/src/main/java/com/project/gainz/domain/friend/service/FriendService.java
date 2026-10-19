package com.project.gainz.domain.friend.service;

import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.friend.dto.request.FriendRequestCreateRequest;
import com.project.gainz.domain.friend.dto.response.FriendCodeResponse;
import com.project.gainz.domain.friend.dto.response.FriendProfileResponse;
import com.project.gainz.domain.friend.dto.response.FriendRequestResponse;
import com.project.gainz.domain.friend.dto.response.FriendResponse;
import com.project.gainz.domain.friend.dto.response.FriendUserResponse;
import com.project.gainz.domain.friend.entity.Friendship;
import com.project.gainz.domain.friend.enums.FriendshipStatus;
import com.project.gainz.domain.friend.repository.FriendshipRepository;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendService {

    public static final int FRIEND_CODE_LENGTH = 8;
    public static final int MAX_FRIENDS = 100;

    // 헷갈리는 글자(0·O, 1·I·L)는 뺀다 (밴드 초대 코드와 같은 글자)
    static final String FRIEND_CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int FRIEND_CODE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // 친구 프로필 연습 잔디 기간 (화면은 30주, 이번 주 월요일 기준이라 한 주 더)
    static final int PROFILE_PRACTICE_DAYS = 7 * 31;

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final PracticeLogRepository practiceLogRepository;
    private final BandMemberRepository bandMemberRepository;

    /* ---------- 친구 코드 ---------- */

    // 아직 코드가 없으면 이때 만든다
    @Transactional
    public FriendCodeResponse getMyCode(Long userId) {
        User user = findUser(userId);
        if (user.getFriendCode() == null) {
            user.changeFriendCode(newFriendCode());
        }
        return new FriendCodeResponse(user.getFriendCode());
    }

    // 새 코드를 받으면 예전 코드로는 신청할 수 없다
    @Transactional
    public FriendCodeResponse regenerateCode(Long userId) {
        User user = findUser(userId);
        user.changeFriendCode(newFriendCode());
        return new FriendCodeResponse(user.getFriendCode());
    }

    /* ---------- 친구 신청 ---------- */

    @Transactional
    public FriendRequestResponse sendRequest(Long userId, FriendRequestCreateRequest request) {
        User me = findUser(userId);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        User target = userRepository.findByFriendCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_FRIEND_CODE));
        if (target.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANNOT_FRIEND_SELF);
        }

        for (Friendship existing : friendshipRepository.findAllBetween(userId, target.getId())) {
            if (existing.isAccepted()) {
                throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
            }
            throw new BusinessException(existing.getRequester().getId().equals(userId)
                    ? ErrorCode.FRIEND_REQUEST_ALREADY_SENT
                    : ErrorCode.FRIEND_REQUEST_ALREADY_RECEIVED);
        }
        checkFriendLimit(userId);

        Friendship saved = friendshipRepository.save(Friendship.builder().requester(me).receiver(target).build());
        return toRequestResponse(saved, target, charactersOf(List.of(target.getId())));
    }

    // 받은 신청 (보낸 사람이 보인다)
    public List<FriendRequestResponse> getReceivedRequests(Long userId) {
        List<Friendship> requests = friendshipRepository.findAllByReceiverIdAndStatusOrderByIdDesc(userId, FriendshipStatus.PENDING);
        return toRequestResponses(requests, userId);
    }

    // 보낸 신청 (받는 사람이 보인다)
    public List<FriendRequestResponse> getSentRequests(Long userId) {
        List<Friendship> requests = friendshipRepository.findAllByRequesterIdAndStatusOrderByIdDesc(userId, FriendshipStatus.PENDING);
        return toRequestResponses(requests, userId);
    }

    // 받은 신청만 수락할 수 있다
    @Transactional
    public FriendResponse acceptRequest(Long userId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .filter(Friendship::isPending)
                .filter(f -> f.getReceiver().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        User requester = friendship.getRequester();
        checkFriendLimit(userId);
        checkFriendLimit(requester.getId());

        friendship.accept();
        Character character = charactersOf(List.of(requester.getId())).get(requester.getId());
        return FriendResponse.of(requester, character, friendship.getAcceptedAt());
    }

    // 받은 신청 거절 / 보낸 신청 취소
    @Transactional
    public void deleteRequest(Long userId, Long requestId) {
        Friendship friendship = friendshipRepository.findById(requestId)
                .filter(Friendship::isPending)
                .filter(f -> f.involves(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
        friendshipRepository.delete(friendship);
    }

    /* ---------- 친구 목록 ---------- */

    // 이름 순
    public List<FriendResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findAllByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);
        Map<Long, Character> characters = charactersOf(friendships.stream().map(f -> f.other(userId).getId()).toList());
        return friendships.stream()
                .map(f -> {
                    User friend = f.other(userId);
                    return FriendResponse.of(friend, characters.get(friend.getId()), f.getAcceptedAt());
                })
                .sorted(Comparator.comparing(FriendResponse::name).thenComparing(FriendResponse::userId))
                .toList();
    }

    // 친구 프로필 (친구만 볼 수 있다)
    public FriendProfileResponse getFriendProfile(Long userId, Long friendUserId) {
        Friendship friendship = findFriendship(userId, friendUserId);
        User friend = friendship.other(userId);
        Character character = characterRepository.findByUserId(friendUserId).orElse(null);

        LocalDate today = LocalDate.now();
        List<FriendProfileResponse.PracticeDay> daily = practiceLogRepository
                .findDailyTotals(friendUserId, today.minusDays(PROFILE_PRACTICE_DAYS - 1), today).stream()
                .filter(d -> d.totalSeconds() != null && d.totalSeconds() > 0)
                .map(d -> new FriendProfileResponse.PracticeDay(d.date(), d.totalSeconds()))
                .toList();

        Set<Long> myBandIds = bandMemberRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(m -> m.getBand().getId())
                .collect(Collectors.toSet());
        List<FriendProfileResponse.FriendBand> bands = bandMemberRepository.findAllByUserIdOrderByIdAsc(friendUserId).stream()
                .map(m -> FriendProfileResponse.FriendBand.of(m, myBandIds.contains(m.getBand().getId())))
                .toList();

        return new FriendProfileResponse(friend.getId(), friend.getName(), MyInfoResponse.profileImageUrl(friend),
                character != null ? CharacterResponse.from(character) : null,
                friendship.getAcceptedAt(), friend.getCreatedAt(), daily, bands);
    }

    // 친구 끊기 (어느 쪽이든)
    @Transactional
    public void removeFriend(Long userId, Long friendUserId) {
        friendshipRepository.delete(findFriendship(userId, friendUserId));
    }

    // 두 사람이 친구인지 (공개 범위 '친구에게만' 판단용)
    public boolean areFriends(Long userId, Long otherUserId) {
        return friendshipRepository.findAllBetween(userId, otherUserId).stream().anyMatch(Friendship::isAccepted);
    }

    // 회원 탈퇴: 친구·신청을 모두 지운다
    @Transactional
    public void removeAll(Long userId) {
        friendshipRepository.deleteAll(friendshipRepository.findAllByUserId(userId));
    }

    /* ---------- 내부 ---------- */

    private Friendship findFriendship(Long userId, Long friendUserId) {
        return friendshipRepository.findAllBetween(userId, friendUserId).stream()
                .filter(Friendship::isAccepted)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FRIEND));
    }

    private void checkFriendLimit(Long userId) {
        if (friendshipRepository.countByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED) >= MAX_FRIENDS) {
            throw new BusinessException(ErrorCode.FRIEND_LIMIT_EXCEEDED);
        }
    }

    String newFriendCode() {
        for (int attempt = 0; attempt < FRIEND_CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(FRIEND_CODE_LENGTH);
            for (int i = 0; i < FRIEND_CODE_LENGTH; i++) {
                code.append(FRIEND_CODE_CHARS.charAt(RANDOM.nextInt(FRIEND_CODE_CHARS.length())));
            }
            if (!userRepository.existsByFriendCode(code.toString())) {
                return code.toString();
            }
        }
        throw new BusinessException(ErrorCode.FRIEND_CODE_GENERATION_FAILED);
    }

    private List<FriendRequestResponse> toRequestResponses(List<Friendship> requests, Long userId) {
        Map<Long, Character> characters = charactersOf(requests.stream().map(f -> f.other(userId).getId()).toList());
        return requests.stream().map(f -> toRequestResponse(f, f.other(userId), characters)).toList();
    }

    private FriendRequestResponse toRequestResponse(Friendship friendship, User other, Map<Long, Character> characters) {
        return new FriendRequestResponse(friendship.getId(), FriendUserResponse.of(other, characters.get(other.getId())),
                friendship.getCreatedAt());
    }

    private Map<Long, Character> charactersOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return characterRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(c -> c.getUser().getId(), Function.identity(), (a, b) -> a));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
