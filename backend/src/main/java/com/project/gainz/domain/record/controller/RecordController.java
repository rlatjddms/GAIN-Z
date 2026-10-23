package com.project.gainz.domain.record.controller;

import com.project.gainz.domain.record.dto.response.RecordResponse;
import com.project.gainz.domain.record.service.RecordService;
import com.project.gainz.global.common.dto.ApiResponse;
import com.project.gainz.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 업적 레코드 도감 (MY RECORDS)
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    // 전체 도감: 얻은 것·못 얻은 것과 진행도. 이미 달성한 것이 있으면 이때 지급한다
    @GetMapping
    public ResponseEntity<ApiResponse<RecordResponse.Collection>> getCollection(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(recordService.getCollection(principal.getUserId())));
    }
}
