package com.project.gainz.domain.band.service;

import com.project.gainz.global.common.converter.CheckConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// 밴드 매칭(#87·#88)은 만들지 않기로 해서 미리 만들어 둔 테이블·칸을 지운다 (#306).
// ddl-auto는 엔티티에서 빠진 테이블·칸을 지우지 않으니 서버가 켜질 때 한 번 정리한다. 이미 없으면 아무것도 안 한다.
// 쓰는 코드가 없어서 비어 있어야 정상이지만, 혹시 데이터가 들어 있으면 지우지 않고 경고만 남긴다.
// 그때도 band.is_recruiting(NOT NULL)은 NULL 허용으로 바꿔서, 그 칸을 모르는 새 코드가 밴드를 만들 수 있게 한다
@Slf4j
@Component
@RequiredArgsConstructor
public class BandMatchingCleanup implements ApplicationRunner {

    static final String APPLICATION_TABLE = "band_application";
    static final List<String> BAND_COLUMNS = List.of(
            "profile_image_url", "is_recruiting", "recruitment_title", "recruitment_content", "recruitment_image_url");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean mariaDb = CheckConstraints.isMariaDb(jdbcTemplate);
            dropApplicationTable(mariaDb);
            dropBandColumns(mariaDb);
        } catch (Exception e) {
            log.warn("밴드 매칭 정리 실패 ({})", e.getMessage());
        }
    }

    private void dropApplicationTable(boolean mariaDb) {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = " + schema(mariaDb)
                        + " AND table_name = '" + APPLICATION_TABLE + "'",
                Integer.class);
        if (tables == null || tables == 0) return;
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + APPLICATION_TABLE, Integer.class);
        if (rows != null && rows > 0) {
            log.warn("밴드 매칭 정리: {}에 데이터 {}개가 있어 지우지 않았어요", APPLICATION_TABLE, rows);
            return;
        }
        jdbcTemplate.execute("DROP TABLE " + APPLICATION_TABLE);
        log.info("밴드 매칭 정리: {} 테이블 삭제", APPLICATION_TABLE);
    }

    private void dropBandColumns(boolean mariaDb) {
        List<String> present = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = " + schema(mariaDb)
                        + " AND table_name = 'band' AND column_name IN ('" + String.join("', '", BAND_COLUMNS) + "')",
                String.class).stream().map(String::toLowerCase).toList();
        if (present.isEmpty()) return;

        // 매칭 칸에 값이 하나라도 있는 밴드 (is_recruiting은 기본값 false라 true만 값으로 본다)
        String used = present.stream()
                .map(c -> c.equals("is_recruiting") ? c + " = true" : c + " IS NOT NULL")
                .reduce((a, b) -> a + " OR " + b).orElseThrow();
        Integer withData = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM band WHERE " + used, Integer.class);
        if (withData != null && withData > 0) {
            log.warn("밴드 매칭 정리: band의 매칭 칸에 값이 있는 밴드가 {}개라 칸을 지우지 않았어요", withData);
            if (present.contains("is_recruiting")) {
                jdbcTemplate.execute(mariaDb
                        ? "ALTER TABLE band MODIFY is_recruiting BIT(1) NULL"
                        : "ALTER TABLE band ALTER COLUMN is_recruiting DROP NOT NULL");
            }
            return;
        }
        for (String column : present) {
            jdbcTemplate.execute("ALTER TABLE band DROP COLUMN " + column);
        }
        log.info("밴드 매칭 정리: band 칸 {} 삭제", present);
    }

    private static String schema(boolean mariaDb) {
        return mariaDb ? "DATABASE()" : "current_schema()";
    }
}
