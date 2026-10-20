package com.project.gainz.domain.character.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.gainz.domain.character.enums.DecorItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

// 방 꾸미기 배치 [{ "key": "TURNTABLE", "x": 46, "y": 58 }] ↔ JSON 문자열 (y는 없을 수 있음). 모르는 소품이나 읽지 못한 값은 건너뛴다
@Converter
public class DecorLayoutConverter implements AttributeConverter<List<DecorPlacement>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<DecorPlacement> layout) {
        if (layout == null || layout.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(layout);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<DecorPlacement> convertToEntityAttribute(String json) {
        List<DecorPlacement> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            for (JsonNode node : MAPPER.readTree(json)) {
                try {
                    JsonNode y = node.get("y");
                    out.add(new DecorPlacement(DecorItem.valueOf(node.path("key").asText()), node.path("x").asInt(),
                            y == null || y.isNull() ? null : y.asInt()));
                } catch (IllegalArgumentException ignored) {
                    // 목록에서 빠진 소품
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return out;
    }
}
