package com.project.gainz.domain.character.service;

import com.project.gainz.global.common.converter.CheckConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// ddl-auto: update는 이미 있는 컬럼 타입을 바꾸지 않는다.
// characters.hair_style이 예전 머리 값만 가진 MariaDB enum 타입이거나 값 목록 CHECK 제약이 남아 있으면
// 레벨 보상 머리(PONYTAIL 등)를 저장할 수 없으니 varchar로 고치고 제약을 지운다
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                            + " AND TABLE_NAME = 'characters' AND COLUMN_NAME = 'hair_style' AND DATA_TYPE = 'enum'",
                    Integer.class);
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE characters MODIFY hair_style VARCHAR(30) NOT NULL");
                log.info("캐릭터 스키마 수정: characters.hair_style");
            }
        } catch (Exception e) {
            log.warn("캐릭터 스키마 수정 실패: characters.hair_style ({})", e.getMessage());
        }
        try {
            int dropped = CheckConstraints.dropOnColumn(jdbcTemplate, "characters", "hair_style");
            if (dropped > 0) log.info("캐릭터 스키마 수정: characters.hair_style 값 목록 제약 {}개 제거", dropped);
        } catch (Exception e) {
            log.warn("캐릭터 스키마 수정 실패: characters.hair_style 값 목록 제약 ({})", e.getMessage());
        }
    }
}
