package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotNull;

// 리더 넘기기: 새 리더가 될 멤버
public record BandLeaderRequest(

        @NotNull(message = "리더를 넘길 멤버를 골라 주세요.")
        Long memberId
) {
}
