package com.project.gainz.domain.practice.service;

import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.service.CharacterService;
import com.project.gainz.domain.practice.dto.request.DailyPracticeUpdateRequest;
import com.project.gainz.domain.practice.dto.request.PracticeLogCreateRequest;
import com.project.gainz.domain.practice.dto.response.DailyPracticeResponse;
import com.project.gainz.domain.practice.dto.response.PracticeDaySplit;
import com.project.gainz.domain.practice.dto.response.PracticeRecordResponse;
import com.project.gainz.domain.practice.entity.PracticeLog;
import com.project.gainz.domain.practice.enums.PracticeSource;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeLogServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now();

    @Mock private PracticeLogRepository practiceLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterService characterService;

    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private PracticeLogService practiceLogService;

    private void givenUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
    }

    private void givenDaily(LocalDate date, long totalSeconds, long expGained) {
        List<DailyPracticeResponse> rows = totalSeconds == 0 && expGained == 0
                ? List.of()
                : List.of(new DailyPracticeResponse(date, totalSeconds, expGained));
        when(practiceLogRepository.findDailyTotals(USER_ID, date, date)).thenReturn(rows);
    }

    // 그날 타이머 시간·직접 입력 합(음수 가능)·지금까지 받은 경험치
    private void givenSplit(LocalDate date, long timerSeconds, long manualSeconds, long expGained) {
        when(practiceLogRepository.findDaySplit(USER_ID, date)).thenReturn(new PracticeDaySplit(timerSeconds, manualSeconds, expGained));
    }

    private PracticeLog savedLog() {
        ArgumentCaptor<PracticeLog> captor = ArgumentCaptor.forClass(PracticeLog.class);
        verify(practiceLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void 유저가_없으면_예외가_발생한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practiceLogService.recordSession(USER_ID, new PracticeLogCreateRequest(600L, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        verify(practiceLogRepository, never()).save(any());
    }

    @Test
    void 타이머_구간을_그날_기록에_더하고_늘어난_만큼_경험치를_지급한다() {
        givenUser();
        // 이미 20분 연습해서 20exp를 받은 날에 10분을 더하면 10exp
        givenDaily(TODAY, 20 * 60, 20);
        givenSplit(TODAY, 20 * 60, 0, 20);
        ExperienceResult experience = new ExperienceResult(1, 1, 30, false, List.of());
        when(characterService.processExperienceAndUnlocks(USER_ID, 10, 10)).thenReturn(experience);

        PracticeRecordResponse response = practiceLogService.recordSession(USER_ID, new PracticeLogCreateRequest(10 * 60L, null));

        PracticeLog saved = savedLog();
        assertThat(saved.getDurationSeconds()).isEqualTo(600L);
        assertThat(saved.getExpGained()).isEqualTo(10);
        assertThat(saved.getCreatedDate()).isEqualTo(TODAY);
        assertThat(saved.getSource()).isEqualTo(PracticeSource.TIMER);

        assertThat(response.dayTotalSeconds()).isEqualTo(30 * 60L);
        assertThat(response.expGained()).isEqualTo(10);
        assertThat(response.experience()).isSameAs(experience);
        // 연습 레코드(연속·총 시간) 판정 요청
        verify(eventPublisher).publishEvent(RecordCheckEvent.of(USER_ID, RecordMetric.STREAK_DAYS, RecordMetric.PRACTICE_HOURS));
    }

    @Test
    void 지정한_날짜에_기록한다() {
        givenUser();
        LocalDate yesterday = TODAY.minusDays(1);
        givenDaily(yesterday, 0, 0);
        givenSplit(yesterday, 0, 0, 0);
        when(characterService.processExperienceAndUnlocks(USER_ID, 1, 1))
                .thenReturn(new ExperienceResult(1, 1, 1, false, List.of()));

        PracticeRecordResponse response = practiceLogService.recordSession(USER_ID, new PracticeLogCreateRequest(60L, yesterday));

        assertThat(response.practiceDate()).isEqualTo(yesterday);
        assertThat(savedLog().getCreatedDate()).isEqualTo(yesterday);
    }

    @Test
    void 하루_합계가_24시간을_넘으면_예외가_발생한다() {
        givenUser();
        givenDaily(TODAY, 86_000, 300);

        assertThatThrownBy(() -> practiceLogService.recordSession(USER_ID, new PracticeLogCreateRequest(600L, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRACTICE_DAY_LIMIT_EXCEEDED);
        verify(practiceLogRepository, never()).save(any());
    }

    @Test
    void 직접_입력은_하루_30분까지만_경험치로_친다() {
        givenUser();
        // 타이머 10분(10exp) 날에 직접 50분을 더하면 30분만 쳐서 +30exp
        givenDaily(TODAY, 10 * 60, 10);
        givenSplit(TODAY, 10 * 60, 0, 10);
        when(characterService.processExperienceAndUnlocks(USER_ID, 30, 50))
                .thenReturn(new ExperienceResult(1, 1, 40, false, List.of()));

        PracticeRecordResponse response = practiceLogService.updateDailyTotal(USER_ID, TODAY, new DailyPracticeUpdateRequest(60 * 60L));

        PracticeLog saved = savedLog();
        assertThat(saved.getDurationSeconds()).isEqualTo(50 * 60L);
        assertThat(saved.getExpGained()).isEqualTo(30);
        assertThat(saved.getSource()).isEqualTo(PracticeSource.MANUAL);
        assertThat(response.dayTotalSeconds()).isEqualTo(60 * 60L);
        assertThat(response.expGained()).isEqualTo(30);
    }

    @Test
    void 줄이면_직접_입력분부터_빼고_줄어든_만큼_경험치를_되돌린다() {
        givenUser();
        // 타이머 60분 + 직접 30분 = 90exp 날을 70분으로 → 직접 입력 10분만 남아 70exp, -20
        givenDaily(TODAY, 90 * 60, 90);
        givenSplit(TODAY, 60 * 60, 30 * 60, 90);
        when(characterService.processExperienceAndUnlocks(USER_ID, -20, -20))
                .thenReturn(new ExperienceResult(3, 3, -5, false, List.of()));

        PracticeRecordResponse response = practiceLogService.updateDailyTotal(USER_ID, TODAY, new DailyPracticeUpdateRequest(70 * 60L));

        assertThat(savedLog().getExpGained()).isEqualTo(-20);
        assertThat(response.expGained()).isEqualTo(-20);
    }

    @Test
    void 직접_입력분보다_많이_줄이면_타이머_기록에서_빼고_되돌린다() {
        givenUser();
        // 타이머를 끄는 걸 잊어 3시간(180exp)이 된 날을 1시간으로 → 120 되돌림
        givenDaily(TODAY, 180 * 60, 180);
        givenSplit(TODAY, 180 * 60, 0, 180);
        when(characterService.processExperienceAndUnlocks(USER_ID, -120, -120))
                .thenReturn(new ExperienceResult(4, 4, -40, false, List.of()));

        practiceLogService.updateDailyTotal(USER_ID, TODAY, new DailyPracticeUpdateRequest(60 * 60L));

        assertThat(savedLog().getExpGained()).isEqualTo(-120);
    }

    @Test
    void 줄였다가_되돌리면_되돌린_만큼_다시_받는다() {
        givenUser();
        // 타이머 60분을 30분으로 줄여 30exp만 남은 날을 다시 60분으로 → +30 (두 번 받는 것은 아님)
        givenDaily(TODAY, 30 * 60, 30);
        givenSplit(TODAY, 60 * 60, -30 * 60, 30);
        when(characterService.processExperienceAndUnlocks(USER_ID, 30, 30))
                .thenReturn(new ExperienceResult(1, 1, 60, false, List.of()));

        practiceLogService.updateDailyTotal(USER_ID, TODAY, new DailyPracticeUpdateRequest(60 * 60L));

        assertThat(savedLog().getExpGained()).isEqualTo(30);
    }

    @Test
    void 타이머로_더할_때는_예전_규칙으로_더_받은_경험치를_되돌리지_않는다() {
        givenUser();
        // 예전 규칙으로 직접 입력 2시간에 120exp를 받은 날에 타이머 10분 → 새 규칙으론 40exp지만 회수하지 않음
        givenDaily(TODAY, 120 * 60, 120);
        givenSplit(TODAY, 0, 120 * 60, 120);
        when(characterService.processExperienceAndUnlocks(USER_ID, 0, 10))
                .thenReturn(new ExperienceResult(2, 2, 10, false, List.of()));

        practiceLogService.recordSession(USER_ID, new PracticeLogCreateRequest(10 * 60L, null));

        assertThat(savedLog().getExpGained()).isZero();
    }

    @Test
    void 시간이_같으면_아무것도_저장하지_않는다() {
        givenUser();
        givenDaily(TODAY, 30 * 60, 30);

        PracticeRecordResponse response = practiceLogService.updateDailyTotal(USER_ID, TODAY, new DailyPracticeUpdateRequest(30 * 60L));

        verify(practiceLogRepository, never()).save(any());
        verify(characterService, never()).processExperienceAndUnlocks(any(), anyInt(), anyLong());
        assertThat(response.experience()).isNull();
    }

    @Test
    void 일주일보다_오래된_날짜는_수정할_수_없다() {
        assertThatThrownBy(() -> practiceLogService.updateDailyTotal(USER_ID, TODAY.minusDays(7), new DailyPracticeUpdateRequest(60L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRACTICE_EDIT_PERIOD_EXPIRED);
    }

    @Test
    void 미래_날짜는_수정할_수_없다() {
        assertThatThrownBy(() -> practiceLogService.updateDailyTotal(USER_ID, TODAY.plusDays(1), new DailyPracticeUpdateRequest(60L)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRACTICE_EDIT_PERIOD_EXPIRED);
    }

    @Test
    void 기간별_일별_합계를_조회한다() {
        LocalDate from = TODAY.minusDays(6);
        List<DailyPracticeResponse> rows = List.of(new DailyPracticeResponse(from, 600L, 10L));
        when(practiceLogRepository.findDailyTotals(USER_ID, from, TODAY)).thenReturn(rows);

        assertThat(practiceLogService.getDailyTotals(USER_ID, from, TODAY)).isEqualTo(rows);
    }

    @Test
    void 조회_기간이_뒤집히거나_1년을_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> practiceLogService.getDailyTotals(USER_ID, TODAY, TODAY.minusDays(1)))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRACTICE_PERIOD);
        assertThatThrownBy(() -> practiceLogService.getDailyTotals(USER_ID, TODAY.minusDays(400), TODAY))
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PRACTICE_PERIOD);
    }
}
