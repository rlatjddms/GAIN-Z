package com.project.gainz.domain.practice.service;

import com.project.gainz.domain.character.dto.response.ExperienceResult;
import com.project.gainz.domain.character.service.CharacterService;
import com.project.gainz.domain.practice.dto.request.PracticeLogCreateRequest;
import com.project.gainz.domain.practice.dto.response.PracticeLogResponse;
import com.project.gainz.domain.practice.entity.PracticeLog;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
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

    @Mock private PracticeLogRepository practiceLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterService characterService;

    @InjectMocks private PracticeLogService practiceLogService;

    @Test
    void 유저가_없으면_예외가_발생한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        PracticeLogCreateRequest request = new PracticeLogCreateRequest(600L, 1.0, null);

        assertThatThrownBy(() -> practiceLogService.createPracticeLog(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        verify(practiceLogRepository, never()).save(any());
        verify(characterService, never()).processExperienceAndUnlocks(any(), anyInt(), anyLong());
    }

    @Test
    void 연습_로그를_저장하고_환산된_경험치를_캐릭터에_반영한다() {
        User user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        ExperienceResult experience = new ExperienceResult(1, 1, 80, false, List.of());
        when(characterService.processExperienceAndUnlocks(USER_ID, 80, 30)).thenReturn(experience);

        LocalDate practiceDate = LocalDate.of(2026, 10, 12);
        PracticeLogCreateRequest request = new PracticeLogCreateRequest(30 * 60L, 1.0, practiceDate);

        PracticeLogResponse response = practiceLogService.createPracticeLog(USER_ID, request);

        ArgumentCaptor<PracticeLog> captor = ArgumentCaptor.forClass(PracticeLog.class);
        verify(practiceLogRepository).save(captor.capture());
        PracticeLog saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getDurationSeconds()).isEqualTo(1800L);
        assertThat(saved.getAchievementRate()).isEqualTo(1.0);
        assertThat(saved.getExpGained()).isEqualTo(80);
        assertThat(saved.getCreatedDate()).isEqualTo(practiceDate);

        assertThat(response.expGained()).isEqualTo(80);
        assertThat(response.practiceDate()).isEqualTo(practiceDate);
        assertThat(response.experience()).isSameAs(experience);
    }

    @Test
    void 연습_날짜를_생략하면_오늘_날짜로_기록한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(characterService.processExperienceAndUnlocks(USER_ID, 1, 1))
                .thenReturn(new ExperienceResult(1, 1, 1, false, List.of()));

        PracticeLogResponse response = practiceLogService.createPracticeLog(
                USER_ID, new PracticeLogCreateRequest(60L, 0.0, null));

        assertThat(response.practiceDate()).isEqualTo(LocalDate.now());
    }
}
