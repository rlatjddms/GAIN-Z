package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.friend.enums.FriendRelation;
import com.project.gainz.global.common.enums.InstrumentType;

import java.time.LocalDate;
import java.util.List;

// 다른 사람 프로필 (프로필 팝업). 이메일은 넣지 않는다
// visible이 false(공개 범위 때문에 못 봄)면 이름·사진·관계만 주고 나머지는 null·빈 목록
public record UserProfileResponse(
        Long userId,
        String name,
        String profileImageUrl,
        boolean visible,
        FriendRelation relation,
        ProfileMusicResponse profileMusic,
        CharacterResponse character,
        List<PracticeDay> dailyPractice,
        List<ProfileBand> bands
) {

    public static UserProfileResponse hidden(Long userId, String name, String profileImageUrl, FriendRelation relation) {
        return new UserProfileResponse(userId, name, profileImageUrl, false, relation, null, null, List.of(), List.of());
    }

    // 연습한 날만 (경험치는 빼고 시간만)
    public record PracticeDay(LocalDate date, Long totalSeconds) {
    }

    // together: 보는 사람도 들어간 밴드인지
    public record ProfileBand(Long bandId, String name, InstrumentType part, Position position, boolean together) {
        public static ProfileBand of(BandMember member, boolean together) {
            return new ProfileBand(member.getBand().getId(), member.getBand().getName(), member.getPart(),
                    member.getPosition(), together);
        }
    }
}
