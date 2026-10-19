package com.project.gainz.domain.friend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FriendRequestCreateRequest(

        @NotBlank(message = "친구 코드를 입력해 주세요.")
        String code
) {
}
