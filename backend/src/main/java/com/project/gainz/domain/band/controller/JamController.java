package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.JamRequest;
import com.project.gainz.domain.band.dto.response.BandUploadResponse;
import com.project.gainz.domain.band.dto.response.JamResponse;
import com.project.gainz.domain.band.service.BandBoardService.AttachmentFile;
import com.project.gainz.domain.band.service.BandAttachmentStorage;
import com.project.gainz.domain.band.service.JamService;
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

// 레이어 잼 (비동기 합주 멀티트랙)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bands/{bandId}/jams")
public class JamController {
    private final JamService service;
    private final BandAttachmentStorage storage;

    @GetMapping
    public ApiResponse<List<JamResponse.Summary>> list(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId) {
        return ApiResponse.success(service.list(me.getUserId(), bandId));
    }

    @PostMapping
    public ApiResponse<JamResponse.Jam> create(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                               @Valid @RequestBody JamRequest.Save request) {
        return ApiResponse.success(service.create(me.getUserId(), bandId, request));
    }

    @GetMapping("/{jamId}")
    public ApiResponse<JamResponse.Jam> get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long jamId) {
        return ApiResponse.success(service.get(me.getUserId(), bandId, jamId));
    }

    @PutMapping("/{jamId}")
    public ApiResponse<JamResponse.Jam> update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long jamId,
                                               @Valid @RequestBody JamRequest.Save request) {
        return ApiResponse.success(service.update(me.getUserId(), bandId, jamId, request));
    }

    @PatchMapping("/{jamId}/favorite")
    public ApiResponse<JamResponse.Summary> favorite(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long jamId,
                                                    @Valid @RequestBody JamRequest.Favorite request) {
        return ApiResponse.success(service.setFavorite(me.getUserId(), bandId, jamId, request.favorite()));
    }

    @DeleteMapping("/{jamId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long jamId) {
        service.delete(me.getUserId(), bandId, jamId);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/{jamId}/tracks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JamResponse.Track> addTrack(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long jamId,
                                                   @Valid @RequestPart("track") JamRequest.Track request,
                                                   @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.success(service.addTrack(me.getUserId(), bandId, jamId, request, file));
    }

    @PatchMapping("/{jamId}/tracks/{trackId}")
    public ApiResponse<JamResponse.Track> updateTrack(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                      @PathVariable Long jamId, @PathVariable Long trackId,
                                                      @Valid @RequestBody JamRequest.TrackUpdate request) {
        return ApiResponse.success(service.updateTrack(me.getUserId(), bandId, jamId, trackId, request));
    }

    @DeleteMapping("/{jamId}/tracks/{trackId}")
    public ApiResponse<Void> deleteTrack(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                         @PathVariable Long jamId, @PathVariable Long trackId) {
        service.deleteTrack(me.getUserId(), bandId, jamId, trackId);
        return ApiResponse.success(null);
    }

    // 녹음 파일을 저장소에서 바로 받는 몇 분짜리 주소 (#262)
    @GetMapping("/{jamId}/tracks/{trackId}/file/link")
    public ApiResponse<BandUploadResponse.Link> fileLink(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                         @PathVariable Long jamId, @PathVariable Long trackId,
                                                         @RequestParam(defaultValue = "false") boolean download) {
        AttachmentFile file = service.file(me.getUserId(), bandId, jamId, trackId);
        return ApiResponse.success(new BandUploadResponse.Link(storage.link(file.key(), file.name(), file.type(), download)));
    }

    @GetMapping("/{jamId}/tracks/{trackId}/file")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                         @PathVariable Long jamId, @PathVariable Long trackId,
                                         @RequestParam(defaultValue = "false") boolean download) {
        AttachmentFile file = service.file(me.getUserId(), bandId, jamId, trackId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                .header("Content-Disposition", ContentDisposition.builder(download ? "attachment" : "inline")
                        .filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .body(file.resource());
    }
}
