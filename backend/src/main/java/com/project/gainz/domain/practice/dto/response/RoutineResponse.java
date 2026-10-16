package com.project.gainz.domain.practice.dto.response;

import com.project.gainz.domain.practice.entity.Routine;
import com.project.gainz.domain.practice.entity.RoutineCheck;
import com.project.gainz.domain.practice.enums.RoutineRepeat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

// 루틴 + 반복 설정 + 해당 날짜 체크 현황 (체크가 없으면 0회)
public record RoutineResponse(
        Long id,
        String content,
        Integer targetCount,
        RoutineRepeat repeatType,
        List<DayOfWeek> repeatDays,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate date,
        Integer count,
        Boolean completed
) {
    public static RoutineResponse of(Routine routine, LocalDate date, RoutineCheck check) {
        int count = check != null ? check.getPracticeCount() : 0;
        boolean completed = check != null && check.getIsCompleted();
        return new RoutineResponse(
                routine.getId(), routine.getContent(), routine.getTargetCount(),
                routine.getRepeatType(), List.copyOf(routine.getRepeatDaySet()), routine.effectiveStartDate(), routine.getEndDate(),
                date, count, completed);
    }
}
