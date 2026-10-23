package com.project.gainz.domain.character.enums;

import com.project.gainz.domain.record.enums.RecordRarity;
import com.project.gainz.domain.record.enums.RecordType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoomDecorTest {

    private static Set<RecordType> allOf(RecordRarity rarity) {
        Set<RecordType> out = EnumSet.noneOf(RecordType.class);
        Arrays.stream(RecordType.values()).filter(t -> t.rarity() == rarity).forEach(out::add);
        return out;
    }

    @Test
    void 그_종류_레코드를_모두_가져야_세트를_얻는다() {
        Set<RecordType> commons = allOf(RecordRarity.COMMON);
        assertThat(RoomDecor.CASSETTE.isCompletedBy(commons)).isTrue();
        assertThat(RoomDecor.CD.isCompletedBy(commons)).isFalse();

        Set<RecordType> missingOne = EnumSet.copyOf(commons);
        missingOne.remove(commons.iterator().next());
        assertThat(RoomDecor.CASSETTE.isCompletedBy(missingOne)).isFalse();
    }

    @Test
    void 전부_모아야_리스닝_룸() {
        assertThat(RoomDecor.allCollected(EnumSet.allOf(RecordType.class))).isTrue();
        Set<RecordType> almost = EnumSet.allOf(RecordType.class);
        almost.remove(RecordType.values()[0]);
        assertThat(RoomDecor.allCollected(almost)).isFalse();
        assertThat(StageBackground.LISTENING_ROOM.isRecordReward()).isTrue();
        assertThat(StageBackground.highestUnlockedAt(30)).isEqualTo(StageBackground.LEGEND);
    }

    @Test
    void 알림_문구() {
        assertThat(RoomDecor.CASSETTE.completedMessage()).isEqualTo("카세트를 모두 모았어요! 카세트 데크와 테이프 보관함으로 방을 꾸밀 수 있어요");
    }
}
