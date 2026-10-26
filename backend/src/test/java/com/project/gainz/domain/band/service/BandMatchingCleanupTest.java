package com.project.gainz.domain.band.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 만들지 않기로 한 밴드 매칭의 빈 테이블·칸 정리 (#306)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BandMatchingCleanupTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks BandMatchingCleanup cleanup;

    private void database(String product, int applicationTables, int applicationRows, List<String> bandColumns, int bandsWithData) {
        when(jdbc.execute(any(ConnectionCallback.class))).thenReturn(product);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class))).thenReturn(applicationTables);
        if (applicationTables > 0) {
            when(jdbc.queryForObject(eq("SELECT COUNT(*) FROM band_application"), eq(Integer.class))).thenReturn(applicationRows);
        }
        when(jdbc.queryForList(contains("information_schema.columns"), eq(String.class))).thenReturn(bandColumns);
        if (!bandColumns.isEmpty()) {
            when(jdbc.queryForObject(startsWith("SELECT COUNT(*) FROM band WHERE"), eq(Integer.class))).thenReturn(bandsWithData);
        }
    }

    @Test void emptyLeftoversAreDroppedOnPostgres() {
        database("PostgreSQL", 1, 0, List.of("profile_image_url", "is_recruiting", "recruitment_title"), 0);
        cleanup.run(null);
        verify(jdbc).execute("DROP TABLE band_application");
        verify(jdbc).execute("ALTER TABLE band DROP COLUMN profile_image_url");
        verify(jdbc).execute("ALTER TABLE band DROP COLUMN is_recruiting");
        verify(jdbc).execute("ALTER TABLE band DROP COLUMN recruitment_title");
    }

    @Test void alreadyCleanDoesNothing() {
        database("MariaDB", 0, 0, List.of(), 0);
        cleanup.run(null);
        verify(jdbc, never()).execute(anyString());
    }

    // 데이터가 있으면 지우지 않는다. 그래도 is_recruiting은 NULL 허용으로 바꿔 새 밴드를 만들 수 있게
    @Test void leftoversWithDataAreKeptButRecruitingBecomesNullable() {
        database("MariaDB", 1, 2, List.of("is_recruiting", "recruitment_title"), 1);
        cleanup.run(null);
        verify(jdbc, never()).execute("DROP TABLE band_application");
        verify(jdbc, never()).execute(startsWith("ALTER TABLE band DROP COLUMN"));
        verify(jdbc).execute("ALTER TABLE band MODIFY is_recruiting BIT(1) NULL");
    }

    @Test void failureDoesNotStopStartup() {
        when(jdbc.execute(any(ConnectionCallback.class))).thenThrow(new IllegalStateException("DB 없음"));
        cleanup.run(null);
        verify(jdbc, never()).execute(anyString());
    }
}
