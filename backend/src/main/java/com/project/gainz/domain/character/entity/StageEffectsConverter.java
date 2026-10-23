package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.StageEffect;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

// 켜 둔 무대 효과 목록 ↔ "SPARKLE,SMOKE". 모르는 효과는 건너뛴다
@Converter
public class StageEffectsConverter implements AttributeConverter<List<StageEffect>, String> {

    @Override
    public String convertToDatabaseColumn(List<StageEffect> effects) {
        if (effects == null || effects.isEmpty()) return null;
        return String.join(",", effects.stream().map(Enum::name).toList());
    }

    @Override
    public List<StageEffect> convertToEntityAttribute(String value) {
        List<StageEffect> out = new ArrayList<>();
        if (value == null || value.isBlank()) return out;
        for (String name : value.split(",")) {
            try {
                out.add(StageEffect.valueOf(name.trim()));
            } catch (IllegalArgumentException ignored) {
                // 목록에서 빠진 효과
            }
        }
        return out;
    }
}
