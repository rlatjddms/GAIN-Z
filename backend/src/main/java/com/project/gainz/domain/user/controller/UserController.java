package com.project.gainz.domain.user.controller;

import com.project.gainz.domain.user.dto.request.PasswordChangeRequest;
import com.project.gainz.domain.user.dto.request.UpdateNameRequest;
import com.project.gainz.domain.user.dto.request.WithdrawRequest;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.service.UserService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 내 계정 (마이페이지)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(principal.getUserId())));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> updateName(
            @Valid @RequestBody UpdateNameRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateName(principal.getUserId(), request)));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 소셜 가입자는 본문 없이 보내도 된다
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestBody(required = false) WithdrawRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.withdraw(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
