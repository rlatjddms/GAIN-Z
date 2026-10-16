package com.project.gainz.domain.user.dto.response;

import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.domain.user.enums.SocialProvider;

import java.time.LocalDateTime;

// 내 계정 정보 (마이페이지). 캐릭터가 아직 없으면 character는 null
public record MyInfoResponse(
        Long id,
        String email,
        String name,
        SocialProvider provider,
        LocalDateTime createdAt,
        CharacterSummary character
) {
    public record CharacterSummary(String nickname, Integer level) {
        static CharacterSummary from(Character character) {
            return character == null ? null : new CharacterSummary(character.getNickname(), character.getLevel());
        }
    }

    public static MyInfoResponse of(User user, Character character) {
        return new MyInfoResponse(user.getId(), user.getEmail(), user.getName(), user.getProvider(),
                user.getCreatedAt(), CharacterSummary.from(character));
    }
}
