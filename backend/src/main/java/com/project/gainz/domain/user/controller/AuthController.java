package com.project.gainz.domain.user.controller;

import com.project.gainz.domain.user.dto.request.LoginRequest;
import com.project.gainz.domain.user.dto.request.ReissueRequest;
import com.project.gainz.domain.user.dto.request.SignupRequest;
import com.project.gainz.domain.user.dto.response.TokenResponseDto;
import com.project.gainz.domain.user.service.AuthService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @RequestParam String email
    ) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ResponseEntity.ok(ApiResponse.success(isDuplicate));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponseDto tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponseDto>> reissue(
            @Valid @RequestBody ReissueRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.reissue(request)));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.logout(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
