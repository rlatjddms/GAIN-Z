package com.project.gainz.domain.user.service;

import com.project.gainz.domain.user.dto.response.PasswordResetRequestResponse;
import com.project.gainz.domain.user.entity.EmailToken;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.EmailTokenPurpose;
import com.project.gainz.domain.user.repository.EmailTokenRepository;
import com.project.gainz.domain.user.repository.RefreshTokenRepository;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.global.mail.MailClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

// 이메일 인증·비밀번호 찾기 메일 (#193). 너무 자주 요청하면(1분 안·하루 10번 넘게) 보내지 않고 조용히 넘긴다
@Slf4j
@Service
@Transactional
public class AccountMailService {

    static final Duration VERIFY_TTL = Duration.ofHours(24);
    static final Duration RESET_TTL = Duration.ofMinutes(30);
    // 같은 메일은 1분에 한 번, 하루 10번까지
    static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
    static final int DAILY_LIMIT = 10;

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailClient mailClient;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public AccountMailService(UserRepository userRepository, EmailTokenRepository emailTokenRepository,
                              RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                              MailClient mailClient, @Value("${gainz.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailClient = mailClient;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /* ---------- 이메일 인증 ---------- */

    // 가입 직후 (AuthService.signup)
    public void sendVerification(User user) {
        String token = issue(user, EmailTokenPurpose.VERIFY_EMAIL, VERIFY_TTL);
        String link = frontendUrl + "/verify-email?token=" + token;
        sendAfterCommit(user.getEmail(), "[GAIN-Z] 이메일 인증을 마쳐 주세요", page(user.getName(),
                "GAIN-Z에 가입해 주셔서 고마워요. 아래 버튼을 누르면 이메일 인증이 끝나요.",
                "이메일 인증하기", link, "이 링크는 24시간 동안 쓸 수 있어요. 직접 가입하지 않았다면 이 메일은 무시해 주세요."));
    }

    public void resendVerification(String email) {
        userRepository.findByEmail(email.trim())
                .filter(user -> !user.isEmailVerified())
                .filter(user -> canSend(user, EmailTokenPurpose.VERIFY_EMAIL))
                .ifPresent(this::sendVerification);
    }

    public void confirmEmail(String rawToken) {
        EmailToken token = usableToken(rawToken, EmailTokenPurpose.VERIFY_EMAIL);
        token.getUser().verifyEmail();
        token.use(LocalDateTime.now());
    }

    /* ---------- 비밀번호 찾기 ---------- */

    // 가입되지 않은 이메일이면 NOT_FOUND, 비밀번호 없이 소셜로만 가입했으면 SOCIAL_ONLY(그 계정으로 로그인)
    public PasswordResetRequestResponse requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return PasswordResetRequestResponse.notFound();
        }
        if (user.getPassword() == null) {
            return PasswordResetRequestResponse.socialOnly(user.getProvider());
        }
        if (canSend(user, EmailTokenPurpose.RESET_PASSWORD)) {
            String token = issue(user, EmailTokenPurpose.RESET_PASSWORD, RESET_TTL);
            String link = frontendUrl + "/reset-password?token=" + token;
            sendAfterCommit(user.getEmail(), "[GAIN-Z] 비밀번호를 다시 정해 주세요", page(user.getName(),
                    "비밀번호 재설정을 요청하셨어요. 아래 버튼을 눌러 새 비밀번호를 정해 주세요.",
                    "새 비밀번호 정하기", link, "이 링크는 30분 동안 한 번만 쓸 수 있어요. 직접 요청하지 않았다면 이 메일은 무시해 주세요. 비밀번호는 바뀌지 않아요."));
        }
        return PasswordResetRequestResponse.sent();
    }

    // 새 비밀번호로 바꾸고, 다른 재설정 링크는 못 쓰게 하고, 모든 기기에서 로그아웃시킨다.
    // 메일의 링크를 열었으니 이메일 인증도 된 것으로 본다
    public void resetPassword(String rawToken, String newPassword) {
        EmailToken token = usableToken(rawToken, EmailTokenPurpose.RESET_PASSWORD);
        User user = token.getUser();
        LocalDateTime now = LocalDateTime.now();
        user.changePassword(passwordEncoder.encode(newPassword));
        user.verifyEmail();
        emailTokenRepository.findAllByUserIdAndPurposeAndUsedAtIsNull(user.getId(), EmailTokenPurpose.RESET_PASSWORD)
                .forEach(t -> t.use(now));
        token.use(now);
        refreshTokenRepository.deleteById(user.getEmail());
    }

    /* ---------- 도움 ---------- */

    private boolean canSend(User user, EmailTokenPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        boolean tooSoon = emailTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .map(last -> last.getCreatedAt() != null && last.getCreatedAt().isAfter(now.minus(RESEND_INTERVAL)))
                .orElse(false);
        if (tooSoon) return false;
        return emailTokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, now.minusDays(1)) < DAILY_LIMIT;
    }

    // 원래 토큰(메일에 들어감)을 돌려주고 DB에는 해시만
    private String issue(User user, EmailTokenPurpose purpose, Duration ttl) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        emailTokenRepository.save(new EmailToken(user, purpose, hash(raw), LocalDateTime.now().plus(ttl)));
        return raw;
    }

    private EmailToken usableToken(String rawToken, EmailTokenPurpose purpose) {
        return emailTokenRepository.findByTokenHashAndPurpose(hash(rawToken.trim()), purpose)
                .filter(t -> t.isUsable(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_EMAIL_TOKEN));
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // DB에 토큰이 저장된 게 확정된 뒤에 보낸다. 메일 서비스가 실패해도 요청은 성공으로 두고 로그만 남긴다 (다시 보내기로 해결)
    private void sendAfterCommit(String to, String subject, String html) {
        Runnable send = () -> {
            try {
                mailClient.send(to, subject, html);
            } catch (RuntimeException e) {
                log.error("메일 전송 실패: {} ({})", subject, e.getMessage());
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }

    // 메일 본문 (표가 아닌 단순한 HTML. 메일 앱마다 CSS 지원이 달라 인라인 스타일만 쓴다)
    private static String page(String name, String message, String button, String link, String note) {
        String who = name == null || name.isBlank() ? "" : HtmlUtils.htmlEscape(name) + "님, ";
        return """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;color:#1a1a1a">
                  <p style="margin:0 0 24px;font-size:22px;font-weight:900;color:#12b886">GAIN-Z</p>
                  <p style="margin:0 0 12px;font-size:15px;line-height:1.7">%s%s</p>
                  <p style="margin:28px 0"><a href="%s" style="display:inline-block;padding:14px 22px;border-radius:12px;background:#12b886;color:#ffffff;font-weight:700;text-decoration:none">%s</a></p>
                  <p style="margin:0 0 8px;font-size:13px;line-height:1.6;color:#868e96">%s</p>
                  <p style="margin:0;font-size:12px;line-height:1.6;color:#adb5bd;word-break:break-all">버튼이 안 눌리면 이 주소를 브라우저에 붙여 넣어 주세요.<br>%s</p>
                </div>
                """.formatted(who, message, link, button, note, link);
    }
}
