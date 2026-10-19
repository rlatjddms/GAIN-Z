package com.project.gainz.domain.notification.entity;

import com.project.gainz.domain.notification.enums.NotificationType;
import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

// 알림 한 건. title: 어디서 (밴드 이름·'친구'), content: 무슨 일, redirectUrl: 누르면 갈 화면
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_notification_receiver", columnList = "receiver_id, is_read"))
@SQLDelete(sql = "UPDATE notification SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Notification extends BaseEntity {

    public static final int CONTENT_MAX = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = CONTENT_MAX)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private Boolean isRead = false;

    private String redirectUrl;

    @Builder
    public Notification(User receiver, NotificationType type, String title, String content, String redirectUrl) {
        this.receiver = receiver;
        this.type = type;
        this.title = title;
        this.content = content.length() > CONTENT_MAX ? content.substring(0, CONTENT_MAX - 1) + "…" : content;
        this.redirectUrl = redirectUrl;
    }

    public void markRead() {
        this.isRead = true;
    }
}
