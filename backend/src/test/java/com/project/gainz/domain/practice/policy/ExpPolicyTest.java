package com.project.gainz.domain.practice.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpPolicyTest {

    @Test
    void 타이머로_연습한_1분에_1_경험치() {
        assertThat(ExpPolicy.dailyExp(600, 0)).isEqualTo(10);
    }

    @Test
    void 일분이_안_되는_자투리는_버린다() {
        assertThat(ExpPolicy.dailyExp(59, 0)).isZero();
        assertThat(ExpPolicy.dailyExp(119, 0)).isEqualTo(1);
    }

    @Test
    void 하루_최대_300_경험치() {
        long tenHours = 10 * 3600;
        assertThat(ExpPolicy.dailyExp(tenHours, 0)).isEqualTo(ExpPolicy.MAX_MINUTES_PER_DAY);
        assertThat(ExpPolicy.dailyExp(290 * 60, 30 * 60)).isEqualTo(300);
    }

    @Test
    void 직접_입력은_하루_30분까지만_친다() {
        assertThat(ExpPolicy.dailyExp(0, 20 * 60)).isEqualTo(20);
        assertThat(ExpPolicy.dailyExp(0, 2 * 3600)).isEqualTo(ExpPolicy.MANUAL_MINUTES_PER_DAY);
        assertThat(ExpPolicy.dailyExp(60 * 60, 2 * 3600)).isEqualTo(90);
    }

    @Test
    void 직접_입력_합이_음수면_타이머_시간에서_뺀다() {
        // 타이머 3시간을 1시간으로 줄임 (직접 입력 -2시간)
        assertThat(ExpPolicy.dailyExp(3 * 3600, -2 * 3600)).isEqualTo(60);
        // 타이머보다 더 줄여도 0 아래로는 없다
        assertThat(ExpPolicy.dailyExp(600, -3600)).isZero();
    }
}
