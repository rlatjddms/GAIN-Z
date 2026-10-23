package com.project.gainz.domain.character.dto.request;

import com.project.gainz.domain.character.enums.Gender;
import com.project.gainz.domain.character.enums.HairStyle;
import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CharacterCreateRequest(

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        String nickname,

        @NotNull(message = "악기 종류를 선택해주세요.")
        InstrumentType instrumentType,

        @NotBlank(message = "악기 색상을 선택해주세요.")
        @Size(max = 7, message = "HEX 코드 형식(#FFFFFF)이어야 합니다.")
        String instrumentColor,

        @NotNull(message = "성별을 선택해주세요.")
        Gender gender,

        @NotNull(message = "머리 스타일을 선택해주세요.")
        HairStyle hairStyle,

        @NotBlank(message = "머리 색상을 선택해주세요.")
        @Size(max = 7, message = "HEX 코드 형식(#FFFFFF)이어야 합니다.")
        String hairColor,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "피부 색상은 HEX 코드 형식(#FFFFFF)이어야 합니다.")
        String skinColor
) {}
