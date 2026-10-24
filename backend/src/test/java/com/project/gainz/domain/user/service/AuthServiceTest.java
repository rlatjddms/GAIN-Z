package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.dto.request.LoginRequest;
import com.project.gainz.domain.user.dto.request.SignupRequest;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 가입·로그인과 이메일 인증 (#193)
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AccountMailService accountMailService;
    @InjectMocks private AuthService authService;

    @Test
    void 가입하면_인증_전_상태로_만들고_인증_메일을_보낸다() {
        when(passwordEncoder.encode("Test1234!")).thenReturn("encoded");

        authService.signup(new SignupRequest("me@test.invalid", "Test1234!", "나"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().isEmailVerified()).isFalse();
        verify(accountMailService).sendVerification(saved.getValue());
    }

    @Test
    void 인증_전이면_비밀번호가_맞을_때만_인증이_필요하다고_알린다() {
        User user = User.builder().email("me@test.invalid").password("encoded").name("나")
                .provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).emailVerified(false).build();
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        when(passwordEncoder.matches("Test1234!", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("me@test.invalid", "wrong")))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_PASSWORD);
        assertThatThrownBy(() -> authService.login(new LoginRequest("me@test.invalid", "Test1234!")))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
        verify(refreshTokenRepository, never()).save(any());
    }
}
