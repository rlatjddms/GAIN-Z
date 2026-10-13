package com.project.gainz.domain.practice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record PracticeLogCreateRequest(

        @NotNull(message = "연습 시간은 필수 입력 항목입니다.")
        @Min(value = 1, message = "연습 시간은 1초 이상이어야 합니다.")
        @Max(value = 86400, message = "연습 시간은 하루(86400초)를 초과할 수 없습니다.")
        Long durationSeconds,

        @NotNull(message = "달성률은 필수 입력 항목입니다.")
        @DecimalMin(value = "0.0", message = "달성률은 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "달성률은 1 이하이어야 합니다.")
        Double achievementRate,

        @PastOrPresent(message = "연습 날짜는 오늘 이전이어야 합니다.")
        LocalDate practiceDate
) {
    public LocalDate practiceDateOrToday() {
        return practiceDate != null ? practiceDate : LocalDate.now();
    }
}
