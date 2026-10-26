package com.project.gainz.domain.user.controller;

import com.project.gainz.domain.user.dto.request.PasswordCreateRequest;
import com.project.gainz.domain.user.dto.response.SocialLinkStartResponse;
import com.project.gainz.domain.user.enums.SocialProvider;
import com.project.gainz.domain.user.service.SocialAccountService;
import com.project.gainz.domain.user.dto.request.PasswordChangeRequest;
import com.project.gainz.domain.user.dto.request.ProfileMusicRequest;
import com.project.gainz.domain.user.dto.request.ProfileVisibilityRequest;
import com.project.gainz.domain.user.dto.request.WithdrawRequest;
import com.project.gainz.domain.user.dto.response.MyInfoResponse;
import com.project.gainz.domain.user.dto.response.UserProfileResponse;
import com.project.gainz.domain.user.service.UserProfileService;
import com.project.gainz.domain.user.service.UserService;
import com.project.gainz.domain.user.service.UserService.ProfileImageFile;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

// 내 계정 (마이페이지)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final SocialAccountService socialAccountService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(principal.getUserId())));
    }

    // 요즘 듣는 음악 설정·지우기
    @PutMapping("/me/profile-music")
    public ResponseEntity<ApiResponse<MyInfoResponse>> updateProfileMusic(
            @Valid @RequestBody ProfileMusicRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfileMusic(principal.getUserId(), request)));
    }

    @DeleteMapping("/me/profile-music")
    public ResponseEntity<ApiResponse<MyInfoResponse>> deleteProfileMusic(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.deleteProfileMusic(principal.getUserId())));
    }

    // 프로필 공개 범위 (전체 공개·친구에게만·비공개)
    @PatchMapping("/me/visibility")
    public ResponseEntity<ApiResponse<MyInfoResponse>> changeProfileVisibility(
            @Valid @RequestBody ProfileVisibilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.changeProfileVisibility(principal.getUserId(), request)));
    }

    // 다른 사람 프로필 (공개 범위에 따라 이름·사진만 보일 수 있다)
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getProfile(principal.getUserId(), userId)));
    }

    // 프로필 사진 바꾸기 (multipart "file")
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MyInfoResponse>> changeProfileImage(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.changeProfileImage(principal.getUserId(), file)));
    }

    // 프로필 사진 지우기 (기본 프로필로)
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<MyInfoResponse>> deleteProfileImage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.deleteProfileImage(principal.getUserId())));
    }

    // 프로필 사진 파일. 주소에 버전(?v=)이 붙어 있어 사진이 바뀌면 주소도 바뀐다
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Long userId) {
        ProfileImageFile file = userService.getProfileImage(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(file.resource());
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 비밀번호가 없는 계정(소셜로만 가입)이 처음 만들기 (#189)
    @PostMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @Valid @RequestBody PasswordCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.createPassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 소셜 연결 시작 (#189): 받은 표를 /oauth2/authorization/{provider}?link= 에 붙여 보낸다
    @PostMapping("/me/social-links/{provider}/start")
    public ResponseEntity<ApiResponse<SocialLinkStartResponse>> startSocialLink(
            @PathVariable SocialProvider provider,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                new SocialLinkStartResponse(socialAccountService.startLink(principal.getUserId(), provider))));
    }

    // 소셜 연결 해제. 로그인 방법이 하나만 남으면 안 된다
    @DeleteMapping("/me/social-links/{provider}")
    public ResponseEntity<ApiResponse<MyInfoResponse>> unlinkSocial(
            @PathVariable SocialProvider provider,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(socialAccountService.unlink(principal.getUserId(), provider)));
    }

    // 비밀번호가 없는 계정은 본문 없이 보내도 된다
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestBody(required = false) WithdrawRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.withdraw(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
