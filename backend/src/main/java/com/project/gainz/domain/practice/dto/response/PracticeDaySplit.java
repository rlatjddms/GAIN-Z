package com.project.gainz.domain.practice.dto.response;

// 하루 연습 기록을 출처별로 나눈 합계 (경험치 계산용)
// manualSeconds는 직접 고친 차이의 합이라 음수일 수 있다 (타이머 기록을 줄인 경우)
public record PracticeDaySplit(
        Long timerSeconds,
        Long manualSeconds,
        Long expGained
) {
    public long timer() { return timerSeconds == null ? 0 : timerSeconds; }
    public long manual() { return manualSeconds == null ? 0 : manualSeconds; }
    public long exp() { return expGained == null ? 0 : expGained; }
}
