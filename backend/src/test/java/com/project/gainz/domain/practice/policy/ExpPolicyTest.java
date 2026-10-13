package com.project.gainz.domain.practice.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpPolicyTest {

    @Test
    void 연습_1분당_1exp를_지급한다() {
        assertThat(ExpPolicy.calculate(600, 0.0)).isEqualTo(10);
    }

    @Test
    void 일분_미만은_버린다() {
        assertThat(ExpPolicy.calculate(59, 0.0)).isZero();
        assertThat(ExpPolicy.calculate(119, 0.0)).isEqualTo(1);
    }

    @Test
    void 달성률_100퍼센트는_최대_보너스를_지급한다() {
        assertThat(ExpPolicy.calculate(0, 1.0)).isEqualTo(ExpPolicy.MAX_ACHIEVEMENT_BONUS);
    }

    @Test
    void 달성률_보너스는_비례해서_지급한다() {
        assertThat(ExpPolicy.calculate(0, 0.5)).isEqualTo(25);
    }

    @Test
    void 한_번에_인정되는_연습_시간은_상한이_있다() {
        long tenHours = 10 * 60 * 60;
        assertThat(ExpPolicy.calculate(tenHours, 0.0)).isEqualTo(ExpPolicy.MAX_MINUTES_PER_LOG);
    }

    @Test
    void 범위를_벗어난_달성률은_보정한다() {
        assertThat(ExpPolicy.calculate(0, 1.5)).isEqualTo(ExpPolicy.MAX_ACHIEVEMENT_BONUS);
        assertThat(ExpPolicy.calculate(0, -1.0)).isZero();
    }

    @Test
    void 연습_시간과_보너스를_합산한다() {
        assertThat(ExpPolicy.calculate(30 * 60, 1.0)).isEqualTo(80);
    }
}
