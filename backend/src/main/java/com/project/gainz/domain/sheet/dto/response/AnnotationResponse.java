package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetAnnotation;

import java.time.LocalDateTime;

// 장(쪽) 하나의 내 필기
public record AnnotationResponse(
        Long pageId,
        int pdfPage,
        String data,
        LocalDateTime updatedAt
) {
    public static AnnotationResponse from(SheetAnnotation annotation) {
        return new AnnotationResponse(annotation.getSheetPage().getId(), annotation.getPdfPage(),
                annotation.getData(), annotation.getUpdatedAt());
    }
}
