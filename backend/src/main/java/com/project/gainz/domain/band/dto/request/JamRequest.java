package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class JamRequest {
    private JamRequest() {}

    public static final int OFFSET_MIN_MS = -10_000;
    public static final int OFFSET_MAX_MS = 600_000;
    public static final int VOLUME_MAX = 150;

    // 잼 만들기·수정. setlistItemId는 연결할 셋리스트 곡 (없으면 연결 안 함)
    // 즐겨찾기 켜기·끄기
    public record Favorite(@jakarta.validation.constraints.NotNull Boolean favorite) {
    }

    public record Save(
            @NotBlank @Size(max = 60) String title,
            @NotNull @Min(40) @Max(240) Integer bpm,
            Long setlistItemId
    ) {}

    // 녹음 올리기. 파일은 multipart file로 받는다
    public record Track(
            @NotNull @Min(OFFSET_MIN_MS) @Max(OFFSET_MAX_MS) Integer offsetMs,
            @Min(0) @Max(VOLUME_MAX) Integer volume,
            @Min(0) @Max(3_600_000) Integer durationMs
    ) {}

    // 보낸 값만 바꾼다. 뮤트·솔로는 듣는 사람 화면에서만 바꾼다
    public record TrackUpdate(
            @Min(0) @Max(VOLUME_MAX) Integer volume,
            @Min(OFFSET_MIN_MS) @Max(OFFSET_MAX_MS) Integer offsetMs
    ) {}
}
