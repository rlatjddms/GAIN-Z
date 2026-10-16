package com.project.gainz.domain.practice.entity;

import com.project.gainz.domain.user.entity.User;
import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 정한 매일 연습 루틴 (예: 스케일 10회). 삭제하면 비활성화만 해서 지난 체크 기록은 남긴다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer targetCount; // 목표 횟수

    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder
    public Routine(User user, String content, Integer targetCount) {
        this.user = user;
        this.content = content;
        this.targetCount = targetCount;
        this.isActive = true;
    }

    public void update(String content, Integer targetCount) {
        this.content = content;
        this.targetCount = targetCount;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
