package com.project.gainz.domain.character.enums;

public enum LevelPolicy {

    LEVEL_1_TO_9(1, 9, 100),
    LEVEL_10_TO_29(10, 29, 300),
    LEVEL_30_TO_49(30, 49, 1000),
    LEVEL_50_MAX(50, 50, Integer.MAX_VALUE);

    private final int minLevel;
    private final int maxLevel;
    private final int requiredExp;

    LevelPolicy(int minLevel, int maxLevel, int requiredExp) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.requiredExp = requiredExp;
    }

    public static int getRequiredExpForLevel(int level) {
        for (LevelPolicy policy : values()) {
            if (level >= policy.minLevel && level <= policy.maxLevel) {
                return policy.requiredExp;
            }
        }
        return 100;
    }
}
