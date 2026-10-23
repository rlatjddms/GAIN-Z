package com.project.gainz.domain.sheet.dto.request;

import jakarta.validation.constraints.NotNull;

// 장(쪽) 하나의 필기 저장. data = 필기 JSON 글 (획이 없으면 필기를 지운다)
public record AnnotationRequest(
        @NotNull(message = "필기 내용이 없습니다.")
        String data
) {
}
