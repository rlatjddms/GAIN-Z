package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotNull;

// 친구에게 밴드 초대 보내기 (#229)
public record BandInviteRequest(
        @NotNull(message = "초대할 친구를 골라 주세요.")
        Long userId
) {
}
