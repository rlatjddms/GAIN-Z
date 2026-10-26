package com.project.gainz.domain.band.dto.request;

import com.project.gainz.domain.band.enums.BandUploadKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class BandUploadRequest {
    private BandUploadRequest() {}

    // 올리기 전에 크기·이름을 알린다. 실제 형식은 다 올린 뒤 파일 머리로 다시 확인한다
    public record Start(
            @NotNull BandUploadKind kind,
            @NotBlank @Size(max = 255) String name,
            @NotNull @Positive Long size,
            @Size(max = 100) String contentType
    ) {}
}
