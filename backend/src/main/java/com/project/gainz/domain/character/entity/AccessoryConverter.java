package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class AccessoryConverter extends EnumNameConverter<Accessory> {

    public AccessoryConverter() {
        super(Accessory.class);
    }
}
