package com.project.gainz.domain.friend.dto.response;

import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;

import java.time.LocalDateTime;

// 친구 목록의 친구. since: 친구가 된 때
public record FriendResponse(
        Long userId,
        String name,
        String profileImageUrl,
        CharacterResponse character,
        LocalDateTime since
) {
    public static FriendResponse of(User user, Character character, LocalDateTime since) {
        return new FriendResponse(user.getId(), user.getName(), MyInfoResponse.profileImageUrl(user),
                character != null ? CharacterResponse.from(character) : null, since);
    }
}
