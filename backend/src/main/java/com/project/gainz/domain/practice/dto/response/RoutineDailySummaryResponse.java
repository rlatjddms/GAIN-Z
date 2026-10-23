package com.project.gainz.domain.practice.dto.response;

import java.time.LocalDate;

// 날짜별 루틴 달성 요약 (그날 일정 루틴 수, 완료한 수)
public record RoutineDailySummaryResponse(
        LocalDate date,
        Integer total,
        Integer completed
) {
}
