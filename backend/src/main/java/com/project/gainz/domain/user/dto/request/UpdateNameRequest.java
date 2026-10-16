package com.project.gainz.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 이름 바꾸기
public record UpdateNameRequest(

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요.")
        String name
) {
}
