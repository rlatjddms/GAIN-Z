package com.project.gainz.domain.user.service;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.friend.enums.FriendRelation;
import com.project.gainz.domain.friend.service.FriendService;
import com.project.gainz.domain.practice.dto.response.DailyPracticeResponse;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.user.dto.response.ProfileMusicResponse;
import com.project.gainz.domain.user.dto.response.UserProfileResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.ProfileVisibility;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final Long ME = 1L;
    private static final Long TARGET = 2L;

    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private PracticeLogRepository practiceLogRepository;
    @Mock private BandMemberRepository bandMemberRepository;
    @Mock private FriendService friendService;

    @InjectMocks private UserProfileService userProfileService;

    /* ---------- 준비 ---------- */

    // 캐릭터가 없으면 이메일 앞부분이 보이는 이름이라 name으로 이메일을 만든다 (#227)
    private User user(Long id, String name) {
        User user = User.builder()
                .email(name + "@example.com")
                .password("pw")
                .provider(SocialProvider.LOCAL)
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        lenient().when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private Character character(User user, String nickname) {
        return Character.builder()
                .user(user)
                .nickname(nickname)
                .instrumentType(InstrumentType.DRUM)
                .instrumentColor("#000000")
                .gender(Gender.values()[0])
                .hairStyle(HairStyle.values()[0])
                .hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class))
                .build();
    }

    private BandMember bandMember(Long bandId, String bandName, User user, Position position, InstrumentType part) {
        Band band = Band.builder().name(bandName).leader(user).inviteCode("CODE" + bandId).build();
        ReflectionTestUtils.setField(band, "id", bandId);
        return BandMember.builder().band(band).user(user).position(position).part(part).build();
    }

    // 음악을 걸어 두고 공개 범위를 정한 상대
    private User target(ProfileVisibility visibility) {
        User target = user(TARGET, "드러머");
        target.changeProfileMusic("Hysteria", "Muse", "https://youtu.be/abc");
        if (visibility != null) target.changeProfileVisibility(visibility);
        return target;
    }

    /* ---------- 공개 범위 ---------- */

    // 공개 범위 × 관계 → 볼 수 있는지 (null: 컬럼이 없던 기존 사용자 = 전체 공개)
    @ParameterizedTest(name = "{0} 프로필을 {1} 관계에서 보면 visible={2}")
    @CsvSource({
            "PUBLIC, NONE, true",
            ", NONE, true",
            "FRIENDS, FRIEND, true",
            "FRIENDS, NONE, false",
            "FRIENDS, SENT, false",
            "FRIENDS, RECEIVED, false",
            "PRIVATE, FRIEND, false",
            "PRIVATE, NONE, false",
    })
    void 공개_범위와_친구_관계에_따라_볼_수_있는지가_정해진다(ProfileVisibility visibility, FriendRelation relation, boolean visible) {
        target(visibility);
        when(friendService.relationOf(ME, TARGET)).thenReturn(relation);

        UserProfileResponse profile = userProfileService.getProfile(ME, TARGET);

        assertThat(profile.visible()).isEqualTo(visible);
        assertThat(profile.relation()).isEqualTo(relation);
        assertThat(profile.name()).isEqualTo("드러머");
    }

    @Test
    void 못_보는_프로필은_이름_사진_관계만_주고_나머지는_숨긴다() {
        User target = target(ProfileVisibility.PRIVATE);
        when(friendService.relationOf(ME, TARGET)).thenReturn(FriendRelation.FRIEND);
        when(characterRepository.findByUserId(TARGET)).thenReturn(Optional.of(character(target, "쿵쿵")));

        UserProfileResponse profile = userProfileService.getProfile(ME, TARGET);

        // 이름은 캐릭터 닉네임 (#227), 캐릭터 자체는 숨긴다
        assertThat(profile.name()).isEqualTo("쿵쿵");
        assertThat(profile.visible()).isFalse();
        assertThat(profile.profileMusic()).isNull();
        assertThat(profile.character()).isNull();
        assertThat(profile.dailyPractice()).isEmpty();
        assertThat(profile.bands()).isEmpty();
        verify(practiceLogRepository, never()).findDailyTotals(any(), any(), any());
    }

    @Test
    void 비공개여도_내_프로필은_다_보인다() {
        User me = user(ME, "나");
        me.changeProfileVisibility(ProfileVisibility.PRIVATE);
        when(friendService.relationOf(ME, ME)).thenReturn(FriendRelation.ME);

        UserProfileResponse profile = userProfileService.getProfile(ME, ME);

        assertThat(profile.visible()).isTrue();
        assertThat(profile.relation()).isEqualTo(FriendRelation.ME);
    }

    /* ---------- 보이는 내용 ---------- */

    @Test
    void 볼_수_있으면_음악_캐릭터_최근_연습_함께하는_밴드가_나온다() {
        User me = user(ME, "나");
        User target = target(ProfileVisibility.FRIENDS);
        when(friendService.relationOf(ME, TARGET)).thenReturn(FriendRelation.FRIEND);
        when(characterRepository.findByUserId(TARGET)).thenReturn(Optional.of(character(target, "쿵쿵")));
        LocalDate today = LocalDate.now();
        when(practiceLogRepository.findDailyTotals(eq(TARGET), eq(today.minusDays(UserProfileService.PROFILE_PRACTICE_DAYS - 1)), eq(today)))
                .thenReturn(List.of(new DailyPracticeResponse(today.minusDays(1), 1800L, 30L),
                        new DailyPracticeResponse(today, 0L, 0L)));
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(ME))
                .thenReturn(List.of(bandMember(10L, "같이하는 밴드", me, Position.MEMBER, InstrumentType.GUITAR)));
        when(bandMemberRepository.findAllByUserIdOrderByIdAsc(TARGET))
                .thenReturn(List.of(bandMember(10L, "같이하는 밴드", target, Position.LEADER, InstrumentType.DRUM),
                        bandMember(20L, "다른 밴드", target, Position.MEMBER, InstrumentType.DRUM)));

        UserProfileResponse profile = userProfileService.getProfile(ME, TARGET);

        assertThat(profile.visible()).isTrue();
        assertThat(profile.profileMusic()).isEqualTo(new ProfileMusicResponse("Hysteria", "Muse", "https://youtu.be/abc"));
        assertThat(profile.character().nickname()).isEqualTo("쿵쿵");
        // 연습 안 한 날은 빼고, 경험치 없이 시간만
        assertThat(profile.dailyPractice()).containsExactly(new UserProfileResponse.PracticeDay(today.minusDays(1), 1800L));
        assertThat(profile.bands()).extracting(UserProfileResponse.ProfileBand::name).containsExactly("같이하는 밴드", "다른 밴드");
        assertThat(profile.bands()).extracting(UserProfileResponse.ProfileBand::together).containsExactly(true, false);
        assertThat(profile.bands().get(0).position()).isEqualTo(Position.LEADER);
    }

    @Test
    void 없는_사용자는_볼_수_없다() {
        when(userRepository.findById(TARGET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile(ME, TARGET))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
