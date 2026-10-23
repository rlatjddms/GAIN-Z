package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 밴드 이름·소개 고치기 (리더만)
public record BandUpdateRequest(

        @NotBlank(message = "밴드 이름을 입력해 주세요.")
        @Size(max = 30, message = "밴드 이름은 30자 이하로 입력해 주세요.")
        String name,

        @Size(max = 200, message = "밴드 소개는 200자 이하로 입력해 주세요.")
        String description
) {
}
