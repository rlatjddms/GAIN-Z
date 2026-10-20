package com.project.gainz.domain.character.entity;

import com.project.gainz.domain.character.enums.DecorItem;
import com.project.gainz.domain.character.enums.StageEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecorConvertersTest {

    @Test
    void 소품_배치를_JSON으로_저장하고_모르는_소품은_건너뛴다() {
        DecorLayoutConverter converter = new DecorLayoutConverter();
        String json = converter.convertToDatabaseColumn(List.of(new DecorPlacement(DecorItem.TURNTABLE, 46)));

        assertThat(json).isEqualTo("[{\"key\":\"TURNTABLE\",\"x\":46}]");
        assertThat(converter.convertToEntityAttribute("[{\"key\":\"TURNTABLE\",\"x\":46},{\"key\":\"GONE\",\"x\":1}]"))
                .containsExactly(new DecorPlacement(DecorItem.TURNTABLE, 46));
        assertThat(converter.convertToEntityAttribute("broken")).isEmpty();
    }

    @Test
    void 무대_효과_목록() {
        StageEffectsConverter converter = new StageEffectsConverter();
        assertThat(converter.convertToDatabaseColumn(List.of(StageEffect.SPARKLE, StageEffect.SMOKE))).isEqualTo("SPARKLE,SMOKE");
        assertThat(converter.convertToEntityAttribute("SPARKLE,GONE,SMOKE")).containsExactly(StageEffect.SPARKLE, StageEffect.SMOKE);
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
    }
}
