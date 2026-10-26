package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.SetlistRequest;
import com.project.gainz.domain.band.dto.response.BandUploadResponse;
import com.project.gainz.domain.band.dto.response.SetlistResponse;
import com.project.gainz.domain.band.service.BandBoardService.AttachmentFile;
import com.project.gainz.domain.band.service.BandAttachmentStorage;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bands/{bandId}/setlists")
public class SetlistController {
    private final SetlistService service;
    private final BandAttachmentStorage storage;

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

    // 첨부를 저장소에서 바로 받는 몇 분짜리 주소 (#262)
    @GetMapping("/{setlistId}/items/{itemId}/attachments/{attachmentId}/link")
    public ApiResponse<BandUploadResponse.Link> attachmentLink(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                               @PathVariable Long setlistId, @PathVariable Long itemId,
                                                               @PathVariable String attachmentId,
                                                               @RequestParam(defaultValue = "false") boolean download) {
        AttachmentFile file = service.file(me.getUserId(), bandId, setlistId, itemId, attachmentId);
        return ApiResponse.success(new BandUploadResponse.Link(storage.link(file.key(), file.name(), file.type(), download)));
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
