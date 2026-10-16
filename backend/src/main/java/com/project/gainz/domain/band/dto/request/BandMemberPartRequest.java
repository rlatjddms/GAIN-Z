package com.project.gainz.domain.band.dto.request;

import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.validation.constraints.NotNull;

// 멤버 파트 바꾸기 (본인 또는 리더)
public record BandMemberPartRequest(

        @NotNull(message = "파트를 골라 주세요.")
        InstrumentType part
) {
}
