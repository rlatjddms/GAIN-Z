package com.project.gainz.domain.character.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LevelRewardsTest {

    @Test
    void Lv2부터_최대_레벨까지_매_레벨_보상이_있다() {
        for (int level = 2; level <= LevelPolicy.MAX_LEVEL; level++) {
            assertThat(LevelRewards.at(level)).as("Lv.%d", level).isNotEmpty();
        }
        assertThat(LevelRewards.at(1)).isEmpty();
    }

    @Test
    void 다섯_레벨마다_상의_하의_한_벌이고_그_사이는_소품이나_헤어_하나() {
        for (int level = 2; level <= LevelPolicy.MAX_LEVEL; level++) {
            List<LevelRewards.Type> types = LevelRewards.at(level).stream()
                    .map(LevelRewards.Reward::type)
                    .filter(t -> t != LevelRewards.Type.STAGE && t != LevelRewards.Type.EFFECT).toList();
            if (level % 5 == 0) {
                assertThat(types).as("Lv.%d", level).containsExactly(LevelRewards.Type.OUTFIT, LevelRewards.Type.BOTTOM);
            } else {
                assertThat(types).as("Lv.%d", level).hasSize(1)
                        .first().isIn(LevelRewards.Type.ACCESSORY, LevelRewards.Type.HAIR);
            }
        }
    }

    @Test
    void 다섯_레벨마다_무대_배경이나_효과가_함께() {
        assertThat(LevelRewards.at(5)).extracting(LevelRewards.Reward::key).containsExactly("HOODIE", "JOGGERS", "SPARKLE");
        assertThat(LevelRewards.at(10)).extracting(LevelRewards.Reward::key).containsExactly("BAND_TEE", "RIPPED_JEANS", "RISING");
        assertThat(LevelRewards.at(15)).extracting(LevelRewards.Reward::key).containsExactly("DENIM_JACKET", "CARGO_PANTS", "SPOTLIGHT");
        assertThat(LevelRewards.at(30)).extracting(LevelRewards.Reward::key)
                .containsExactly("SEQUIN_JACKET", "SEQUIN_PANTS", "LEGEND", "FIREWORKS");
        for (int level = 5; level <= LevelPolicy.MAX_LEVEL; level += 5) {
            assertThat(LevelRewards.at(level)).as("Lv.%d", level).extracting(LevelRewards.Reward::type)
                    .containsAnyOf(LevelRewards.Type.STAGE, LevelRewards.Type.EFFECT);
        }
    }

    @Test
    void 사이_레벨의_보상을_모두_모은다() {
        assertThat(LevelRewards.between(3, 5)).extracting(LevelRewards.Reward::level, LevelRewards.Reward::key)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(4, "PONYTAIL"),
                        org.assertj.core.groups.Tuple.tuple(5, "HOODIE"),
                        org.assertj.core.groups.Tuple.tuple(5, "JOGGERS"),
                        org.assertj.core.groups.Tuple.tuple(5, "SPARKLE"));
        assertThat(LevelRewards.between(30, 30)).isEmpty();
    }

    @Test
    void 처음부터_고를_수_있는_머리는_열_개() {
        assertThat(java.util.Arrays.stream(HairStyle.values()).filter(h -> h.isUnlockedAt(1))).hasSize(10);
    }
}
