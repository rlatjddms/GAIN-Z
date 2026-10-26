package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.BandBoardRequest;
import com.project.gainz.domain.band.dto.response.BandUploadResponse;
import com.project.gainz.domain.band.dto.response.BandBoardResponse.*;
import com.project.gainz.domain.band.service.BandAttachmentStorage;
import com.project.gainz.domain.band.service.BandBoardService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bands/{bandId}/posts")
public class BandBoardController {
    private final BandBoardService service;
    private final BandAttachmentStorage storage;

    @GetMapping
    public ApiResponse<PostPage> list(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
                                      @RequestParam(required = false) Boolean notice,
                                      @RequestParam(defaultValue = "false") boolean mine,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if ((q != null && !q.isBlank()) || from != null || to != null) {
            return ApiResponse.success(service.list(me.getUserId(), bandId, page, size, notice, mine, q, from, to));
        }
        return ApiResponse.success(mine ? service.list(me.getUserId(), bandId, page, size, notice, true)
                : service.list(me.getUserId(), bandId, page, size, notice));
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

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId, @PathVariable Long postId) {
        service.delete(me.getUserId(), bandId, postId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{postId}/notice")
    public ApiResponse<Post> notice(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                    @PathVariable Long postId, @RequestBody BandBoardRequest.Notice request) {
        return ApiResponse.success(service.notice(me.getUserId(), bandId, postId, request.notice(), request.noticeExpiresAt()));
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

    // 첨부를 저장소에서 바로 받는 몇 분짜리 주소 (#262). 큰 영상도 서버를 거치지 않고 <video src>로 바로 튼다
    @GetMapping("/{postId}/attachment/link")
    public ApiResponse<BandUploadResponse.Link> fileLink(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                         @PathVariable Long postId, @RequestParam(defaultValue = "false") boolean download) {
        return ApiResponse.success(link(service.file(me.getUserId(), bandId, postId), download));
    }

    @GetMapping("/{postId}/attachments/{attachmentId}/link")
    public ApiResponse<BandUploadResponse.Link> attachmentLink(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                               @PathVariable Long postId, @PathVariable String attachmentId,
                                                               @RequestParam(defaultValue = "false") boolean download) {
        return ApiResponse.success(link(service.file(me.getUserId(), bandId, postId, attachmentId), download));
    }

    private BandUploadResponse.Link link(BandBoardService.AttachmentFile file, boolean download) {
        return new BandUploadResponse.Link(storage.link(file.key(), file.name(), file.type(), download));
    }

    @GetMapping("/{postId}/attachments.zip")
    public void archive(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                        @PathVariable Long postId, HttpServletResponse response) throws IOException {
        var files = service.allFiles(me.getUserId(), bandId, postId);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=community-" + postId + ".zip");
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 현재 인증 요청에서 스트리밍해 ZIP 전체를 메모리에 쌓지 않는다.
        try (var zip = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            for (int i = 0; i < files.size(); i++) {
                var file = files.get(i);
                // 순번으로 중복 이름을 구분하고 ZIP 내부 경로를 만들지 못하게 한다.
                String name = file.name().replaceAll("[\\\\/\\p{Cntrl}]", "_");
                zip.putNextEntry(new ZipEntry((i + 1) + "_" + name));
                try (var input = file.resource().getInputStream()) { input.transferTo(zip); }
                zip.closeEntry();
            }
        }
    }

    private ResponseEntity<Resource> fileResponse(BandBoardService.AttachmentFile file, boolean download) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.type()))
                .header("Content-Disposition", ContentDisposition.builder(download ? "attachment" : "inline")
                        .filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-store")
                .body(file.resource());
    }

    // 댓글은 쪽 단위로 (#188)
    @GetMapping("/{postId}/comments")
    public ApiResponse<CommentPage> commentPage(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                @PathVariable Long postId, @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.commentPage(me.getUserId(), bandId, postId, page, size));
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
