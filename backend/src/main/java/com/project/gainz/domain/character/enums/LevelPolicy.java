package com.project.gainz.domain.character.enums;

// 레벨 규칙. 최대 Lv.30, 다음 레벨까지 필요 경험치 = 50 × 지금 레벨 + 20
// (하루 1시간 연습하면 Lv.10 약 40일, Lv.20 약 5.5개월, Lv.30 약 1년)
// 프론트 constants/levelTier.js와 같은 식이다. 한쪽을 바꾸면 같이 바꾼다
public final class LevelPolicy {

    public static final int MAX_LEVEL = 30;
    // 이 규칙이 몇 번째 레벨 규칙인지 (기존 캐릭터를 새 규칙으로 옮겼는지 표시)
    public static final int RULE_VERSION = 2;

    private LevelPolicy() {
    }

    // level에서 다음 레벨로 가는 데 필요한 경험치 (최대 레벨이면 도달할 수 없는 값)
    public static int getRequiredExpForLevel(int level) {
        if (level >= MAX_LEVEL) return Integer.MAX_VALUE;
        return 50 * Math.max(level, 1) + 20;
    }

    // Lv.1에서 level이 되기까지 모은 경험치
    public static long totalExpToReach(int level) {
        long total = 0;
        for (int l = 1; l < Math.min(level, MAX_LEVEL); l++) total += getRequiredExpForLevel(l);
        return total;
    }

    // 지금까지 모은 총 경험치 → { 레벨, 그 레벨에서 모은 경험치 } (최대 레벨이면 경험치 0)
    public static int[] levelForTotalExp(long totalExp) {
        int level = 1;
        long rest = Math.max(totalExp, 0);
        while (level < MAX_LEVEL && rest >= getRequiredExpForLevel(level)) {
            rest -= getRequiredExpForLevel(level);
            level++;
        }
        return new int[]{level, level >= MAX_LEVEL ? 0 : (int) rest};
    }
}
