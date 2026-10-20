package com.project.gainz.domain.character.enums;

// 무대 꾸미기 효과. 5레벨 보상 중 배경이 없는 Lv.5·15·25와 마지막 Lv.30에 의상과 함께 받는다
// 필요 레벨에 닿으면 가진 것으로 친다. 프론트 constants/rewards.js의 STAGE_EFFECTS와 같은 값
public enum StageEffect {

    SPARKLE("반짝이", 5),
    SMOKE("스모크", 15),
    SPOTLIGHT("스포트라이트", 25),
    FIREWORKS("불꽃놀이", 30);

    private final String label;
    private final int requiredLevel;

    StageEffect(String label, int requiredLevel) {
        this.label = label;
        this.requiredLevel = requiredLevel;
    }

    public String getLabel() {
        return label;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public boolean isUnlockedAt(int level) {
        return level >= requiredLevel;
    }
}
