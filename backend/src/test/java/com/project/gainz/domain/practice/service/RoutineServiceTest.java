package com.project.gainz.domain.practice.service;

import com.project.gainz.domain.practice.dto.request.RoutineCheckRequest;
import com.project.gainz.domain.practice.dto.request.RoutineRequest;
import com.project.gainz.domain.practice.dto.response.RoutineDailySummaryResponse;
import com.project.gainz.domain.practice.dto.response.RoutineResponse;
import com.project.gainz.domain.practice.entity.Routine;
import com.project.gainz.domain.practice.entity.RoutineCheck;
import com.project.gainz.domain.practice.enums.RoutineRepeat;
import com.project.gainz.domain.practice.repository.RoutineCheckRepository;
import com.project.gainz.domain.practice.repository.RoutineRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import com.project.gainz.domain.record.enums.RecordMetric;
import com.project.gainz.domain.record.event.RecordCheckEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now();

    @Mock private RoutineRepository routineRepository;
    @Mock private RoutineCheckRepository routineCheckRepository;
    @Mock private UserRepository userRepository;

    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private RoutineService routineService;

    private Routine routine(Long id, String content, int target) {
        Routine routine = Routine.builder().user(mock(User.class)).content(content).targetCount(target).build();
        ReflectionTestUtils.setField(routine, "id", id);
        return routine;
    }

    private RoutineRequest req(String content, int target) {
        return new RoutineRequest(content, target, null, null, null, null);
    }

    private RoutineCheck check(Routine routine, LocalDate date, int count) {
        RoutineCheck check = RoutineCheck.builder().routine(routine).practiceDate(date).build();
        check.updateCount(count);
        return check;
    }

    @Test
    void 루틴_목록에_그날_체크_현황을_붙여_준다() {
        Routine scale = routine(10L, "스케일", 10);
        Routine chord = routine(11L, "코드 전환", 5);
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of(scale, chord));
        when(routineCheckRepository.findAllByRoutineIdInAndPracticeDate(List.of(10L, 11L), TODAY))
                .thenReturn(List.of(check(chord, TODAY, 5)));

        List<RoutineResponse> result = routineService.getRoutines(USER_ID, TODAY);

        assertThat(result).extracting(RoutineResponse::count).containsExactly(0, 5);
        assertThat(result).extracting(RoutineResponse::completed).containsExactly(false, true);
    }

    @Test
    void 루틴이_없으면_체크를_조회하지_않는다() {
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of());

        assertThat(routineService.getRoutines(USER_ID, TODAY)).isEmpty();
        verify(routineCheckRepository, never()).findAllByRoutineIdInAndPracticeDate(any(), any());
    }

    @Test
    void 루틴을_만들면_내용_앞뒤_공백을_지운다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of());
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse result = routineService.createRoutine(USER_ID, req("  스케일  ", 10));

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("스케일");
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(result.count()).isZero();
    }

    @Test
    void 루틴은_20개까지만_만들_수_있다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        List<Routine> twenty = java.util.stream.LongStream.rangeClosed(1, 20).mapToObj(i -> routine(i, "루틴" + i, 1)).toList();
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(twenty);

        assertThatThrownBy(() -> routineService.createRoutine(USER_ID, req("스케일", 10)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTINE_LIMIT_EXCEEDED);
        verify(routineRepository, never()).save(any());
    }

    @Test
    void 목표_횟수를_바꾸면_오늘_완료_여부를_다시_계산한다() {
        Routine scale = routine(10L, "스케일", 10);
        RoutineCheck todayCheck = check(scale, TODAY, 6);
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));
        when(routineCheckRepository.findByRoutineIdAndPracticeDate(10L, TODAY)).thenReturn(Optional.of(todayCheck));

        RoutineResponse result = routineService.updateRoutine(USER_ID, 10L, req("스케일", 5));

        assertThat(scale.getTargetCount()).isEqualTo(5);
        assertThat(todayCheck.getIsCompleted()).isTrue();
        assertThat(result.completed()).isTrue();
    }

    @Test
    void 지난_기록이_있는_루틴을_삭제하면_종료일을_어제로_정한다() {
        Routine scale = routine(10L, "스케일", 10);   // 시작일 없음 = 예전부터 하던 루틴
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));

        routineService.deleteRoutine(USER_ID, 10L);

        assertThat(scale.getEndDate()).isEqualTo(TODAY.minusDays(1));
        assertThat(scale.isScheduledOn(TODAY.minusDays(1))).isTrue();
        assertThat(scale.isScheduledOn(TODAY)).isFalse();
        verify(routineRepository, never()).delete(any());
    }

    @Test
    void 오늘_만든_루틴을_삭제하면_비활성화한다() {
        Routine scale = Routine.builder().user(mock(User.class)).content("스케일").targetCount(10).startDate(TODAY).build();
        ReflectionTestUtils.setField(scale, "id", 10L);
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));

        routineService.deleteRoutine(USER_ID, 10L);

        assertThat(scale.getIsActive()).isFalse();
    }

    @Test
    void 다른_사람_루틴이나_없는_루틴은_찾을_수_없다() {
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.deleteRoutine(USER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTINE_NOT_FOUND);
    }

    @Test
    void 처음_체크하면_체크를_만들고_횟수를_정한다() {
        Routine scale = routine(10L, "스케일", 10);
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));
        when(routineCheckRepository.findByRoutineIdAndPracticeDate(10L, TODAY)).thenReturn(Optional.empty());
        when(routineCheckRepository.save(any(RoutineCheck.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse result = routineService.checkRoutine(USER_ID, 10L, TODAY, new RoutineCheckRequest(3));

        assertThat(result.count()).isEqualTo(3);
        assertThat(result.completed()).isFalse();
        verify(eventPublisher).publishEvent(RecordCheckEvent.of(USER_ID, RecordMetric.ROUTINE_DAYS));
    }

    @Test
    void 목표_횟수에_닿으면_완료된다() {
        Routine scale = routine(10L, "스케일", 10);
        RoutineCheck existing = check(scale, TODAY, 9);
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));
        when(routineCheckRepository.findByRoutineIdAndPracticeDate(10L, TODAY)).thenReturn(Optional.of(existing));

        RoutineResponse result = routineService.checkRoutine(USER_ID, 10L, TODAY, new RoutineCheckRequest(10));

        assertThat(result.completed()).isTrue();
        verify(routineCheckRepository, never()).save(any());
    }

    @Test
    void 체크를_줄이면_완료가_풀린다() {
        Routine scale = routine(10L, "스케일", 10);
        RoutineCheck existing = check(scale, TODAY, 10);
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));
        when(routineCheckRepository.findByRoutineIdAndPracticeDate(10L, TODAY)).thenReturn(Optional.of(existing));

        RoutineResponse result = routineService.checkRoutine(USER_ID, 10L, TODAY, new RoutineCheckRequest(9));

        assertThat(result.completed()).isFalse();
    }

    @Test
    void 일주일보다_오래된_날짜나_미래는_체크할_수_없다() {
        assertThatThrownBy(() -> routineService.checkRoutine(USER_ID, 10L, TODAY.minusDays(7), new RoutineCheckRequest(1)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTINE_CHECK_PERIOD_EXPIRED);
        assertThatThrownBy(() -> routineService.checkRoutine(USER_ID, 10L, TODAY.plusDays(1), new RoutineCheckRequest(1)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTINE_CHECK_PERIOD_EXPIRED);
    }

    // ---------- 반복 설정 ----------

    private Routine weekly(Long id, DayOfWeek... days) {
        Routine routine = Routine.builder().user(mock(User.class)).content("요일 루틴").targetCount(1)
                .repeatType(RoutineRepeat.WEEKLY).repeatDays(Set.of(days)).startDate(TODAY.minusDays(30)).build();
        ReflectionTestUtils.setField(routine, "id", id);
        return routine;
    }

    @Test
    void 그날_일정인_루틴만_목록에_나온다() {
        Routine daily = routine(10L, "매일", 3);
        Routine notToday = weekly(11L, TODAY.plusDays(1).getDayOfWeek());
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of(daily, notToday));
        when(routineCheckRepository.findAllByRoutineIdInAndPracticeDate(List.of(10L), TODAY)).thenReturn(List.of());

        assertThat(routineService.getRoutines(USER_ID, TODAY)).extracting(RoutineResponse::id).containsExactly(10L);
    }

    @Test
    void 요일_반복을_고르면_요일과_기간이_저장된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of());
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse result = routineService.createRoutine(USER_ID, new RoutineRequest("스케일", 10,
                RoutineRepeat.WEEKLY, List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), TODAY, TODAY.plusDays(30)));

        assertThat(result.repeatType()).isEqualTo(RoutineRepeat.WEEKLY);
        assertThat(result.repeatDays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(result.startDate()).isEqualTo(TODAY);
        assertThat(result.endDate()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void 하루만이면_시작일_하루가_기간이다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of());
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse result = routineService.createRoutine(USER_ID,
                new RoutineRequest("공연 리허설", 1, RoutineRepeat.NONE, null, TODAY.plusDays(3), null));

        assertThat(result.startDate()).isEqualTo(TODAY.plusDays(3));
        assertThat(result.endDate()).isEqualTo(TODAY.plusDays(3));
    }

    @Test
    void 요일_반복인데_요일이_없거나_종료일이_시작일보다_앞이면_예외() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> routineService.createRoutine(USER_ID,
                new RoutineRequest("스케일", 10, RoutineRepeat.WEEKLY, List.of(), null, null)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROUTINE_SCHEDULE);
        assertThatThrownBy(() -> routineService.createRoutine(USER_ID,
                new RoutineRequest("스케일", 10, RoutineRepeat.DAILY, null, TODAY, TODAY.minusDays(1))))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROUTINE_SCHEDULE);
        verify(routineRepository, never()).save(any());
    }

    @Test
    void 종료된_루틴은_개수_제한에_세지_않는다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        List<Routine> ended = LongStream.rangeClosed(1, 20).mapToObj(i -> {
            Routine r = routine(i, "지난 루틴" + i, 1);
            r.stopFrom(TODAY);
            return r;
        }).toList();
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(ended);
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(routineService.createRoutine(USER_ID, req("새 루틴", 1)).content()).isEqualTo("새 루틴");
    }

    @Test
    void 일정이_없는_날은_체크할_수_없다() {
        Routine notToday = weekly(11L, TODAY.plusDays(1).getDayOfWeek());
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(11L, USER_ID)).thenReturn(Optional.of(notToday));

        assertThatThrownBy(() -> routineService.checkRoutine(USER_ID, 11L, TODAY, new RoutineCheckRequest(1)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUTINE_NOT_SCHEDULED);
        verify(routineCheckRepository, never()).save(any());
    }

    @Test
    void 날짜별로_일정_루틴_수와_완료_수를_센다() {
        Routine daily = routine(10L, "매일", 3);
        Routine todayOnly = weekly(11L, TODAY.getDayOfWeek());
        when(routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(USER_ID)).thenReturn(List.of(daily, todayOnly));
        LocalDate yesterday = TODAY.minusDays(1);
        when(routineCheckRepository.findAllByRoutineIdInAndPracticeDateBetween(List.of(10L, 11L), yesterday, TODAY))
                .thenReturn(List.of(check(daily, yesterday, 3), check(daily, TODAY, 1), check(todayOnly, TODAY, 1)));

        List<RoutineDailySummaryResponse> result = routineService.getDailySummary(USER_ID, yesterday, TODAY);

        // 어제: 매일 루틴 1개(완료), 오늘: 매일 + 오늘 요일 루틴 2개 중 1개 완료
        assertThat(result).containsExactly(
                new RoutineDailySummaryResponse(yesterday, 1, 1),
                new RoutineDailySummaryResponse(TODAY, 2, 1));
    }

    @Test
    void 요약_기간이_뒤집히거나_1년을_넘으면_예외() {
        assertThatThrownBy(() -> routineService.getDailySummary(USER_ID, TODAY, TODAY.minusDays(1)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRACTICE_PERIOD);
        assertThatThrownBy(() -> routineService.getDailySummary(USER_ID, TODAY.minusDays(400), TODAY))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRACTICE_PERIOD);
    }
}
