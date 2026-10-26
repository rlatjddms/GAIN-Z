package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class SetlistRequest {
    private SetlistRequest() {}

    public record Create(@NotBlank @Size(max = 80) String title) {}

    // 보낸 값만 바꾼다
    public record Update(@Size(max = 80) String title, Boolean favorite) {}

    public record Order(@NotNull @Size(max = 50) List<Long> ids) {}

    // 곡 저장. keepAttachmentIds는 남길 기존 첨부, 새 파일은 multipart files로 받는다
    public record Item(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 120) String artist,
            @Size(max = 500) String memo,
            Long sheetId,
            @Size(max = 8) List<Long> memberIds,
            @Size(max = 8) List<String> keepAttachmentIds,
            // 브라우저가 바로 올린 새 첨부 (#262)
            @Size(max = 8) List<String> uploadIds
    ) {
        public Item(String title, String artist, String memo, Long sheetId, List<Long> memberIds, List<String> keepAttachmentIds) {
            this(title, artist, memo, sheetId, memberIds, keepAttachmentIds, null);
        }
    }
}
