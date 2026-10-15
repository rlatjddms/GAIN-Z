package com.project.gainz.domain.practice.dto.response;

import java.time.LocalDate;

// 하루 연습 합계 (연습 시간, 그날 받은 경험치)
public record DailyPracticeResponse(
        LocalDate date,
        Long totalSeconds,
        Long expGained
) {
    public static DailyPracticeResponse empty(LocalDate date) {
        return new DailyPracticeResponse(date, 0L, 0L);
    }
}
