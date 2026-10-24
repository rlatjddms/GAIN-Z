package com.project.gainz.domain.band.repository;

import com.project.gainz.domain.band.entity.Band;
import com.project.gainz.domain.band.entity.BandComment;
import com.project.gainz.domain.band.entity.BandPost;
import com.project.gainz.domain.band.service.BandBoardService;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.entity.OutfitItem;
import com.project.gainz.domain.character.enums.OutfitCategory;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.global.common.enums.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 게시판 검색(검색어·작성일)과 댓글 쪽 나누기 JPQL을 실제 DB(H2)로 확인한다 (#188)
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BandBoardSearchRepositoryTest.Config.class)
class BandBoardSearchRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.band.repository")
    static class Config {}

    private static final LocalDateTime MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(3000, 1, 1, 0, 0);

    @Autowired TestEntityManager em;
    @Autowired BandPostRepository posts;
    @Autowired BandCommentRepository comments;

    private final Instant now = Instant.parse("2030-01-01T00:00:00Z");
    private User me;
    private User drummer;
    private Band band;

    @BeforeEach void setup() {
        me = em.persist(User.builder().email("me@test.invalid").name("김기타").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
        drummer = em.persist(User.builder().email("drum@test.invalid").name("박드럼").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
        var outfit = em.persist(OutfitItem.builder().name("기본").category(OutfitCategory.CLOTHES).requiredLevel(1).spriteKey("DEFAULT").build());
        em.persist(Character.builder().user(drummer).nickname("Thunder").instrumentType(InstrumentType.DRUM).currentOutfit(outfit)
                .gender(Gender.MALE).hairStyle(HairStyle.SHORT_CUT).hairColor("#000000").instrumentColor("#ff0000").build());
        band = em.persist(Band.builder().name("밴드").leader(me).inviteCode("ABCDEFGH").build());
    }

    @Test void keywordMatchesTitleContentNameAndNickname() {
        var title = save(me, "합주 일정 공유", "토요일", false, LocalDateTime.of(2029, 10, 1, 12, 0));
        var content = save(me, "공지 아님", "다음 합주는 홍대", false, LocalDateTime.of(2029, 10, 2, 12, 0));
        var byNickname = save(drummer, "드럼 세팅", "스네어", false, LocalDateTime.of(2029, 10, 3, 12, 0));
        save(me, "관계없는 글", "본문", false, LocalDateTime.of(2029, 10, 4, 12, 0));
        flush();

        assertThat(ids(search("합주", MIN, MAX))).containsExactly(content.getId(), title.getId());
        assertThat(ids(search("thun", MIN, MAX))).containsExactly(byNickname.getId());
        assertThat(ids(search("박드럼", MIN, MAX))).containsExactly(byNickname.getId());
        assertThat(search("없는말", MIN, MAX)).isEmpty();
    }

    @Test void likeWildcardsInKeywordAreLiteral() {
        var percent = save(me, "진도 100% 달성", "본문", false, LocalDateTime.of(2029, 10, 1, 12, 0));
        save(me, "진도 100 달성", "본문", false, LocalDateTime.of(2029, 10, 2, 12, 0));
        var underscore = save(me, "file_name", "본문", false, LocalDateTime.of(2029, 10, 3, 12, 0));
        save(me, "fileXname", "본문", false, LocalDateTime.of(2029, 10, 4, 12, 0));
        flush();

        assertThat(ids(search("100%", MIN, MAX))).containsExactly(percent.getId());
        assertThat(ids(search("e_n", MIN, MAX))).containsExactly(underscore.getId());
    }

    @Test void dateRangeIncludesWholeDays() {
        save(me, "9월 30일", "본문", false, LocalDateTime.of(2029, 9, 30, 23, 59));
        var start = save(me, "10월 1일 0시", "본문", false, LocalDateTime.of(2029, 10, 1, 0, 0));
        var end = save(me, "10월 7일 밤", "본문", false, LocalDateTime.of(2029, 10, 7, 23, 59, 59));
        save(me, "10월 8일", "본문", false, LocalDateTime.of(2029, 10, 8, 0, 0));
        flush();

        // 서비스가 to 다음 날 0시를 끝(미포함)으로 넘긴다
        var result = search("", LocalDateTime.of(2029, 10, 1, 0, 0), LocalDateTime.of(2029, 10, 8, 0, 0));
        assertThat(ids(result)).containsExactly(end.getId(), start.getId());
    }

    @Test void searchKeepsActiveNoticesFirstAndMineFilter() {
        var older = save(me, "합주 공지", "본문", true, LocalDateTime.of(2029, 10, 1, 12, 0));
        var newer = save(drummer, "합주 후기", "본문", false, LocalDateTime.of(2029, 10, 2, 12, 0));
        flush();

        assertThat(ids(search("합주", MIN, MAX))).containsExactly(older.getId(), newer.getId());
        var mine = posts.search(band.getId(), me.getId(), true, null, now, pattern("합주"), MIN, MAX, PageRequest.of(0, 20));
        assertThat(ids(mine.getContent())).containsExactly(older.getId());
    }

    @Test void commentRootsArePagedNewestFirstWithRepliesLoadedSeparately() {
        var post = save(me, "글", "본문", false, LocalDateTime.of(2029, 10, 1, 12, 0));
        var first = em.persist(new BandComment(post, me, "첫 댓글", null));
        var second = em.persist(new BandComment(post, drummer, "둘째 댓글", null));
        var third = em.persist(new BandComment(post, me, "셋째 댓글", null));
        var reply = em.persist(new BandComment(post, drummer, "첫 댓글 답글", first));
        flush();

        var page0 = comments.findRootPage(post.getId(), PageRequest.of(0, 2));
        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent()).extracting(BandComment::getId).containsExactly(third.getId(), second.getId());
        var page1 = comments.findRootPage(post.getId(), PageRequest.of(1, 2));
        assertThat(page1.getContent()).extracting(BandComment::getId).containsExactly(first.getId());
        assertThat(comments.findAllByParentIdInOrderByCreatedAtAscIdAsc(List.of(first.getId())))
                .extracting(BandComment::getId).containsExactly(reply.getId());
    }

    private List<BandPost> search(String keyword, LocalDateTime from, LocalDateTime to) {
        return posts.search(band.getId(), me.getId(), false, null, now, pattern(keyword), from, to, PageRequest.of(0, 20)).getContent();
    }

    private String pattern(String keyword) {
        return BandBoardService.likePattern(keyword);
    }

    private BandPost save(User author, String title, String content, boolean notice, LocalDateTime createdAt) {
        var post = new BandPost(band, author, title, content);
        post.changeNotice(notice, null);
        em.persist(post);
        em.flush();
        // createdAt은 감사 기능이 정하고 수정 불가 칸이라, 검증용 날짜는 벌크 수정으로 넣는다
        em.getEntityManager().createQuery("update BandPost p set p.createdAt = :t where p.id = :id")
                .setParameter("t", createdAt).setParameter("id", post.getId()).executeUpdate();
        return post;
    }

    private void flush() {
        em.flush();
        em.clear();
    }

    private static List<Long> ids(List<BandPost> list) {
        return list.stream().map(BandPost::getId).toList();
    }
}
