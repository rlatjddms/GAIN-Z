package com.project.gainz.domain.practice.repository;

import com.project.gainz.domain.practice.dto.response.PracticeDaySplit;
import com.project.gainz.domain.practice.entity.PracticeLog;
import com.project.gainz.domain.practice.enums.PracticeSource;
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
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 외부 DB 없이 하루 기록을 타이머·직접 입력으로 나눈 합계를 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PracticeLogRepositoryTest.Config.class)
class PracticeLogRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.practice.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired PracticeLogRepository logs;

    @Test void daySplitSumsTimerAndManualSeparatelyForThatDayOnly() {
        User me = em.persist(User.builder().email("me@test.invalid").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
        User other = em.persist(User.builder().email("other@test.invalid").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
        LocalDate day = LocalDate.of(2026, 10, 19);
        log(me, day, 3600, 60, PracticeSource.TIMER);
        log(me, day, 1200, 20, PracticeSource.TIMER);
        log(me, day, 1800, 30, PracticeSource.MANUAL);
        log(me, day, -2400, -10, PracticeSource.MANUAL);
        log(me, day.minusDays(1), 600, 10, PracticeSource.TIMER);
        log(other, day, 999, 9, PracticeSource.TIMER);
        em.flush();
        em.clear();

        PracticeDaySplit split = logs.findDaySplit(me.getId(), day);
        assertThat(split.timer()).isEqualTo(4800);
        assertThat(split.manual()).isEqualTo(-600);
        assertThat(split.exp()).isEqualTo(100);

        PracticeDaySplit empty = logs.findDaySplit(me.getId(), day.plusDays(1));
        assertThat(empty.timer()).isZero();
        assertThat(empty.manual()).isZero();
        assertThat(empty.exp()).isZero();
    }

    private void log(User user, LocalDate date, long seconds, int exp, PracticeSource source) {
        em.persist(PracticeLog.builder().user(user).durationSeconds(seconds).expGained(exp).createdDate(date).source(source).build());
    }
}
