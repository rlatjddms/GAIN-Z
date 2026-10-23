package com.project.gainz.domain.sheet.dto.request;

import jakarta.validation.constraints.NotNull;

// 이미 있는 악보(공용 악보)를 보관함에 담기
public record WorkspaceAddRequest(

        @NotNull(message = "담을 악보를 골라 주세요.")
        Long sheetId
) {
}
