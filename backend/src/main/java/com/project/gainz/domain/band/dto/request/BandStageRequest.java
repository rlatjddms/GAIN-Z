package com.project.gainz.domain.band.dto.request;

import com.project.gainz.domain.band.enums.BandStage;
import jakarta.validation.constraints.NotNull;

// 합주실 배경 바꾸기 (리더만)
public record BandStageRequest(

        @NotNull(message = "합주실 배경을 골라 주세요.")
        BandStage stage
) {
}
