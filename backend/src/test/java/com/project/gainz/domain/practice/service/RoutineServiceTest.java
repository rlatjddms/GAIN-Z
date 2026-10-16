package com.project.gainz.domain.practice.service;

import com.project.gainz.domain.practice.dto.request.RoutineCheckRequest;
import com.project.gainz.domain.practice.dto.request.RoutineRequest;
import com.project.gainz.domain.practice.dto.response.RoutineResponse;
import com.project.gainz.domain.practice.entity.Routine;
import com.project.gainz.domain.practice.entity.RoutineCheck;
import com.project.gainz.domain.practice.repository.RoutineCheckRepository;
import com.project.gainz.domain.practice.repository.RoutineRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @InjectMocks private RoutineService routineService;

    private Routine routine(Long id, String content, int target) {
        Routine routine = Routine.builder().user(mock(User.class)).content(content).targetCount(target).build();
        ReflectionTestUtils.setField(routine, "id", id);
        return routine;
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
        when(routineRepository.countByUserIdAndIsActiveTrue(USER_ID)).thenReturn(0L);
        when(routineRepository.save(any(Routine.class))).thenAnswer(inv -> inv.getArgument(0));

        RoutineResponse result = routineService.createRoutine(USER_ID, new RoutineRequest("  스케일  ", 10));

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("스케일");
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(result.count()).isZero();
    }

    @Test
    void 루틴은_20개까지만_만들_수_있다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(routineRepository.countByUserIdAndIsActiveTrue(USER_ID)).thenReturn(20L);

        assertThatThrownBy(() -> routineService.createRoutine(USER_ID, new RoutineRequest("스케일", 10)))
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

        RoutineResponse result = routineService.updateRoutine(USER_ID, 10L, new RoutineRequest("스케일", 5));

        assertThat(scale.getTargetCount()).isEqualTo(5);
        assertThat(todayCheck.getIsCompleted()).isTrue();
        assertThat(result.completed()).isTrue();
    }

    @Test
    void 삭제하면_비활성화만_한다() {
        Routine scale = routine(10L, "스케일", 10);
        when(routineRepository.findByIdAndUserIdAndIsActiveTrue(10L, USER_ID)).thenReturn(Optional.of(scale));

        routineService.deleteRoutine(USER_ID, 10L);

        assertThat(scale.getIsActive()).isFalse();
        verify(routineRepository, never()).delete(any());
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
}
