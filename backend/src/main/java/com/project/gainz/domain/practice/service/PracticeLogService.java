package com.project.gainz.domain.practice.service;

import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.service.CharacterService;
import com.project.gainz.domain.practice.dto.request.DailyPracticeUpdateRequest;
import com.project.gainz.domain.practice.dto.request.PracticeLogCreateRequest;
import com.project.gainz.domain.practice.dto.response.DailyPracticeResponse;
import com.project.gainz.domain.practice.dto.response.PracticeRecordResponse;
import com.project.gainz.domain.practice.entity.PracticeLog;
import com.project.gainz.domain.practice.enums.PracticeSource;
import com.project.gainz.domain.practice.policy.ExpPolicy;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PracticeLogService {

    public static final long MAX_SECONDS_PER_DAY = 86_400;
    public static final int EDITABLE_DAYS = 7;          // 오늘 포함 최근 7일만 직접 수정 가능
    public static final int MAX_QUERY_DAYS = 366;

    private final PracticeLogRepository practiceLogRepository;
    private final UserRepository userRepository;
    private final CharacterService characterService;

    // 타이머 구간을 그날 기록에 더하고, 늘어난 하루 합계만큼 경험치 지급
    public PracticeRecordResponse recordSession(Long userId, PracticeLogCreateRequest request) {
        User user = findUser(userId);
        LocalDate date = request.practiceDateOrToday();
        DailyPracticeResponse before = getDaily(userId, date);

        if (before.totalSeconds() + request.durationSeconds() > MAX_SECONDS_PER_DAY) {
            throw new BusinessException(ErrorCode.PRACTICE_DAY_LIMIT_EXCEEDED);
        }
        return applyChange(userId, user, date, before, request.durationSeconds(), PracticeSource.TIMER);
    }

    // 하루 총 연습 시간을 직접 고친다. 차이분을 MANUAL 기록으로 남긴다 (줄여도 받은 경험치는 회수하지 않음)
    public PracticeRecordResponse updateDailyTotal(Long userId, LocalDate date, DailyPracticeUpdateRequest request) {
        validateEditable(date);
        User user = findUser(userId);
        DailyPracticeResponse before = getDaily(userId, date);

        long diff = request.totalSeconds() - before.totalSeconds();
        if (diff == 0) {
            return new PracticeRecordResponse(date, before.totalSeconds(), 0, null);
        }
        return applyChange(userId, user, date, before, diff, PracticeSource.MANUAL);
    }

    // 기간별 일별 합계 (기록이 있는 날만)
    @Transactional(readOnly = true)
    public List<DailyPracticeResponse> getDailyTotals(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) >= MAX_QUERY_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_PRACTICE_PERIOD);
        }
        return practiceLogRepository.findDailyTotals(userId, from, to);
    }

    private PracticeRecordResponse applyChange(Long userId, User user, LocalDate date, DailyPracticeResponse before,
                                               long diffSeconds, PracticeSource source) {
        long afterSeconds = before.totalSeconds() + diffSeconds;
        int expGained = ExpPolicy.expToGrant(afterSeconds, before.expGained());

        practiceLogRepository.save(PracticeLog.builder()
                .user(user)
                .durationSeconds(diffSeconds)
                .expGained(expGained)
                .createdDate(date)
                .source(source)
                .build());

        long minutesDiff = ExpPolicy.toMinutes(afterSeconds) - ExpPolicy.toMinutes(before.totalSeconds());
        ExperienceResult experience = characterService.processExperienceAndUnlocks(userId, expGained, minutesDiff);

        return new PracticeRecordResponse(date, afterSeconds, expGained, experience);
    }

    private DailyPracticeResponse getDaily(Long userId, LocalDate date) {
        return practiceLogRepository.findDailyTotals(userId, date, date).stream()
                .findFirst()
                .orElse(DailyPracticeResponse.empty(date));
    }

    private void validateEditable(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusDays(EDITABLE_DAYS - 1))) {
            throw new BusinessException(ErrorCode.PRACTICE_EDIT_PERIOD_EXPIRED);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
