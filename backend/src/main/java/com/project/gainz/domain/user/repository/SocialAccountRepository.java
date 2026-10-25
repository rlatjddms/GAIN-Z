package com.project.gainz.domain.user.repository;

import com.project.gainz.domain.user.entity.SocialAccount;
import com.project.gainz.domain.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    // 사용자도 함께 읽는다. 소셜 로그인은 트랜잭션이 끝난 뒤 보안 필터가 사용자 권한을 읽어서,
    // 나중에 읽는 프록시면 LazyInitializationException이 난다
    @Query("select s from SocialAccount s join fetch s.user where s.provider = :provider and s.providerId = :providerId")
    Optional<SocialAccount> findByProviderAndProviderId(@Param("provider") SocialProvider provider, @Param("providerId") String providerId);
}
