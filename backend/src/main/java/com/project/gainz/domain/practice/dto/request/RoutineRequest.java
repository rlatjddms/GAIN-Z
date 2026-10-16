package com.project.gainz.domain.practice.dto.request;

import com.project.gainz.domain.practice.enums.RoutineRepeat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

// 루틴 만들기·고치기 요청. 반복 설정을 생략하면 오늘부터 매일
public record RoutineRequest(

        @NotBlank(message = "루틴 내용을 입력해 주세요.")
        @Size(max = 50, message = "루틴 내용은 50자 이하로 입력해 주세요.")
        String content,

        @NotNull(message = "목표 횟수는 필수 입력 항목입니다.")
        @Min(value = 1, message = "목표 횟수는 1 이상이어야 합니다.")
        @Max(value = 99, message = "목표 횟수는 99 이하이어야 합니다.")
        Integer targetCount,

        RoutineRepeat repeatType,      // NONE(하루만) / DAILY(매일) / WEEKLY(요일 선택)
        List<DayOfWeek> repeatDays,    // WEEKLY일 때 1개 이상 (MONDAY …)
        LocalDate startDate,           // 생략하면 오늘. 하루만이면 그 날짜
        LocalDate endDate              // 생략하면 계속
) {
    public RoutineRepeat repeatTypeOrDaily() {
        return repeatType != null ? repeatType : RoutineRepeat.DAILY;
    }

    public LocalDate startDateOr(LocalDate today) {
        return startDate != null ? startDate : today;
    }
}
