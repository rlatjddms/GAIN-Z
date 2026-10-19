package com.project.gainz.domain.notification.dto.response;

import com.project.gainz.domain.notification.entity.Notification;
import com.project.gainz.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String content,
        String redirectUrl,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getContent(), n.getRedirectUrl(),
                Boolean.TRUE.equals(n.getIsRead()), n.getCreatedAt());
    }

    // 알림 한 쪽 (최근 순). hasNext: 더 오래된 알림이 있는지
    public record Page(List<NotificationResponse> items, boolean hasNext) {
    }

    public record UnreadCount(long count) {
    }
}
