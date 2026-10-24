package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.entity.EmailToken;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.EmailTokenPurpose;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.EmailTokenRepository;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.mail.MailClient;
import com.project.gainz.global.mail.MailSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 이메일 인증·비밀번호 찾기 (#193)
@ExtendWith(MockitoExtension.class)
class AccountMailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailClient mailClient;

    private AccountMailService service;

    @BeforeEach
    void setUp() {
        service = new AccountMailService(userRepository, emailTokenRepository, refreshTokenRepository, passwordEncoder,
                mailClient, "https://gainz.example/");
    }

    private User user(Boolean verified, String password) {
        User user = User.builder().email("me@test.invalid").name("나").password(password)
                .provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).emailVerified(verified).build();
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    private EmailToken token(User user, EmailTokenPurpose purpose, LocalDateTime expiresAt, LocalDateTime createdAt) {
        EmailToken token = new EmailToken(user, purpose, "hash", expiresAt);
        ReflectionTestUtils.setField(token, "createdAt", createdAt);
        return token;
    }

    private String sentLink() {
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailClient).send(eq("me@test.invalid"), anyString(), html.capture());
        Matcher m = Pattern.compile("href=\"([^\"]+)\"").matcher(html.getValue());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void 인증_메일에는_원래_토큰을_DB에는_해시만_24시간짜리로_둔다() {
        User user = user(false, "x");

        service.sendVerification(user);

        ArgumentCaptor<EmailToken> saved = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(saved.capture());
        String link = sentLink();
        assertThat(link).startsWith("https://gainz.example/verify-email?token=");
        String raw = link.substring(link.indexOf("token=") + 6);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(AccountMailService.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getPurpose()).isEqualTo(EmailTokenPurpose.VERIFY_EMAIL);
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
    }

    @Test
    void 인증_메일_다시_보내기는_없는_이메일_인증된_계정_1분_안이면_조용히_넘긴다() {
        when(userRepository.findByEmail("none@test.invalid")).thenReturn(Optional.empty());
        service.resendVerification("none@test.invalid");

        User verified = user(true, "x");
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(verified));
        service.resendVerification("me@test.invalid");

        User fresh = user(false, "x");
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(fresh));
        when(emailTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(7L, EmailTokenPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(token(fresh, EmailTokenPurpose.VERIFY_EMAIL, LocalDateTime.now().plusHours(1), LocalDateTime.now().minusSeconds(10))));
        service.resendVerification("me@test.invalid");

        verify(mailClient, never()).send(anyString(), anyString(), anyString());
        verify(emailTokenRepository, never()).save(any());
    }

    @Test
    void 인증_메일은_하루_10번까지() {
        User fresh = user(false, "x");
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(fresh));
        when(emailTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(7L, EmailTokenPurpose.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(emailTokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(eq(7L), eq(EmailTokenPurpose.VERIFY_EMAIL), any())).thenReturn(10L);

        service.resendVerification(" me@test.invalid ");

        verify(mailClient, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 인증_링크를_열면_인증되고_토큰은_한_번만_쓴다() {
        User user = user(false, "x");
        EmailToken token = token(user, EmailTokenPurpose.VERIFY_EMAIL, LocalDateTime.now().plusHours(1), LocalDateTime.now());
        when(emailTokenRepository.findByTokenHashAndPurpose(AccountMailService.hash("raw"), EmailTokenPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(token));

        service.confirmEmail("raw");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        assertThatThrownBy(() -> service.confirmEmail("raw"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMAIL_TOKEN);
    }

    @Test
    void 만료되거나_없는_토큰은_받지_않는다() {
        User user = user(false, "x");
        when(emailTokenRepository.findByTokenHashAndPurpose(AccountMailService.hash("old"), EmailTokenPurpose.VERIFY_EMAIL))
                .thenReturn(Optional.of(token(user, EmailTokenPurpose.VERIFY_EMAIL, LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusDays(1))));
        when(emailTokenRepository.findByTokenHashAndPurpose(AccountMailService.hash("nope"), EmailTokenPurpose.RESET_PASSWORD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmEmail("old"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMAIL_TOKEN);
        assertThatThrownBy(() -> service.resetPassword("nope", "Test1234!"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMAIL_TOKEN);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void 비밀번호_재설정은_가입_안_된_이메일과_소셜로만_가입한_계정을_알려_주고_메일은_보내지_않는다() {
        when(userRepository.findByEmail("none@test.invalid")).thenReturn(Optional.empty());
        assertThat(service.requestPasswordReset("none@test.invalid").status()).isEqualTo("NOT_FOUND");

        User social = User.builder().email("social@test.invalid").name("소셜").provider(SocialProvider.KAKAO)
                .role(UserRole.ROLE_USER).emailVerified(true).build();
        when(userRepository.findByEmail("social@test.invalid")).thenReturn(Optional.of(social));
        var result = service.requestPasswordReset("social@test.invalid");
        assertThat(result.status()).isEqualTo("SOCIAL_ONLY");
        assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);

        verify(mailClient, never()).send(anyString(), anyString(), anyString());
        verify(emailTokenRepository, never()).save(any());
    }

    @Test
    void 비밀번호_재설정_메일은_비밀번호가_있는_계정에_30분짜리로() {
        User local = user(true, "encoded");
        when(userRepository.findByEmail("me@test.invalid")).thenReturn(Optional.of(local));
        when(emailTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(7L, EmailTokenPurpose.RESET_PASSWORD)).thenReturn(Optional.empty());
        assertThat(service.requestPasswordReset("me@test.invalid").status()).isEqualTo("SENT");

        ArgumentCaptor<EmailToken> saved = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getPurpose()).isEqualTo(EmailTokenPurpose.RESET_PASSWORD);
        assertThat(saved.getValue().getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(31));
        assertThat(sentLink()).startsWith("https://gainz.example/reset-password?token=");
    }

    @Test
    void 비밀번호를_바꾸면_다른_재설정_링크를_막고_모든_기기에서_로그아웃한다() {
        User user = user(false, "old-encoded");
        EmailToken token = token(user, EmailTokenPurpose.RESET_PASSWORD, LocalDateTime.now().plusMinutes(10), LocalDateTime.now());
        EmailToken other = token(user, EmailTokenPurpose.RESET_PASSWORD, LocalDateTime.now().plusMinutes(20), LocalDateTime.now());
        when(emailTokenRepository.findByTokenHashAndPurpose(AccountMailService.hash("raw"), EmailTokenPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(token));
        when(emailTokenRepository.findAllByUserIdAndPurposeAndUsedAtIsNull(7L, EmailTokenPurpose.RESET_PASSWORD)).thenReturn(List.of(other));
        when(passwordEncoder.encode("New1234!")).thenReturn("new-encoded");

        service.resetPassword("raw", "New1234!");

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(other.getUsedAt()).isNotNull();
        verify(refreshTokenRepository).deleteById("me@test.invalid");
    }

    @Test
    void 메일_서비스가_실패해도_요청은_실패하지_않는다() {
        doThrow(new MailSendException("down", null)).when(mailClient).send(anyString(), anyString(), anyString());

        service.sendVerification(user(false, "x"));

        verify(mailClient).send(anyString(), anyString(), anyString());
    }

    @Test
    void 이메일_인증_칸이_없던_예전_계정은_인증된_것으로_본다() {
        assertThat(user(null, "x").isEmailVerified()).isTrue();
        assertThat(user(false, "x").isEmailVerified()).isFalse();
    }
}
