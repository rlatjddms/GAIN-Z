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
    // 직접 입력한 시간은 하루 30분까지만 경험치로 친다 (타이머를 켜는 걸 잊은 날을 위한 여유, 부풀리기 방지)
    public static final int MANUAL_MINUTES_PER_DAY = 30;

    private ExpPolicy() {
    }

    // 하루 경험치 = 타이머 시간 + 직접 입력(하루 30분까지), 하루 최대 300.
    // 직접 입력 합이 음수면(기록을 줄여 타이머 시간을 깎은 것) 타이머 시간에서 뺀다 → 줄이면 직접 입력분부터 빠진다
    public static int dailyExp(long timerSeconds, long manualSeconds) {
        long timer = Math.max(0, timerSeconds + Math.min(manualSeconds, 0));
        long manual = Math.max(manualSeconds, 0);
        long minutes = toMinutes(timer) + Math.min(toMinutes(manual), MANUAL_MINUTES_PER_DAY);
        return (int) (Math.min(minutes, MAX_MINUTES_PER_DAY) * EXP_PER_MINUTE);
    }

    public static long toMinutes(long durationSeconds) {
        return Math.max(durationSeconds, 0) / 60;
    }
}
