package com.project.gainz.domain.notification.service;

import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.notification.repository.NotificationRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.enums.UserRole;
import com.project.gainz.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 JPA 트랜잭션으로: 알림은 원래 일이 커밋된 뒤 저장되고, 롤백되면 저장되지 않는다
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = NotificationAfterCommitTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationAfterCommitTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories(basePackages = {
            "com.project.gainz.domain.notification.repository",
            "com.project.gainz.domain.user.repository",
            "com.project.gainz.domain.character.repository"})
    @Import(NotificationService.class)
    static class Config {}

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notifications;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void savedAfterCommitAndNotSavedOnRollback() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        User receiver = tx.execute(s -> users.save(User.builder().email("r" + System.nanoTime() + "@test.invalid")
                .name("받는 사람").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build()));
        long before = notifications.countByReceiverIdAndIsReadFalse(receiver.getId());

        // 원래 일 안에서 알림을 만들면, 커밋된 뒤 저장된다
        tx.executeWithoutResult(s -> {
            User managed = users.findById(receiver.getId()).orElseThrow();
            notificationService.notify(managed, NotificationType.SETLIST_ASSIGN, "밴드", "곡 담당", "/bands/1/setlists");
            assertThat(notifications.countByReceiverIdAndIsReadFalse(receiver.getId())).isEqualTo(before);
        });
        assertThat(notifications.countByReceiverIdAndIsReadFalse(receiver.getId())).isEqualTo(before + 1);

        // 원래 일이 롤백되면 알림도 없다
        tx.executeWithoutResult(s -> {
            User managed = users.findById(receiver.getId()).orElseThrow();
            notificationService.notify(managed, NotificationType.FRIEND_REQUEST, "친구", "신청", "/friends");
            s.setRollbackOnly();
        });
        assertThat(notifications.countByReceiverIdAndIsReadFalse(receiver.getId())).isEqualTo(before + 1);
    }
}
