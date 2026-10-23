package com.project.gainz.domain.record.entity;

import com.project.gainz.domain.record.enums.RecordType;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class RecordTypeConverter extends EnumNameConverter<RecordType> {

    public RecordTypeConverter() {
        super(RecordType.class);
    }
}
