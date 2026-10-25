import { defineStore } from 'pinia'
import { getUnreadCount } from '../api/notification'
import { openNotificationStream } from '../api/notificationStream'

const TOAST_MS = 6000
const MAX_TOASTS = 3
const RETRY_STEPS_MS = [1000, 2000, 5000, 10000, 30000]

let controller = null
let retryTimer = null
let run = null            // 지금 붙어 있는(또는 붙는 중인) 연결 하나
let lastActivity = 0      // 마지막으로 서버에서 뭐라도 받은 시각 (ping 포함)
let dropped = false       // 연결이 잘못 끊겼었는지 (서버가 30분마다 끊는 정상 종료는 빼고). 다시 붙으면 resync
const STALE_MS = 35_000   // 앱으로 돌아왔을 때 이만큼 조용했으면 끊긴 것으로 보고 새로 붙는다 (서버 ping은 25초마다)
let toastSeq = 0

// 헤더 배지의 안 읽은 개수, 실시간 연결, 화면 오른쪽 위 알림 팝업
export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
    connected: false,
    // 화면에 떠 있는 팝업 [{ key, notification }]
    toasts: [],
    // 마지막으로 받은 새 알림 (알림함·알림 페이지가 지켜보다가 목록 위에 붙인다)
    latest: null,
    // 끊겼다가 다시 붙을 때마다 1씩 오른다. 그동안 놓친 내용이 있을 수 있어서 화면들이 지켜보다가 다시 불러온다 (#224)
    resync: 0,
  }),

  actions: {
    async refreshUnread() {
      try {
        this.unreadCount = (await getUnreadCount()).count
      } catch {
        // 배지는 못 세도 화면은 그대로 둔다
      }
    },

    setUnread(count) {
      this.unreadCount = Math.max(0, count)
    },

    /* ---------- 실시간 연결 ---------- */

    // 로그인하면 연다. 끊기면 점점 길게 기다렸다가 다시 붙는다
    connect() {
      if (controller) return
      controller = new AbortController()
      dropped = false
      document.addEventListener('visibilitychange', this.wake)
      window.addEventListener('online', this.wake)
      this.runStream(controller, 0)
    },

    disconnect() {
      clearTimeout(retryTimer)
      document.removeEventListener('visibilitychange', this.wake)
      window.removeEventListener('online', this.wake)
      controller?.abort()
      controller = null
      run = null
      this.connected = false
      this.toasts = []
    },

    // 앱으로 돌아오거나 인터넷이 다시 되면: 기다리는 중이면 바로 붙고, 붙은 줄 알았는데 조용했으면 새로 붙는다
    wake() {
      if (!controller || document.visibilityState !== 'visible') return
      if (run) {
        if (!this.connected || Date.now() - lastActivity < STALE_MS) return
        run.abort()             // 멈춘 연결: 아래 runStream이 끝나면서 바로 다시 붙는다
        return
      }
      clearTimeout(retryTimer)
      this.runStream(controller, 0)
    },

    async runStream(mine, attempt) {
      let nextAttempt = attempt + 1
      const current = new AbortController()
      run = current
      const stop = () => current.abort()
      mine.signal.addEventListener('abort', stop)
      try {
        await openNotificationStream({
          signal: current.signal,
          onActivity: () => { lastActivity = Date.now() },
          onUnread: (count) => {
            this.connected = true
            this.unreadCount = count
            nextAttempt = 0
            if (dropped) this.resync += 1
            dropped = false
          },
          onNotification: (n) => this.receive(n),
        })
        nextAttempt = 0       // 서버가 시간이 다 돼서 끊은 것: 바로 다시
      } catch (e) {
        if (mine.signal.aborted) return
        dropped = true
        // 토큰이 만료됐으면 axios 요청으로 새 토큰을 받은 뒤 다시 (못 받으면 axios가 로그인 화면으로 보낸다)
        if (e.status === 401) await this.refreshUnread()
        // 멈춘 연결을 끊은 것(wake): 기다리지 않고 바로
        if (current.signal.aborted) nextAttempt = 0
      } finally {
        mine.signal.removeEventListener('abort', stop)
        if (run === current) run = null
      }
      if (mine !== controller || mine.signal.aborted) return
      this.connected = false
      const wait = nextAttempt === 0 ? 300 : RETRY_STEPS_MS[Math.min(nextAttempt - 1, RETRY_STEPS_MS.length - 1)]
      retryTimer = setTimeout(() => this.runStream(mine, nextAttempt), wait)
    },

    /* ---------- 새 알림 ---------- */

    receive(notification) {
      this.unreadCount += 1
      this.latest = notification
      // 레코드는 작은 팝업 대신 획득 연출(RecordAcquiredModal)로
      if (notification.type === 'RECORD') return
      const key = ++toastSeq
      this.toasts = [...this.toasts, { key, notification }].slice(-MAX_TOASTS)
      setTimeout(() => this.dismiss(key), TOAST_MS)
    },

    dismiss(key) {
      this.toasts = this.toasts.filter((t) => t.key !== key)
    },
  },
})
