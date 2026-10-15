package com.project.gainz.domain.practice.dto.response;

import com.project.gainz.domain.character.dto.response.ExperienceResult;

import java.time.LocalDate;

// 연습 기록 저장·수정 결과. 시간이 바뀌지 않았으면 experience는 null
public record PracticeRecordResponse(
        LocalDate practiceDate,
        Long dayTotalSeconds,
        Integer expGained,
        ExperienceResult experience
) {
}
