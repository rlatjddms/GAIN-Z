package com.project.gainz.domain.record.repository;

import com.project.gainz.domain.record.entity.UserRecord;
import com.project.gainz.domain.record.enums.RecordType;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 외부 DB 없이 얻은 레코드 저장(얻은 때 기록), 같은 레코드 중복 금지, 사람별 조회·지우기를 검증한다.
@DataJpaTest(properties = "spring.config.location=classpath:band-board-test.properties", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UserRecordRepositoryTest.Config.class)
class UserRecordRepositoryTest {
    @Configuration
    @EnableJpaAuditing
    @EntityScan("com.project.gainz.domain")
    @EnableJpaRepositories("com.project.gainz.domain.record.repository")
    static class Config {}

    @Autowired TestEntityManager em;
    @Autowired UserRecordRepository records;

    @Test void recordsAreSavedPerUserWithAcquiredTimeAndCannotBeDuplicated() {
        User me = user("me");
        User other = user("other");
        records.save(new UserRecord(me, RecordType.STREAK_7));
        records.save(new UserRecord(me, RecordType.HOURS_10));
        records.save(new UserRecord(other, RecordType.STREAK_7));
        em.flush();
        em.clear();

        assertThat(records.findAllByUserId(me.getId())).extracting(UserRecord::getRecordType)
                .containsExactlyInAnyOrder(RecordType.STREAK_7, RecordType.HOURS_10);
        assertThat(records.findAllByUserId(me.getId())).allSatisfy(r -> assertThat(r.getCreatedAt()).isNotNull());

        assertThatThrownBy(() -> {
            records.save(new UserRecord(me, RecordType.STREAK_7));
            em.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class);
    }

    @Test void deletingRemovesOnlyThatUsersRecords() {
        User me = user("me2");
        User other = user("other2");
        records.save(new UserRecord(me, RecordType.FIRST_BAND));
        records.save(new UserRecord(other, RecordType.FIRST_BAND));
        em.flush();

        assertThat(records.deleteAllByUserId(me.getId())).isEqualTo(1);
        em.clear();
        assertThat(records.findAllByUserId(me.getId())).isEmpty();
        assertThat(records.findAllByUserId(other.getId())).hasSize(1);
    }

    @Autowired javax.sql.DataSource dataSource;

    @Test void recordsRemovedFromTheCollectionAreCleanedUpAtStartup() {
        User me = user("me3");
        records.save(new UserRecord(me, RecordType.FIRST_BAND));
        em.flush();
        // 예전 도감에 있던 레코드 (지금 RecordType에는 없음)
        em.getEntityManager().createNativeQuery("insert into user_record (user_id, record_type, created_at, is_deleted)"
                + " values (?, 'FIRST_PRACTICE', current_timestamp, false)").setParameter(1, me.getId()).executeUpdate();

        new com.project.gainz.domain.record.service.RecordSchemaPatcher(
                new org.springframework.jdbc.core.JdbcTemplate(dataSource)).run(null);
        em.clear();

        assertThat(records.findAllByUserId(me.getId())).extracting(UserRecord::getRecordType)
                .containsExactly(RecordType.FIRST_BAND);
    }

    private User user(String name) {
        return em.persist(User.builder().email(name + "@test.invalid").provider(SocialProvider.LOCAL).role(UserRole.ROLE_USER).build());
    }
}
