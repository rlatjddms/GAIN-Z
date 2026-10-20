package com.project.gainz.domain.character.enums;

// 캐릭터 무대 배경. 필요 레벨에 닿으면 해금되고, 한 번 얻은 배경은 계속 고를 수 있다 (레벨은 내려가지 않음)
// 필요 레벨은 프론트 constants/levelTier.js의 구간과 같다
public enum StageBackground {

    ROOKIE(1, "방구석"),
    RISING(10, "동네 클럽"),
    PRO(20, "투어 공연장"),
    LEGEND(30, "전설의 무대"),   // 최대 레벨

    // 레코드 12개를 전부 모으면 얻는 무대 (레벨로는 얻지 않음, RoomDecor.allCollected)
    LISTENING_ROOM(Integer.MAX_VALUE, "리스닝 룸");

    private final int requiredLevel;
    private final String label;

    StageBackground(int requiredLevel, String label) {
        this.requiredLevel = requiredLevel;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    // 레벨이 아니라 레코드 수집으로 얻는 무대인지
    public boolean isRecordReward() {
        return this == LISTENING_ROOM;
    }

    public boolean isUnlockedAt(int level) {
        return level >= requiredLevel;
    }

    // 그 레벨에서 얻은 가장 큰 무대
    public static StageBackground highestUnlockedAt(int level) {
        StageBackground result = ROOKIE;
        for (StageBackground bg : values()) {
            if (bg.isUnlockedAt(level)) result = bg;
        }
        return result;
    }
}
