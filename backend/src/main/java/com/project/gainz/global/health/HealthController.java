package com.project.gainz.global.health;

import com.project.gainz.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 서버가 살아 있는지 (#180). 로그인 없이 부른다.
// Render 헬스 체크와 5분마다 깨우는 UptimeRobot(#195)이 쓴다. DB까지 한 번 물어서 Supabase가 쉬는 프로젝트로 멈추지 않게 한다
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/v1/health")
    public ResponseEntity<ApiResponse<String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(ApiResponse.success("ok"));
        } catch (Exception e) {
            log.warn("헬스 체크: DB 연결 실패 ({})", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.success("db-down"));
        }
    }
}
