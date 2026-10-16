import axiosInstance from './axios'

// 그날 일정인 루틴 + 그날 체크 현황
// → [{ id, content, targetCount, repeatType, repeatDays, startDate, endDate, date, count, completed }]
export async function getRoutines(date) {
  const response = await axiosInstance.get('/api/v1/routines', { params: { date } })
  return response.data.data
}

// 날짜별 루틴 달성 요약 → [{ date, total, completed }] (일정이 있는 날만)
export async function getRoutineSummary(from, to) {
  const response = await axiosInstance.get('/api/v1/routines/daily', { params: { from, to } })
  return response.data.data
}

// 루틴 만들기·고치기. repeatType: NONE(하루만) / DAILY / WEEKLY, repeatDays: ['MONDAY', …]
export async function createRoutine(routine) {
  const response = await axiosInstance.post('/api/v1/routines', routineBody(routine))
  return response.data.data
}

export async function updateRoutine(id, routine) {
  const response = await axiosInstance.patch(`/api/v1/routines/${id}`, routineBody(routine))
  return response.data.data
}

const routineBody = ({ content, targetCount, repeatType, repeatDays, startDate, endDate }) => ({
  content, targetCount, repeatType, repeatDays, startDate, endDate: endDate || null,
})

// 오늘부터 그만하기 (지난 체크 기록은 서버에 남는다)
export async function deleteRoutine(id) {
  await axiosInstance.delete(`/api/v1/routines/${id}`)
}

// 그날 수행 횟수 정하기 (최근 7일만)
export async function checkRoutine(id, date, count) {
  const response = await axiosInstance.put(`/api/v1/routines/${id}/checks/${date}`, { count })
  return response.data.data
}
