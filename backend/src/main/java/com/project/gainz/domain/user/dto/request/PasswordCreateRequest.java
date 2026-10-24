package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 비밀번호 만들기 (비밀번호가 없는 소셜 가입자, #189). 규칙은 SignupRequest와 같다
public record PasswordCreateRequest(

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp = "(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,16}",
                message = "비밀번호는 8~16자 영문 대 소문자, 숫자, 특수문자를 사용하세요.")
        String newPassword
) {
}
