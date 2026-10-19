package com.project.gainz.domain.notification.enums;

public enum NotificationType {

    /* 친구 */
    FRIEND_REQUEST,   // 친구 신청 받음
    FRIEND_ACCEPT,    // 내 신청을 상대가 수락함

    /* 밴드 멤버 */
    BAND_JOIN,        // 새 멤버 가입 (기존 멤버에게)
    BAND_KICKED,      // 내보내짐
    BAND_LEADER,      // 리더를 넘겨받음
    BAND_DISBAND,     // 밴드 해체 (남은 멤버에게)
    BAND_LEAVE,       // 멤버가 나감 (리더에게)

    /* 커뮤니티 */
    NOTICE,           // 새 공지
    COMMENT,          // 내 글에 댓글
    REPLY,            // 내 댓글에 답글
    MENTION,          // 글·댓글에서 @멘션

    /* 합주 */
    SETLIST_ASSIGN,   // 셋리스트 곡 담당으로 지정됨
    JAM_NEW,          // 새 잼
    JAM_TRACK,        // 내 잼에 트랙 추가

    /* 밴드 매칭 (#87에서 사용) */
    BAND_INVITE,      // 밴드 초대
    BAND_ACCEPT       // 밴드 가입 요청 수락
}
