package com.project.gainz.domain.character.entity;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ItemColorsConverterTest {

    private final ItemColorsConverter converter = new ItemColorsConverter();

    @Test
    void 아이템_색을_JSON으로_저장하고_다시_읽는다() {
        String json = converter.convertToDatabaseColumn(Map.of("JEANS", "#212529", "HOODIE", "#ffd8a8"));

        assertThat(json).isEqualTo("{\"HOODIE\":\"#ffd8a8\",\"JEANS\":\"#212529\"}");
        assertThat(converter.convertToEntityAttribute(json)).containsEntry("HOODIE", "#ffd8a8").containsEntry("JEANS", "#212529");
    }

    @Test
    void 비어_있거나_읽지_못하면_빈_표() {
        assertThat(converter.convertToDatabaseColumn(Map.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("not json")).isEmpty();
    }
}
