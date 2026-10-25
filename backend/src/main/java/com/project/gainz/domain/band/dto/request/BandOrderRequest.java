package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 홈 밴드 탭 순서 바꾸기 (#266, 나한테만). 내 밴드 전부를 왼쪽부터
public record BandOrderRequest(

        @NotEmpty(message = "밴드 순서를 보내 주세요.")
        List<Long> bandIds
) {
}
