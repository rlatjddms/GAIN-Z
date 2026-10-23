package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class HairStyleConverter extends EnumNameConverter<HairStyle> {

    public HairStyleConverter() {
        super(HairStyle.class);
    }
}
