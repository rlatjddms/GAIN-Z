package com.project.gainz.domain.user.controller;

import com.project.gainz.domain.user.dto.response.SongSearchResponse;
import com.project.gainz.domain.user.service.SongSearchService;
import com.project.gainz.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 프로필 뮤직 곡 검색 (#228). 브라우저로 iTunes를 못 부르는 아이폰에서만 쓴다
@RestController
@RequestMapping("/api/v1/music")
@RequiredArgsConstructor
public class MusicController {

    private final SongSearchService songSearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SongSearchResponse>>> search(@RequestParam(defaultValue = "") String term) {
        return ResponseEntity.ok(ApiResponse.success(songSearchService.search(term)));
    }
}
