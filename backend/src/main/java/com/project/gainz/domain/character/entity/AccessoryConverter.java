package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class AccessoryConverter extends EnumNameConverter<Accessory> {

    public AccessoryConverter() {
        super(Accessory.class);
    }

    // 목록에서 빠진 소품을 쓰고 있던 캐릭터는 그 자리를 비운다
    @Override
    public Accessory convertToEntityAttribute(String name) {
        try {
            return super.convertToEntityAttribute(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
