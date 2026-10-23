package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.entity.SheetPage;
import com.project.gainz.domain.sheet.entity.TabData;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.enums.InstrumentType;

import java.util.List;

// 악보 본문 (올린 파일 장들 + 마디별 악보 + 코드 붙여넣기로 만든 코드 악보)
public record SheetDetailResponse(
        Long id,
        String title,
        String artist,
        InstrumentType instrumentType,
        List<SheetType> sheetTypes,
        String originalKey,
        Integer bpm,
        String content,
        List<PageView> pages,   // 파일은 GET /api/v1/sheets/{id}/pages/{pageId}
        List<TabView> tabs,
        boolean mine            // 내가 올린 악보 (고칠 수 있음)
) {
    public record PageView(Long id, Integer pageNumber, String contentType, String fileName, Long size) {
        static PageView from(SheetPage page) {
            return new PageView(page.getId(), page.getPageNumber(), page.getContentType(), page.getFileName(), page.getSize());
        }
    }

    public record TabView(Integer measureNumber, String noteData, String recommendedFingering) {
        static TabView from(TabData tab) {
            return new TabView(tab.getMeasureNumber(), tab.getNoteData(), tab.getRecommendedFingering());
        }
    }

    public static SheetDetailResponse of(SheetMusic sheet, List<SheetPage> pages, List<TabData> tabs, Long userId) {
        return new SheetDetailResponse(sheet.getId(), sheet.getTitle(), sheet.getArtist(), sheet.getInstrumentType(),
                List.copyOf(sheet.getSheetTypes()), sheet.getOriginalKey(), sheet.getBpm(), sheet.getContent(),
                pages.stream().map(PageView::from).toList(), tabs.stream().map(TabView::from).toList(),
                sheet.isOwnedBy(userId));
    }
}
