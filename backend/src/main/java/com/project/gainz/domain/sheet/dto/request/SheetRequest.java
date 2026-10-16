package com.project.gainz.domain.sheet.dto.request;

import com.project.gainz.global.common.enums.InstrumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 직접 쓰는 코드 악보 만들기·고치기 요청
public record SheetRequest(

        @NotBlank(message = "곡 제목을 입력해 주세요.")
        @Size(max = 100, message = "곡 제목은 100자 이하로 입력해 주세요.")
        String title,

        @Size(max = 100, message = "아티스트는 100자 이하로 입력해 주세요.")
        String artist,

        @NotBlank(message = "원래 키를 골라 주세요.")
        @Pattern(regexp = SheetRequest.KEY_PATTERN, message = "키 형식이 올바르지 않습니다. (예: C, F#m, Bb)")
        String originalKey,

        @Min(value = 20, message = "BPM은 20 이상이어야 합니다.")
        @Max(value = 300, message = "BPM은 300 이하이어야 합니다.")
        Integer bpm,             // 모르면 생략

        @NotNull(message = "악기를 골라 주세요.")
        InstrumentType instrumentType,

        @Size(max = 20000, message = "악보 내용은 20,000자 이하로 입력해 주세요.")
        String content           // 가사 사이에 [C] [Am7] 처럼 코드를 넣는 형식
) {
    // 으뜸음(A~G) + 올림/내림(#, b) + 단조(m)
    public static final String KEY_PATTERN = "^[A-G][#b]?m?$";

    public String artistOrEmpty() {
        return artist == null ? "" : artist.trim();
    }
}
