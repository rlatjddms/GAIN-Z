package com.project.gainz.domain.character.enums;

// 레벨 보상 착용 자리. 자리마다 하나씩 (하의는 5레벨마다 상의와 한 벌로, 나머지는 소품)
public enum AccessorySlot {

    HAT,      // 모자
    GLASSES,  // 안경
    EARRINGS, // 귀걸이
    NECK,     // 목
    WAIST,    // 허리 (벨트)
    SHOES,    // 신발 (늘 하나는 신음, 기본 BASIC_SHOES)
    BOTTOM    // 하의 (늘 하나는 입음, 기본 JEANS. 몸 그림은 속옷 차림)
}
