package com.project.gainz.domain.band.controller;

import com.project.gainz.domain.band.dto.request.BandUploadRequest;
import com.project.gainz.domain.band.dto.response.BandUploadResponse;
import com.project.gainz.domain.band.service.BandUploadService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 큰 첨부·녹음을 브라우저가 저장소에 바로 올린다 (#262).
// 1) 여기서 주소를 받고 2) 그 주소에 PUT 한 뒤 3) 글·곡·트랙을 저장할 때 uploadId를 보낸다
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bands/{bandId}/uploads")
public class BandUploadController {

    private final BandUploadService service;

    @PostMapping
    public ApiResponse<BandUploadResponse.Started> start(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long bandId,
                                                         @Valid @RequestBody BandUploadRequest.Start request) {
        return ApiResponse.success(service.start(me.getUserId(), bandId, request));
    }
}
