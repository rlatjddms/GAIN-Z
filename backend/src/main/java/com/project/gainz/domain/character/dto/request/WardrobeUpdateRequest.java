package com.project.gainz.domain.character.dto.request;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.enums.StageEffect;
import jakarta.validation.constraints.NotNull;

// 옷장 저장: 입을 상의(보유한 것만), 무대 배경(얻은 것만), 무대 효과·하의·자리마다 쓸 소품(레벨이 닿은 것만, null이면 안 씀.
// 하의·신발은 null이면 기본 청바지·신발)
public record WardrobeUpdateRequest(

        @NotNull(message = "입을 의상을 골라 주세요.")
        Long outfitId,

        @NotNull(message = "무대 배경을 골라 주세요.")
        StageBackground stageBackground,

        Accessory hat,
        Accessory glasses,
        Accessory neck,
        Accessory shoes,
        Accessory bottom,
        Accessory earrings,
        StageEffect stageEffect,
        Accessory waist
) {
    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground, Accessory hat, Accessory glasses, Accessory neck,
                                 Accessory shoes, Accessory bottom, Accessory earrings, StageEffect stageEffect) {
        this(outfitId, stageBackground, hat, glasses, neck, shoes, bottom, earrings, stageEffect, null);
    }

    // 소품 없이 의상·배경만 고르는 경우
    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground) {
        this(outfitId, stageBackground, null, null, null, null, null, null, null);
    }

    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground, Accessory hat, Accessory glasses, Accessory neck, Accessory shoes) {
        this(outfitId, stageBackground, hat, glasses, neck, shoes, null, null, null);
    }

    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground, Accessory hat, Accessory glasses, Accessory neck, Accessory shoes, Accessory bottom) {
        this(outfitId, stageBackground, hat, glasses, neck, shoes, bottom, null, null);
    }
}
