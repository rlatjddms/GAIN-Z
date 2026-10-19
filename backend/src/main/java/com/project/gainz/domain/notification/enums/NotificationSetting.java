package com.project.gainz.domain.notification.enums;

import java.util.EnumSet;
import java.util.Set;

// 알림 설정 항목 (마이페이지에서 종류별로 켜고 끈다). 화면 constants/notification.js NOTIFICATION_SETTING_GROUPS와 같다
// 설정 항목이 없는 종류(밴드 매칭 초대·수락)는 항상 받는다
public enum NotificationSetting {

    FRIEND_REQUEST(EnumSet.of(NotificationType.FRIEND_REQUEST)),
    FRIEND_ACCEPT(EnumSet.of(NotificationType.FRIEND_ACCEPT)),
    BAND_JOIN(EnumSet.of(NotificationType.BAND_JOIN)),
    BAND_ROLE(EnumSet.of(NotificationType.BAND_KICKED, NotificationType.BAND_LEADER)),
    BAND_LEAVE(EnumSet.of(NotificationType.BAND_DISBAND, NotificationType.BAND_LEAVE)),
    NOTICE(EnumSet.of(NotificationType.NOTICE)),
    COMMENT(EnumSet.of(NotificationType.COMMENT, NotificationType.REPLY)),
    MENTION(EnumSet.of(NotificationType.MENTION)),
    SETLIST_ASSIGN(EnumSet.of(NotificationType.SETLIST_ASSIGN)),
    JAM_NEW(EnumSet.of(NotificationType.JAM_NEW)),
    JAM_TRACK(EnumSet.of(NotificationType.JAM_TRACK));

    private final Set<NotificationType> types;

    NotificationSetting(Set<NotificationType> types) {
        this.types = types;
    }

    public Set<NotificationType> types() {
        return types;
    }

    // 이 알림 종류를 켜고 끄는 항목 (없으면 null = 항상 받음)
    public static NotificationSetting of(NotificationType type) {
        for (NotificationSetting setting : values()) {
            if (setting.types.contains(type)) return setting;
        }
        return null;
    }
}
