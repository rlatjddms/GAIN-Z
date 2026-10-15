// 연습 기록 날짜·시간 도우미와 규칙 (백엔드 PracticeLogService와 같은 값)
export const MAX_SECONDS_PER_DAY = 86400
export const EDITABLE_DAYS = 7          // 오늘 포함 최근 7일만 직접 수정 가능

const DOW = ['일', '월', '화', '수', '목', '금', '토']
const pad = (n) => String(n).padStart(2, '0')

// 로컬 날짜 → 'YYYY-MM-DD'
export function dateKey(date = new Date()) {
  const d = new Date(date)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function addDays(date, days) {
  const d = new Date(date)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + days)
}

export function startOfDay(date = new Date()) {
  const d = new Date(date)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

// 'YYYY-MM-DD' → '10월 14일 (화)'
export function dateLabel(key) {
  const [y, m, d] = key.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  return `${m}월 ${d}일 (${DOW[date.getDay()]})`
}

export function dowLabel(date) {
  return DOW[new Date(date).getDay()]
}

// 시작~끝 시각(ms)을 자정 기준으로 나눠 날짜별 초로 바꾼다
export function splitByDay(startMs, endMs) {
  const parts = []
  let cursor = startMs
  while (cursor < endMs) {
    const nextMidnight = addDays(startOfDay(cursor), 1).getTime()
    const segEnd = Math.min(endMs, nextMidnight)
    const seconds = Math.floor((segEnd - cursor) / 1000)
    if (seconds > 0) parts.push({ date: dateKey(cursor), seconds })
    cursor = segEnd
  }
  return parts
}

// 초 → '01:23:45'
export function clock(seconds) {
  const s = Math.max(0, Math.floor(seconds))
  return `${pad(Math.floor(s / 3600))}:${pad(Math.floor((s % 3600) / 60))}:${pad(s % 60)}`
}

// 초 → '1시간 20분' / '25분' / '40초'
export function durationText(seconds) {
  const s = Math.max(0, Math.floor(seconds))
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60)
  if (h) return m ? `${h}시간 ${m}분` : `${h}시간`
  if (m) return `${m}분`
  return s ? `${s}초` : '0분'
}

// 초 → '1:20' (작은 칸 표시용)
export function shortDuration(seconds) {
  const s = Math.max(0, Math.floor(seconds))
  if (!s) return '—'
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60)
  return h ? `${h}:${pad(m)}` : `${m || 1}분`
}

// 연습량 단계 0~4 (잔디·최근 7일 칸 색). 0분 / 30분 미만 / 1시간 미만 / 2시간 미만 / 그 이상
export function practiceLevel(seconds) {
  if (seconds <= 0) return 0
  if (seconds < 30 * 60) return 1
  if (seconds < 60 * 60) return 2
  if (seconds < 2 * 3600) return 3
  return 4
}

// 수정 가능한 날짜인지 (오늘 포함 최근 7일)
export function isEditable(key, today = new Date()) {
  const oldest = dateKey(addDays(today, -(EDITABLE_DAYS - 1)))
  return key >= oldest && key <= dateKey(today)
}
