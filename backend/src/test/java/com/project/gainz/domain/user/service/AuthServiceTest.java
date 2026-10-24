package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.dto.request.SignupRequest;
import com.project.gainz.domain.user.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 가입할 때 이메일 인증 (#193)
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AccountMailService accountMailService;
    @InjectMocks private AuthService authService;

    @Test
    void 이메일_인증_표를_쓰고_인증된_계정으로_가입한다() {
        when(passwordEncoder.encode("Test1234!")).thenReturn("encoded");

        authService.signup(new SignupRequest("me@test.invalid", "Test1234!", "나", "ticket"));

        verify(accountMailService).useSignupTicket("me@test.invalid", "ticket");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().isEmailVerified()).isTrue();
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void 인증_표가_없거나_틀리면_가입하지_않는다() {
        doThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED))
                .when(accountMailService).useSignupTicket("me@test.invalid", "bad");

        assertThatThrownBy(() -> authService.signup(new SignupRequest("me@test.invalid", "Test1234!", "나", "bad")))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 이미_가입된_이메일이면_인증_표를_쓰지_않는다() {
        when(userRepository.existsByEmail("me@test.invalid")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("me@test.invalid", "Test1234!", "나", "ticket")))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(accountMailService, never()).useSignupTicket(anyString(), anyString());
    }
}
