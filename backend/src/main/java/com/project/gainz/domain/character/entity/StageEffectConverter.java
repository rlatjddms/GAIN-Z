package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.StageEffect;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class StageEffectConverter extends EnumNameConverter<StageEffect> {

    public StageEffectConverter() {
        super(StageEffect.class);
    }

    // 목록에서 빠진 효과는 없는 것으로
    @Override
    public StageEffect convertToEntityAttribute(String name) {
        try {
            return super.convertToEntityAttribute(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
