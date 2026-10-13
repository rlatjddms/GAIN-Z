package com.project.gainz.domain.practice.policy;

/**
 * 연습 로그를 경험치로 환산하는 정책.
 * - 연습 1분당 EXP_PER_MINUTE exp (1회 기록당 최대 MAX_MINUTES_PER_LOG분까지 인정)
 * - 루틴 달성률(0.0 ~ 1.0)에 비례해 최대 MAX_ACHIEVEMENT_BONUS exp 보너스
 */
public final class ExpPolicy {

    public static final int EXP_PER_MINUTE = 1;
    public static final int MAX_MINUTES_PER_LOG = 300;
    public static final int MAX_ACHIEVEMENT_BONUS = 50;

    private ExpPolicy() {
    }

    public static int calculate(long durationSeconds, double achievementRate) {
        long minutes = Math.min(toMinutes(durationSeconds), MAX_MINUTES_PER_LOG);
        double rate = Math.max(0.0, Math.min(achievementRate, 1.0));

        int practiceExp = (int) (minutes * EXP_PER_MINUTE);
        int bonusExp = (int) Math.floor(rate * MAX_ACHIEVEMENT_BONUS);

        return practiceExp + bonusExp;
    }

    public static long toMinutes(long durationSeconds) {
        return Math.max(durationSeconds, 0) / 60;
    }
}
