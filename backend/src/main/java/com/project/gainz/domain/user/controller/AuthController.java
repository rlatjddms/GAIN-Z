package com.project.gainz.domain.user.controller;

import com.project.gainz.domain.user.dto.request.EmailRequest;
import com.project.gainz.domain.user.dto.request.LoginRequest;
import com.project.gainz.domain.user.dto.request.PasswordResetConfirmRequest;
import com.project.gainz.domain.user.dto.request.ReissueRequest;
import com.project.gainz.domain.user.dto.request.SignupCodeVerifyRequest;
import com.project.gainz.domain.user.dto.request.SignupRequest;
import com.project.gainz.domain.user.dto.response.PasswordResetRequestResponse;
import com.project.gainz.domain.user.dto.response.SignupCodeVerifyResponse;
import com.project.gainz.domain.user.dto.response.TokenResponse;
import com.project.gainz.domain.user.service.AccountMailService;
import com.project.gainz.domain.user.service.AuthService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
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
    private final AccountMailService accountMailService;

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
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
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

    /* ---------- 이메일 인증·비밀번호 찾기 (#193) ---------- */

    // 가입 화면 "인증하기": 이미 가입된 이메일이면 409(U001), 아니면 6자리 코드를 메일로
    @PostMapping("/signup/email-code")
    public ResponseEntity<ApiResponse<Void>> sendSignupCode(@Valid @RequestBody EmailRequest request) {
        accountMailService.sendSignupCode(request.email());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 코드가 맞으면 가입 요청에 넣을 emailVerificationToken
    @PostMapping("/signup/email-code/verify")
    public ResponseEntity<ApiResponse<SignupCodeVerifyResponse>> verifySignupCode(@Valid @RequestBody SignupCodeVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountMailService.verifySignupCode(request.email(), request.code())));
    }

    // 결과: SENT / NOT_FOUND(가입되지 않은 이메일) / SOCIAL_ONLY(provider로 로그인)
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<PasswordResetRequestResponse>> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountMailService.requestPasswordReset(request.email())));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        accountMailService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
