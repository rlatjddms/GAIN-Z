package com.project.gainz.domain.character.enums;

// 헤어스타일. 필요 레벨이 1보다 높은 머리는 레벨 보상 (프론트 constants/rewards.js의 REWARD_HAIR_STYLES와 같은 값)
public enum HairStyle {

    BALD(1, "대머리"),
    SHORT_CUT(1, "숏컷"),
    MID_LENGTH_BOB(1, "보브컷"),
    LONG_STRAIGHT(1, "생머리"),
    NATURAL_WAVE(1, "웨이브"),
    WOLF_CUT(1, "울프컷"),
    SIDE_PART_QUIFF(1, "포마드"),
    MESSY_ROUGH(1, "헝클어짐"),
    DREADLOCKS(1, "드레드락"),
    MOHAWK_SHARP(1, "모히칸"),

    PONYTAIL(4, "포니테일"),
    SPIKY(11, "스파이크"),
    BUN(17, "번 헤어"),
    TWIN_TAILS(23, "트윈테일"),
    AFRO(29, "아프로");

    private final int requiredLevel;
    private final String label;

    HairStyle(int requiredLevel, String label) {
        this.requiredLevel = requiredLevel;
        this.label = label;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public String getLabel() {
        return label;
    }

    public boolean isUnlockedAt(int level) {
        return level >= requiredLevel;
    }
}
