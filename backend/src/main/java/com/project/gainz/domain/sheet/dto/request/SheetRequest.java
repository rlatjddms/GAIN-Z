package com.project.gainz.domain.sheet.dto.request;

import com.project.gainz.domain.sheet.enums.SheetType;
import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 올리는 악보의 곡 정보 (올리기·고치기 공용)
// content: 코드 붙여넣기로 만든 코드 악보 글 ([C]가사 형식, #157). 파일 악보는 생략
public record SheetRequest(

        @NotBlank(message = "곡 제목을 입력해 주세요.")
        @Size(max = 100, message = "곡 제목은 100자 이하로 입력해 주세요.")
        String title,

        @Size(max = 100, message = "아티스트는 100자 이하로 입력해 주세요.")
        String artist,

        @Pattern(regexp = SheetRequest.KEY_PATTERN, message = "키 형식이 올바르지 않습니다. (예: C, F#m, Bb)")
        String originalKey,      // 모르면 생략

        @Min(value = 20, message = "BPM은 20 이상이어야 합니다.")
        @Max(value = 300, message = "BPM은 300 이하이어야 합니다.")
        Integer bpm,             // 모르면 생략

        @NotNull(message = "악기를 골라 주세요.")
        InstrumentType instrumentType,

        @NotNull(message = "악보 종류를 골라 주세요.")
        SheetType sheetType,

        @Size(max = SheetRequest.MAX_CONTENT, message = "코드 악보는 20000자까지 저장할 수 있습니다.")
        String content
) {
    public static final int MAX_CONTENT = 20000;

    // 으뜸음(A~G) + 올림/내림(#, b) + 단조(m)
    public static final String KEY_PATTERN = "^[A-G][#b]?m?$";

    public String artistOrEmpty() {
        return artist == null ? "" : artist.trim();
    }
}
