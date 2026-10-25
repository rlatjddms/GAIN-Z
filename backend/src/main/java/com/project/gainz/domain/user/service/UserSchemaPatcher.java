package com.project.gainz.domain.user.service;

import com.project.gainz.global.common.converter.CheckConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 회원 이름 칸(user.name)은 쓰지 않는다 (#227, 화면에 보이는 이름은 캐릭터 닉네임).
// ddl-auto는 칸을 지우거나 NOT NULL을 풀지 않으니, 이름 없이 가입할 수 있게 NOT NULL만 푼다.
// 예전 이름은 남아 있다. 칸 자체는 배포 뒤 확인하고 직접 지운다 (ALTER TABLE "user" DROP COLUMN name)
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean mariaDb = CheckConstraints.isMariaDb(jdbcTemplate);
            Integer notNull = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = "
                            + (mariaDb ? "DATABASE()" : "current_schema()")
                            + " AND table_name = 'user' AND column_name = 'name' AND is_nullable = 'NO'",
                    Integer.class);
            if (notNull == null || notNull == 0) return;
            jdbcTemplate.execute(mariaDb
                    ? "ALTER TABLE user MODIFY name VARCHAR(255) NULL"
                    : "ALTER TABLE \"user\" ALTER COLUMN name DROP NOT NULL");
            log.info("회원 스키마 수정: user.name NOT NULL 해제");
        } catch (Exception e) {
            log.warn("회원 스키마 수정 실패: user.name ({})", e.getMessage());
        }
    }
}
