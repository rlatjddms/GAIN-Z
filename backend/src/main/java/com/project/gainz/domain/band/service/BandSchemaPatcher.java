package com.project.gainz.domain.band.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// ddl-auto: update는 이미 있는 컬럼 타입을 바꾸지 않는다.
// 합주실 배경(band.stage)이 MariaDB enum 타입으로 만들어진 DB에서는 배경을 새로 추가하면 저장이 안 되니 varchar로 한 번 고쳐 둔다
@Slf4j
@Component
@RequiredArgsConstructor
public class BandSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                            + " AND TABLE_NAME = 'band' AND COLUMN_NAME = 'stage' AND DATA_TYPE = 'enum'",
                    Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE band MODIFY stage VARCHAR(20) NULL");
                log.info("밴드 스키마 수정: band.stage");
            }
        } catch (Exception e) {
            log.warn("밴드 스키마 수정 실패: band.stage ({})", e.getMessage());
        }
    }
}
