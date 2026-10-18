package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandPostFile;
import com.project.gainz.domain.band.entity.Jam;
import com.project.gainz.domain.band.entity.JamTrack;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.global.common.enums.InstrumentType;
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

// 외부 DB 없이 잼·트랙 매핑과 목록·삭제를 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = JamRepositoryTest.Config.class)
class JamRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.band.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired JamRepository jams;

    @Test void jamsAreListedNewestFirstWithTracksAndDeletedTogether() {
        User owner = user("owner");
        Band band = em.persist(Band.builder().name("밴드").leader(owner).inviteCode("ABCDEFGH").build());
        Band other = em.persist(Band.builder().name("다른 밴드").leader(owner).inviteCode("IJKLMNOP").build());
        Jam old = em.persist(new Jam(band, owner, "먼저", 100));
        Jam recent = new Jam(band, owner, "나중", 120);
        recent.linkSong(31L, "Hysteria");
        recent.getTracks().add(track(recent, owner, "a"));
        recent.getTracks().add(track(recent, owner, "b"));
        em.persist(recent);
        em.persist(new Jam(other, owner, "남의 잼", 90));
        em.flush();
        em.clear();

        var result = jams.findAllByBandIdOrderByIdDesc(band.getId());
        assertThat(result).extracting(Jam::getTitle).containsExactly("나중", "먼저");
        Jam loaded = result.get(0);
        assertThat(loaded.getSongTitle()).isEqualTo("Hysteria");
        assertThat(loaded.getTracks()).extracting(t -> t.getFile().getKey()).containsExactly("a", "b");
        assertThat(loaded.getTracks().get(0).getOffsetMs()).isEqualTo(-2170);
        assertThat(loaded.getTracks().get(0).isMuted()).isFalse();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(jams.countByBandId(band.getId())).isEqualTo(2);
        assertThat(jams.findByIdAndBandId(old.getId(), other.getId())).isEmpty();

        // 트랙 하나를 빼면 그 트랙만, 잼을 지우면 남은 트랙도 함께 지운다
        loaded.getTracks().remove(0);
        em.flush();
        em.clear();
        assertThat(jams.findByIdAndBandId(recent.getId(), band.getId()).orElseThrow().getTracks()).hasSize(1);
        jams.delete(jams.findById(recent.getId()).orElseThrow());
        em.flush();
        em.clear();
        assertThat(jams.findAllByBandIdOrderByIdDesc(band.getId())).extracting(Jam::getTitle).containsExactly("먼저");
        assertThat(em.getEntityManager().createNativeQuery("select count(*) from jam_track where is_deleted = false").getSingleResult())
                .isEqualTo(0L);
    }

    private JamTrack track(Jam jam, User uploader, String key) {
        return JamTrack.builder().jam(jam).uploader(uploader).part(InstrumentType.GUITAR)
                .file(new BandPostFile(key, key + ".webm", "audio/webm")).volume(100).offsetMs(-2170).durationMs(3000).build();
    }

    private User user(String name) {
        return em.persist(User.builder().email(name + "@test.invalid").name(name).provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }
}
