package com.project.gainz.global.common.converter;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

// 예전에 @Enumerated(STRING)으로 만들어진 테이블에 남은 "값 목록" CHECK 제약을 지운다 (MariaDB)
public final class CheckConstraints {

    private CheckConstraints() {
    }

    // table의 column을 검사하는 CHECK 제약을 모두 지운다 → 지운 개수
    public static int dropOnColumn(JdbcTemplate jdbcTemplate, String table, String column) {
        List<String> names = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM information_schema.CHECK_CONSTRAINTS"
                        + " WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CHECK_CLAUSE LIKE ?",
                String.class, table, "%" + column + "%");
        for (String name : names) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` DROP CONSTRAINT `" + name.replace("`", "") + "`");
        }
        return names.size();
    }
}
