package com.project.gainz.domain.user.repository;

import com.project.gainz.domain.user.entity.EmailToken;
import com.project.gainz.domain.user.enums.EmailTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional<EmailToken> findByTokenHashAndPurpose(String tokenHash, EmailTokenPurpose purpose);

    Optional<EmailToken> findTopByUserIdAndPurposeOrderByCreatedAtDesc(Long userId, EmailTokenPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, EmailTokenPurpose purpose, LocalDateTime after);

    List<EmailToken> findAllByUserIdAndPurposeAndUsedAtIsNull(Long userId, EmailTokenPurpose purpose);
}
