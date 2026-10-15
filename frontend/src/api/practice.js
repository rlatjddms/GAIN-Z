import axiosInstance from './axios'

// 연습 로그 기록 → 획득 경험치와 레벨 변화(ExperienceResult)를 돌려준다
// practiceDate를 생략하면 서버에서 오늘 날짜로 기록한다
export async function createPracticeLog({ durationSeconds, achievementRate, practiceDate }) {
  const response = await axiosInstance.post('/api/v1/practice-logs', {
    durationSeconds,
    achievementRate,
    ...(practiceDate ? { practiceDate } : {}),
  })
  return response.data.data
}

// 서버 에러 응답(ApiResponse)에서 사용자에게 보여 줄 메시지를 꺼낸다
export function apiErrorMessage(error, fallback = '잠시 후 다시 시도해 주세요.') {
  return error?.response?.data?.message || fallback
}
