package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 가입 전 이메일 인증 코드 확인 (#193)
public record SignupCodeVerifyRequest(

        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "인증 코드를 입력해 주세요.")
        @Pattern(regexp = "\\s*\\d{6}\\s*", message = "인증 코드는 숫자 6자리예요.")
        String code
) {}
