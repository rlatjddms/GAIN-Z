package com.project.gainz.domain.sheet.dto.response;

import com.project.gainz.domain.sheet.entity.SheetMusic;
import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.enums.InstrumentType;

import java.util.List;

// 악보 목록 한 줄 (공용 악보 목록)
public record SheetSummaryResponse(
        Long id,
        String title,
        String artist,
        InstrumentType instrumentType,
        List<SheetType> sheetTypes,
        String originalKey,
        Integer bpm
) {
    public static SheetSummaryResponse from(SheetMusic sheet) {
        return new SheetSummaryResponse(sheet.getId(), sheet.getTitle(), sheet.getArtist(), sheet.getInstrumentType(),
                List.copyOf(sheet.getSheetTypes()), sheet.getOriginalKey(), sheet.getBpm());
    }
}
