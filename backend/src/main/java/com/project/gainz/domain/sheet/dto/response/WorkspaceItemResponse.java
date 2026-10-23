package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.SheetPage;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.enums.InstrumentType;

import java.time.LocalDateTime;
import java.util.List;

// 보관함 목록 한 줄
public record WorkspaceItemResponse(
        Long id,                // 보관함 항목 id
        Long sheetId,
        String title,
        String artist,
        InstrumentType instrumentType,
        SheetType sheetType,    // 대표 악보 종류
        String originalKey,
        String customKey,       // 연습 키. null이면 원래 키
        Integer bpm,
        int pageCount,
        Long coverPageId,       // 미리보기로 쓸 첫 장 (이미지일 때만)
        boolean favorite,
        boolean hasMemo,
        boolean mine,
        LocalDateTime addedAt
) {
    // pages: 이 악보의 장들 (장 순서대로)
    public static WorkspaceItemResponse of(UserWorkspace item, List<SheetPage> pages, Long userId) {
        SheetMusic sheet = item.getSheetMusic();
        Long cover = pages.isEmpty() || !pages.get(0).isImage() ? null : pages.get(0).getId();
        return new WorkspaceItemResponse(item.getId(), sheet.getId(), sheet.getTitle(), sheet.getArtist(),
                sheet.getInstrumentType(), sheet.getSheetTypes().isEmpty() ? null : sheet.getSheetTypes().get(0),
                sheet.getOriginalKey(), item.getCustomKey(), sheet.getBpm(), pages.size(), cover,
                Boolean.TRUE.equals(item.getIsFavorite()), item.getPersonalMemo() != null, sheet.isOwnedBy(userId),
                item.getCreatedAt());
    }
}
