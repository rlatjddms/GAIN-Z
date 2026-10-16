package com.project.gainz.domain.sheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// ddl-auto: update는 이미 있는 컬럼의 NOT NULL·타입을 바꾸지 않는다.
// 예전 스키마로 만들어진 DB에서 직접 쓴 악보(음원·BPM 없음)와 긴 메모를 저장할 수 있게 한 번 고쳐 둔다
@Slf4j
@Component
@RequiredArgsConstructor
public class SheetSchemaPatcher implements ApplicationRunner {

    // 테이블, 컬럼, 고쳐야 하는지 판단할 조건(information_schema), 고치는 SQL
    record Patch(String table, String column, String condition, String sql) {
    }

    static final List<Patch> PATCHES = List.of(
            new Patch("sheet_music", "original_audio_url", "IS_NULLABLE = 'NO'",
                    "ALTER TABLE sheet_music MODIFY original_audio_url VARCHAR(255) NULL"),
            new Patch("sheet_music", "bpm", "IS_NULLABLE = 'NO'",
                    "ALTER TABLE sheet_music MODIFY bpm INT NULL"),
            new Patch("user_workspace", "personal_memo", "DATA_TYPE = 'varchar'",
                    "ALTER TABLE user_workspace MODIFY personal_memo TEXT NULL")
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (Patch patch : PATCHES) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                                + " AND TABLE_NAME = ? AND COLUMN_NAME = ? AND " + patch.condition(),
                        Integer.class, patch.table(), patch.column());
                if (count != null && count > 0) {
                    jdbcTemplate.execute(patch.sql());
                    log.info("악보 스키마 수정: {}.{}", patch.table(), patch.column());
                }
            } catch (Exception e) {
                log.warn("악보 스키마 수정 실패: {}.{} ({})", patch.table(), patch.column(), e.getMessage());
            }
        }
    }
}
