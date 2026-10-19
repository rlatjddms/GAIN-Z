package com.project.gainz.domain.notification.repository;

import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
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

import static org.assertj.core.api.Assertions.assertThat;

// 외부 DB 없이 내 알림 목록(최근 순·쪽 나누기), 안 읽은 개수, 모두 읽음, 지운 알림 숨김을 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = NotificationRepositoryTest.Config.class)
class NotificationRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.notification.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired NotificationRepository notifications;

    @Test void myNotificationsArePagedNewestFirstAndMarkAllReadTouchesOnlyMine() {
        User me = user("me");
        User other = user("other");
        Notification first = em.persist(notification(me, "첫 알림"));
        Notification second = em.persist(notification(me, "둘째 알림"));
        Notification third = em.persist(notification(me, "셋째 알림"));
        Notification others = em.persist(notification(other, "남의 알림"));
        em.flush();
        em.clear();

        var page = notifications.findAllByReceiverIdOrderByIdDesc(me.getId(), PageRequest.of(0, 2));
        assertThat(page.getContent()).extracting(Notification::getContent).containsExactly("셋째 알림", "둘째 알림");
        assertThat(page.hasNext()).isTrue();
        assertThat(notifications.findAllByReceiverIdOrderByIdDesc(me.getId(), PageRequest.of(1, 2)).getContent())
                .extracting(Notification::getId).containsExactly(first.getId());
        assertThat(notifications.findByIdAndReceiverId(others.getId(), me.getId())).isEmpty();
        assertThat(notifications.countByReceiverIdAndIsReadFalse(me.getId())).isEqualTo(3);

        // 지운 알림은 목록·개수에서 빠지고, 모두 읽음은 내 알림만 바꾼다
        notifications.delete(notifications.findById(third.getId()).orElseThrow());
        em.flush();
        em.clear();
        assertThat(notifications.countByReceiverIdAndIsReadFalse(me.getId())).isEqualTo(2);
        assertThat(notifications.markAllRead(me.getId())).isEqualTo(2);
        assertThat(notifications.countByReceiverIdAndIsReadFalse(me.getId())).isZero();
        assertThat(notifications.countByReceiverIdAndIsReadFalse(other.getId())).isEqualTo(1);
        assertThat(notifications.findById(second.getId()).orElseThrow().getIsRead()).isTrue();
        assertThat(notifications.findAllByReceiverId(me.getId())).hasSize(2);
    }

    private Notification notification(User receiver, String content) {
        return Notification.builder().receiver(receiver).type(NotificationType.NOTICE).title("밴드").content(content)
                .redirectUrl("/home").build();
    }

    private User user(String name) {
        return em.persist(User.builder().email(name + "@test.invalid").name(name).provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }
}
