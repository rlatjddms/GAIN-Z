package com.project.gainz.domain.character.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class LevelPolicyTest {

    @ParameterizedTest(name = "레벨 {0} 필요 경험치 = {1}")
    @CsvSource({
            "1, 100",
            "9, 100",
            "10, 300",
            "29, 300",
            "30, 1000",
            "49, 1000"
    })
    void 구간별_필요_경험치를_반환한다(int level, int expected) {
        assertThat(LevelPolicy.getRequiredExpForLevel(level)).isEqualTo(expected);
    }

    @org.junit.jupiter.api.Test
    void 만렙은_도달_불가능한_경험치를_요구한다() {
        assertThat(LevelPolicy.getRequiredExpForLevel(50)).isEqualTo(Integer.MAX_VALUE);
    }
}
