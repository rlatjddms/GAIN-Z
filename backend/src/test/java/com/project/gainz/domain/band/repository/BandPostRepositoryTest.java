package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandPost;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

// 외부 DB·비밀 설정 없이 실제 JPQL의 만료 경계, 필터, 정렬과 count 쿼리를 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BandPostRepositoryTest.Config.class)
class BandPostRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.band.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired BandPostRepository posts;

    @Test void expiryIsAppliedBeforeFilteringOrderingAndPagination() {
        var now = Instant.parse("2030-01-01T00:00:00Z");
        var me = em.persist(User.builder().email("me@test.invalid").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
        var other = em.persist(User.builder().email("other@test.invalid").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
        var band = em.persist(Band.builder().name("밴드").leader(me).inviteCode("ABCDEFGH").build());
        var otherBand = em.persist(Band.builder().name("다른 밴드").leader(me).inviteCode("IJKLMNOP").build());
        var indefinite = save(band, me, "무기한", true, null);
        var future = save(band, me, "미래", true, now.plusSeconds(1));
        var expired = save(band, me, "만료", true, now.minusSeconds(1));
        var boundary = save(band, me, "경계", true, now);
        var ordinary = save(band, me, "일반", false, null);
        var otherNotice = save(band, other, "다른 작성자", true, null);
        save(otherBand, me, "다른 밴드", true, null);
        var deleted = save(band, me, "삭제", true, null); deleted.delete();
        em.flush(); em.clear();

        var all = posts.findVisible(band.getId(), me.getId(), false, null, now, PageRequest.of(0, 2));
        assertThat(all.getTotalElements()).isEqualTo(6);
        assertThat(all.getTotalPages()).isEqualTo(3);
        assertThat(all.getContent()).extracting(BandPost::getId).containsExactly(otherNotice.getId(), future.getId());
        var mine = posts.findVisible(band.getId(), me.getId(), true, true, now, PageRequest.of(0, 20));
        assertThat(mine.getContent()).extracting(BandPost::getId).containsExactly(future.getId(), indefinite.getId());
        var normals = posts.findVisible(band.getId(), me.getId(), false, false, now, PageRequest.of(0, 20));
        assertThat(normals.getContent()).extracting(BandPost::getId).containsExactly(ordinary.getId(), boundary.getId(), expired.getId());
        assertThat(posts.findVisible(band.getId(), me.getId(), true, true, now.plusSeconds(1), PageRequest.of(0, 20)).getContent())
                .extracting(BandPost::getId).containsExactly(indefinite.getId());
        assertThat(posts.findVisible(band.getId(), me.getId(), false, true, now, PageRequest.of(0, 2)).getTotalElements()).isEqualTo(3);
    }

    private BandPost save(Band band, User author, String title, boolean notice, Instant expiry) {
        var post = new BandPost(band, author, title, "본문");
        post.changeNotice(notice, expiry);
        return em.persist(post);
    }
}
