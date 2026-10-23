package com.project.gainz.domain.character.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;
import java.util.TreeMap;

// 아이템마다 고른 색 { "HOODIE": "#ffd8a8" } ↔ JSON 문자열. 읽지 못하면 빈 표 (모두 기본 색)
@Converter
public class ItemColorsConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<TreeMap<String, String>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> colors) {
        if (colors == null || colors.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(new TreeMap<>(colors));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new TreeMap<>();
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            return new TreeMap<>();
        }
    }
}
