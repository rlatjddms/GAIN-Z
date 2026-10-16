package com.project.gainz.domain.practice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 루틴 만들기·고치기 요청
public record RoutineRequest(

        @NotBlank(message = "루틴 내용을 입력해 주세요.")
        @Size(max = 50, message = "루틴 내용은 50자 이하로 입력해 주세요.")
        String content,

        @NotNull(message = "목표 횟수는 필수 입력 항목입니다.")
        @Min(value = 1, message = "목표 횟수는 1 이상이어야 합니다.")
        @Max(value = 99, message = "목표 횟수는 99 이하이어야 합니다.")
        Integer targetCount
) {
}
