package com.project.gainz.domain.character.dto.request;

import com.project.gainz.domain.character.enums.Accessory;
import com.project.gainz.domain.character.enums.StageBackground;
import com.project.gainz.domain.character.enums.StageEffect;

import java.util.Map;
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
        Accessory waist,

        // 아이템마다 고른 색 { "HOODIE": "#ffd8a8" }. null이면 그대로 둠
        Map<String, String> itemColors,

        // 그림에서 악기 내려놓기. null이면 그대로 둠
        Boolean instrumentHidden
) {
    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground, Accessory hat, Accessory glasses, Accessory neck,
                                 Accessory shoes, Accessory bottom, Accessory earrings, StageEffect stageEffect, Accessory waist,
                                 Map<String, String> itemColors) {
        this(outfitId, stageBackground, hat, glasses, neck, shoes, bottom, earrings, stageEffect, waist, itemColors, null);
    }

    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground, Accessory hat, Accessory glasses, Accessory neck,
                                 Accessory shoes, Accessory bottom, Accessory earrings, StageEffect stageEffect, Accessory waist) {
        this(outfitId, stageBackground, hat, glasses, neck, shoes, bottom, earrings, stageEffect, waist, null, null);
    }

    public WardrobeUpdateRequest(Long outfitId, StageBackground stageBackground, Accessory hat, Accessory glasses, Accessory neck,
                                 Accessory shoes, Accessory bottom, Accessory earrings, StageEffect stageEffect) {
        this(outfitId, stageBackground, hat, glasses, neck, shoes, bottom, earrings, stageEffect, null, null);
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
