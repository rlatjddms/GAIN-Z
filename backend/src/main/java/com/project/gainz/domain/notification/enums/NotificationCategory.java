package com.project.gainz.domain.notification.enums;

import java.util.EnumSet;
import java.util.Set;

// 알림 걸러 보기 영역 (알림 페이지 탭). 화면 constants/notification.js와 같다
// 밴드 안에서 생기는 커뮤니티(공지·댓글·답글·멘션)·합주(셋리스트·잼) 알림도 밴드로 묶는다 (#145)
public enum NotificationCategory {

    FRIEND(EnumSet.of(NotificationType.FRIEND_REQUEST, NotificationType.FRIEND_ACCEPT)),
    BAND(EnumSet.of(NotificationType.BAND_JOIN, NotificationType.BAND_KICKED, NotificationType.BAND_LEADER,
            NotificationType.BAND_DISBAND, NotificationType.BAND_LEAVE, NotificationType.BAND_INVITE,
            NotificationType.NOTICE, NotificationType.COMMENT, NotificationType.REPLY, NotificationType.MENTION,
            NotificationType.SETLIST_ASSIGN, NotificationType.JAM_NEW, NotificationType.JAM_TRACK)),
    RECORD(EnumSet.of(NotificationType.RECORD));

    private final Set<NotificationType> types;

    NotificationCategory(Set<NotificationType> types) {
        this.types = types;
    }

    public Set<NotificationType> types() {
        return types;
    }

    // 영역을 안 고르면 모든 종류
    public static Set<NotificationType> typesOf(NotificationCategory category) {
        return category == null ? EnumSet.allOf(NotificationType.class) : category.types;
    }
}
