package com.project.gainz.domain.band.service;

import com.project.gainz.domain.band.dto.response.BandHandOverResponse;
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
import com.project.gainz.domain.band.enums.BandStage;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.band.repository.BandRepository;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.record.enums.RecordMetric;
import com.project.gainz.domain.record.event.RecordCheckEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class BandServiceTest {

    private static final Long BAND_ID = 10L;
    private static final Long LEADER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long OTHER_ID = 3L;

    @Mock private BandRepository bandRepository;
    @Mock private BandMemberRepository bandMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;

    @Mock private NotificationService notificationService;
    @Mock private com.project.gainz.domain.friend.service.FriendService friendService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private BandService bandService;

    /* ---------- 준비 ---------- */

    private User user(Long id) {
        User user = User.builder()
                .email("user" + id + "@example.com")
                .password("pw")
                .provider(SocialProvider.LOCAL)
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Band band(User leader) {
        Band band = Band.builder().name("락앤롤 베이비").description("주말 합주").leader(leader).inviteCode("ABCD2345").build();
        ReflectionTestUtils.setField(band, "id", BAND_ID);
        return band;
    }

    private BandMember member(Long memberId, Band band, User user, Position position, InstrumentType part) {
        BandMember member = BandMember.builder().band(band).user(user).position(position).part(part).build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private Character character(User user, String nickname) {
        return Character.builder()
                .user(user)
                .nickname(nickname)
                .instrumentType(InstrumentType.GUITAR)
                .instrumentColor("#000000")
                .gender(Gender.values()[0])
                .hairStyle(HairStyle.values()[0])
                .hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class))
                .build();
    }

    // 리더(멤버 id 100)와 일반 멤버(멤버 id 200)가 있는 밴드
    private record Fixture(Band band, BandMember leader, BandMember member, List<BandMember> all) {
    }

    private Fixture fixture() {
        User leaderUser = user(LEADER_ID);
        Band band = band(leaderUser);
        BandMember leader = member(100L, band, leaderUser, Position.LEADER, InstrumentType.GUITAR);
        BandMember member = member(200L, band, user(MEMBER_ID), Position.MEMBER, InstrumentType.DRUM);
        List<BandMember> all = new ArrayList<>(List.of(leader, member));
        // 테스트마다 리더나 멤버 한쪽만 쓰므로 lenient
        lenient().when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, LEADER_ID)).thenReturn(Optional.of(leader));
        lenient().when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, MEMBER_ID)).thenReturn(Optional.of(member));
        return new Fixture(band, leader, member, all);
    }

    private void membersAre(List<BandMember> members) {
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(members);
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(code);
    }

    /* ---------- 만들기 ---------- */

    @Test
    void 밴드를_만들면_리더로_들어가고_초대_코드가_생긴다() {
        User leader = user(LEADER_ID);
        when(bandMemberRepository.countByUserId(LEADER_ID)).thenReturn(0L);
        when(userRepository.findById(LEADER_ID)).thenReturn(Optional.of(leader));
        when(bandRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(bandRepository.save(any(Band.class))).thenAnswer(inv -> {
            Band b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", BAND_ID);
            return b;
        });
        when(bandMemberRepository.save(any(BandMember.class))).thenAnswer(inv -> {
            BandMember m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 100L);
            return m;
        });
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(character(leader, "기타 리더")));

        BandResponse response = bandService.createBand(LEADER_ID,
                new BandCreateRequest("  락앤롤 베이비 ", "   ", InstrumentType.BASS));

        assertThat(response.name()).isEqualTo("락앤롤 베이비");
        assertThat(response.description()).isNull();
        assertThat(response.myPosition()).isEqualTo(Position.LEADER);
        assertThat(response.inviteCode()).hasSize(BandService.INVITE_CODE_LENGTH);
        assertThat(response.stage()).isEqualTo(BandStage.STUDIO);
        assertThat(response.members()).singleElement().satisfies(m -> {
            assertThat(m.position()).isEqualTo(Position.LEADER);
            assertThat(m.part()).isEqualTo(InstrumentType.BASS);
            assertThat(m.me()).isTrue();
            assertThat(m.character().nickname()).isEqualTo("기타 리더");
        });
        verify(eventPublisher).publishEvent(RecordCheckEvent.of(LEADER_ID, RecordMetric.BANDS));
    }

    @Test
    void 들어간_밴드가_5개면_더_만들_수_없다() {
        when(bandMemberRepository.countByUserId(LEADER_ID)).thenReturn((long) BandService.MAX_JOINED_BANDS);

        assertError(() -> bandService.createBand(LEADER_ID, new BandCreateRequest("밴드", null, InstrumentType.GUITAR)),
                ErrorCode.BAND_JOIN_LIMIT_EXCEEDED);
        verify(bandRepository, never()).save(any());
    }

    @Test
    void 초대_코드는_헷갈리는_글자_없이_8자리로_만든다() {
        when(bandRepository.existsByInviteCode(anyString())).thenReturn(false);

        String code = bandService.newInviteCode();

        assertThat(code).hasSize(8);
        assertThat(code.chars()).allMatch(c -> BandService.INVITE_CODE_CHARS.indexOf(c) >= 0);
    }

    @Test
    void 초대_코드가_계속_겹치면_실패한다() {
        when(bandRepository.existsByInviteCode(anyString())).thenReturn(true);

        assertError(() -> bandService.newInviteCode(), ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    /* ---------- 조회 ---------- */

    @Test
    void 내_밴드_목록은_무대_순서대로_보여_주고_나를_표시한다() {
        User leaderUser = user(LEADER_ID);
        Band band = band(leaderUser);
        // 먼저 들어온 멤버를 리더 오른쪽에 세운 경우
        BandMember early = member(100L, band, user(MEMBER_ID), Position.MEMBER, InstrumentType.DRUM);
        BandMember leader = member(200L, band, leaderUser, Position.LEADER, InstrumentType.GUITAR);
        early.changeStageOrder(1);
        leader.changeStageOrder(0);
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(MEMBER_ID)).thenReturn(List.of(early));
        when(bandMemberRepository.findAllByBandIdInOrderByIdAsc(List.of(BAND_ID))).thenReturn(List.of(early, leader));
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        List<BandResponse> bands = bandService.getMyBands(MEMBER_ID);

        assertThat(bands).singleElement().satisfies(b -> {
            assertThat(b.myPosition()).isEqualTo(Position.MEMBER);
            assertThat(b.members()).extracting(BandMemberResponse::memberId).containsExactly(200L, 100L);
            assertThat(b.members()).extracting(BandMemberResponse::me).containsExactly(false, true);
        });
    }

    @Test
    void 밴드가_없으면_빈_목록이다() {
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(LEADER_ID)).thenReturn(List.of());

        assertThat(bandService.getMyBands(LEADER_ID)).isEmpty();
    }

    @Test
    void 멤버가_아니면_밴드를_볼_수_없다() {
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(bandRepository.existsById(BAND_ID)).thenReturn(true);

        assertError(() -> bandService.getBand(OTHER_ID, BAND_ID), ErrorCode.NOT_BAND_MEMBER);
    }

    @Test
    void 없는_밴드는_찾을_수_없다() {
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(bandRepository.existsById(BAND_ID)).thenReturn(false);

        assertError(() -> bandService.getBand(OTHER_ID, BAND_ID), ErrorCode.BAND_NOT_FOUND);
    }

    /* ---------- 가입 ---------- */

    @Test
    void 초대_코드로_가입하면_일반_멤버가_된다() {
        Band band = band(user(LEADER_ID));
        when(bandRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(band));
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(bandMemberRepository.countByUserId(OTHER_ID)).thenReturn(0L);
        BandMember leader = member(100L, band, band.getLeader(), Position.LEADER, InstrumentType.GUITAR);
        leader.changeStageOrder(3);
        when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(user(OTHER_ID)));
        when(bandMemberRepository.save(any(BandMember.class))).thenAnswer(inv -> {
            BandMember m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 300L);
            return m;
        });
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(List.of(leader));
        when(characterRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());

        // 소문자·앞뒤 공백은 무시
        BandResponse response = bandService.joinBand(OTHER_ID, new BandJoinRequest(" abcd2345 ", InstrumentType.VOCAL));

        ArgumentCaptor<BandMember> saved = ArgumentCaptor.forClass(BandMember.class);
        verify(bandMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getPosition()).isEqualTo(Position.MEMBER);
        assertThat(saved.getValue().getPart()).isEqualTo(InstrumentType.VOCAL);
        // 새 멤버는 무대 맨 오른쪽
        assertThat(saved.getValue().getStageOrder()).isEqualTo(4);
        assertThat(response.myPosition()).isEqualTo(Position.MEMBER);
        // 기존 멤버에게 새 멤버 알림
        verify(eventPublisher).publishEvent(RecordCheckEvent.of(OTHER_ID, RecordMetric.BANDS));
        verify(notificationService).notifyAll(eq(List.of(band.getLeader())), eq(OTHER_ID), eq(NotificationType.BAND_JOIN),
                eq("락앤롤 베이비"), anyString(), eq("/bands/" + BAND_ID + "/settings"));
    }

    /* ---------- 친구에게 밴드 초대 (#229) ---------- */

    @Test
    void 멤버는_친구에게_초대_알림을_보내고_알림을_누르면_초대_코드_가입_창이_열린다() {
        Fixture f = fixture();
        User friend = user(OTHER_ID);
        when(friendService.relationOf(MEMBER_ID, OTHER_ID)).thenReturn(com.project.gainz.domain.friend.enums.FriendRelation.FRIEND);
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(friend));
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(f.all());
        when(notificationService.nicknameOf(f.member().getUser())).thenReturn("기타왕");

        bandService.inviteFriend(MEMBER_ID, BAND_ID, new com.project.gainz.domain.band.dto.request.BandInviteRequest(OTHER_ID));

        verify(notificationService).notify(friend, NotificationType.BAND_INVITE, "락앤롤 베이비",
                "기타왕 님이 밴드에 초대했어요. 눌러서 들어가 보세요", "/home?join=ABCD2345");
    }

    @Test
    void 친구가_아니면_초대할_수_없다() {
        fixture();
        when(friendService.relationOf(MEMBER_ID, OTHER_ID)).thenReturn(com.project.gainz.domain.friend.enums.FriendRelation.SENT);

        assertError(() -> bandService.inviteFriend(MEMBER_ID, BAND_ID, new com.project.gainz.domain.band.dto.request.BandInviteRequest(OTHER_ID)),
                ErrorCode.BAND_INVITE_NOT_FRIEND);
        verify(notificationService, never()).notify(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void 이미_밴드에_있는_친구는_초대할_수_없다() {
        Fixture f = fixture();
        when(friendService.relationOf(LEADER_ID, MEMBER_ID)).thenReturn(com.project.gainz.domain.friend.enums.FriendRelation.FRIEND);

        assertError(() -> bandService.inviteFriend(LEADER_ID, BAND_ID, new com.project.gainz.domain.band.dto.request.BandInviteRequest(MEMBER_ID)),
                ErrorCode.BAND_INVITE_ALREADY_MEMBER);
    }

    @Test
    void 인원이_찬_밴드로는_초대할_수_없다() {
        Band band = band(user(LEADER_ID));
        List<BandMember> full = new ArrayList<>();
        full.add(member(100L, band, band.getLeader(), Position.LEADER, InstrumentType.GUITAR));
        for (long i = 1; i < BandService.MAX_MEMBERS; i++) {
            full.add(member(100L + i, band, user(100L + i), Position.MEMBER, InstrumentType.GUITAR));
        }
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, LEADER_ID)).thenReturn(Optional.of(full.get(0)));
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(full);
        when(friendService.relationOf(LEADER_ID, OTHER_ID)).thenReturn(com.project.gainz.domain.friend.enums.FriendRelation.FRIEND);

        assertError(() -> bandService.inviteFriend(LEADER_ID, BAND_ID, new com.project.gainz.domain.band.dto.request.BandInviteRequest(OTHER_ID)),
                ErrorCode.BAND_FULL);
    }

    @Test
    void 밴드_멤버가_아니면_초대할_수_없다() {
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(bandRepository.existsById(BAND_ID)).thenReturn(true);

        assertError(() -> bandService.inviteFriend(OTHER_ID, BAND_ID, new com.project.gainz.domain.band.dto.request.BandInviteRequest(MEMBER_ID)),
                ErrorCode.NOT_BAND_MEMBER);
    }

    @Test
    void 없는_초대_코드로는_가입할_수_없다() {
        when(bandRepository.findByInviteCode("ZZZZ9999")).thenReturn(Optional.empty());

        assertError(() -> bandService.joinBand(OTHER_ID, new BandJoinRequest("ZZZZ9999", InstrumentType.GUITAR)),
                ErrorCode.INVALID_INVITE_CODE);
    }

    @Test
    void 이미_멤버면_다시_가입할_수_없다() {
        Fixture f = fixture();
        when(bandRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(f.band()));

        assertError(() -> bandService.joinBand(MEMBER_ID, new BandJoinRequest("ABCD2345", InstrumentType.GUITAR)),
                ErrorCode.ALREADY_BAND_MEMBER);
    }

    @Test
    void 멤버가_8명이면_가입할_수_없다() {
        Band band = band(user(LEADER_ID));
        when(bandRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(band));
        when(bandMemberRepository.findByBandIdAndUserId(BAND_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(bandMemberRepository.countByUserId(OTHER_ID)).thenReturn(0L);
        List<BandMember> full = new ArrayList<>();
        for (long i = 0; i < BandService.MAX_MEMBERS; i++) {
            full.add(member(100L + i, band, user(100L + i), Position.MEMBER, InstrumentType.GUITAR));
        }
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(full);

        assertError(() -> bandService.joinBand(OTHER_ID, new BandJoinRequest("ABCD2345", InstrumentType.GUITAR)),
                ErrorCode.BAND_FULL);
        verify(bandMemberRepository, never()).save(any());
    }

    /* ---------- 고치기·코드 ---------- */

    @Test
    void 리더는_밴드_정보를_고친다() {
        Fixture f = fixture();
        membersAre(f.all());

        BandResponse response = bandService.updateBand(LEADER_ID, BAND_ID, new BandUpdateRequest(" 새 이름 ", " 새 소개 "));

        assertThat(response.name()).isEqualTo("새 이름");
        assertThat(response.description()).isEqualTo("새 소개");
    }

    @Test
    void 리더가_아니면_밴드_정보를_고칠_수_없다() {
        fixture();

        assertError(() -> bandService.updateBand(MEMBER_ID, BAND_ID, new BandUpdateRequest("이름", null)),
                ErrorCode.BAND_LEADER_ONLY);
    }

    @Test
    void 리더는_초대_코드를_새로_받는다() {
        Fixture f = fixture();
        membersAre(f.all());
        when(bandRepository.existsByInviteCode(anyString())).thenReturn(false);

        BandResponse response = bandService.regenerateInviteCode(LEADER_ID, BAND_ID);

        assertThat(response.inviteCode()).hasSize(8).isNotEqualTo("ABCD2345");
    }

    /* ---------- 파트 ---------- */

    @Test
    void 멤버는_자기_파트를_바꾼다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(200L, BAND_ID)).thenReturn(Optional.of(f.member()));
        membersAre(f.all());

        bandService.changePart(MEMBER_ID, BAND_ID, 200L, new BandMemberPartRequest(InstrumentType.KEYBOARD));

        assertThat(f.member().getPart()).isEqualTo(InstrumentType.KEYBOARD);
    }

    @Test
    void 리더는_다른_멤버_파트를_바꾼다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(200L, BAND_ID)).thenReturn(Optional.of(f.member()));
        membersAre(f.all());

        bandService.changePart(LEADER_ID, BAND_ID, 200L, new BandMemberPartRequest(InstrumentType.BASS));

        assertThat(f.member().getPart()).isEqualTo(InstrumentType.BASS);
    }

    @Test
    void 일반_멤버는_다른_멤버_파트를_못_바꾼다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(100L, BAND_ID)).thenReturn(Optional.of(f.leader()));

        assertError(() -> bandService.changePart(MEMBER_ID, BAND_ID, 100L, new BandMemberPartRequest(InstrumentType.BASS)),
                ErrorCode.BAND_PART_CHANGE_DENIED);
        assertThat(f.leader().getPart()).isEqualTo(InstrumentType.GUITAR);
    }

    @Test
    void 다른_밴드의_멤버는_찾을_수_없다() {
        fixture();
        when(bandMemberRepository.findByIdAndBandId(999L, BAND_ID)).thenReturn(Optional.empty());

        assertError(() -> bandService.changePart(LEADER_ID, BAND_ID, 999L, new BandMemberPartRequest(InstrumentType.BASS)),
                ErrorCode.BAND_MEMBER_NOT_FOUND);
    }

    /* ---------- 내보내기·나가기 ---------- */

    @Test
    void 리더가_내보낸_멤버는_목록에서_빠진다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(200L, BAND_ID)).thenReturn(Optional.of(f.member()));
        membersAre(f.all());

        BandResponse response = bandService.removeMember(LEADER_ID, BAND_ID, 200L);

        assertThat(f.member().getIsDeleted()).isTrue();
        verify(notificationService).notify(f.member().getUser(), NotificationType.BAND_KICKED, "락앤롤 베이비",
                "밴드에서 내보내졌어요", "/home");
        assertThat(response.members()).extracting(BandMemberResponse::memberId).containsExactly(100L);
    }

    @Test
    void 리더는_자기를_내보낼_수_없다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(100L, BAND_ID)).thenReturn(Optional.of(f.leader()));

        assertError(() -> bandService.removeMember(LEADER_ID, BAND_ID, 100L), ErrorCode.CANNOT_REMOVE_SELF);
    }

    @Test
    void 일반_멤버는_내보낼_수_없다() {
        fixture();

        assertError(() -> bandService.removeMember(MEMBER_ID, BAND_ID, 100L), ErrorCode.BAND_LEADER_ONLY);
    }

    @Test
    void 멤버는_밴드를_나간다() {
        Fixture f = fixture();

        bandService.leaveBand(MEMBER_ID, BAND_ID);

        assertThat(f.member().getIsDeleted()).isTrue();
        // 리더에게 탈퇴 알림
        verify(notificationService).notify(eq(f.band().getLeader()), eq(NotificationType.BAND_LEAVE), eq("락앤롤 베이비"),
                anyString(), eq("/bands/" + BAND_ID + "/settings"));
    }

    @Test
    void 리더는_리더를_넘기기_전에_나갈_수_없다() {
        Fixture f = fixture();

        assertError(() -> bandService.leaveBand(LEADER_ID, BAND_ID), ErrorCode.BAND_LEADER_CANNOT_LEAVE);
        assertThat(f.leader().getIsDeleted()).isFalse();
    }

    /* ---------- 리더 넘기기 ---------- */

    @Test
    void 리더를_넘기면_자리가_바뀐다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(200L, BAND_ID)).thenReturn(Optional.of(f.member()));
        membersAre(f.all());

        BandResponse response = bandService.transferLeader(LEADER_ID, BAND_ID, new BandLeaderRequest(200L));

        assertThat(f.leader().getPosition()).isEqualTo(Position.MEMBER);
        assertThat(f.member().getPosition()).isEqualTo(Position.LEADER);
        assertThat(f.band().getLeader().getId()).isEqualTo(MEMBER_ID);
        assertThat(response.myPosition()).isEqualTo(Position.MEMBER);
        // 새 리더에게 알림
        verify(notificationService).notify(eq(f.member().getUser()), eq(NotificationType.BAND_LEADER), eq("락앤롤 베이비"),
                anyString(), eq("/bands/" + BAND_ID + "/settings"));
        assertThat(response.members()).extracting(BandMemberResponse::position)
                .containsExactly(Position.MEMBER, Position.LEADER);
    }

    @Test
    void 자기에게는_리더를_넘길_수_없다() {
        Fixture f = fixture();
        when(bandMemberRepository.findByIdAndBandId(100L, BAND_ID)).thenReturn(Optional.of(f.leader()));

        assertError(() -> bandService.transferLeader(LEADER_ID, BAND_ID, new BandLeaderRequest(100L)),
                ErrorCode.ALREADY_BAND_LEADER);
    }

    /* ---------- 배경·무대 순서 ---------- */

    @Test
    void 리더는_합주실_배경을_바꾼다() {
        Fixture f = fixture();
        membersAre(f.all());

        BandResponse response = bandService.changeStage(LEADER_ID, BAND_ID, new BandStageRequest(BandStage.LIVE));

        assertThat(response.stage()).isEqualTo(BandStage.LIVE);
    }

    @Test
    void 일반_멤버는_합주실_배경을_못_바꾼다() {
        Fixture f = fixture();

        assertError(() -> bandService.changeStage(MEMBER_ID, BAND_ID, new BandStageRequest(BandStage.GARAGE)),
                ErrorCode.BAND_LEADER_ONLY);
        assertThat(f.band().getStage()).isEqualTo(BandStage.STUDIO);
    }

    @Test
    void 배경이_비어_있는_예전_밴드는_연습실이다() {
        Band band = band(user(LEADER_ID));
        ReflectionTestUtils.setField(band, "stage", null);

        assertThat(band.getStage()).isEqualTo(BandStage.STUDIO);
    }

    @Test
    void 리더는_무대_자리_순서를_바꾼다() {
        Fixture f = fixture();
        membersAre(f.all());

        BandResponse response = bandService.reorderMembers(LEADER_ID, BAND_ID, new BandMemberOrderRequest(List.of(200L, 100L)));

        assertThat(f.member().getStageOrder()).isZero();
        assertThat(f.leader().getStageOrder()).isEqualTo(1);
        assertThat(response.members()).extracting(BandMemberResponse::memberId).containsExactly(200L, 100L);
    }

    @Test
    void 순서가_없는_예전_멤버는_들어온_순서대로_오른쪽에_선다() {
        Fixture f = fixture();
        f.member().changeStageOrder(0);
        membersAre(f.all());

        BandResponse response = bandService.getBand(LEADER_ID, BAND_ID);

        assertThat(response.members()).extracting(BandMemberResponse::memberId).containsExactly(200L, 100L);
    }

    @Test
    void 멤버가_빠지거나_겹친_순서는_받지_않는다() {
        Fixture f = fixture();
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(f.all());

        assertError(() -> bandService.reorderMembers(LEADER_ID, BAND_ID, new BandMemberOrderRequest(List.of(100L))),
                ErrorCode.INVALID_MEMBER_ORDER);
        assertError(() -> bandService.reorderMembers(LEADER_ID, BAND_ID, new BandMemberOrderRequest(List.of(100L, 100L))),
                ErrorCode.INVALID_MEMBER_ORDER);
        assertError(() -> bandService.reorderMembers(LEADER_ID, BAND_ID, new BandMemberOrderRequest(List.of(100L, 999L))),
                ErrorCode.INVALID_MEMBER_ORDER);
    }

    @Test
    void 일반_멤버는_무대_자리를_못_바꾼다() {
        fixture();

        assertError(() -> bandService.reorderMembers(MEMBER_ID, BAND_ID, new BandMemberOrderRequest(List.of(200L, 100L))),
                ErrorCode.BAND_LEADER_ONLY);
    }

    /* ---------- 없애기·탈퇴 ---------- */

    @Test
    void 밴드를_없애면_멤버가_모두_빠지고_초대_코드를_비운다() {
        Fixture f = fixture();
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(f.all());

        bandService.deleteBand(LEADER_ID, BAND_ID);

        assertThat(f.all()).allMatch(BandMember::getIsDeleted);
        assertThat(f.band().getIsDeleted()).isTrue();
        assertThat(f.band().getInviteCode()).isEqualTo("disbanded_" + BAND_ID);
        // 해체 알림은 남은 멤버에게 (리더 본인은 notifyAll에서 빠진다)
        verify(notificationService).notifyAll(eq(List.of(f.leader().getUser(), f.member().getUser())), eq(LEADER_ID),
                eq(NotificationType.BAND_DISBAND), eq("락앤롤 베이비"), eq("리더가 밴드를 없앴어요"), eq("/home"));
    }

    @Test
    void 일반_멤버는_밴드를_없앨_수_없다() {
        Fixture f = fixture();

        assertError(() -> bandService.deleteBand(MEMBER_ID, BAND_ID), ErrorCode.BAND_LEADER_ONLY);
        assertThat(f.band().getIsDeleted()).isFalse();
    }

    @Test
    void 다른_멤버가_있는_밴드의_리더는_리더를_넘기기_전에_탈퇴할_수_없고_어느_밴드인지_알려_준다() {
        User leaderUser = user(LEADER_ID);
        Band band = band(leaderUser);
        BandMember leader = member(100L, band, leaderUser, Position.LEADER, InstrumentType.GUITAR);
        BandMember first = member(200L, band, user(MEMBER_ID), Position.MEMBER, InstrumentType.DRUM);
        BandMember second = member(300L, band, user(OTHER_ID), Position.MEMBER, InstrumentType.BASS);
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(LEADER_ID)).thenReturn(List.of(leader));
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(List.of(leader, first, second));

        assertThatThrownBy(() -> bandService.leaveAllBands(LEADER_ID))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BAND_LEADER_MUST_HAND_OVER);
                    assertThat(((BandHandOverResponse) e.getData()).bands())
                            .containsExactly(new BandHandOverResponse.BandRef(BAND_ID, band.getName()));
                });

        // 자동으로 넘기지 않는다
        assertThat(leader.getIsDeleted()).isFalse();
        assertThat(first.getPosition()).isEqualTo(Position.MEMBER);
        assertThat(band.getLeader().getId()).isEqualTo(LEADER_ID);
    }

    @Test
    void 탈퇴할_때_혼자_있던_밴드는_없어진다() {
        User leaderUser = user(LEADER_ID);
        Band band = band(leaderUser);
        BandMember leader = member(100L, band, leaderUser, Position.LEADER, InstrumentType.GUITAR);
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(LEADER_ID)).thenReturn(List.of(leader));
        when(bandMemberRepository.findAllByBandIdOrderByIdAsc(BAND_ID)).thenReturn(List.of(leader));

        bandService.leaveAllBands(LEADER_ID);

        assertThat(leader.getIsDeleted()).isTrue();
        assertThat(band.getIsDeleted()).isTrue();
    }

    @Test
    void 탈퇴하면_일반_멤버로_있던_밴드에서_빠지고_리더에게_알린다() {
        Fixture f = fixture();
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(MEMBER_ID)).thenReturn(List.of(f.member()));
        when(notificationService.nicknameOf(f.member().getUser())).thenReturn("드러머");

        bandService.leaveAllBands(MEMBER_ID);

        assertThat(f.member().getIsDeleted()).isTrue();
        assertThat(f.leader().getIsDeleted()).isFalse();
        assertThat(f.band().getIsDeleted()).isFalse();
        verify(notificationService).notify(eq(f.leader().getUser()), eq(NotificationType.BAND_LEAVE), eq(f.band().getName()),
                eq("드러머 님이 탈퇴해서 밴드를 나갔어요"), eq("/bands/" + BAND_ID + "/settings"));
    }
}
