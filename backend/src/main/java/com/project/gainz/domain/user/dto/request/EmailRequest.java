package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 인증 메일 다시 보내기·비밀번호 재설정 메일 (#193)
public record EmailRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {}
