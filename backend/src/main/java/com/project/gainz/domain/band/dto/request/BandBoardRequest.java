package com.project.gainz.domain.band.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BandBoardRequest {
    private BandBoardRequest() {}

    public record Post(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 10000) String content
    ) {}

    public record Comment(@NotBlank @Size(max = 500) String content, Long parentId) {}
    public record Notice(boolean notice) {}
}
