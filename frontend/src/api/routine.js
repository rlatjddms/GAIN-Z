import axiosInstance from './axios'

// 사용 중인 루틴 + 그날 체크 현황 → [{ id, content, targetCount, date, count, completed }]
export async function getRoutines(date) {
  const response = await axiosInstance.get('/api/v1/routines', { params: { date } })
  return response.data.data
}

export async function createRoutine({ content, targetCount }) {
  const response = await axiosInstance.post('/api/v1/routines', { content, targetCount })
  return response.data.data
}

export async function updateRoutine(id, { content, targetCount }) {
  const response = await axiosInstance.patch(`/api/v1/routines/${id}`, { content, targetCount })
  return response.data.data
}

// 삭제 = 비활성화 (지난 체크 기록은 서버에 남는다)
export async function deleteRoutine(id) {
  await axiosInstance.delete(`/api/v1/routines/${id}`)
}

// 그날 수행 횟수 정하기 (최근 7일만)
export async function checkRoutine(id, date, count) {
  const response = await axiosInstance.put(`/api/v1/routines/${id}/checks/${date}`, { count })
  return response.data.data
}
