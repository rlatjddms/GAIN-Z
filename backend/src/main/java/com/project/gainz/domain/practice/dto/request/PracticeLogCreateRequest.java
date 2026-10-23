package com.project.gainz.domain.practice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

// 타이머 구간 저장 요청. 자정을 넘긴 구간은 프론트에서 날짜별로 나눠 보낸다
public record PracticeLogCreateRequest(

        @NotNull(message = "연습 시간은 필수 입력 항목입니다.")
        @Min(value = 1, message = "연습 시간은 1초 이상이어야 합니다.")
        @Max(value = 86400, message = "연습 시간은 하루(86400초)를 초과할 수 없습니다.")
        Long durationSeconds,

        @PastOrPresent(message = "연습 날짜는 오늘 이전이어야 합니다.")
        LocalDate practiceDate
) {
    public LocalDate practiceDateOrToday() {
        return practiceDate != null ? practiceDate : LocalDate.now();
    }
}
