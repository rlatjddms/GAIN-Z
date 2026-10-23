package com.project.gainz.domain.notification.entity;

import com.project.gainz.domain.notification.enums.NotificationSetting;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class NotificationSettingConverter extends EnumNameConverter<NotificationSetting> {

    public NotificationSettingConverter() {
        super(NotificationSetting.class);
    }
}
