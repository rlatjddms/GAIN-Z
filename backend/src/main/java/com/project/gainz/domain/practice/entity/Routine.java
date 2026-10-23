package com.project.gainz.domain.practice.entity;

import com.project.gainz.domain.practice.enums.RoutineRepeat;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

// 사용자가 정한 연습 루틴 (예: 월·수·금 스케일 10회).
// 반복 방식·요일·기간으로 그날 일정인지 판단한다. 그만하면 종료일을 정하거나 비활성화해서 지난 체크 기록은 남긴다
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

    // 기존 데이터는 null (= 매일, 기간 제한 없음)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private RoutineRepeat repeatType;

    // 반복 요일 비트 (월=1, 화=2, 수=4 … 일=64). WEEKLY일 때만 사용
    private Integer repeatDays;

    private LocalDate startDate;

    private LocalDate endDate;   // null이면 계속

    @Builder
    public Routine(User user, String content, Integer targetCount,
                   RoutineRepeat repeatType, Set<DayOfWeek> repeatDays, LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.content = content;
        this.targetCount = targetCount;
        this.isActive = true;
        changeSchedule(repeatType, repeatDays, startDate, endDate);
    }

    public void update(String content, Integer targetCount) {
        this.content = content;
        this.targetCount = targetCount;
    }

    public void changeSchedule(RoutineRepeat repeatType, Set<DayOfWeek> repeatDays, LocalDate startDate, LocalDate endDate) {
        this.repeatType = repeatType != null ? repeatType : RoutineRepeat.DAILY;
        this.repeatDays = this.repeatType == RoutineRepeat.WEEKLY ? toBits(repeatDays) : null;
        this.startDate = startDate;
        // 하루만이면 시작일 하루
        this.endDate = this.repeatType == RoutineRepeat.NONE ? startDate : endDate;
    }

    public RoutineRepeat getRepeatType() {
        return repeatType != null ? repeatType : RoutineRepeat.DAILY;
    }

    public Set<DayOfWeek> getRepeatDaySet() {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (repeatDays == null) return days;
        for (DayOfWeek d : DayOfWeek.values()) {
            if ((repeatDays & bit(d)) != 0) days.add(d);
        }
        return days;
    }

    // 시작일. 예전 루틴처럼 비어 있으면 만든 날부터
    public LocalDate effectiveStartDate() {
        if (startDate != null) return startDate;
        return getCreatedAt() != null ? getCreatedAt().toLocalDate() : null;
    }

    // 그날 해야 하는 루틴인지
    public boolean isScheduledOn(LocalDate date) {
        if (!Boolean.TRUE.equals(isActive)) return false;
        LocalDate start = effectiveStartDate();
        if (start != null && date.isBefore(start)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return switch (getRepeatType()) {
            case NONE, DAILY -> true;
            case WEEKLY -> repeatDays != null && (repeatDays & bit(date.getDayOfWeek())) != 0;
        };
    }

    // 오늘 이후에도 일정이 남아 있는지 (사용 중인 루틴 개수 셀 때)
    public boolean isOngoing(LocalDate today) {
        return Boolean.TRUE.equals(isActive) && (endDate == null || !endDate.isBefore(today));
    }

    // 오늘부터 그만하기: 지난 기록이 있으면 종료일을 어제로, 없으면 비활성화
    public void stopFrom(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        LocalDate start = effectiveStartDate();
        boolean hasPast = start == null || start.isBefore(today);
        if (hasPast) {
            this.endDate = endDate == null || endDate.isAfter(yesterday) ? yesterday : endDate;
        } else {
            this.isActive = false;
        }
    }

    private static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }

    private static Integer toBits(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) return null;
        int bits = 0;
        for (DayOfWeek d : days) bits |= bit(d);
        return bits;
    }
}
