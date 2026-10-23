// 루틴 반복 설정 (백엔드 RoutineRepeat, java.time.DayOfWeek 값과 같다)
import { parseKey } from './practice'

export const REPEAT_TYPES = [
  { key: 'NONE', label: '하루만' },
  { key: 'DAILY', label: '매일' },
  { key: 'WEEKLY', label: '요일 선택' },
]

// 월요일부터
export const WEEK_DAYS = [
  { key: 'MONDAY', label: '월' },
  { key: 'TUESDAY', label: '화' },
  { key: 'WEDNESDAY', label: '수' },
  { key: 'THURSDAY', label: '목' },
  { key: 'FRIDAY', label: '금' },
  { key: 'SATURDAY', label: '토' },
  { key: 'SUNDAY', label: '일' },
]

const shortDate = (key) => {
  const d = parseKey(key)
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

// 루틴 반복 요약 → '매일', '월·수·금', '하루만 · 10월 20일', '매일 · 11월 30일까지'
export function scheduleLabel(routine) {
  const type = routine.repeatType || 'DAILY'
  if (type === 'NONE') return `하루만 · ${shortDate(routine.startDate)}`
  let label = '매일'
  if (type === 'WEEKLY') {
    const days = new Set(routine.repeatDays || [])
    const picked = WEEK_DAYS.filter((d) => days.has(d.key))
    label = picked.length === 5 && !days.has('SATURDAY') && !days.has('SUNDAY') ? '평일'
      : picked.length === 2 && days.has('SATURDAY') && days.has('SUNDAY') ? '주말'
      : picked.map((d) => d.label).join('·')
  }
  return routine.endDate ? `${label} · ${shortDate(routine.endDate)}까지` : label
}
