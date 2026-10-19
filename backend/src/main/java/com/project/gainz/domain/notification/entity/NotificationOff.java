package com.project.gainz.domain.notification.entity;

import com.project.gainz.domain.notification.enums.NotificationSetting;
import com.project.gainz.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 꺼 둔 알림 설정 한 줄. 줄이 없으면 켜진 것 (처음에는 모두 켜짐). 다시 켜면 줄을 지운다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_off", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "setting"}))
public class NotificationOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // MariaDB enum 타입으로 만들면 항목을 추가할 때마다 컬럼을 고쳐야 해서 varchar로 둔다
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    private NotificationSetting setting;

    public NotificationOff(User user, NotificationSetting setting) {
        this.user = user;
        this.setting = setting;
    }
}
