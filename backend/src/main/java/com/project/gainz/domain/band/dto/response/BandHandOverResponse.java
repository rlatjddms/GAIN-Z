package com.project.gainz.domain.band.dto.response;

import java.util.List;

// 회원 탈퇴 전에 리더를 넘겨야 하는 밴드 (#215). BAND_LEADER_MUST_HAND_OVER 오류 응답의 data
public record BandHandOverResponse(List<BandRef> bands) {

    public record BandRef(Long id, String name) {}
}
