package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.entity.Setlist;
import com.project.gainz.domain.band.entity.SetlistItem;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.sheet.entity.SheetMusic;
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

// 외부 DB 없이 셋리스트 악보 권한·순서 JPQL을 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = SetlistRepositoryTest.Config.class)
class SetlistRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.band.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired SetlistRepository setlists;
    @Autowired SetlistItemRepository items;

    @Test void linkedSheetIsVisibleOnlyToCurrentMembersOfThatBand() {
        User owner = user("owner"), member = user("member"), outsider = user("outsider"), left = user("left");
        Band band = em.persist(Band.builder().name("밴드").leader(owner).inviteCode("ABCDEFGH").build());
        Band otherBand = em.persist(Band.builder().name("다른 밴드").leader(outsider).inviteCode("IJKLMNOP").build());
        member(band, owner);
        member(band, member);
        member(otherBand, outsider);
        member(band, left).delete();
        SheetMusic linked = em.persist(SheetMusic.builder().owner(owner).title("곡").artist("아티스트").instrumentType(InstrumentType.GUITAR).build());
        SheetMusic unlinked = em.persist(SheetMusic.builder().owner(owner).title("다른 곡").artist("아티스트").instrumentType(InstrumentType.GUITAR).build());
        Setlist setlist = em.persist(new Setlist(band, "정기 합주", 0));
        SetlistItem item = new SetlistItem(setlist, 0);
        item.update("곡", null, null, linked);
        setlist.getItems().add(item);
        em.flush();
        em.clear();

        assertThat(items.existsLinkedForMember(linked.getId(), member.getId())).isTrue();
        assertThat(items.existsLinkedForMember(linked.getId(), outsider.getId())).isFalse();
        assertThat(items.existsLinkedForMember(linked.getId(), left.getId())).isFalse();
        assertThat(items.existsLinkedForMember(unlinked.getId(), member.getId())).isFalse();
        assertThat(items.existsBySheetMusicId(linked.getId())).isTrue();

        // 셋리스트를 지우면 곡도 지워져 권한이 사라진다
        setlists.delete(setlists.findById(setlist.getId()).orElseThrow());
        em.flush();
        em.clear();
        assertThat(items.existsLinkedForMember(linked.getId(), member.getId())).isFalse();
        assertThat(setlists.findAllByBandIdOrderByListOrderAscIdAsc(band.getId())).isEmpty();
    }

    @Test void setlistsAreOrderedByListOrderThenId() {
        User owner = user("owner");
        Band band = em.persist(Band.builder().name("밴드").leader(owner).inviteCode("ABCDEFGH").build());
        Setlist second = em.persist(new Setlist(band, "둘째", 1));
        Setlist first = em.persist(new Setlist(band, "첫째", 0));
        em.flush();
        em.clear();
        assertThat(setlists.findAllByBandIdOrderByListOrderAscIdAsc(band.getId())).extracting(Setlist::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(setlists.countByBandId(band.getId())).isEqualTo(2);
    }

    private User user(String name) {
        return em.persist(User.builder().email(name + "@test.invalid").name(name).provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }

    private BandMember member(Band band, User user) {
        return em.persist(BandMember.builder().band(band).user(user).position(Position.MEMBER).part(InstrumentType.GUITAR).build());
    }
}
