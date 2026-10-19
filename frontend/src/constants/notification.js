// 알림 종류·영역·시간 표시 (헤더 알림함과 알림 페이지가 함께 쓴다)

// 걸러 보기 탭. value는 백엔드 NotificationCategory와 같다
export const NOTIFICATION_CATEGORIES = [
  { value: 'FRIEND', label: '친구' },
  { value: 'BAND', label: '밴드' },
  { value: 'COMMUNITY', label: '커뮤니티' },
  { value: 'JAM', label: '합주' },
]

const CATEGORY_OF_TYPE = {
  FRIEND_REQUEST: 'FRIEND', FRIEND_ACCEPT: 'FRIEND',
  NOTICE: 'COMMUNITY', COMMENT: 'COMMUNITY', REPLY: 'COMMUNITY', MENTION: 'COMMUNITY',
  SETLIST_ASSIGN: 'JAM', JAM_NEW: 'JAM', JAM_TRACK: 'JAM',
}

// 알림 종류 → 영역 (나머지는 밴드)
export const categoryOf = (type) => CATEGORY_OF_TYPE[type] || 'BAND'

// '방금', '5분 전', '3시간 전', '2일 전', '10월 3일'
export function agoText(at) {
  const d = new Date(at)
  if (Number.isNaN(d.getTime())) return ''
  const minutes = Math.floor((Date.now() - d.getTime()) / 60000)
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${minutes}분 전`
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}시간 전`
  if (minutes < 60 * 24 * 7) return `${Math.floor(minutes / (60 * 24))}일 전`
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

// 오늘 / 어제 / 이번 주(최근 7일) / 이전
function dateGroupOf(at, now = new Date()) {
  const d = new Date(at)
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const day = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const diff = Math.round((today - day) / 86400000)
  if (diff <= 0) return '오늘'
  if (diff === 1) return '어제'
  if (diff < 7) return '이번 주'
  return '이전'
}

// 최근 순 목록을 날짜 묶음으로 → [{ label, items }]
export function groupByDate(items) {
  const groups = []
  for (const item of items) {
    const label = dateGroupOf(item.createdAt)
    const last = groups[groups.length - 1]
    if (last && last.label === label) last.items.push(item)
    else groups.push({ label, items: [item] })
  }
  return groups
}
