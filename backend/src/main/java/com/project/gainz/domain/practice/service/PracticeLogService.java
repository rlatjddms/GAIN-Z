package com.project.gainz.domain.practice.service;

import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.service.CharacterService;
import com.project.gainz.domain.practice.dto.request.PracticeLogCreateRequest;
import com.project.gainz.domain.practice.dto.response.PracticeLogResponse;
import com.project.gainz.domain.practice.entity.PracticeLog;
import com.project.gainz.domain.practice.policy.ExpPolicy;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PracticeLogService {

    private final PracticeLogRepository practiceLogRepository;
    private final UserRepository userRepository;
    private final CharacterService characterService;

    // 연습 로그 기록 후 경험치 합산
    public PracticeLogResponse createPracticeLog(Long userId, PracticeLogCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int expGained = ExpPolicy.calculate(request.durationSeconds(), request.achievementRate());

        PracticeLog practiceLog = PracticeLog.builder()
                .user(user)
                .durationSeconds(request.durationSeconds())
                .achievementRate(request.achievementRate())
                .expGained(expGained)
                .createdDate(request.practiceDateOrToday())
                .build();
        practiceLogRepository.save(practiceLog);

        ExperienceResult experience = characterService.processExperienceAndUnlocks(
                userId,
                expGained,
                ExpPolicy.toMinutes(request.durationSeconds())
        );

        return PracticeLogResponse.of(practiceLog, experience);
    }
}
