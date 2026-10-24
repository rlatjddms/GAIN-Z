package com.project.gainz.domain.sheet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// 보관함 악보의 내 설정 수정. 보낸 항목만 바꾼다 (null이면 그대로)
public record WorkspaceUpdateRequest(

        @Pattern(regexp = SheetRequest.KEY_PATTERN, message = "키 형식이 올바르지 않습니다. (예: C, F#m, Bb)")
        String customKey,        // 원래 키를 보내면 원래 키로 되돌림

        @Size(max = 2000, message = "메모는 2,000자 이하로 입력해 주세요.")
        String personalMemo,     // 빈 문자열이면 메모 지움

        Boolean isFavorite,

        // 키 바꿔 치기 표에 내가 추가한 코드 (#192). 보낸 목록으로 통째로 바뀜, 빈 목록이면 지움
        @Size(max = 20, message = "코드는 20개까지 추가할 수 있어요.")
        List<@Size(max = 16, message = "코드가 너무 길어요.")
             @Pattern(regexp = CHORD_PATTERN, message = "코드 형식이 올바르지 않습니다. (예: Bb, E7, D/F#)") String> customChords
) {
    // 코드 한 개: 근음(A~G, #/b) + 꼬리(m7, sus4, add9, (b5) …) + /베이스
    public static final String CHORD_PATTERN = "^[A-G][#b]?[A-Za-z0-9()+#°ø-]{0,10}(/[A-G][#b]?)?$";

    public WorkspaceUpdateRequest(String customKey, String personalMemo, Boolean isFavorite) {
        this(customKey, personalMemo, isFavorite, null);
    }
}
