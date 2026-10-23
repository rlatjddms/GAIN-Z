package com.project.gainz.domain.friend.dto.response;

import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;

// 친구 신청에 나오는 사람. 이메일은 넣지 않는다. 캐릭터가 없으면 character는 null
public record FriendUserResponse(
        Long userId,
        String name,
        String profileImageUrl,
        CharacterResponse character
) {
    public static FriendUserResponse of(User user, Character character) {
        return new FriendUserResponse(user.getId(), user.getName(), MyInfoResponse.profileImageUrl(user),
                character != null ? CharacterResponse.from(character) : null);
    }
}
