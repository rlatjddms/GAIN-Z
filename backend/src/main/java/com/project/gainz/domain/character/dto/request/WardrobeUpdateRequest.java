package com.project.gainz.domain.character.dto.request;

import com.project.gainz.domain.character.enums.StageBackground;
import jakarta.validation.constraints.NotNull;

// 옷장 저장: 입을 의상(보유한 것만), 무대 배경(얻은 것만)
public record WardrobeUpdateRequest(

        @NotNull(message = "입을 의상을 골라 주세요.")
        Long outfitId,

        @NotNull(message = "무대 배경을 골라 주세요.")
        StageBackground stageBackground
) {
}
