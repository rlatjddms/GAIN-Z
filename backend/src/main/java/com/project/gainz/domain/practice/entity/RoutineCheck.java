package com.project.gainz.domain.practice.entity;

import com.project.gainz.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 루틴의 날짜별 수행 횟수 (루틴 하나에 하루 한 줄)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_routine_check_routine_date", columnNames = {"routine_id", "practice_date"}))
public class RoutineCheck extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Column(nullable = false)
    private LocalDate practiceDate;

    @Column(nullable = false)
    private Integer practiceCount = 0; // 현재 수행 횟수

    @Column(nullable = false)
    private Boolean isCompleted = false;

    @Builder
    public RoutineCheck(Routine routine, LocalDate practiceDate) {
        this.routine = routine;
        this.practiceDate = practiceDate;
        this.practiceCount = 0;
        this.isCompleted = false;
    }

    // 수행 횟수를 정하고, 목표 횟수에 닿으면 완료로 표시
    public void updateCount(int count) {
        this.practiceCount = count;
        this.isCompleted = count >= routine.getTargetCount();
    }
}
