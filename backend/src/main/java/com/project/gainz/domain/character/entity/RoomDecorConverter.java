package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.RoomDecor;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class RoomDecorConverter extends EnumNameConverter<RoomDecor> {

    public RoomDecorConverter() {
        super(RoomDecor.class);
    }

    // 목록에서 빠진 값은 없는 것으로
    @Override
    public RoomDecor convertToEntityAttribute(String name) {
        try {
            return super.convertToEntityAttribute(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
