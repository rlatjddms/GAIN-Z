package com.project.gainz.domain.practice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 하루 총 연습 시간 직접 수정 요청
public record DailyPracticeUpdateRequest(

        @NotNull(message = "연습 시간은 필수 입력 항목입니다.")
        @Min(value = 0, message = "연습 시간은 0 이상이어야 합니다.")
        @Max(value = 86400, message = "하루 연습 시간은 24시간(86400초)을 넘을 수 없습니다.")
        Long totalSeconds
) {
}
