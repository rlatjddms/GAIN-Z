package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.TabData;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.enums.InstrumentType;

import java.util.List;

// 악보 본문 (직접 쓴 코드 악보 + AI가 만든 마디별 악보)
public record SheetDetailResponse(
        Long id,
        String title,
        String artist,
        InstrumentType instrumentType,
        List<SheetType> sheetTypes,
        String originalKey,
        Integer bpm,
        String content,
        List<TabView> tabs,
        boolean mine            // 내가 만든 악보 (고칠 수 있음)
) {
    public record TabView(Integer measureNumber, String noteData, String recommendedFingering) {
        static TabView from(TabData tab) {
            return new TabView(tab.getMeasureNumber(), tab.getNoteData(), tab.getRecommendedFingering());
        }
    }

    public static SheetDetailResponse of(SheetMusic sheet, List<TabData> tabs, Long userId) {
        return new SheetDetailResponse(sheet.getId(), sheet.getTitle(), sheet.getArtist(), sheet.getInstrumentType(),
                List.copyOf(sheet.getSheetTypes()), sheet.getOriginalKey(), sheet.getBpm(), sheet.getContent(),
                tabs.stream().map(TabView::from).toList(), sheet.isOwnedBy(userId));
    }
}
