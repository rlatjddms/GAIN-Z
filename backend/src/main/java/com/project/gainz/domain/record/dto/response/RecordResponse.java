package com.project.gainz.domain.record.dto.response;

import com.project.gainz.domain.record.enums.RecordMetric;
import com.project.gainz.domain.record.enums.RecordRarity;
import com.project.gainz.domain.record.enums.RecordType;

import java.time.LocalDateTime;
import java.util.List;

// 도감의 레코드 한 장. 못 얻은 것도 조건·진행도와 함께 보여 준다
public record RecordResponse(
        String key,
        String name,
        String description,
        String condition,
        RecordRarity rarity,
        String color,
        boolean owned,
        LocalDateTime acquiredAt,
        Progress progress
) {
    public static RecordResponse of(RecordType type, LocalDateTime acquiredAt, long current) {
        return new RecordResponse(type.name(), type.title(), type.description(), type.condition(), type.rarity(),
                type.color(), acquiredAt != null, acquiredAt,
                new Progress(Math.min(current, type.goal()), type.goal(), type.metric().unit()));
    }

    public record Progress(long current, long goal, RecordMetric.Unit unit) {
    }

    // 도감 전체 (도감 순서)
    public record Collection(long ownedCount, int total, List<RecordResponse> records) {
    }
}
