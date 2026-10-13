package com.project.gainz.domain.practice.dto.response;

import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.practice.entity.PracticeLog;

import java.time.LocalDate;

public record PracticeLogResponse(

        Long id,
        Long durationSeconds,
        Double achievementRate,
        LocalDate practiceDate,
        Integer expGained,
        ExperienceResult experience
) {
    public static PracticeLogResponse of(PracticeLog practiceLog, ExperienceResult experience) {
        return new PracticeLogResponse(
                practiceLog.getId(),
                practiceLog.getDurationSeconds(),
                practiceLog.getAchievementRate(),
                practiceLog.getCreatedDate(),
                practiceLog.getExpGained(),
                experience
        );
    }
}
