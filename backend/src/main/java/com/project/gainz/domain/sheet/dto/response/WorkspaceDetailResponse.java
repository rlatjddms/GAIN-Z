package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetPage;
import com.project.gainz.domain.sheet.entity.TabData;
import com.project.gainz.domain.sheet.entity.UserWorkspace;

import java.time.LocalDateTime;
import java.util.List;

// 보관함 악보 보기: 악보 본문 + 내 설정
public record WorkspaceDetailResponse(
        Long id,
        SheetDetailResponse sheet,
        String customKey,
        String personalMemo,
        boolean favorite,
        LocalDateTime addedAt
) {
    public static WorkspaceDetailResponse of(UserWorkspace item, List<SheetPage> pages, List<TabData> tabs, Long userId) {
        return new WorkspaceDetailResponse(item.getId(), SheetDetailResponse.of(item.getSheetMusic(), pages, tabs, userId),
                item.getCustomKey(), item.getPersonalMemo(), Boolean.TRUE.equals(item.getIsFavorite()), item.getCreatedAt());
    }
}
