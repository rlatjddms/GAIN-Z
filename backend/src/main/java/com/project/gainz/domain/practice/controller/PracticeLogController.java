package com.project.gainz.domain.practice.controller;

import com.project.gainz.domain.practice.dto.request.PracticeLogCreateRequest;
import com.project.gainz.domain.practice.dto.response.PracticeLogResponse;
import com.project.gainz.domain.practice.service.PracticeLogService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice-logs")
@RequiredArgsConstructor
public class PracticeLogController {

    private final PracticeLogService practiceLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<PracticeLogResponse>> createPracticeLog(
            @Valid @RequestBody PracticeLogCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PracticeLogResponse response = practiceLogService.createPracticeLog(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
