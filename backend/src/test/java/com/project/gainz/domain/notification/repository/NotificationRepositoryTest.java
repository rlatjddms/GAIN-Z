package com.project.gainz.domain.notification.repository;

import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.entity.NotificationOff;
import com.project.gainz.domain.notification.enums.NotificationCategory;
import com.project.gainz.domain.notification.enums.NotificationSetting;
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

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    @Autowired NotificationOffRepository offs;

    @Test void myNotificationsArePagedNewestFirstAndMarkAllReadTouchesOnlyMine() {
        User me = user("me");
        User other = user("other");
        Notification first = em.persist(notification(me, "첫 알림"));
        Notification second = em.persist(notification(me, "둘째 알림"));
        Notification third = em.persist(notification(me, "셋째 알림"));
        Notification others = em.persist(notification(other, "남의 알림"));
        em.flush();
        em.clear();

        var page = notifications.findPage(me.getId(), ALL, false, PageRequest.of(0, 2));
        assertThat(page.getContent()).extracting(Notification::getContent).containsExactly("셋째 알림", "둘째 알림");
        assertThat(page.hasNext()).isTrue();
        assertThat(notifications.findPage(me.getId(), ALL, false, PageRequest.of(1, 2)).getContent())
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

    @Test void filtersByCategoryAndUnreadAndPicksOnlyMineAndCleansOldRead() {
        User me = user("me2");
        User other = user("other2");
        Notification friend = em.persist(notification(me, NotificationType.FRIEND_REQUEST, "친구 신청"));
        Notification jam = em.persist(notification(me, NotificationType.JAM_NEW, "새 잼"));
        Notification readJam = notification(me, NotificationType.JAM_TRACK, "트랙");
        readJam.markRead();
        em.persist(readJam);
        Notification others = em.persist(notification(other, NotificationType.JAM_NEW, "남의 잼"));
        em.flush();
        em.clear();

        // 영역·안 읽음으로 거르기
        assertThat(notifications.findPage(me.getId(), NotificationCategory.JAM.types(), false, PageRequest.of(0, 10)).getContent())
                .extracting(Notification::getContent).containsExactly("트랙", "새 잼");
        assertThat(notifications.findPage(me.getId(), NotificationCategory.JAM.types(), true, PageRequest.of(0, 10)).getContent())
                .extracting(Notification::getContent).containsExactly("새 잼");
        assertThat(notifications.findPage(me.getId(), NotificationCategory.FRIEND.types(), false, PageRequest.of(0, 10)).getContent())
                .extracting(Notification::getId).containsExactly(friend.getId());

        // 고른 id 중 내 것만, 읽은 것만
        assertThat(notifications.findAllByReceiverIdAndIdIn(me.getId(), List.of(jam.getId(), others.getId())))
                .extracting(Notification::getId).containsExactly(jam.getId());
        assertThat(notifications.findAllByReceiverIdAndIsReadTrue(me.getId()))
                .extracting(Notification::getId).containsExactly(readJam.getId());

        // 기준 시각보다 먼저 만든 읽은 알림만 정리 (안 읽은 알림은 남는다)
        LocalDateTime now = LocalDateTime.now();
        assertThat(notifications.deleteReadBefore(now.minusDays(1), now)).isZero();
        assertThat(notifications.deleteReadBefore(now.plusMinutes(1), now)).isEqualTo(1);
        assertThat(notifications.findAllByReceiverId(me.getId())).extracting(Notification::getId)
                .containsExactlyInAnyOrder(friend.getId(), jam.getId());
    }

    @Test void turnedOffSettingsAreFoundPerUserAndTurnedOnByDeleting() {
        User me = user("me3");
        User other = user("other3");
        offs.save(new NotificationOff(me, NotificationSetting.NOTICE));
        offs.save(new NotificationOff(me, NotificationSetting.JAM_NEW));
        offs.save(new NotificationOff(other, NotificationSetting.NOTICE));
        em.flush();
        em.clear();

        assertThat(offs.findUserIdsTurnedOff(NotificationSetting.NOTICE, List.of(me.getId(), other.getId())))
                .containsExactlyInAnyOrder(me.getId(), other.getId());
        assertThat(offs.findUserIdsTurnedOff(NotificationSetting.JAM_NEW, List.of(other.getId()))).isEmpty();

        assertThat(offs.turnOn(me.getId(), Set.of(NotificationSetting.NOTICE))).isEqualTo(1);
        assertThat(offs.findAllByUserId(me.getId())).extracting(NotificationOff::getSetting)
                .containsExactly(NotificationSetting.JAM_NEW);
        assertThat(offs.deleteAllByUserId(me.getId())).isEqualTo(1);
        assertThat(offs.findAllByUserId(other.getId())).hasSize(1);
    }

    private static final Set<NotificationType> ALL = EnumSet.allOf(NotificationType.class);

    private Notification notification(User receiver, String content) {
        return notification(receiver, NotificationType.NOTICE, content);
    }

    private Notification notification(User receiver, NotificationType type, String content) {
        return Notification.builder().receiver(receiver).type(type).title("밴드").content(content)
                .redirectUrl("/home").build();
    }

    private User user(String name) {
        return em.persist(User.builder().email(name + "@test.invalid").name(name).provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }
}
