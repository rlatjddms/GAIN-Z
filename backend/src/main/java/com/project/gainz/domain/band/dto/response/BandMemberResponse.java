package com.project.gainz.domain.band.dto.response;

import com.project.gainz.domain.band.entity.BandMember;
import com.project.gainz.domain.band.enums.Position;
import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.global.common.enums.InstrumentType;

import java.time.LocalDateTime;

// 밴드 멤버. character는 합주실 무대에 픽셀 캐릭터를 그리는 데 쓴다 (캐릭터가 없으면 null)
public record BandMemberResponse(
        Long memberId,
        Long userId,
        InstrumentType part,
        Position position,
        boolean me,
        LocalDateTime joinedAt,
        CharacterResponse character
) {
    public static BandMemberResponse of(BandMember member, Character character, Long viewerId) {
        Long userId = member.getUser().getId();
        return new BandMemberResponse(
                member.getId(),
                userId,
                member.getPart(),
                member.getPosition(),
                userId.equals(viewerId),
                member.getCreatedAt(),
                character != null ? CharacterResponse.from(character) : null
        );
    }
}
