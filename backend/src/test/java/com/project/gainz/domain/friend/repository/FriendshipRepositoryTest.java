package com.project.gainz.domain.friend.repository;

import com.project.gainz.domain.friend.entity.Friendship;
import com.project.gainz.domain.friend.enums.FriendshipStatus;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.UserRepository;
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

// 외부 DB 없이 친구 관계 쿼리(양방향 조회·상태별 목록·소프트 삭제)와 친구 코드 조회를 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = FriendshipRepositoryTest.Config.class)
class FriendshipRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories(basePackages = {"com.project.gainz.domain.friend.repository", "com.project.gainz.domain.user.repository"})
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired FriendshipRepository friendships;
    @Autowired UserRepository users;

    @Test void friendshipsAreFoundFromEitherSideByStatusAndSoftDeleted() {
        User me = user("me");
        User a = user("a");
        User b = user("b");
        User c = user("c");
        Friendship sentByMe = em.persist(Friendship.builder().requester(me).receiver(a).build());
        sentByMe.accept();
        Friendship receivedByMe = em.persist(Friendship.builder().requester(b).receiver(me).build());
        receivedByMe.accept();
        Friendship pendingToMe = em.persist(Friendship.builder().requester(c).receiver(me).build());
        em.persist(Friendship.builder().requester(a).receiver(b).build());
        em.flush();
        em.clear();

        // 내가 신청했든 받았든 친구로 보이고, 대기 중인 신청은 빠진다
        assertThat(friendships.findAllByUserIdAndStatus(me.getId(), FriendshipStatus.ACCEPTED))
                .extracting(Friendship::getId).containsExactlyInAnyOrder(sentByMe.getId(), receivedByMe.getId());
        assertThat(friendships.countByUserIdAndStatus(me.getId(), FriendshipStatus.ACCEPTED)).isEqualTo(2);
        assertThat(friendships.findAllByReceiverIdAndStatusOrderByIdDesc(me.getId(), FriendshipStatus.PENDING))
                .extracting(Friendship::getId).containsExactly(pendingToMe.getId());
        assertThat(friendships.findAllByRequesterIdAndStatusOrderByIdDesc(me.getId(), FriendshipStatus.PENDING)).isEmpty();

        // 두 사람 사이는 어느 순서로 물어도 같은 관계
        assertThat(friendships.findAllBetween(me.getId(), b.getId())).extracting(Friendship::getId).containsExactly(receivedByMe.getId());
        assertThat(friendships.findAllBetween(b.getId(), me.getId())).extracting(Friendship::getId).containsExactly(receivedByMe.getId());
        assertThat(friendships.findAllBetween(me.getId(), me.getId())).isEmpty();
        assertThat(friendships.findAllByUserId(me.getId())).hasSize(3);

        // 친구를 끊으면 목록에서 빠지고, 같은 사람에게 다시 신청할 수 있다
        friendships.delete(friendships.findById(receivedByMe.getId()).orElseThrow());
        em.flush();
        em.clear();
        assertThat(friendships.findAllBetween(me.getId(), b.getId())).isEmpty();
        assertThat(friendships.findAllByUserIdAndStatus(me.getId(), FriendshipStatus.ACCEPTED))
                .extracting(Friendship::getId).containsExactly(sentByMe.getId());
        Friendship again = em.persist(Friendship.builder().requester(me).receiver(b).build());
        em.flush();
        assertThat(friendships.findAllBetween(b.getId(), me.getId())).extracting(Friendship::getId).containsExactly(again.getId());
    }

    @Test void usersAreFoundByFriendCode() {
        User me = user("me");
        me.changeFriendCode("ABCD2345");
        em.flush();
        em.clear();

        assertThat(users.findByFriendCode("ABCD2345")).map(User::getId).contains(me.getId());
        assertThat(users.existsByFriendCode("ABCD2345")).isTrue();
        assertThat(users.existsByFriendCode("ZZZZ9999")).isFalse();
    }

    private User user(String name) {
        return em.persist(User.builder().email(name + "@test.invalid").name(name).provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }
}
