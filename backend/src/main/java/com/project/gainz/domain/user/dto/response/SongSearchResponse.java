package com.project.gainz.domain.user.dto.response;

// 곡 검색 결과 한 곡 (#228). 프론트 api/user.js searchSongs의 결과와 같은 모양
public record SongSearchResponse(
        Long id,
        String title,
        String artist,
        String album,
        String artworkUrl,
        String previewUrl,
        String url
) {
}
