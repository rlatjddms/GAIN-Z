package com.project.gainz.domain.practice.controller;

import com.project.gainz.domain.practice.dto.request.RoutineCheckRequest;
import com.project.gainz.domain.practice.dto.request.RoutineRequest;
import com.project.gainz.domain.practice.dto.response.RoutineDailySummaryResponse;
import com.project.gainz.domain.practice.dto.response.RoutineResponse;
import com.project.gainz.domain.practice.service.RoutineService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    // 그날 일정 루틴 + 그날 체크 현황 (date 생략 시 오늘)
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoutineResponse>>> getRoutines(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(routineService.getRoutines(principal.getUserId(), target)));
    }

    // 기간별 날짜마다 일정 루틴 수 / 완료 수 (일정이 있는 날만)
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<RoutineDailySummaryResponse>>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(routineService.getDailySummary(principal.getUserId(), from, to)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoutineResponse>> createRoutine(
            @Valid @RequestBody RoutineRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(routineService.createRoutine(principal.getUserId(), request)));
    }

    @PatchMapping("/{routineId}")
    public ResponseEntity<ApiResponse<RoutineResponse>> updateRoutine(
            @PathVariable Long routineId,
            @Valid @RequestBody RoutineRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(routineService.updateRoutine(principal.getUserId(), routineId, request)));
    }

    // 오늘부터 그만하기 (지난 기록은 남김)
    @DeleteMapping("/{routineId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoutine(
            @PathVariable Long routineId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        routineService.deleteRoutine(principal.getUserId(), routineId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 그날 수행 횟수 정하기
    @PutMapping("/{routineId}/checks/{date}")
    public ResponseEntity<ApiResponse<RoutineResponse>> checkRoutine(
            @PathVariable Long routineId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody RoutineCheckRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(routineService.checkRoutine(principal.getUserId(), routineId, date, request)));
    }
}
