package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.Instant;

public final class BandBoardRequest {
    private BandBoardRequest() {}

    public record Post(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 10000) String content
    ) {}

    public record Comment(@NotBlank @Size(max = 500) String content, Long parentId) {}
    public record Notice(boolean notice, Instant noticeExpiresAt) {}
    public record Save(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 10000) String content,
            Boolean notice,
            @Size(max = 8) List<String> keepAttachmentIds,
            Instant noticeExpiresAt
    ) {
        public Save(String title, String content, Boolean notice, List<String> keepAttachmentIds) {
            this(title, content, notice, keepAttachmentIds, null);
        }
    }
}
