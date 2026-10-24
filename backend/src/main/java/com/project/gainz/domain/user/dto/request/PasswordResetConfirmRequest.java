package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 메일 링크의 토큰으로 새 비밀번호 정하기 (#193). 비밀번호 규칙은 회원가입과 같다
public record PasswordResetConfirmRequest(
        @NotBlank(message = "재설정 링크가 올바르지 않아요.")
        @Size(max = 100, message = "재설정 링크가 올바르지 않아요.")
        String token,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Pattern(regexp = "(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,16}",
                message = "비밀번호는 8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요.")
        String newPassword
) {}
