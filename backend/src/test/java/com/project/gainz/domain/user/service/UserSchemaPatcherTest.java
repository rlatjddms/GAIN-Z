package com.project.gainz.domain.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 서버가 켜질 때 남은 user.name 칸을 지운다 (#264)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UserSchemaPatcherTest {

    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks UserSchemaPatcher patcher;

    private void database(String product, int nameColumns) {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenReturn(product);
        when(jdbcTemplate.queryForObject(contains("column_name = 'name'"), eq(Integer.class))).thenReturn(nameColumns);
    }

    @Test void dropsLeftoverNameColumnOnPostgres() {
        database("PostgreSQL", 1);
        patcher.run(null);
        verify(jdbcTemplate).execute("ALTER TABLE \"user\" DROP COLUMN name");
    }

    @Test void dropsLeftoverNameColumnOnMariaDb() {
        database("MariaDB", 1);
        patcher.run(null);
        verify(jdbcTemplate).execute("ALTER TABLE user DROP COLUMN name");
    }

    @Test void doesNothingWhenAlreadyGone() {
        database("PostgreSQL", 0);
        patcher.run(null);
        verify(jdbcTemplate, never()).execute(anyString());
    }

    // 지우다 실패해도 서버는 그대로 켜진다
    @Test void failureDoesNotStopStartup() {
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenThrow(new IllegalStateException("DB 없음"));
        patcher.run(null);
        verify(jdbcTemplate, never()).execute(anyString());
    }
}
