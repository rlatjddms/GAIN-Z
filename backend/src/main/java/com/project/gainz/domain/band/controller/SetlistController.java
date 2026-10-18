package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.SetlistRequest;
import com.project.gainz.domain.band.dto.response.SetlistResponse;
import com.project.gainz.domain.band.service.BandBoardService.AttachmentFile;
import com.project.gainz.domain.band.service.SetlistService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bands/{bandId}/setlists")
public class SetlistController {
    private final SetlistService service;

    @GetMapping
    public ApiResponse<List<SetlistResponse.Setlist>> list(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId) {
        return ApiResponse.success(service.list(me.getUserId(), bandId));
    }

    @PostMapping
    public ApiResponse<SetlistResponse.Setlist> create(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                       @Valid @RequestBody SetlistRequest.Create request) {
        return ApiResponse.success(service.create(me.getUserId(), bandId, request));
    }

    @PutMapping("/order")
    public ApiResponse<List<SetlistResponse.Setlist>> reorder(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                              @Valid @RequestBody SetlistRequest.Order request) {
        return ApiResponse.success(service.reorder(me.getUserId(), bandId, request));
    }

    // 멤버별 이번 주(월~일) 연습 시간과 루틴 달성
    @GetMapping("/practice")
    public ApiResponse<SetlistResponse.PracticeWeek> practice(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId) {
        return ApiResponse.success(service.practice(me.getUserId(), bandId, LocalDate.now()));
    }

    @PatchMapping("/{setlistId}")
    public ApiResponse<SetlistResponse.Setlist> update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                       @PathVariable Long setlistId, @Valid @RequestBody SetlistRequest.Update request) {
        return ApiResponse.success(service.update(me.getUserId(), bandId, setlistId, request));
    }

    @DeleteMapping("/{setlistId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long setlistId) {
        service.delete(me.getUserId(), bandId, setlistId);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/{setlistId}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SetlistResponse.Setlist> addItem(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                        @PathVariable Long setlistId, @Valid @RequestPart("item") SetlistRequest.Item request,
                                                        @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.success(service.saveItem(me.getUserId(), bandId, setlistId, null, request, files));
    }

    @PutMapping(value = "/{setlistId}/items/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SetlistResponse.Setlist> saveItem(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                         @PathVariable Long setlistId, @PathVariable Long itemId,
                                                         @Valid @RequestPart("item") SetlistRequest.Item request,
                                                         @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.success(service.saveItem(me.getUserId(), bandId, setlistId, itemId, request, files));
    }

    @DeleteMapping("/{setlistId}/items/{itemId}")
    public ApiResponse<SetlistResponse.Setlist> deleteItem(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                           @PathVariable Long setlistId, @PathVariable Long itemId) {
        return ApiResponse.success(service.deleteItem(me.getUserId(), bandId, setlistId, itemId));
    }

    @PutMapping("/{setlistId}/items/order")
    public ApiResponse<SetlistResponse.Setlist> reorderItems(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                             @PathVariable Long setlistId, @Valid @RequestBody SetlistRequest.Order request) {
        return ApiResponse.success(service.reorderItems(me.getUserId(), bandId, setlistId, request));
    }

    @GetMapping("/{setlistId}/items/{itemId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> attachment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                               @PathVariable Long setlistId, @PathVariable Long itemId,
                                               @PathVariable String attachmentId, @RequestParam(defaultValue = "false") boolean download) {
        AttachmentFile file = service.file(me.getUserId(), bandId, setlistId, itemId, attachmentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                .header("Content-Disposition", ContentDisposition.builder(download ? "attachment" : "inline")
                        .filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .body(file.resource());
    }
}
