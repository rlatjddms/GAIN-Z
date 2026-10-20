package com.project.gainz.domain.character.dto.response;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.enums.StageEffect;

import java.util.Map;
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
        StageEffect stageEffect,          // 고른 무대 효과 (없으면 null)
        Accessory hat,                    // 쓰고 있는 소품 (없으면 null)
        Accessory glasses,
        Accessory earrings,
        Accessory neck,
        Accessory waist,
        Accessory shoes,                  // 늘 하나 (기본 BASIC_SHOES)
        Accessory bottom,                 // 늘 하나 (기본 JEANS)
        Map<String, String> itemColors    // 아이템마다 고른 색 (없는 아이템은 기본 색)
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
                character.getStageEffect(),
                character.getHat(),
                character.getGlasses(),
                character.getEarrings(),
                character.getNeck(),
                character.getWaist(),
                character.getWornShoes(),
                character.getWornBottom(),
                character.getItemColors()
        );
    }
}
