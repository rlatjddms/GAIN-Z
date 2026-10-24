package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.entity.EmailToken;
import com.project.gainz.domain.user.entity.SignupEmailCode;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.EmailTokenPurpose;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.EmailTokenRepository;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.SignupEmailCodeRepository;
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

// 가입 전 이메일 인증 코드·비밀번호 찾기 (#193)
@ExtendWith(MockitoExtension.class)
class AccountMailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private SignupEmailCodeRepository signupEmailCodeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailClient mailClient;

    private AccountMailService service;

    @BeforeEach
    void setUp() {
        service = new AccountMailService(userRepository, emailTokenRepository, signupEmailCodeRepository, refreshTokenRepository, passwordEncoder,
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

    private SignupEmailCode code(String email, String code, LocalDateTime expiresAt, LocalDateTime createdAt) {
        SignupEmailCode c = new SignupEmailCode(email, AccountMailService.codeHash(email, code), expiresAt);
        ReflectionTestUtils.setField(c, "createdAt", createdAt);
        return c;
    }

    @Test
    void 가입_인증_코드는_6자리를_메일로_DB에는_이메일과_묶은_해시만_10분짜리로() {
        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid")).thenReturn(Optional.empty());

        service.sendSignupCode(" new@test.invalid ");

        ArgumentCaptor<SignupEmailCode> saved = ArgumentCaptor.forClass(SignupEmailCode.class);
        verify(signupEmailCodeRepository).save(saved.capture());
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailClient).send(eq("new@test.invalid"), subject.capture(), html.capture());
        Matcher m = Pattern.compile("(\\d{6})").matcher(subject.getValue());
        assertThat(m.find()).isTrue();
        String sent = m.group(1);
        assertThat(html.getValue()).contains(sent);
        assertThat(saved.getValue().getEmail()).isEqualTo("new@test.invalid");
        assertThat(saved.getValue().getCodeHash()).isEqualTo(AccountMailService.codeHash("new@test.invalid", sent)).doesNotContain(sent);
        assertThat(saved.getValue().getExpiresAt()).isBetween(LocalDateTime.now().plusMinutes(9), LocalDateTime.now().plusMinutes(11));
    }

    @Test
    void 이미_가입된_이메일이면_코드를_보내지_않고_중복이라고_알린다() {
        when(userRepository.existsByEmail("me@test.invalid")).thenReturn(true);

        assertThatThrownBy(() -> service.sendSignupCode("me@test.invalid"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        verify(mailClient, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 가입_인증_코드는_1분에_한_번_하루_10번까지() {
        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid"))
                .thenReturn(Optional.of(code("new@test.invalid", "123456", LocalDateTime.now().plusMinutes(9), LocalDateTime.now().minusSeconds(10))));
        assertThatThrownBy(() -> service.sendSignupCode("new@test.invalid"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MAIL_SEND_LIMIT);

        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid")).thenReturn(Optional.empty());
        when(signupEmailCodeRepository.countByEmailAndCreatedAtAfter(eq("new@test.invalid"), any())).thenReturn(10L);
        assertThatThrownBy(() -> service.sendSignupCode("new@test.invalid"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MAIL_SEND_LIMIT);

        verify(signupEmailCodeRepository, never()).save(any());
        verify(mailClient, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 코드가_맞으면_가입용_인증_표를_주고_코드는_다시_못_쓴다() {
        SignupEmailCode c = code("new@test.invalid", "123456", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid")).thenReturn(Optional.of(c));

        String ticket = service.verifySignupCode("new@test.invalid", " 123456 ").emailVerificationToken();

        assertThat(ticket).isNotBlank();
        assertThat(c.getTicketHash()).isEqualTo(AccountMailService.hash(ticket));
        assertThatThrownBy(() -> service.verifySignupCode("new@test.invalid", "123456"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED);
    }

    @Test
    void 코드를_5번_틀리면_새_코드를_받아야_한다() {
        SignupEmailCode c = code("new@test.invalid", "123456", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid")).thenReturn(Optional.of(c));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.verifySignupCode("new@test.invalid", "000000"))
                    .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMAIL_CODE);
        }
        assertThatThrownBy(() -> service.verifySignupCode("new@test.invalid", "000000"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_TOO_MANY_ATTEMPTS);
        // 이제는 맞는 코드도 받지 않는다
        assertThatThrownBy(() -> service.verifySignupCode("new@test.invalid", "123456"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED);
        assertThat(c.getTicketHash()).isNull();
    }

    @Test
    void 만료된_코드는_받지_않는다() {
        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid"))
                .thenReturn(Optional.of(code("new@test.invalid", "123456", LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusMinutes(11))));

        assertThatThrownBy(() -> service.verifySignupCode("new@test.invalid", "123456"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED);
    }

    @Test
    void 인증_표는_그_이메일로_30분_안에_한_번만_쓴다() {
        SignupEmailCode c = code("new@test.invalid", "123456", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
        c.verify(AccountMailService.hash("ticket"), LocalDateTime.now());
        when(signupEmailCodeRepository.findByTicketHash(AccountMailService.hash("ticket"))).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.useSignupTicket("other@test.invalid", "ticket"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
        service.useSignupTicket("new@test.invalid", "ticket");
        assertThat(c.getTicketUsedAt()).isNotNull();
        assertThatThrownBy(() -> service.useSignupTicket("new@test.invalid", "ticket"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        SignupEmailCode old = code("old@test.invalid", "123456", LocalDateTime.now(), LocalDateTime.now().minusHours(1));
        old.verify(AccountMailService.hash("old-ticket"), LocalDateTime.now().minusMinutes(31));
        when(signupEmailCodeRepository.findByTicketHash(AccountMailService.hash("old-ticket"))).thenReturn(Optional.of(old));
        assertThatThrownBy(() -> service.useSignupTicket("old@test.invalid", "old-ticket"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void 없거나_만료된_재설정_링크는_받지_않는다() {
        User user = user(true, "x");
        when(emailTokenRepository.findByTokenHashAndPurpose(AccountMailService.hash("old"), EmailTokenPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(token(user, EmailTokenPurpose.RESET_PASSWORD, LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusHours(1))));
        when(emailTokenRepository.findByTokenHashAndPurpose(AccountMailService.hash("nope"), EmailTokenPurpose.RESET_PASSWORD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("old", "Test1234!"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMAIL_TOKEN);
        assertThatThrownBy(() -> service.resetPassword("nope", "Test1234!"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_EMAIL_TOKEN);
        verify(passwordEncoder, never()).encode(anyString());
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

        when(signupEmailCodeRepository.findTopByEmailOrderByCreatedAtDesc("new@test.invalid")).thenReturn(Optional.empty());

        service.sendSignupCode("new@test.invalid");

        verify(mailClient).send(anyString(), anyString(), anyString());
    }

    @Test
    void 이메일_인증_칸이_없던_예전_계정은_인증된_것으로_본다() {
        assertThat(user(null, "x").isEmailVerified()).isTrue();
        assertThat(user(false, "x").isEmailVerified()).isFalse();
    }
}
