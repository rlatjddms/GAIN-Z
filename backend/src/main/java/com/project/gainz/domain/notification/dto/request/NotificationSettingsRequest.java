package com.project.gainz.domain.notification.dto.request;

import com.project.gainz.domain.notification.enums.NotificationSetting;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

// 바꿀 항목만 { FRIEND_REQUEST: false, NOTICE: true, … }
public record NotificationSettingsRequest(

        @NotEmpty(message = "바꿀 알림 설정을 골라 주세요.")
        Map<NotificationSetting, Boolean> settings
) {
}
