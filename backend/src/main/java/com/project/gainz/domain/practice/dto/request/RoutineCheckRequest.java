package com.project.gainz.domain.practice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 그날 수행 횟수 정하기 (+/- 버튼이 바뀐 횟수를 그대로 보낸다)
public record RoutineCheckRequest(

        @NotNull(message = "수행 횟수는 필수 입력 항목입니다.")
        @Min(value = 0, message = "수행 횟수는 0 이상이어야 합니다.")
        @Max(value = 999, message = "수행 횟수는 999 이하이어야 합니다.")
        Integer count
) {
}
