package com.project.gainz.domain.user.entity;

import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

// 가입 전 이메일 인증 코드 (#193). 계정이 아직 없어서 이메일로 묶는다.
// 6자리 코드는 메일에만 있고 DB에는 해시만. 코드가 맞으면 가입할 때 낼 인증 표(ticket)를 주고, 가입하면 그 표는 끝
@Entity
@Getter
@Table(indexes = @Index(name = "idx_signup_email_code_email", columnList = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignupEmailCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 틀린 횟수
    @Column(nullable = false)
    private int attempts;

    private LocalDateTime verifiedAt;

    @Column(unique = true, length = 64)
    private String ticketHash;

    private LocalDateTime ticketUsedAt;

    public SignupEmailCode(String email, String codeHash, LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isCodeUsable(LocalDateTime now, int maxAttempts) {
        return verifiedAt == null && attempts < maxAttempts && now.isBefore(expiresAt);
    }

    public void fail() {
        this.attempts++;
    }

    public void verify(String ticketHash, LocalDateTime now) {
        this.ticketHash = ticketHash;
        this.verifiedAt = now;
    }

    public boolean isTicketUsable(LocalDateTime now, Duration ttl) {
        return verifiedAt != null && ticketUsedAt == null && now.isBefore(verifiedAt.plus(ttl));
    }

    public void useTicket(LocalDateTime now) {
        this.ticketUsedAt = now;
    }
}
