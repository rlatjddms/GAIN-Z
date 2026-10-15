package com.project.gainz.domain.practice.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpPolicyTest {

    @Test
    void 하루_연습_1분당_1exp() {
        assertThat(ExpPolicy.dailyExp(600)).isEqualTo(10);
    }

    @Test
    void 일분_미만은_버린다() {
        assertThat(ExpPolicy.dailyExp(59)).isZero();
        assertThat(ExpPolicy.dailyExp(119)).isEqualTo(1);
    }

    @Test
    void 하루에_인정되는_연습_시간은_상한이_있다() {
        long tenHours = 10 * 60 * 60;
        assertThat(ExpPolicy.dailyExp(tenHours)).isEqualTo(ExpPolicy.MAX_MINUTES_PER_DAY);
    }

    @Test
    void 이미_받은_경험치를_빼고_지급한다() {
        // 하루 30분 → 30exp 중 이미 20exp를 받았으면 10exp
        assertThat(ExpPolicy.expToGrant(30 * 60, 20)).isEqualTo(10);
    }

    @Test
    void 짧게_나눠_기록해도_하루_합계로_계산한다() {
        // 40초 + 40초: 첫 구간 0exp, 합계 80초가 되면 1exp
        int first = ExpPolicy.expToGrant(40, 0);
        int second = ExpPolicy.expToGrant(80, first);
        assertThat(first + second).isEqualTo(1);
    }

    @Test
    void 시간이_줄면_지급하지_않는다() {
        assertThat(ExpPolicy.expToGrant(10 * 60, 30)).isZero();
    }

    @Test
    void 음수_시간은_0분으로_본다() {
        assertThat(ExpPolicy.toMinutes(-120)).isZero();
    }
}
