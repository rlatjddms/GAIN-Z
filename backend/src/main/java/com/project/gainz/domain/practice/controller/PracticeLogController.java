package com.project.gainz.domain.practice.controller;

import com.project.gainz.domain.practice.dto.request.DailyPracticeUpdateRequest;
import com.project.gainz.domain.practice.dto.request.PracticeLogCreateRequest;
import com.project.gainz.domain.practice.dto.response.DailyPracticeResponse;
import com.project.gainz.domain.practice.dto.response.PracticeRecordResponse;
import com.project.gainz.domain.practice.service.PracticeLogService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/practice-logs")
@RequiredArgsConstructor
public class PracticeLogController {

    private final PracticeLogService practiceLogService;

    // 타이머 구간 저장
    @PostMapping
    public ResponseEntity<ApiResponse<PracticeRecordResponse>> recordSession(
            @Valid @RequestBody PracticeLogCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(practiceLogService.recordSession(principal.getUserId(), request)));
    }

    // 기간별 일별 합계
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<DailyPracticeResponse>>> getDailyTotals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(practiceLogService.getDailyTotals(principal.getUserId(), from, to)));
    }

    // 하루 총 연습 시간 직접 수정
    @PutMapping("/daily/{date}")
    public ResponseEntity<ApiResponse<PracticeRecordResponse>> updateDailyTotal(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DailyPracticeUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(practiceLogService.updateDailyTotal(principal.getUserId(), date, request)));
    }
}
