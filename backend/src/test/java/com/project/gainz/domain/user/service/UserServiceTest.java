package com.project.gainz.domain.user.service;

import com.project.gainz.domain.band.service.BandService;
import com.project.gainz.domain.friend.service.FriendService;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.dto.request.PasswordChangeRequest;
import com.project.gainz.domain.user.dto.request.ProfileMusicRequest;
import com.project.gainz.domain.user.dto.request.ProfileVisibilityRequest;
import com.project.gainz.domain.user.dto.response.ProfileMusicResponse;
import com.project.gainz.domain.user.enums.ProfileVisibility;
import com.project.gainz.domain.user.dto.request.UpdateNameRequest;
import com.project.gainz.domain.user.dto.request.WithdrawRequest;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "tester@example.com";
    private static final String ENCODED = "encoded-password";

    @Mock private UserRepository userRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProfileImageStorage profileImageStorage;
    @Mock private BandService bandService;
    @Mock private FriendService friendService;

    @InjectMocks private UserService userService;

    private User localUser() {
        return user(SocialProvider.LOCAL, ENCODED);
    }

    private User user(SocialProvider provider, String password) {
        User user = User.builder()
                .email(EMAIL)
                .password(password)
                .name("테스터")
                .provider(provider)
                .role(UserRole.ROLE_USER)
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    private Character character(User user) {
        return Character.builder()
                .user(user)
                .nickname("방구석 락스타")
                .instrumentType(InstrumentType.values()[0])
                .instrumentColor("#000000")
                .gender(Gender.values()[0])
                .hairStyle(HairStyle.values()[0])
                .hairColor("#000000")
                .currentOutfit(mock(OutfitItem.class))
                .build();
    }

    /* ---------- 조회·이름 ---------- */

    @Test
    void 내_정보를_조회한다() {
        localUser();

        MyInfoResponse response = userService.getMyInfo(USER_ID);

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.provider()).isEqualTo(SocialProvider.LOCAL);
        assertThat(response.profileImageUrl()).isNull();
    }

    @Test
    void 없는_유저면_예외가_발생한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 이름을_앞뒤_공백_없이_바꾼다() {
        User user = localUser();

        MyInfoResponse response = userService.updateName(USER_ID, new UpdateNameRequest("  새이름 "));

        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(response.name()).isEqualTo("새이름");
    }

    /* ---------- 프로필 뮤직·공개 범위 ---------- */

    @Test
    void 요즘_듣는_음악을_앞뒤_공백_없이_저장하고_빈_링크는_비운다() {
        User user = localUser();

        MyInfoResponse response = userService.updateProfileMusic(USER_ID, new ProfileMusicRequest(" Hysteria ", " Muse ", "  "));

        assertThat(user.getProfileMusicTitle()).isEqualTo("Hysteria");
        assertThat(user.getProfileMusicArtist()).isEqualTo("Muse");
        assertThat(user.getProfileMusicUrl()).isNull();
        assertThat(response.profileMusic()).isEqualTo(new ProfileMusicResponse("Hysteria", "Muse", null));
    }

    @Test
    void 링크가_있으면_함께_저장하고_지우면_음악이_없어진다() {
        User user = localUser();
        userService.updateProfileMusic(USER_ID, new ProfileMusicRequest("Hysteria", "Muse", " https://youtu.be/abc "));
        assertThat(user.getProfileMusicUrl()).isEqualTo("https://youtu.be/abc");

        MyInfoResponse response = userService.deleteProfileMusic(USER_ID);

        assertThat(user.hasProfileMusic()).isFalse();
        assertThat(user.getProfileMusicUrl()).isNull();
        assertThat(response.profileMusic()).isNull();
    }

    @Test
    void 공개_범위는_처음엔_전체_공개고_바꿀_수_있다() {
        User user = localUser();
        assertThat(userService.getMyInfo(USER_ID).profileVisibility()).isEqualTo(ProfileVisibility.PUBLIC);

        MyInfoResponse response = userService.changeProfileVisibility(USER_ID, new ProfileVisibilityRequest(ProfileVisibility.FRIENDS));

        assertThat(user.getProfileVisibility()).isEqualTo(ProfileVisibility.FRIENDS);
        assertThat(response.profileVisibility()).isEqualTo(ProfileVisibility.FRIENDS);
    }

    /* ---------- 프로필 사진 ---------- */

    private final MockMultipartFile photo = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1, 2, 3});

    @Test
    void 프로필_사진을_바꾸면_주소에_새_버전이_붙고_예전_파일을_지운다() {
        User user = localUser();
        user.changeProfileImage("old-uuid.png", "image/png");
        when(profileImageStorage.store(photo)).thenReturn(new ProfileImageStorage.StoredImage("new-uuid.jpg", "image/jpeg"));

        MyInfoResponse response = userService.changeProfileImage(USER_ID, photo);

        assertThat(user.getProfileImage()).isEqualTo("new-uuid.jpg");
        assertThat(user.getProfileImageType()).isEqualTo("image/jpeg");
        assertThat(response.profileImageUrl()).isEqualTo("/api/v1/users/1/profile-image?v=new-uuid");
        verify(profileImageStorage).delete("old-uuid.png");
    }

    @Test
    void 처음_올리는_사진이면_지울_파일이_없다() {
        localUser();
        when(profileImageStorage.store(photo)).thenReturn(new ProfileImageStorage.StoredImage("new-uuid.png", "image/png"));

        userService.changeProfileImage(USER_ID, photo);

        verify(profileImageStorage, never()).delete(anyString());
    }

    @Test
    void 사진을_저장하지_못하면_예전_사진을_그대로_둔다() {
        User user = localUser();
        user.changeProfileImage("old-uuid.png", "image/png");
        when(profileImageStorage.store(photo)).thenThrow(new BusinessException(ErrorCode.INVALID_PROFILE_IMAGE));

        assertThatThrownBy(() -> userService.changeProfileImage(USER_ID, photo))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PROFILE_IMAGE);
        assertThat(user.getProfileImage()).isEqualTo("old-uuid.png");
        verify(profileImageStorage, never()).delete(anyString());
    }

    @Test
    void 프로필_사진을_지우면_기본_프로필이_된다() {
        User user = localUser();
        user.changeProfileImage("old-uuid.png", "image/png");

        MyInfoResponse response = userService.deleteProfileImage(USER_ID);

        assertThat(user.hasProfileImage()).isFalse();
        assertThat(response.profileImageUrl()).isNull();
        verify(profileImageStorage).delete("old-uuid.png");
    }

    @Test
    void 사진이_없는_유저의_사진을_찾으면_예외가_발생한다() {
        localUser();

        assertThatThrownBy(() -> userService.getProfileImage(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_IMAGE_NOT_FOUND);
    }

    @Test
    void 사진_파일과_형식을_돌려준다() {
        User user = localUser();
        user.changeProfileImage("uuid.webp", "image/webp");
        Resource resource = mock(Resource.class);
        when(profileImageStorage.load("uuid.webp")).thenReturn(resource);

        UserService.ProfileImageFile file = userService.getProfileImage(USER_ID);

        assertThat(file.resource()).isSameAs(resource);
        assertThat(file.contentType()).isEqualTo("image/webp");
    }

    /* ---------- 비밀번호 ---------- */

    @Test
    void 소셜_가입자는_비밀번호를_바꿀_수_없다() {
        user(SocialProvider.KAKAO, null);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, new PasswordChangeRequest("old", "New1234!")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_ACCOUNT_PASSWORD);
    }

    @Test
    void 지금_비밀번호가_틀리면_바꾸지_않는다() {
        User user = localUser();
        when(passwordEncoder.matches("wrong", ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, new PasswordChangeRequest("wrong", "New1234!")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
        assertThat(user.getPassword()).isEqualTo(ENCODED);
    }

    @Test
    void 같은_비밀번호로는_바꾸지_않는다() {
        localUser();
        when(passwordEncoder.matches("Same1234!", ENCODED)).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(USER_ID, new PasswordChangeRequest("Same1234!", "Same1234!")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_PASSWORD);
    }

    @Test
    void 비밀번호를_암호화해서_바꾼다() {
        User user = localUser();
        when(passwordEncoder.matches("Old1234!", ENCODED)).thenReturn(true);
        when(passwordEncoder.matches("New1234!", ENCODED)).thenReturn(false);
        when(passwordEncoder.encode("New1234!")).thenReturn("encoded-new");

        userService.changePassword(USER_ID, new PasswordChangeRequest("Old1234!", "New1234!"));

        assertThat(user.getPassword()).isEqualTo("encoded-new");
    }

    /* ---------- 회원 탈퇴 ---------- */

    @Test
    void 이메일_가입자가_비밀번호를_틀리면_탈퇴하지_않는다() {
        User user = localUser();
        when(passwordEncoder.matches("wrong", ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> userService.withdraw(USER_ID, new WithdrawRequest("wrong")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
        assertThat(user.getIsDeleted()).isFalse();
        verify(refreshTokenRepository, never()).deleteById(anyString());
    }

    @Test
    void 이메일_가입자가_비밀번호_없이_보내면_탈퇴하지_않는다() {
        User user = localUser();

        assertThatThrownBy(() -> userService.withdraw(USER_ID, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
        assertThat(user.getIsDeleted()).isFalse();
    }

    @Test
    void 탈퇴하면_토큰과_사진을_지우고_밴드에서_빠지고_친구와_음악을_정리하고_유저와_캐릭터를_삭제하고_이메일을_비운다() {
        User user = localUser();
        user.changeProfileImage("uuid.png", "image/png");
        user.changeFriendCode("ABCD2345");
        user.changeProfileMusic("Hysteria", "Muse", "https://youtu.be/abc");
        Character character = character(user);
        when(passwordEncoder.matches("Pass1234!", ENCODED)).thenReturn(true);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        userService.withdraw(USER_ID, new WithdrawRequest("Pass1234!"));

        verify(refreshTokenRepository).deleteById(EMAIL);
        verify(profileImageStorage).delete("uuid.png");
        verify(bandService).leaveAllBands(USER_ID);
        verify(friendService).removeAll(USER_ID);
        assertThat(user.hasProfileImage()).isFalse();
        assertThat(user.getFriendCode()).isNull();
        assertThat(user.hasProfileMusic()).isFalse();
        assertThat(user.getIsDeleted()).isTrue();
        assertThat(user.getEmail()).isNotEqualTo(EMAIL).endsWith("@gainz.invalid");
        assertThat(user.getPassword()).isNull();
        assertThat(character.getIsDeleted()).isTrue();
    }

    @Test
    void 소셜_가입자는_비밀번호_없이_탈퇴한다() {
        User user = user(SocialProvider.GOOGLE, null);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        userService.withdraw(USER_ID, null);

        verify(refreshTokenRepository).deleteById(EMAIL);
        assertThat(user.getIsDeleted()).isTrue();
    }
}
