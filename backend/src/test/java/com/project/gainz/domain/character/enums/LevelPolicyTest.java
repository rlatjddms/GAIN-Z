package com.project.gainz.domain.character.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LevelPolicyTest {

    @ParameterizedTest(name = "레벨 {0} → 다음 레벨까지 {1}")
    @CsvSource({
            "1, 70",
            "2, 120",
            "10, 520",
            "29, 1470"
    })
    void 다음_레벨까지_필요_경험치는_50_곱하기_레벨_더하기_20(int level, int expected) {
        assertThat(LevelPolicy.getRequiredExpForLevel(level)).isEqualTo(expected);
    }

    @Test
    void 최대_레벨은_30이고_도달할_수_없는_경험치를_요구한다() {
        assertThat(LevelPolicy.MAX_LEVEL).isEqualTo(30);
        assertThat(LevelPolicy.getRequiredExpForLevel(30)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void 하루_1시간이면_약_40일에_Lv10_1년에_Lv30() {
        assertThat(LevelPolicy.totalExpToReach(10)).isEqualTo(2430);
        assertThat(LevelPolicy.totalExpToReach(20)).isEqualTo(9880);
        assertThat(LevelPolicy.totalExpToReach(30)).isEqualTo(22330);
    }

    @ParameterizedTest(name = "총 {0} EXP → Lv.{1}, 남은 {2}")
    @CsvSource({
            "0, 1, 0",
            "69, 1, 69",
            "70, 2, 0",
            "2500, 10, 70",
            "22329, 29, 1469",
            "22330, 30, 0",
            "999999, 30, 0"
    })
    void 총_경험치로_레벨과_남은_경험치를_구한다(long total, int level, int rest) {
        assertThat(LevelPolicy.levelForTotalExp(total)).containsExactly(level, rest);
    }
}
