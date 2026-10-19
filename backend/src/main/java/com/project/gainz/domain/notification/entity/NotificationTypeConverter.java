package com.project.gainz.domain.notification.entity;

import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.global.common.converter.EnumNameConverter;
import jakarta.persistence.Converter;

@Converter
public class NotificationTypeConverter extends EnumNameConverter<NotificationType> {

    public NotificationTypeConverter() {
        super(NotificationType.class);
    }
}
