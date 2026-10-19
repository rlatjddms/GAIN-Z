package com.project.gainz.domain.character.enums;

// 레벨 보상 착용 아이템(소품·하의). 필요 레벨에 닿으면 가진 것으로 친다 (레벨은 내려가지 않음)
// 프론트 constants/rewards.js의 ACCESSORIES·BOTTOMS와 같은 값. 스프라이트는 assets/sprites/<자리>/<이름>.png
public enum Accessory {

    SNEAKERS(AccessorySlot.SHOES, "운동화", 2),
    BEANIE(AccessorySlot.HAT, "비니", 3),
    ROUND_GLASSES(AccessorySlot.GLASSES, "동그란 안경", 6),
    HEADPHONES(AccessorySlot.NECK, "헤드폰", 7),
    HIGH_TOPS(AccessorySlot.SHOES, "하이탑", 8),
    CAP(AccessorySlot.HAT, "볼캡", 9),
    SUNGLASSES(AccessorySlot.GLASSES, "선글라스", 12),
    PICK_NECKLACE(AccessorySlot.NECK, "피크 목걸이", 13),
    CHELSEA_BOOTS(AccessorySlot.SHOES, "첼시 부츠", 14),
    BUCKET_HAT(AccessorySlot.HAT, "버킷햇", 16),
    SCARF(AccessorySlot.NECK, "스카프", 18),
    STAR_GLASSES(AccessorySlot.GLASSES, "별 선글라스", 19),
    COMBAT_BOOTS(AccessorySlot.SHOES, "워커", 21),
    BERET(AccessorySlot.HAT, "베레모", 22),
    CHAIN(AccessorySlot.NECK, "체인 목걸이", 24),
    HEART_GLASSES(AccessorySlot.GLASSES, "하트 선글라스", 26),
    GOLD_SNEAKERS(AccessorySlot.SHOES, "골드 운동화", 27),
    CROWN(AccessorySlot.HAT, "왕관", 28),

    // 하의: 5레벨마다 상의(LevelOutfit)와 한 벌
    JOGGERS(AccessorySlot.BOTTOM, "조거 팬츠", 5),
    RIPPED_JEANS(AccessorySlot.BOTTOM, "찢어진 청바지", 10),
    CARGO_PANTS(AccessorySlot.BOTTOM, "카고 바지", 15),
    LEATHER_PANTS(AccessorySlot.BOTTOM, "가죽 바지", 20),
    SUIT_PANTS(AccessorySlot.BOTTOM, "정장 바지", 25),
    SEQUIN_PANTS(AccessorySlot.BOTTOM, "반짝이 바지", 30);

    private final AccessorySlot slot;
    private final String label;
    private final int requiredLevel;

    Accessory(AccessorySlot slot, String label, int requiredLevel) {
        this.slot = slot;
        this.label = label;
        this.requiredLevel = requiredLevel;
    }

    public AccessorySlot getSlot() {
        return slot;
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
