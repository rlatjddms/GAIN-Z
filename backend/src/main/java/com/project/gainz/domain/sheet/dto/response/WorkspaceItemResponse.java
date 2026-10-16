package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.UserWorkspace;
import com.project.gainz.global.common.enums.InstrumentType;

import java.time.LocalDateTime;

// 보관함 목록 한 줄
public record WorkspaceItemResponse(
        Long id,                // 보관함 항목 id
        Long sheetId,
        String title,
        String artist,
        InstrumentType instrumentType,
        String originalKey,
        String customKey,       // null이면 원래 키로 봄
        Integer bpm,
        boolean favorite,
        boolean hasMemo,
        boolean mine,
        LocalDateTime addedAt
) {
    public static WorkspaceItemResponse of(UserWorkspace item, Long userId) {
        SheetMusic sheet = item.getSheetMusic();
        return new WorkspaceItemResponse(item.getId(), sheet.getId(), sheet.getTitle(), sheet.getArtist(),
                sheet.getInstrumentType(), sheet.getOriginalKey(), item.getCustomKey(), sheet.getBpm(),
                Boolean.TRUE.equals(item.getIsFavorite()), item.getPersonalMemo() != null, sheet.isOwnedBy(userId),
                item.getCreatedAt());
    }
}
