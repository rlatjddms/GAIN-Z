package com.project.gainz.domain.character.enums;

// 방 꾸미기 소품 하나하나. 그 세트(RoomDecor)의 레코드를 다 모으면 얻고, 무대에 하나씩 올려 자리를 정한다
// 프론트 constants/recordRewards.js의 ROOM_DECOR_ITEMS와 같은 값. 그림은 assets/decor/<이름>.png
public enum DecorItem {

    CASSETTE_DECK(RoomDecor.CASSETTE, "카세트 데크"),
    TAPE_RACK(RoomDecor.CASSETTE, "테이프 보관함"),
    CD_PLAYER(RoomDecor.CD, "CD 플레이어"),
    CD_RACK(RoomDecor.CD, "CD 랙"),
    TURNTABLE(RoomDecor.LP, "턴테이블"),
    LP_SHELF(RoomDecor.LP, "LP 선반");

    // 무대 가로 208칸 중 소품 그림(24칸) 왼쪽 끝이 놓일 수 있는 범위
    public static final int MIN_X = 0;
    public static final int MAX_X = 208 - 24;
    // 그림 바닥 줄이 놓일 수 있는 범위 (무대 세로 72칸, 그림 높이 32칸)
    public static final int MIN_Y = 32;
    public static final int MAX_Y = 72;

    private final RoomDecor set;
    private final String label;

    DecorItem(RoomDecor set, String label) {
        this.set = set;
        this.label = label;
    }

    public RoomDecor getSet() {
        return set;
    }

    public String getLabel() {
        return label;
    }
}
