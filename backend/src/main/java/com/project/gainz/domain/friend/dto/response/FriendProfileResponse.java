package com.project.gainz.domain.friend.dto.response;

import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.global.common.enums.InstrumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 친구 프로필: 꾸민 캐릭터(의상·무대 배경 포함), 최근 연습 잔디, 함께하는 밴드
// since: 친구가 된 때, joinedAt: 가입한 때. 이메일은 넣지 않는다
public record FriendProfileResponse(
        Long userId,
        String name,
        String profileImageUrl,
        CharacterResponse character,
        LocalDateTime since,
        LocalDateTime joinedAt,
        List<PracticeDay> dailyPractice,
        List<FriendBand> bands
) {

    // 연습한 날만 (경험치는 빼고 시간만)
    public record PracticeDay(LocalDate date, Long totalSeconds) {
    }

    // together: 나도 들어간 밴드인지
    public record FriendBand(Long bandId, String name, InstrumentType part, Position position, boolean together) {
        public static FriendBand of(BandMember member, boolean together) {
            return new FriendBand(member.getBand().getId(), member.getBand().getName(), member.getPart(),
                    member.getPosition(), together);
        }
    }
}
