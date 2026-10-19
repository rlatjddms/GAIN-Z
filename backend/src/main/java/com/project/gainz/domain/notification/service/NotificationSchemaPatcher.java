package com.project.gainz.domain.notification.service;

import com.project.gainz.global.common.converter.CheckConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// ddl-auto: update는 이미 있는 컬럼 타입을 바꾸지 않는다.
// 알림 종류(notification.type)가 예전 enum 값만 가진 MariaDB enum 타입으로 만들어진 DB에서는
// 새 종류(친구·합주 등) 알림이 저장되지 않으니 varchar로 한 번 고쳐 둔다.
// 값 목록 CHECK 제약(notification.type, notification_off.setting)도 지운다
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                            + " AND TABLE_NAME = 'notification' AND COLUMN_NAME = 'type' AND DATA_TYPE = 'enum'",
                    Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE notification MODIFY type VARCHAR(30) NOT NULL");
                log.info("알림 스키마 수정: notification.type");
            }
        } catch (Exception e) {
            log.warn("알림 스키마 수정 실패: notification.type ({})", e.getMessage());
        }
        // 예전 @Enumerated(STRING)이 붙인 값 목록 제약이 남아 있으면 새 종류·설정을 저장할 수 없다
        dropValueChecks("notification", "type");
        dropValueChecks("notification_off", "setting");
    }

    private void dropValueChecks(String table, String column) {
        try {
            int dropped = CheckConstraints.dropOnColumn(jdbcTemplate, table, column);
            if (dropped > 0) log.info("알림 스키마 수정: {}.{} 값 목록 제약 {}개 제거", table, column, dropped);
        } catch (Exception e) {
            log.warn("알림 스키마 수정 실패: {}.{} 값 목록 제약 ({})", table, column, e.getMessage());
        }
    }
}
