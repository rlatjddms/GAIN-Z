package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.BandBoardRequest;
import com.project.gainz.domain.band.dto.response.BandBoardResponse.*;
import com.project.gainz.domain.band.service.BandBoardService;
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
@RequestMapping("/api/v1/bands/{bandId}/posts")
public class BandBoardController {
    private final BandBoardService service;

    @GetMapping
    public ApiResponse<PostPage> list(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
                                      @RequestParam(required = false) Boolean notice) {
        return ApiResponse.success(service.list(me.getUserId(), bandId, page, size, notice));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Post> create(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                    @Valid @RequestBody BandBoardRequest.Post request) {
        return ApiResponse.success(service.create(me.getUserId(), bandId, request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Post> createWithFiles(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                             @Valid @RequestPart("post") BandBoardRequest.Save request,
                                             @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.success(service.save(me.getUserId(), bandId, null, request, files));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Post> saveWithFiles(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                           @PathVariable Long postId, @Valid @RequestPart("post") BandBoardRequest.Save request,
                                           @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ApiResponse.success(service.save(me.getUserId(), bandId, postId, request, files));
    }

    @GetMapping("/{postId}")
    public ApiResponse<Post> get(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long postId) {
        return ApiResponse.success(service.get(me.getUserId(), bandId, postId));
    }

    @PatchMapping("/{postId}")
    public ApiResponse<Post> update(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                    @PathVariable Long postId, @Valid @RequestBody BandBoardRequest.Post request) {
        return ApiResponse.success(service.update(me.getUserId(), bandId, postId, request));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long postId) {
        service.delete(me.getUserId(), bandId, postId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{postId}/notice")
    public ApiResponse<Post> notice(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                    @PathVariable Long postId, @RequestBody BandBoardRequest.Notice request) {
        return ApiResponse.success(service.notice(me.getUserId(), bandId, postId, request.notice()));
    }

    @PutMapping(value = "/{postId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Post> attach(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                    @PathVariable Long postId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(service.attach(me.getUserId(), bandId, postId, file));
    }

    @DeleteMapping("/{postId}/attachment")
    public ApiResponse<Post> removeAttachment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                             @PathVariable Long postId) {
        return ApiResponse.success(service.removeAttachment(me.getUserId(), bandId, postId));
    }

    @GetMapping("/{postId}/attachment")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                         @PathVariable Long postId, @RequestParam(defaultValue = "false") boolean download) {
        return fileResponse(service.file(me.getUserId(), bandId, postId), download);
    }

    @GetMapping("/{postId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> attachment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                               @PathVariable Long postId, @PathVariable String attachmentId,
                                               @RequestParam(defaultValue = "false") boolean download) {
        return fileResponse(service.file(me.getUserId(), bandId, postId, attachmentId), download);
    }

    private ResponseEntity<Resource> fileResponse(BandBoardService.AttachmentFile file, boolean download) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                .header("Content-Disposition", ContentDisposition.builder(download ? "attachment" : "inline")
                        .filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .body(file.resource());
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<List<Comment>> comments(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                               @PathVariable Long postId) {
        return ApiResponse.success(service.comments(me.getUserId(), bandId, postId));
    }

    @PostMapping("/{postId}/comments")
    public ApiResponse<Comment> addComment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                           @PathVariable Long postId, @Valid @RequestBody BandBoardRequest.Comment request) {
        return ApiResponse.success(service.addComment(me.getUserId(), bandId, postId, request));
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Comment> updateComment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                              @PathVariable Long postId, @PathVariable Long commentId,
                                              @Valid @RequestBody BandBoardRequest.Comment request) {
        return ApiResponse.success(service.updateComment(me.getUserId(), bandId, postId, commentId, request));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                          @PathVariable Long postId, @PathVariable Long commentId) {
        service.deleteComment(me.getUserId(), bandId, postId, commentId);
        return ApiResponse.success(null);
    }
}
