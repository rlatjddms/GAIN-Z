package com.project.gainz.domain.character.enums;

// 레벨 보상 상의: 5레벨마다 + 크롭티(Lv.7) (하의는 Accessory의 BOTTOM 자리). 서버가 뜰 때 OutfitItem으로 맞춰 둔다 (스프라이트 키 = 이름)
// 프론트 constants/rewards.js의 REWARD_OUTFITS와 같은 값
public enum LevelOutfit {

    HOODIE("후드티", 5),
    CROP("크롭티", 7),
    BAND_TEE("밴드 티셔츠", 10),
    DENIM_JACKET("데님 재킷", 15),
    LEATHER_JACKET("가죽 재킷", 20),
    STAGE_SUIT("무대 정장", 25),
    GOLD_TRIM_JACKET("금장 재킷", 30);

    private final String label;
    private final int requiredLevel;

    LevelOutfit(String label, int requiredLevel) {
        this.label = label;
        this.requiredLevel = requiredLevel;
    }

    public String getLabel() {
        return label;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public String getSpriteKey() {
        return name();
    }
}
