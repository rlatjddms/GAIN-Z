package com.project.gainz.domain.user.service;

import com.project.gainz.domain.band.repository.BandMemberRepository;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.character.repository.CharacterRepository;
import com.project.gainz.domain.friend.enums.FriendRelation;
import com.project.gainz.domain.friend.service.FriendService;
import com.project.gainz.domain.practice.repository.PracticeLogRepository;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.dto.response.ProfileMusicResponse;
import com.project.gainz.domain.user.dto.response.UserProfileResponse;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.repository.UserRepository;
import com.project.gainz.global.error.BusinessException;
import com.project.gainz.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 다른 사람 프로필 보기. 공개 범위(전체 공개·친구에게만·비공개)에 따라 숨긴다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    // 연습 잔디 기간 (화면은 30주, 이번 주 월요일 기준이라 한 주 더)
    static final int PROFILE_PRACTICE_DAYS = 7 * 31;

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final PracticeLogRepository practiceLogRepository;
    private final BandMemberRepository bandMemberRepository;
    private final FriendService friendService;

    public UserProfileResponse getProfile(Long viewerId, Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        FriendRelation relation = friendService.relationOf(viewerId, targetUserId);
        if (!canView(target, relation)) {
            return UserProfileResponse.hidden(target.getId(), target.getName(), MyInfoResponse.profileImageUrl(target), relation);
        }

        Character character = characterRepository.findByUserId(targetUserId).orElse(null);

        LocalDate today = LocalDate.now();
        List<UserProfileResponse.PracticeDay> daily = practiceLogRepository
                .findDailyTotals(targetUserId, today.minusDays(PROFILE_PRACTICE_DAYS - 1), today).stream()
                .filter(d -> d.totalSeconds() != null && d.totalSeconds() > 0)
                .map(d -> new UserProfileResponse.PracticeDay(d.date(), d.totalSeconds()))
                .toList();

        Set<Long> viewerBandIds = bandMemberRepository.findAllByUserIdOrderByIdAsc(viewerId).stream()
                .map(m -> m.getBand().getId())
                .collect(Collectors.toSet());
        List<UserProfileResponse.ProfileBand> bands = bandMemberRepository.findAllByUserIdOrderByIdAsc(targetUserId).stream()
                .map(m -> UserProfileResponse.ProfileBand.of(m, viewerBandIds.contains(m.getBand().getId())))
                .toList();

        return new UserProfileResponse(target.getId(), target.getName(), MyInfoResponse.profileImageUrl(target), true,
                relation, ProfileMusicResponse.from(target),
                character != null ? CharacterResponse.from(character) : null, daily, bands);
    }

    // 내 프로필은 항상, 전체 공개는 누구나, 친구에게만은 친구만, 비공개는 나만
    private boolean canView(User target, FriendRelation relation) {
        if (relation == FriendRelation.ME) return true;
        return switch (target.getProfileVisibility()) {
            case PUBLIC -> true;
            case FRIENDS -> relation == FriendRelation.FRIEND;
            case PRIVATE -> false;
        };
    }
}
