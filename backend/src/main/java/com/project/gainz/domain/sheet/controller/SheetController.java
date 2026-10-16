package com.project.gainz.domain.sheet.controller;

import com.project.gainz.domain.sheet.dto.request.SheetRequest;
import com.project.gainz.domain.sheet.dto.response.SheetDetailResponse;
import com.project.gainz.domain.sheet.dto.response.SheetSummaryResponse;
import com.project.gainz.domain.sheet.dto.response.WorkspaceDetailResponse;
import com.project.gainz.domain.sheet.service.SheetService;
import com.project.gainz.domain.sheet.service.SheetService.PageFile;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    // 악보 파일 올리기 (multipart: info = 곡 정보 JSON, files = 이미지·PDF 여러 장). 올리면 바로 내 보관함에 담김
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>> uploadSheet(
            @Valid @RequestPart("info") SheetRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.uploadSheet(principal.getUserId(), request, files)));
    }

    // 내가 올린 악보의 곡 정보 고치기
    @PutMapping("/{sheetId}")
    public ResponseEntity<ApiResponse<SheetDetailResponse>> updateSheet(
            @PathVariable Long sheetId,
            @Valid @RequestBody SheetRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.updateSheet(principal.getUserId(), sheetId, request)));
    }

    // 장 더 올리기 (뒤에 붙음)
    @PostMapping(value = "/{sheetId}/pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SheetDetailResponse>> addPages(
            @PathVariable Long sheetId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.addPages(principal.getUserId(), sheetId, files)));
    }

    @DeleteMapping("/{sheetId}/pages/{pageId}")
    public ResponseEntity<ApiResponse<SheetDetailResponse>> deletePage(
            @PathVariable Long sheetId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(sheetService.deletePage(principal.getUserId(), sheetId, pageId)));
    }

    // 장 파일 (화면에서 바로 보여 주도록 inline, 다른 형식으로 해석하지 않게 nosniff)
    @GetMapping("/{sheetId}/pages/{pageId}")
    public ResponseEntity<Resource> getPageFile(
            @PathVariable Long sheetId,
            @PathVariable Long pageId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PageFile file = sheetService.getPageFile(principal.getUserId(), sheetId, pageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .body(file.resource());
    }
}
