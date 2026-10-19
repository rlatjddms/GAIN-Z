package com.project.gainz.domain.record.enums;

// 레코드 도감. 목록·이름·조건은 코드에 정하고, DB에는 누가 언제 얻었는지만 저장한다 (UserRecord)
// 순서가 곧 도감 순서 (희귀도 → 정한 순서)
public enum RecordType {

    /* 카세트 (일반) */
    STREAK_7("일주일 세션", "일주일 내내 이어 간 연습", "7일 연속 연습하기",
            RecordRarity.COMMON, "#2f9e44", RecordMetric.STREAK_DAYS, 7),
    HOURS_10("10시간 테이프", "꽉 채운 10시간의 연습", "총 연습 10시간 채우기",
            RecordRarity.COMMON, "#e8590c", RecordMetric.PRACTICE_HOURS, 10),
    ROUTINE_7("루틴 데뷔", "루틴을 끝까지 해낸 첫 일주일", "루틴을 모두 완료한 날 7일 채우기",
            RecordRarity.COMMON, "#ae3ec9", RecordMetric.ROUTINE_DAYS, 7),
    FIRST_BAND("첫 무대", "함께 연주할 밴드를 만난 날", "밴드에 처음 들어가기",
            RecordRarity.COMMON, "#1098ad", RecordMetric.BANDS, 1),
    FIRST_FRIEND("듀엣", "음악을 나눌 친구가 생겼어요", "친구 처음 맺기",
            RecordRarity.COMMON, "#f06595", RecordMetric.FRIENDS, 1),
    FIRST_JAM_TRACK("첫 합주", "밴드와 처음으로 소리를 겹친 날", "레이어 잼에 트랙 처음 올리기",
            RecordRarity.COMMON, "#0b7285", RecordMetric.JAM_TRACKS, 1),

    /* CD (레어) */
    STREAK_30("한 달의 투어", "한 달 동안 하루도 빠짐없이", "30일 연속 연습하기",
            RecordRarity.RARE, "#3b5bdb", RecordMetric.STREAK_DAYS, 30),
    HOURS_50("50시간 앨범", "50시간이 담긴 첫 앨범", "총 연습 50시간 채우기",
            RecordRarity.RARE, "#d9480f", RecordMetric.PRACTICE_HOURS, 50),
    ROUTINE_30("루틴 마스터", "루틴을 30일 동안 끝까지 해냈어요", "루틴을 모두 완료한 날 30일 채우기",
            RecordRarity.RARE, "#862e9c", RecordMetric.ROUTINE_DAYS, 30),
    JAM_TRACKS_10("멀티트랙", "레이어 잼에 쌓아 올린 열 개의 트랙", "레이어 잼에 트랙 10번 올리기",
            RecordRarity.RARE, "#099268", RecordMetric.JAM_TRACKS, 10),

    /* LP (전설) */
    STREAK_100("100일의 월드 투어", "100일 동안 하루도 빠짐없이", "100일 연속 연습하기",
            RecordRarity.LEGENDARY, "#f59f00", RecordMetric.STREAK_DAYS, 100),
    HOURS_200("200시간 명반", "200시간이 새겨진 명반", "총 연습 200시간 채우기",
            RecordRarity.LEGENDARY, "#c92a2a", RecordMetric.PRACTICE_HOURS, 200);

    private final String title;
    private final String description;
    private final String condition;
    private final RecordRarity rarity;
    private final String color;
    private final RecordMetric metric;
    private final long goal;

    RecordType(String title, String description, String condition, RecordRarity rarity, String color,
               RecordMetric metric, long goal) {
        this.title = title;
        this.description = description;
        this.condition = condition;
        this.rarity = rarity;
        this.color = color;
        this.metric = metric;
        this.goal = goal;
    }

    public String title() { return title; }
    public String description() { return description; }
    public String condition() { return condition; }
    public RecordRarity rarity() { return rarity; }
    public String color() { return color; }
    public RecordMetric metric() { return metric; }
    public long goal() { return goal; }

    // 형태 이름 (알림 문구용)
    public String shape() {
        return switch (rarity) {
            case COMMON -> "카세트";
            case RARE -> "CD";
            case LEGENDARY -> "LP";
        };
    }
}
