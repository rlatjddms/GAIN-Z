package com.project.gainz.domain.friend.controller;

import com.project.gainz.domain.friend.dto.request.FriendRequestCreateRequest;
import com.project.gainz.domain.friend.dto.response.BandMateResponse;
import com.project.gainz.domain.friend.dto.response.FriendCodeResponse;
import com.project.gainz.domain.friend.dto.response.FriendProfileResponse;
import com.project.gainz.domain.friend.dto.response.FriendRequestResponse;
import com.project.gainz.domain.friend.dto.response.FriendResponse;
import com.project.gainz.domain.friend.service.FriendService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // 내 친구 코드 (없으면 만들어서)
    @GetMapping("/code")
    public ResponseEntity<ApiResponse<FriendCodeResponse>> getMyCode(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getMyCode(principal.getUserId())));
    }

    // 친구 코드 새로 받기
    @PostMapping("/code")
    public ResponseEntity<ApiResponse<FriendCodeResponse>> regenerateCode(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.regenerateCode(principal.getUserId())));
    }

    // 친구 코드로 신청
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequest(
            @Valid @RequestBody FriendRequestCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.sendRequest(principal.getUserId(), request)));
    }

    // 같은 밴드 멤버에게 코드 없이 신청
    @PostMapping("/requests/users/{targetUserId}")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendRequestToBandMate(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.sendRequestToBandMate(principal.getUserId(), targetUserId)));
    }

    // 같은 밴드 멤버 (친구 화면의 바로 신청 목록)
    @GetMapping("/band-mates")
    public ResponseEntity<ApiResponse<List<BandMateResponse>>> getBandMates(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getBandMates(principal.getUserId())));
    }

    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getReceivedRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getReceivedRequests(principal.getUserId())));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> getSentRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getSentRequests(principal.getUserId())));
    }

    // 받은 신청 수락
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.acceptRequest(principal.getUserId(), requestId)));
    }

    // 받은 신청 거절 / 보낸 신청 취소
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        friendService.deleteRequest(principal.getUserId(), requestId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendResponse>>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getFriends(principal.getUserId())));
    }

    // 친구 프로필 (캐릭터·무대, 최근 연습, 함께하는 밴드)
    @GetMapping("/{friendUserId}/profile")
    public ResponseEntity<ApiResponse<FriendProfileResponse>> getFriendProfile(
            @PathVariable Long friendUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getFriendProfile(principal.getUserId(), friendUserId)));
    }

    // 친구 끊기
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable Long friendUserId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        friendService.removeFriend(principal.getUserId(), friendUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
