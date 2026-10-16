package com.project.gainz.domain.user.service;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.user.dto.request.PasswordChangeRequest;
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
    void 내_정보에_캐릭터_요약이_담긴다() {
        User user = localUser();
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character(user)));

        MyInfoResponse response = userService.getMyInfo(USER_ID);

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.provider()).isEqualTo(SocialProvider.LOCAL);
        assertThat(response.character().nickname()).isEqualTo("방구석 락스타");
        assertThat(response.character().level()).isEqualTo(1);
    }

    @Test
    void 캐릭터가_없으면_캐릭터_요약은_null이다() {
        localUser();
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(userService.getMyInfo(USER_ID).character()).isNull();
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
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        MyInfoResponse response = userService.updateName(USER_ID, new UpdateNameRequest("  새이름 "));

        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(response.name()).isEqualTo("새이름");
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
    void 탈퇴하면_토큰을_지우고_유저와_캐릭터를_삭제하고_이메일을_비운다() {
        User user = localUser();
        Character character = character(user);
        when(passwordEncoder.matches("Pass1234!", ENCODED)).thenReturn(true);
        when(characterRepository.findByUserId(USER_ID)).thenReturn(Optional.of(character));

        userService.withdraw(USER_ID, new WithdrawRequest("Pass1234!"));

        verify(refreshTokenRepository).deleteById(EMAIL);
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
