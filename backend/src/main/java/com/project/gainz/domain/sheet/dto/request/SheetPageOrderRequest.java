package com.project.gainz.domain.sheet.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// 장 순서 바꾸기 (#191): 이 악보의 장 ID를 원하는 순서대로 빠짐없이
public record SheetPageOrderRequest(
        @NotEmpty(message = "장 순서를 보내 주세요.")
        @Size(max = 20, message = "악보는 최대 20장까지예요.")
        List<Long> pageIds
) {
}
