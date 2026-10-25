package com.project.gainz.domain.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// 알림 없이 "바뀌었다"는 것만 알리는 실시간 신호 (#259). 받은 화면은 보고 있는 목록을 다시 불러온다
// topic: BAND_BOARD(글·댓글·공지), BAND(멤버·밴드 정보), FRIENDS(친구·신청)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveChange(String topic, Long bandId, Long postId) {

    public static LiveChange bandBoard(Long bandId, Long postId) {
        return new LiveChange("BAND_BOARD", bandId, postId);
    }

    public static LiveChange band(Long bandId) {
        return new LiveChange("BAND", bandId, null);
    }

    public static LiveChange friends() {
        return new LiveChange("FRIENDS", null, null);
    }
}
