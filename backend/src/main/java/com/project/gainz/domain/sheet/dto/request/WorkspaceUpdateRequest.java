package com.project.gainz.domain.sheet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 보관함 악보의 내 설정 수정. 보낸 항목만 바꾼다 (null이면 그대로)
public record WorkspaceUpdateRequest(

        @Pattern(regexp = SheetRequest.KEY_PATTERN, message = "키 형식이 올바르지 않습니다. (예: C, F#m, Bb)")
        String customKey,        // 원래 키를 보내면 원래 키로 되돌림

        @Size(max = 2000, message = "메모는 2,000자 이하로 입력해 주세요.")
        String personalMemo,     // 빈 문자열이면 메모 지움

        Boolean isFavorite
) {
}
