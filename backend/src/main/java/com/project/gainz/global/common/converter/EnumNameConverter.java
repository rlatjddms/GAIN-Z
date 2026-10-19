package com.project.gainz.global.common.converter;

import jakarta.persistence.AttributeConverter;

// enum을 이름 그대로 varchar에 저장한다.
// @Enumerated(STRING)은 Hibernate가 테이블을 만들 때 "그때의 값만 허용"하는 CHECK 제약을 붙여서,
// 나중에 값을 추가하면 저장이 막힌다. 값이 자주 늘어나는 컬럼(알림 종류·설정, 레코드)은 이 변환기를 쓴다
public abstract class EnumNameConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected EnumNameConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.name();
    }

    @Override
    public E convertToEntityAttribute(String name) {
        return name == null ? null : Enum.valueOf(type, name);
    }
}
