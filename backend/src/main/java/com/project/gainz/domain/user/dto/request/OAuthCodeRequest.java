package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// 소셜 로그인 한 번용 코드를 토큰으로 바꾸기 (#281)
public record OAuthCodeRequest(

        @NotBlank(message = "로그인 코드가 없어요.")
        String code
) {
}
