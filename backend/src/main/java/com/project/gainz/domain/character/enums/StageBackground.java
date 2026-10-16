package com.project.gainz.domain.character.enums;

// 캐릭터 무대 배경. 필요 레벨에 닿으면 해금된다 (프론트 constants/levelTier.js의 구간과 같다)
public enum StageBackground {

    ROOKIE(1),    // 방구석
    RISING(10),   // 동네 클럽
    PRO(30),      // 투어 공연장
    LEGEND(50);   // 전설의 무대

    private final int requiredLevel;

    StageBackground(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public boolean isUnlockedAt(int level) {
        return level >= requiredLevel;
    }
}
