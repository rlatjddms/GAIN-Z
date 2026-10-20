package com.project.gainz.domain.character.enums;

import com.project.gainz.domain.record.enums.RecordRarity;
import com.project.gainz.domain.record.enums.RecordType;

import java.util.Arrays;
import java.util.Collection;

// 방 꾸미기 세트 (레코드 수집 보상). 그 종류(희귀도)의 레코드를 다 모으면 플레이어와 보관함을 무대에 놓을 수 있다
// 프론트 constants/recordRewards.js의 ROOM_DECOR_SETS와 같은 값
public enum RoomDecor {

    CASSETTE(RecordRarity.COMMON, "카세트", "카세트 데크", "테이프 보관함"),
    CD(RecordRarity.RARE, "CD", "CD 플레이어", "CD 랙"),
    LP(RecordRarity.LEGENDARY, "LP", "턴테이블", "LP 선반");

    private final RecordRarity rarity;
    private final String recordLabel;
    private final String player;
    private final String rack;

    RoomDecor(RecordRarity rarity, String recordLabel, String player, String rack) {
        this.rarity = rarity;
        this.recordLabel = recordLabel;
        this.player = player;
        this.rack = rack;
    }

    public RecordRarity getRarity() {
        return rarity;
    }

    // 알림 문구: "카세트를 모두 모았어요! 카세트 데크와 테이프 보관함으로 방을 꾸밀 수 있어요"
    public String completedMessage() {
        return recordLabel + "를 모두 모았어요! " + player + "와 " + rack + "으로 방을 꾸밀 수 있어요";
    }

    // 이 종류의 레코드를 모두 가졌는지
    public boolean isCompletedBy(Collection<RecordType> owned) {
        return Arrays.stream(RecordType.values())
                .filter(t -> t.rarity() == rarity)
                .allMatch(owned::contains);
    }

    // 레코드를 전부 모았는지 (리스닝 룸 무대)
    public static boolean allCollected(Collection<RecordType> owned) {
        return Arrays.stream(RecordType.values()).allMatch(owned::contains);
    }
}
