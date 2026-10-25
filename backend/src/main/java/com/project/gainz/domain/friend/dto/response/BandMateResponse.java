package com.project.gainz.domain.friend.dto.response;

import com.project.gainz.domain.character.dto.response.CharacterResponse;
import com.project.gainz.domain.character.entity.Character;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.entity.User;

import java.util.List;

// 나와 같은 밴드에 있는 멤버. bandNames: 같이 있는 밴드 이름 (들어간 순서)
public record BandMateResponse(
        Long userId,
        String name,
        String profileImageUrl,
        CharacterResponse character,
        List<String> bandNames
) {
    public static BandMateResponse of(User user, Character character, List<String> bandNames) {
        return new BandMateResponse(user.getId(), MyInfoResponse.displayName(user, character), MyInfoResponse.profileImageUrl(user),
                character != null ? CharacterResponse.from(character) : null, bandNames);
    }
}
