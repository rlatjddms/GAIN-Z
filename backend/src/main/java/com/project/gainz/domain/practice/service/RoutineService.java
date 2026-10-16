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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineService {

    public static final int MAX_ACTIVE_ROUTINES = 20;

    private final RoutineRepository routineRepository;
    private final RoutineCheckRepository routineCheckRepository;
    private final UserRepository userRepository;

    // 사용 중인 루틴 목록 + 그날 체크 현황
    @Transactional(readOnly = true)
    public List<RoutineResponse> getRoutines(Long userId, LocalDate date) {
        List<Routine> routines = routineRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(userId);
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

    public RoutineResponse createRoutine(Long userId, RoutineRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (routineRepository.countByUserIdAndIsActiveTrue(userId) >= MAX_ACTIVE_ROUTINES) {
            throw new BusinessException(ErrorCode.ROUTINE_LIMIT_EXCEEDED);
        }
        Routine routine = routineRepository.save(Routine.builder()
                .user(user)
                .content(request.content().trim())
                .targetCount(request.targetCount())
                .build());
        return RoutineResponse.of(routine, LocalDate.now(), null);
    }

    // 내용·목표 횟수 수정. 오늘 체크의 완료 여부도 새 목표로 다시 계산한다
    public RoutineResponse updateRoutine(Long userId, Long routineId, RoutineRequest request) {
        Routine routine = findOwnRoutine(userId, routineId);
        routine.update(request.content().trim(), request.targetCount());

        LocalDate today = LocalDate.now();
        RoutineCheck todayCheck = routineCheckRepository.findByRoutineIdAndPracticeDate(routineId, today).orElse(null);
        if (todayCheck != null) {
            todayCheck.updateCount(todayCheck.getPracticeCount());
        }
        return RoutineResponse.of(routine, today, todayCheck);
    }

    // 삭제 = 비활성화 (지난 체크 기록은 남김)
    public void deleteRoutine(Long userId, Long routineId) {
        findOwnRoutine(userId, routineId).deactivate();
    }

    // 그날 수행 횟수를 정한다 (최근 7일만)
    public RoutineResponse checkRoutine(Long userId, Long routineId, LocalDate date, RoutineCheckRequest request) {
        validateCheckable(date);
        Routine routine = findOwnRoutine(userId, routineId);
        RoutineCheck check = routineCheckRepository.findByRoutineIdAndPracticeDate(routineId, date)
                .orElseGet(() -> routineCheckRepository.save(RoutineCheck.builder().routine(routine).practiceDate(date).build()));
        check.updateCount(request.count());
        return RoutineResponse.of(routine, date, check);
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
