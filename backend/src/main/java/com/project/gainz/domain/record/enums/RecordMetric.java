package com.project.gainz.domain.record.enums;

// 업적 판정에 쓰는 수치. unit은 화면의 진행도 글자 (DAY: '일', HOUR: '시간', COUNT: 단위 없음)
public enum RecordMetric {

    STREAK_DAYS(Unit.DAY),      // 가장 길게 이어 간 연속 연습 일수
    PRACTICE_HOURS(Unit.HOUR),  // 총 연습 시간 (시간, 내림)
    ROUTINE_DAYS(Unit.DAY),     // 그날 루틴을 모두 완료한 날 수
    BANDS(Unit.COUNT),          // 지금 들어가 있는 밴드 수
    FRIENDS(Unit.COUNT),        // 친구 수
    JAM_TRACKS(Unit.COUNT);     // 레이어 잼에 올린 트랙 수

    public enum Unit { DAY, HOUR, COUNT }

    private final Unit unit;

    RecordMetric(Unit unit) {
        this.unit = unit;
    }

    public Unit unit() {
        return unit;
    }
}
