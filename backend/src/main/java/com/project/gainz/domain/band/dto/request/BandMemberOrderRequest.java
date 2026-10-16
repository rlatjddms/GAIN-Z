package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 무대 자리 순서 바꾸기 (리더만). 지금 멤버 전부를 왼쪽부터
public record BandMemberOrderRequest(

        @NotEmpty(message = "멤버 순서를 보내 주세요.")
        List<Long> memberIds
) {
}
