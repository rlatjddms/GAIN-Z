package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class StageBackgroundConverter extends EnumNameConverter<StageBackground> {

    public StageBackgroundConverter() {
        super(StageBackground.class);
    }

    // 목록에서 빠진 값은 없는 것으로
    @Override
    public StageBackground convertToEntityAttribute(String name) {
        try {
            return super.convertToEntityAttribute(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
