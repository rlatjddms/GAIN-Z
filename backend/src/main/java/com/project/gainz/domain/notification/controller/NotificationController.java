package com.project.gainz.domain.notification.controller;

import com.project.gainz.domain.notification.dto.request.NotificationIdsRequest;
import com.project.gainz.domain.notification.dto.response.NotificationResponse;
import com.project.gainz.domain.notification.enums.NotificationCategory;
import com.project.gainz.domain.notification.service.NotificationService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 내 알림 (헤더 알림함·알림 페이지)
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 최근 순 (page는 0부터). category: FRIEND·BAND·COMMUNITY·JAM (없으면 전체), unread=true면 안 읽은 것만
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationResponse.Page>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(defaultValue = "false") boolean unread,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.list(principal.getUserId(), page, size, category, unread)));
    }

    // 고른 알림 읽음
    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markSelectedRead(
            @Valid @RequestBody NotificationIdsRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markRead(principal.getUserId(), request.ids());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 고른 알림 지우기 (DELETE에 본문을 싣지 않으려고 POST)
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteSelected(
            @Valid @RequestBody NotificationIdsRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.delete(principal.getUserId(), request.ids());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 읽은 알림만 지우기
    @DeleteMapping("/read")
    public ResponseEntity<ApiResponse<Void>> deleteRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.deleteRead(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
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
