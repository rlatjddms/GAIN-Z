package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.global.common.enums.InstrumentType;
import com.project.gainz.domain.character.entity.Character;

public record CharacterResponse(

        Long id,
        String nickname,
        InstrumentType instrumentType,
        String instrumentColor,
        Gender gender,
        HairStyle hairStyle,
        String hairColor,
        Integer level,
        Integer exp,
        Integer currentStreak,
        Integer maxStreak,
        Long totalPracticeTime
) {
    public static CharacterResponse from(Character character) {
        return new CharacterResponse(
                character.getId(),
                character.getNickname(),
                character.getInstrumentType(),
                character.getInstrumentColor(),
                character.getGender(),
                character.getHairStyle(),
                character.getHairColor(),
                character.getLevel(),
                character.getExp(),
                character.getCurrentStreak(),
                character.getMaxStreak(),
                character.getTotalPracticeTime()
        );
    }
}
