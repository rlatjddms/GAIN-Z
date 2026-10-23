package com.project.gainz.domain.band.dto.response;

import com.project.gainz.global.common.enums.InstrumentType;

import java.time.LocalDateTime;
import java.util.List;

public final class JamResponse {
    private JamResponse() {}

    // 목록 카드. favorite: 즐겨찾기(밴드 전체), canManage: 보는 사람이 고치거나 지울 수 있는지
    public record Summary(Long id, String title, int bpm, Song song, int trackCount, String creatorName, LocalDateTime updatedAt,
                          boolean favorite, boolean canManage) {}

    // canManage: 보는 사람이 잼을 고치거나 지울 수 있는지 (만든 멤버·리더)
    public record Jam(Long id, String title, int bpm, Song song, String creatorName, boolean canManage, List<Track> tracks) {}

    public record Song(Long setlistItemId, String title) {}

    // canDelete: 올린 멤버·리더만 지울 수 있다. url로 녹음 파일을 받는다
    public record Track(Long id, Long userId, String nickname, InstrumentType part, String name, String type,
                        int volume, int offsetMs, Integer durationMs, boolean canDelete,
                        String url, LocalDateTime createdAt) {}
}
