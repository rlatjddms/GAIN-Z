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
    void 다섯_레벨마다_의상이고_그_사이는_소품이나_헤어_하나() {
        for (int level = 2; level <= LevelPolicy.MAX_LEVEL; level++) {
            List<LevelRewards.Type> types = LevelRewards.at(level).stream()
                    .map(LevelRewards.Reward::type).filter(t -> t != LevelRewards.Type.STAGE).toList();
            if (level % 5 == 0) {
                assertThat(types).as("Lv.%d", level).containsExactly(LevelRewards.Type.OUTFIT);
            } else {
                assertThat(types).as("Lv.%d", level).hasSize(1)
                        .first().isIn(LevelRewards.Type.ACCESSORY, LevelRewards.Type.HAIR);
            }
        }
    }

    @Test
    void 무대_배경은_Lv10_20_30에_의상과_함께() {
        assertThat(LevelRewards.at(10)).extracting(LevelRewards.Reward::key).containsExactly("BAND_TEE", "RISING");
        assertThat(LevelRewards.at(30)).extracting(LevelRewards.Reward::key).containsExactly("SEQUIN_JACKET", "LEGEND");
    }

    @Test
    void 사이_레벨의_보상을_모두_모은다() {
        assertThat(LevelRewards.between(3, 5)).extracting(LevelRewards.Reward::level, LevelRewards.Reward::key)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(4, "PONYTAIL"),
                        org.assertj.core.groups.Tuple.tuple(5, "HOODIE"));
        assertThat(LevelRewards.between(30, 30)).isEmpty();
    }

    @Test
    void 처음부터_고를_수_있는_머리는_열_개() {
        assertThat(java.util.Arrays.stream(HairStyle.values()).filter(h -> h.isUnlockedAt(1))).hasSize(10);
    }
}
