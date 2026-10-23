// 알림 종류·영역·시간 표시 (헤더 알림함과 알림 페이지가 함께 쓴다)

// 걸러 보기 탭. value는 백엔드 NotificationCategory와 같다
// 걸러 보기 탭 (#145): 밴드 안에서 생기는 커뮤니티·합주 알림도 '밴드'로 묶는다. 백엔드 NotificationCategory와 같다
export const NOTIFICATION_CATEGORIES = [
  { value: 'FRIEND', label: '친구' },
  { value: 'BAND', label: '밴드' },
  { value: 'RECORD', label: '레코드' },
]

// 알림 종류 → 아이콘 (탭은 묶였어도 아이콘은 커뮤니티 말풍선·합주 음표로 구분한다)
const ICON_OF_TYPE = {
  FRIEND_REQUEST: 'FRIEND', FRIEND_ACCEPT: 'FRIEND',
  NOTICE: 'COMMUNITY', COMMENT: 'COMMUNITY', REPLY: 'COMMUNITY', MENTION: 'COMMUNITY',
  SETLIST_ASSIGN: 'JAM', JAM_NEW: 'JAM', JAM_TRACK: 'JAM',
  RECORD: 'RECORD',
}
export const iconOf = (type) => ICON_OF_TYPE[type] || 'BAND'

// 알림 종류 → 걸러 보기 탭 (친구·레코드 말고는 모두 밴드)
export const categoryOf = (type) => {
  const icon = iconOf(type)
  return icon === 'FRIEND' || icon === 'RECORD' ? icon : 'BAND'
}

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

// 알림 설정 항목 (영역별). key는 백엔드 NotificationSetting과 같다
export const NOTIFICATION_SETTING_GROUPS = [
  {
    category: 'FRIEND', label: '친구',
    items: [
      { key: 'FRIEND_REQUEST', label: '친구 신청', desc: '누가 나에게 친구 신청을 보냈을 때' },
      { key: 'FRIEND_ACCEPT', label: '친구 수락', desc: '내가 보낸 신청을 상대가 수락했을 때' },
    ],
  },
  {
    category: 'BAND', label: '밴드',
    items: [
      { key: 'BAND_JOIN', label: '새 멤버', desc: '내 밴드에 새 멤버가 들어왔을 때' },
      { key: 'BAND_ROLE', label: '내보내짐·리더 변경', desc: '밴드에서 내보내졌거나 리더를 넘겨받았을 때' },
      { key: 'BAND_LEAVE', label: '해체·멤버 탈퇴', desc: '밴드가 없어졌거나, 내가 리더인 밴드에서 멤버가 나갔을 때' },
    ],
  },
  {
    category: 'COMMUNITY', label: '커뮤니티',
    items: [
      { key: 'NOTICE', label: '공지', desc: '밴드에 새 공지가 올라왔을 때' },
      { key: 'COMMENT', label: '댓글·답글', desc: '내 글에 댓글, 내 댓글에 답글이 달렸을 때' },
      { key: 'MENTION', label: '멘션', desc: '글이나 댓글에서 @로 나를 불렀을 때' },
    ],
  },
  {
    category: 'JAM', label: '합주',
    items: [
      { key: 'SETLIST_ASSIGN', label: '곡 담당 지정', desc: '셋리스트 곡의 담당으로 지정됐을 때' },
      { key: 'JAM_NEW', label: '새 잼', desc: '밴드에 새 잼이 만들어졌을 때' },
      { key: 'JAM_TRACK', label: '내 잼에 트랙', desc: '내가 만든 잼에 트랙이 올라왔을 때' },
    ],
  },
  {
    category: 'RECORD', label: '레코드',
    items: [
      { key: 'RECORD', label: '새 레코드', desc: '업적을 달성해 레코드를 얻었을 때 (획득 연출도 함께 꺼져요)' },
    ],
  },
]
