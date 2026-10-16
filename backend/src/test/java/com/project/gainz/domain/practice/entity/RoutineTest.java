package com.project.gainz.domain.practice.entity;

import com.project.gainz.domain.practice.enums.RoutineRepeat;
import com.project.gainz.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RoutineTest {

    // 2026-10-12는 월요일
    private static final LocalDate MON = LocalDate.of(2026, 10, 12);

    private Routine routine(RoutineRepeat type, Set<DayOfWeek> days, LocalDate start, LocalDate end) {
        return Routine.builder().user(mock(User.class)).content("루틴").targetCount(1)
                .repeatType(type).repeatDays(days).startDate(start).endDate(end).build();
    }

    @Test
    void 반복_설정을_안_하면_매일이고_예전_루틴처럼_기간_제한이_없다() {
        Routine r = routine(null, null, null, null);

        assertThat(r.getRepeatType()).isEqualTo(RoutineRepeat.DAILY);
        assertThat(r.isScheduledOn(MON.minusYears(1))).isTrue();
        assertThat(r.isScheduledOn(MON.plusYears(1))).isTrue();
    }

    @Test
    void 시작일이_없는_예전_루틴은_만든_날부터_일정이_있다() {
        Routine r = routine(null, null, null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(r, "createdAt", MON.atTime(21, 0));

        assertThat(r.effectiveStartDate()).isEqualTo(MON);
        assertThat(r.isScheduledOn(MON.minusDays(1))).isFalse();
        assertThat(r.isScheduledOn(MON)).isTrue();
    }

    @Test
    void 요일_반복은_고른_요일에만_일정이_있다() {
        Routine r = routine(RoutineRepeat.WEEKLY, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), MON, null);

        assertThat(r.isScheduledOn(MON)).isTrue();
        assertThat(r.isScheduledOn(MON.plusDays(1))).isFalse();
        assertThat(r.isScheduledOn(MON.plusDays(2))).isTrue();
        assertThat(r.getRepeatDaySet()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    }

    @Test
    void 기간_밖은_일정이_없다() {
        Routine r = routine(RoutineRepeat.DAILY, null, MON, MON.plusDays(6));

        assertThat(r.isScheduledOn(MON.minusDays(1))).isFalse();
        assertThat(r.isScheduledOn(MON.plusDays(6))).isTrue();
        assertThat(r.isScheduledOn(MON.plusDays(7))).isFalse();
    }

    @Test
    void 하루만은_시작일_하루만_일정이_있다() {
        Routine r = routine(RoutineRepeat.NONE, null, MON, MON.plusDays(10));

        assertThat(r.getEndDate()).isEqualTo(MON);
        assertThat(r.isScheduledOn(MON)).isTrue();
        assertThat(r.isScheduledOn(MON.plusDays(1))).isFalse();
    }

    @Test
    void 요일_반복이_아니면_요일은_지운다() {
        Routine r = routine(RoutineRepeat.WEEKLY, Set.of(DayOfWeek.MONDAY), MON, null);
        r.changeSchedule(RoutineRepeat.DAILY, Set.of(DayOfWeek.MONDAY), MON, null);

        assertThat(r.getRepeatDays()).isNull();
        assertThat(r.isScheduledOn(MON.plusDays(1))).isTrue();
    }

    @Test
    void 아직_시작하지_않은_루틴을_그만하면_비활성화된다() {
        Routine r = routine(RoutineRepeat.DAILY, null, MON.plusDays(5), null);
        r.stopFrom(MON);

        assertThat(r.getIsActive()).isFalse();
        assertThat(r.isOngoing(MON)).isFalse();
    }

    @Test
    void 이미_끝난_루틴의_종료일은_그만해도_바뀌지_않는다() {
        Routine r = routine(RoutineRepeat.DAILY, null, MON.minusDays(30), MON.minusDays(10));
        r.stopFrom(MON);

        assertThat(r.getEndDate()).isEqualTo(MON.minusDays(10));
    }
}
