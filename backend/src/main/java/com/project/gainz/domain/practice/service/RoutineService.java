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
import com.project.gainz.domain.record.event.RecordCheckEvent;
import com.project.gainz.domain.record.enums.RecordMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineService {

    public static final int MAX_ACTIVE_ROUTINES = 20;
    public static final int MAX_SUMMARY_DAYS = 366;

    private final RoutineRepository routineRepository;
    private final RoutineCheckRepository routineCheckRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 그날 일정에 해당하는 루틴 + 그날 체크 현황
    @Transactional(readOnly = true)
    public List<RoutineResponse> getRoutines(Long userId, LocalDate date) {
        List<Routine> routines = routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(userId).stream()
                .filter(r -> r.isScheduledOn(date))
                .toList();
        if (routines.isEmpty()) {
            return List.of();
        }
        Map<Long, RoutineCheck> checks = routineCheckRepository
                .findAllByRoutineIdInAndPracticeDate(routines.stream().map(Routine::getId).toList(), date).stream()
                .collect(Collectors.toMap(c -> c.getRoutine().getId(), Function.identity()));
        return routines.stream()
                .map(r -> RoutineResponse.of(r, date, checks.get(r.getId())))
                .toList();
    }

    // 기간별 날짜마다 일정 루틴 수와 완료한 수 (일정이 있는 날만)
    @Transactional(readOnly = true)
    public List<RoutineDailySummaryResponse> getDailySummary(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) >= MAX_SUMMARY_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_PRACTICE_PERIOD);
        }
        List<Routine> routines = routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(userId);
        if (routines.isEmpty()) {
            return List.of();
        }
        Set<String> completed = new HashSet<>();
        routineCheckRepository.findAllByRoutineIdInAndPracticeDateBetween(routines.stream().map(Routine::getId).toList(), from, to)
                .stream()
                .filter(RoutineCheck::getIsCompleted)
                .forEach(c -> completed.add(c.getRoutine().getId() + "|" + c.getPracticeDate()));

        List<RoutineDailySummaryResponse> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            int total = 0;
            int done = 0;
            for (Routine r : routines) {
                if (!r.isScheduledOn(d)) continue;
                total++;
                if (completed.contains(r.getId() + "|" + d)) done++;
            }
            if (total > 0) {
                result.add(new RoutineDailySummaryResponse(d, total, done));
            }
        }
        return result;
    }

    public RoutineResponse createRoutine(Long userId, RoutineRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now();
        long ongoing = routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(userId).stream()
                .filter(r -> r.isOngoing(today))
                .count();
        if (ongoing >= MAX_ACTIVE_ROUTINES) {
            throw new BusinessException(ErrorCode.ROUTINE_LIMIT_EXCEEDED);
        }
        Set<DayOfWeek> days = validateSchedule(request, today);
        Routine routine = routineRepository.save(Routine.builder()
                .user(user)
                .content(request.content().trim())
                .targetCount(request.targetCount())
                .repeatType(request.repeatTypeOrDaily())
                .repeatDays(days)
                .startDate(request.startDateOr(today))
                .endDate(request.endDate())
                .build());
        return RoutineResponse.of(routine, today, null);
    }

    // 내용·목표 횟수·반복 설정 수정. 오늘 체크의 완료 여부도 새 목표로 다시 계산한다
    public RoutineResponse updateRoutine(Long userId, Long routineId, RoutineRequest request) {
        Routine routine = findOwnRoutine(userId, routineId);
        LocalDate today = LocalDate.now();
        Set<DayOfWeek> days = validateSchedule(request, today);
        routine.update(request.content().trim(), request.targetCount());
        // 시작일을 생략하면 기존 시작일 유지 (예전 루틴은 없을 수 있음). 하루만인데 시작일이 없으면 오늘
        LocalDate start = request.startDate() != null ? request.startDate() : routine.effectiveStartDate();
        if (start == null && request.repeatTypeOrDaily() == RoutineRepeat.NONE) {
            start = today;
        }
        routine.changeSchedule(request.repeatTypeOrDaily(), days, start, request.endDate());

        RoutineCheck todayCheck = routineCheckRepository.findByRoutineIdAndPracticeDate(routineId, today).orElse(null);
        if (todayCheck != null) {
            todayCheck.updateCount(todayCheck.getPracticeCount());
        }
        return RoutineResponse.of(routine, today, todayCheck);
    }

    // 오늘부터 그만하기 (지난 기록이 있으면 종료일을 어제로, 없으면 비활성화)
    public void deleteRoutine(Long userId, Long routineId) {
        findOwnRoutine(userId, routineId).stopFrom(LocalDate.now());
    }

    // 그날 수행 횟수를 정한다 (최근 7일, 그날 일정이 있는 루틴만)
    public RoutineResponse checkRoutine(Long userId, Long routineId, LocalDate date, RoutineCheckRequest request) {
        validateCheckable(date);
        Routine routine = findOwnRoutine(userId, routineId);
        if (!routine.isScheduledOn(date)) {
            throw new BusinessException(ErrorCode.ROUTINE_NOT_SCHEDULED);
        }
        RoutineCheck check = routineCheckRepository.findByRoutineIdAndPracticeDate(routineId, date)
                .orElseGet(() -> routineCheckRepository.save(RoutineCheck.builder().routine(routine).practiceDate(date).build()));
        check.updateCount(request.count());
        eventPublisher.publishEvent(RecordCheckEvent.of(userId, RecordMetric.ROUTINE_DAYS));
        return RoutineResponse.of(routine, date, check);
    }

    // 요일 반복은 요일 1개 이상, 종료일은 시작일 이후. 확인한 반복 요일을 돌려준다
    private Set<DayOfWeek> validateSchedule(RoutineRequest request, LocalDate today) {
        Set<DayOfWeek> days = request.repeatDays() == null || request.repeatDays().isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class)
                : EnumSet.copyOf(request.repeatDays());
        if (request.repeatTypeOrDaily() == RoutineRepeat.WEEKLY && days.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ROUTINE_SCHEDULE);
        }
        LocalDate start = request.startDateOr(today);
        if (request.endDate() != null && request.endDate().isBefore(start)) {
            throw new BusinessException(ErrorCode.INVALID_ROUTINE_SCHEDULE);
        }
        return days;
    }

    private Routine findOwnRoutine(Long userId, Long routineId) {
        return routineRepository.findByIdAndUserIdAndIsActiveTrue(routineId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTINE_NOT_FOUND));
    }

    private void validateCheckable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusDays(PracticeLogService.EDITABLE_DAYS - 1))) {
            throw new BusinessException(ErrorCode.ROUTINE_CHECK_PERIOD_EXPIRED);
        }
    }
}
