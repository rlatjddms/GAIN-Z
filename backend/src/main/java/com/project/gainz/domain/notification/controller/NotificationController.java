package com.project.gainz.domain.notification.controller;

import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 내 알림 (헤더 알림함)
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 최근 순 (page는 0부터)
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationResponse.Page>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.list(principal.getUserId(), page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationResponse.UnreadCount>> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.unreadCount(principal.getUserId())));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markRead(principal.getUserId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.delete(principal.getUserId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 내 알림 모두 지우기
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.deleteAll(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
