package com.project.gainz.domain.practice.entity;

import com.project.gainz.domain.practice.enums.PracticeSource;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

// 연습 기록 한 건 = 타이머 구간 하나 또는 수동 수정 차이분. 하루 기록은 같은 날짜의 합계로 계산한다
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE practice_log SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class PracticeLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 수동 수정으로 하루 시간을 줄이면 음수가 된다
    @Column(nullable = false)
    private Long durationSeconds = 0L;

    // 달성률 보너스는 없어졌다. 기존 데이터 호환을 위해 컬럼만 남긴다
    @Column(nullable = false)
    private Double achievementRate = 0.0;

    @Column(nullable = false)
    private Integer expGained = 0;

    @Column(nullable = false)
    private LocalDate createdDate;

    // 기존 데이터는 null (= TIMER)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PracticeSource source;

    @Builder
    public PracticeLog(User user, Long durationSeconds, Integer expGained, LocalDate createdDate, PracticeSource source) {
        this.user = user;
        this.durationSeconds = durationSeconds;
        this.expGained = expGained;
        this.createdDate = createdDate;
        this.source = source;
    }
}
