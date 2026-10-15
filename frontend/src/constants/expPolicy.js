// 백엔드 ExpPolicy와 동일한 경험치 환산 규칙 (예상 경험치 미리보기용). 한쪽을 바꾸면 같이 바꾼다
// - 연습 1분당 1 exp (1회 기록당 최대 300분까지 인정)
// - 달성률(0.0 ~ 1.0)에 비례해 최대 50 exp 보너스
export const EXP_PER_MINUTE = 1
export const MAX_MINUTES_PER_LOG = 300
export const MAX_ACHIEVEMENT_BONUS = 50

// 백엔드 PracticeLogCreateRequest 검증 범위
export const MIN_DURATION_SECONDS = 1
export const MAX_DURATION_SECONDS = 86400

export function calculateExp(durationSeconds, achievementRate) {
  const minutes = Math.min(Math.floor(Math.max(durationSeconds, 0) / 60), MAX_MINUTES_PER_LOG)
  const rate = Math.max(0, Math.min(achievementRate, 1))
  return minutes * EXP_PER_MINUTE + Math.floor(rate * MAX_ACHIEVEMENT_BONUS)
}
