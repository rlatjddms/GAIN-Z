package com.project.gainz.domain.sheet.controller;

import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.SheetSummaryResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.service.SheetService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sheets")
@RequiredArgsConstructor
public class SheetController {

    private final SheetService sheetService;

    // 공용 악보 목록
    @GetMapping
    public ResponseEntity<ApiResponse<List<SheetSummaryResponse>>> getSharedSheets() {
        return ResponseEntity.ok(ApiResponse.success(sheetService.getSharedSheets()));
    }

    @GetMapping("/{sheetId}")
    public ResponseEntity<ApiResponse<SheetDetailResponse>> getSheet(
            @PathVariable Long sheetId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.getSheet(principal.getUserId(), sheetId)));
    }

    // 코드 악보 직접 쓰기 (만들면 바로 내 보관함에 담김)
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> createSheet(
            @Valid @RequestBody SheetRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.createSheet(principal.getUserId(), request)));
    }

    // 내가 만든 악보 고치기
    @PutMapping("/{sheetId}")
    public ResponseEntity<ApiResponse<SheetDetailResponse>> updateSheet(
            @PathVariable Long sheetId,
            @Valid @RequestBody SheetRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.updateSheet(principal.getUserId(), sheetId, request)));
    }
}
