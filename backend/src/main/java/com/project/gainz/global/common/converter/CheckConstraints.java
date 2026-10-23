package com.project.gainz.global.common.converter;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

// 예전에 @Enumerated(STRING)으로 만들어진 테이블에 남은 "값 목록" CHECK 제약을 지운다 (MariaDB)
public final class CheckConstraints {

    private CheckConstraints() {
    }

    // 예전 스키마를 고치는 SchemaPatcher들은 MariaDB(MySQL) 전용이다.
    // PostgreSQL(운영, #178)은 처음부터 Hibernate가 지금 엔티티대로 만들어서 고칠 것이 없다
    public static boolean isMariaDb(JdbcTemplate jdbcTemplate) {
        String product = jdbcTemplate.execute((ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
        return "MariaDB".equalsIgnoreCase(product) || "MySQL".equalsIgnoreCase(product);
    }

    // table의 column을 검사하는 CHECK 제약을 모두 지운다 → 지운 개수 (MariaDB가 아니면 0)
    public static int dropOnColumn(JdbcTemplate jdbcTemplate, String table, String column) {
        if (!isMariaDb(jdbcTemplate)) {
            return 0;
        }
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
