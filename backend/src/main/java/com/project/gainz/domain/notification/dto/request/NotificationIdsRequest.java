package com.project.gainz.domain.notification.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

// 골라서 읽음·지우기 (한 번에 100개까지)
public record NotificationIdsRequest(

        @NotEmpty(message = "알림을 하나 이상 골라 주세요.")
        @Size(max = 100, message = "알림은 한 번에 100개까지 고를 수 있습니다.")
        List<Long> ids
) {
}
