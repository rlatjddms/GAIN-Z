import axiosInstance from './axios'

// 타이머 구간 저장 → { practiceDate, dayTotalSeconds, expGained, experience }
export async function recordPracticeSession({ durationSeconds, practiceDate }) {
  const response = await axiosInstance.post('/api/v1/practice-logs', { durationSeconds, practiceDate })
  return response.data.data
}

// 기간별 일별 합계 → [{ date, totalSeconds, expGained }] (기록이 있는 날만)
export async function getDailyPractice(from, to) {
  const response = await axiosInstance.get('/api/v1/practice-logs/daily', { params: { from, to } })
  return response.data.data
}

// 하루 총 연습 시간 직접 수정 (최근 7일만) → 저장 결과와 같은 모양
export async function updateDailyPractice(date, totalSeconds) {
  const response = await axiosInstance.put(`/api/v1/practice-logs/daily/${date}`, { totalSeconds })
  return response.data.data
}

// 서버 에러 응답(ApiResponse)에서 사용자에게 보여 줄 메시지를 꺼낸다
export function apiErrorMessage(error, fallback = '잠시 후 다시 시도해 주세요.') {
  return error?.response?.data?.message || fallback
}
