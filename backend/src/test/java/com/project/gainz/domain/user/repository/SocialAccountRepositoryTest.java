package com.project.gainz.domain.user.repository;

import com.project.gainz.domain.user.entity.SocialAccount;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

// 외부 DB 없이 소셜 연결 쿼리를 검증한다 (#189)
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = SocialAccountRepositoryTest.Config.class)
class SocialAccountRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories(basePackages = "com.project.gainz.domain.user.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired SocialAccountRepository socialAccounts;

    private User user(String name, SocialProvider provider, String password) {
        return em.persist(User.builder().email(name + "@test.invalid").password(password)
                .provider(provider).role(UserRole.ROLE_USER).build());
    }

    // 소셜 로그인은 트랜잭션이 끝난 뒤 보안 필터가 사용자 권한을 읽는다 → 연결과 함께 사용자도 읽혀 있어야 한다
    @Test void 소셜_고유_ID로_찾으면_사용자도_함께_읽는다() {
        User me = user("me", SocialProvider.LOCAL, "encoded");
        me.addSocialAccount(SocialProvider.NAVER, "n-1");
        em.flush();
        em.clear();

        SocialAccount found = socialAccounts.findByProviderAndProviderId(SocialProvider.NAVER, "n-1").orElseThrow();
        em.clear();

        assertThat(Hibernate.isInitialized(found.getUser())).isTrue();
        assertThat(found.getUser().getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(socialAccounts.findByProviderAndProviderId(SocialProvider.NAVER, "other")).isEmpty();
    }
}
