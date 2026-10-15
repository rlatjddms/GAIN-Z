package com.project.gainz.domain.practice.policy;

/**
 * 연습 시간을 경험치로 환산하는 정책 (하루 총 연습 시간 기준).
 * - 연습 1분당 EXP_PER_MINUTE exp, 하루 최대 MAX_MINUTES_PER_DAY분까지 인정
 * - 같은 날을 여러 번 나눠 기록해도 하루 합계로 계산하므로 손해가 없고,
 *   그날 이미 받은 경험치를 빼고 지급하므로 시간을 줄였다 늘려도 중복 지급되지 않는다
 */
public final class ExpPolicy {

    public static final int EXP_PER_MINUTE = 1;
    public static final int MAX_MINUTES_PER_DAY = 300;

    private ExpPolicy() {
    }

    // 하루 총 연습 시간으로 받을 수 있는 경험치
    public static int dailyExp(long dayTotalSeconds) {
        long minutes = Math.min(toMinutes(dayTotalSeconds), MAX_MINUTES_PER_DAY);
        return (int) (minutes * EXP_PER_MINUTE);
    }

    // 하루 총 시간이 dayTotalSeconds가 됐을 때 새로 지급할 경험치
    public static int expToGrant(long dayTotalSeconds, long alreadyGrantedExp) {
        return (int) Math.max(0, dailyExp(dayTotalSeconds) - alreadyGrantedExp);
    }

    public static long toMinutes(long durationSeconds) {
        return Math.max(durationSeconds, 0) / 60;
    }
}
