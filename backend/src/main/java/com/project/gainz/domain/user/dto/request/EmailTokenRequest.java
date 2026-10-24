package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 메일 링크의 토큰으로 이메일 인증 (#193)
public record EmailTokenRequest(
        @NotBlank(message = "인증 링크가 올바르지 않아요.")
        @Size(max = 100, message = "인증 링크가 올바르지 않아요.")
        String token
) {}
