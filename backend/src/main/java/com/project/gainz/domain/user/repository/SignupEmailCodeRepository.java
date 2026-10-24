package com.project.gainz.domain.user.repository;

import com.project.gainz.domain.user.entity.SignupEmailCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SignupEmailCodeRepository extends JpaRepository<SignupEmailCode, Long> {
    Optional<SignupEmailCode> findTopByEmailOrderByCreatedAtDesc(String email);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    Optional<SignupEmailCode> findByTicketHash(String ticketHash);
}
