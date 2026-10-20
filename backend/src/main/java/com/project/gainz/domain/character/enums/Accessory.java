package com.project.gainz.domain.character.enums;

// 레벨 보상 착용 아이템(소품·하의). 필요 레벨에 닿으면 가진 것으로 친다 (레벨은 내려가지 않음)
// 프론트 constants/rewards.js의 ACCESSORIES·BOTTOMS와 같은 값. 스프라이트는 assets/sprites/<자리>/<이름>.png
public enum Accessory {

    // 처음부터 가진 기본 아이템 (하의·신발은 늘 하나는 입는다)
    BASIC_SHOES(AccessorySlot.SHOES, "기본 신발", 1),
    JEANS(AccessorySlot.BOTTOM, "청바지", 1),

    SNEAKERS(AccessorySlot.SHOES, "운동화", 2),
    BEANIE(AccessorySlot.HAT, "비니", 3),
    ROUND_GLASSES(AccessorySlot.GLASSES, "동그란 안경", 6),
    SKIRT(AccessorySlot.BOTTOM, "치마", 8),
    CAP(AccessorySlot.HAT, "볼캡", 9),
    SUNGLASSES(AccessorySlot.GLASSES, "선글라스", 12),
    // Lv.13 헤드폰은 목에 걸기·머리에 쓰기 두 가지로 받는다
    HEADPHONES(AccessorySlot.NECK, "헤드폰", 13),
    HEAD_HEADPHONES(AccessorySlot.HAT, "헤드폰", 13),
    STUD_EARRINGS(AccessorySlot.EARRINGS, "스터드 귀걸이", 14),
    BELT(AccessorySlot.WAIST, "체인 벨트", 16),
    HOOP_EARRINGS(AccessorySlot.EARRINGS, "링 귀걸이", 18),
    STAR_GLASSES(AccessorySlot.GLASSES, "별 선글라스", 19),
    HIGH_TOPS(AccessorySlot.SHOES, "하이탑", 21),
    STAR_EARRINGS(AccessorySlot.EARRINGS, "별 귀걸이", 22),
    CHAIN(AccessorySlot.NECK, "체인 목걸이", 24),
    HEART_GLASSES(AccessorySlot.GLASSES, "하트 선글라스", 26),
    COMBAT_BOOTS(AccessorySlot.SHOES, "워커", 27),
    CROWN(AccessorySlot.HAT, "왕관", 28),

    // 하의: 5레벨마다 상의(LevelOutfit)와 한 벌
    JOGGERS(AccessorySlot.BOTTOM, "조거 팬츠", 5),
    RIPPED_JEANS(AccessorySlot.BOTTOM, "찢어진 청바지", 10),
    CARGO_PANTS(AccessorySlot.BOTTOM, "카고 바지", 15),
    LEATHER_PANTS(AccessorySlot.BOTTOM, "가죽 바지", 20),
    SUIT_PANTS(AccessorySlot.BOTTOM, "정장 바지", 25),
    GOLD_STRIPE_PANTS(AccessorySlot.BOTTOM, "금줄 바지", 30);

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

    // 늘 입는 자리(하의·신발)를 비워 두면 입히는 기본 아이템
    public static Accessory defaultFor(AccessorySlot slot) {
        return switch (slot) {
            case BOTTOM -> JEANS;
            case SHOES -> BASIC_SHOES;
            default -> null;
        };
    }

    public boolean isUnlockedAt(int level) {
        return level >= requiredLevel;
    }
}
