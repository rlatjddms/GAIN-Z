package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.StageBackground;
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
        String skinColor,
        Integer level,
        Integer exp,
        Integer currentStreak,
        Integer maxStreak,
        Long totalPracticeTime,
        Long currentOutfitId,
        String currentOutfitSpriteKey,    // 입은 의상 스프라이트 키 (성별에 맞는 그림은 프론트에서 고름)
        StageBackground stageBackground,
        Accessory hat,                    // 쓰고 있는 소품 (없으면 null)
        Accessory glasses,
        Accessory neck,
        Accessory shoes,
        Accessory bottom                  // 입고 있는 하의 (null이면 기본 청바지)
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
                character.getSkinColor(),
                character.getLevel(),
                character.getExp(),
                character.getCurrentStreak(),
                character.getMaxStreak(),
                character.getTotalPracticeTime(),
                character.getCurrentOutfit() != null ? character.getCurrentOutfit().getId() : null,
                character.getCurrentOutfit() != null ? character.getCurrentOutfit().getSpriteKey() : null,
                character.getCurrentStageBackground(),
                character.getHat(),
                character.getGlasses(),
                character.getNeck(),
                character.getShoes(),
                character.getBottom()
        );
    }
}
