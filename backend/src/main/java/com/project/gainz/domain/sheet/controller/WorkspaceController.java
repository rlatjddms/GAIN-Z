package com.project.gainz.domain.sheet.controller;

import com.project.gainz.domain.sheet.dto.request.WorkspaceAddRequest;
import com.project.gainz.domain.sheet.dto.request.WorkspaceUpdateRequest;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceItemResponse;
import com.project.gainz.domain.sheet.service.SheetService;
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

import java.util.List;

// 내 악보 보관함 (본인 것만 보이고 고칠 수 있다)
@RestController
@RequestMapping("/api/v1/workspace/sheets")
@RequiredArgsConstructor
public class WorkspaceController {

    private final SheetService sheetService;

    // favorite=true면 즐겨찾기만, sort=recent(기본)/title
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceItemResponse>>> getWorkspace(
            @RequestParam(defaultValue = "false") boolean favorite,
            @RequestParam(defaultValue = "recent") String sort,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.getWorkspace(principal.getUserId(), favorite, sort)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceItemResponse>> addToWorkspace(
            @Valid @RequestBody WorkspaceAddRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.addToWorkspace(principal.getUserId(), request.sheetId())));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> getWorkspaceItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.getWorkspaceItem(principal.getUserId(), itemId)));
    }

    // 메모·바꾼 키·즐겨찾기 (보낸 항목만)
    @PatchMapping("/{itemId}")
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> updateWorkspaceItem(
            @PathVariable Long itemId,
            @Valid @RequestBody WorkspaceUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.updateWorkspaceItem(principal.getUserId(), itemId, request)));
    }

    // 내 악보로 복사 (#191): 이 항목이 사본을 가리키게 된다 (항목 ID는 그대로)
    @PostMapping("/{itemId}/copy")
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> copyToMine(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.copyToMine(principal.getUserId(), itemId)));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWorkspace(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sheetService.removeFromWorkspace(principal.getUserId(), itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
