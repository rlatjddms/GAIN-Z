package com.project.gainz.domain.character.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageBackgroundTest {

    @Test
    void 레벨로_얻은_가장_큰_무대() {
        assertThat(StageBackground.highestUnlockedAt(1)).isEqualTo(StageBackground.ROOKIE);
        assertThat(StageBackground.highestUnlockedAt(10)).isEqualTo(StageBackground.RISING);
        assertThat(StageBackground.highestUnlockedAt(29)).isEqualTo(StageBackground.RISING);
        assertThat(StageBackground.highestUnlockedAt(50)).isEqualTo(StageBackground.LEGEND);
    }

    @Test
    void 필요_레벨에_닿으면_해금된다() {
        assertThat(StageBackground.PRO.isUnlockedAt(29)).isFalse();
        assertThat(StageBackground.PRO.isUnlockedAt(30)).isTrue();
    }
}
