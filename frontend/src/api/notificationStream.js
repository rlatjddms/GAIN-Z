import axiosInstance from './axios'

// 실시간 알림 연결 (SSE). 브라우저 EventSource는 로그인 토큰을 헤더에 못 실어서 fetch로 직접 읽는다.
// 이벤트: unread → { count } (붙자마자), notification → 알림 (새 알림마다),
//        change → { topic, bandId?, postId? } (알림 없이 화면만 다시 불러오라는 신호, #259)
// 연결이 끝나면(서버가 30분마다 끊음) 정상 종료, 실패하면 { status } 를 가진 에러를 던진다
// 서버는 25초마다 빈 신호(ping)를 보낸다. 휴대폰이 잠들었다 깨면 연결이 끊긴 줄 모르고 멈춰 있을 수 있어서 (#224)
// IDLE_MS 동안 아무것도 안 오면 끊긴 것으로 보고 에러를 던진다 (다시 붙는 건 부르는 쪽)
const IDLE_MS = 60_000

export async function openNotificationStream({ onUnread, onNotification, onChange, onActivity, signal }) {
  const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken')
  if (!token) throw Object.assign(new Error('로그인 필요'), { status: 401 })

  const response = await fetch(`${axiosInstance.defaults.baseURL}/api/v1/notifications/stream`, {
    headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
    signal,
  })
  if (!response.ok || !response.body) throw Object.assign(new Error('연결 실패'), { status: response.status })

  const reader = response.body.getReader()
  let idle = null
  let stale = false
  const watch = () => {
    clearTimeout(idle)
    idle = setTimeout(() => { stale = true; reader.cancel().catch(() => {}) }, IDLE_MS)
    onActivity?.()
  }
  const decoder = new TextDecoder()
  let buffer = ''
  let event = 'message'
  let data = []

  const dispatch = () => {
    if (data.length) {
      try {
        const payload = JSON.parse(data.join('\n'))
        if (event === 'unread') onUnread?.(payload.count)
        else if (event === 'notification') onNotification?.(payload)
        else if (event === 'change') onChange?.(payload)
      } catch {
        // 알 수 없는 이벤트는 넘긴다
      }
    }
    event = 'message'
    data = []
  }

  watch()
  try {
    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      watch()
      buffer += decoder.decode(value, { stream: true })
      let newline
      while ((newline = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/, '')
        buffer = buffer.slice(newline + 1)
        if (line === '') dispatch()
        else if (line.startsWith(':')) continue                 // 서버의 ping
        else if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
      }
    }
  } finally {
    clearTimeout(idle)
  }
  if (stale) throw Object.assign(new Error('연결 멈춤'), { status: 0 })
}
