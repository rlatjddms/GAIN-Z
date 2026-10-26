package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.BandCreateRequest;
import com.project.gainz.domain.band.dto.request.BandInviteRequest;
import com.project.gainz.domain.band.dto.request.BandJoinRequest;
import com.project.gainz.domain.band.dto.request.BandLeaderRequest;
import com.project.gainz.domain.band.dto.request.BandMemberOrderRequest;
import com.project.gainz.domain.band.dto.request.BandOrderRequest;
import com.project.gainz.domain.band.dto.request.BandMemberPartRequest;
import com.project.gainz.domain.band.dto.request.BandStageRequest;
import com.project.gainz.domain.band.dto.request.BandUpdateRequest;
import com.project.gainz.domain.band.dto.response.BandResponse;
import com.project.gainz.domain.band.service.BandService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bands")
@RequiredArgsConstructor
public class BandController {

    private final BandService bandService;

    // 내가 들어간 밴드 (홈 BAND 탭)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BandResponse>>> getMyBands(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.getMyBands(principal.getUserId())));
    }

    // 홈 밴드 탭 순서 바꾸기 (#266, 나한테만)
    @PutMapping("/me/order")
    public ResponseEntity<ApiResponse<List<BandResponse>>> reorderMyBands(
            @Valid @RequestBody BandOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.reorderMyBands(principal.getUserId(), request)));
    }

    @GetMapping("/{bandId}")
    public ResponseEntity<ApiResponse<BandResponse>> getBand(
            @PathVariable Long bandId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.getBand(principal.getUserId(), bandId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BandResponse>> createBand(
            @Valid @RequestBody BandCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.createBand(principal.getUserId(), request)));
    }

    @PatchMapping("/{bandId}")
    public ResponseEntity<ApiResponse<BandResponse>> updateBand(
            @PathVariable Long bandId,
            @Valid @RequestBody BandUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.updateBand(principal.getUserId(), bandId, request)));
    }

    // 합주실 배경 바꾸기 (리더만)
    @PatchMapping("/{bandId}/stage")
    public ResponseEntity<ApiResponse<BandResponse>> changeStage(
            @PathVariable Long bandId,
            @Valid @RequestBody BandStageRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.changeStage(principal.getUserId(), bandId, request)));
    }

    // 무대 자리 순서 바꾸기 (리더만)
    @PutMapping("/{bandId}/members/order")
    public ResponseEntity<ApiResponse<BandResponse>> reorderMembers(
            @PathVariable Long bandId,
            @Valid @RequestBody BandMemberOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.reorderMembers(principal.getUserId(), bandId, request)));
    }

    @DeleteMapping("/{bandId}")
    public ResponseEntity<ApiResponse<Void>> deleteBand(
            @PathVariable Long bandId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        bandService.deleteBand(principal.getUserId(), bandId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 초대 코드 새로 받기 (리더만)
    @PostMapping("/{bandId}/invite-code")
    public ResponseEntity<ApiResponse<BandResponse>> regenerateInviteCode(
            @PathVariable Long bandId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.regenerateInviteCode(principal.getUserId(), bandId)));
    }

    // 친구에게 밴드 초대 보내기 (#229)
    @PostMapping("/{bandId}/invitations")
    public ResponseEntity<ApiResponse<Void>> inviteFriend(
            @PathVariable Long bandId,
            @Valid @RequestBody BandInviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        bandService.inviteFriend(principal.getUserId(), bandId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 초대 코드로 가입
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<BandResponse>> joinBand(
            @Valid @RequestBody BandJoinRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.joinBand(principal.getUserId(), request)));
    }

    // 파트 바꾸기 (본인 또는 리더)
    @PatchMapping("/{bandId}/members/{memberId}")
    public ResponseEntity<ApiResponse<BandResponse>> changePart(
            @PathVariable Long bandId,
            @PathVariable Long memberId,
            @Valid @RequestBody BandMemberPartRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                bandService.changePart(principal.getUserId(), bandId, memberId, request)));
    }

    // 밴드 나가기
    @DeleteMapping("/{bandId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveBand(
            @PathVariable Long bandId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        bandService.leaveBand(principal.getUserId(), bandId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 멤버 내보내기 (리더만)
    @DeleteMapping("/{bandId}/members/{memberId}")
    public ResponseEntity<ApiResponse<BandResponse>> removeMember(
            @PathVariable Long bandId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.removeMember(principal.getUserId(), bandId, memberId)));
    }

    // 리더 넘기기 (리더만)
    @PatchMapping("/{bandId}/leader")
    public ResponseEntity<ApiResponse<BandResponse>> transferLeader(
            @PathVariable Long bandId,
            @Valid @RequestBody BandLeaderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(bandService.transferLeader(principal.getUserId(), bandId, request)));
    }
}
