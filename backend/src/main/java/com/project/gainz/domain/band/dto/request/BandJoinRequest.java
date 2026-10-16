package com.project.gainz.domain.band.dto.request;

import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 초대 코드로 가입 (대소문자·앞뒤 공백은 무시)
public record BandJoinRequest(

        @NotBlank(message = "초대 코드를 입력해 주세요.")
        String inviteCode,

        @NotNull(message = "내 파트를 골라 주세요.")
        InstrumentType part
) {
}
