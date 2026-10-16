package com.project.gainz.domain.band.dto.request;

import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 밴드 만들기. 만든 사람이 리더로 들어가고 part는 내 파트
public record BandCreateRequest(

        @NotBlank(message = "밴드 이름을 입력해 주세요.")
        @Size(max = 30, message = "밴드 이름은 30자 이하로 입력해 주세요.")
        String name,

        @Size(max = 200, message = "밴드 소개는 200자 이하로 입력해 주세요.")
        String description,

        @NotNull(message = "내 파트를 골라 주세요.")
        InstrumentType part
) {
}
