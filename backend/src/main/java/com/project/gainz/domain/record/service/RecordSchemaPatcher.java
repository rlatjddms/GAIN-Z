package com.project.gainz.domain.record.service;

import com.project.gainz.domain.record.enums.RecordType;
import com.project.gainz.global.common.converter.CheckConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// 도감에서 빠진 레코드(RecordType에 없는 값)를 얻은 기록이 남아 있으면 불러올 때 오류가 나니, 시작할 때 지운다
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 첫 버전 테이블에 남은 값 목록 CHECK 제약이 있으면 새 레코드를 저장할 수 없다
        try {
            int dropped = CheckConstraints.dropOnColumn(jdbcTemplate, "user_record", "record_type");
            if (dropped > 0) log.info("레코드 스키마 수정: user_record.record_type 값 목록 제약 {}개 제거", dropped);
        } catch (Exception e) {
            log.warn("레코드 스키마 수정 실패: user_record.record_type 값 목록 제약 ({})", e.getMessage());
        }
        try {
            List<String> keys = Arrays.stream(RecordType.values()).map(Enum::name).toList();
            String placeholders = String.join(",", Collections.nCopies(keys.size(), "?"));
            int removed = jdbcTemplate.update(
                    "DELETE FROM user_record WHERE record_type NOT IN (" + placeholders + ")", keys.toArray());
            if (removed > 0) log.info("도감에서 빠진 레코드 기록 정리: {}개", removed);
        } catch (Exception e) {
            log.warn("레코드 기록 정리 실패 ({})", e.getMessage());
        }
    }
}
