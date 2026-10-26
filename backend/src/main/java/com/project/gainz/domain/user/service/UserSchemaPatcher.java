package com.project.gainz.domain.user.service;

import com.project.gainz.global.common.converter.CheckConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 회원 이름 칸(user.name)은 쓰지 않는다 (#227, 화면에 보이는 이름은 캐릭터 닉네임).
// ddl-auto는 칸을 지우지 않으니, 서버가 켜질 때 칸이 남아 있으면 지운다 (#264). 이미 없으면 아무것도 안 한다.
// 지운 칸(예전 이름)은 되돌릴 수 없다
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean mariaDb = CheckConstraints.isMariaDb(jdbcTemplate);
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = "
                            + (mariaDb ? "DATABASE()" : "current_schema()")
                            + " AND table_name = 'user' AND column_name = 'name'",
                    Integer.class);
            if (exists == null || exists == 0) return;
            jdbcTemplate.execute(mariaDb ? "ALTER TABLE user DROP COLUMN name" : "ALTER TABLE \"user\" DROP COLUMN name");
            log.info("회원 스키마 수정: 쓰지 않는 user.name 칸 삭제");
        } catch (Exception e) {
            log.warn("회원 스키마 수정 실패: user.name 삭제 ({})", e.getMessage());
        }
    }
}
