import { defineStore } from 'pinia'
import { getUnreadCount } from '../api/notification'
import { openNotificationStream } from '../api/notificationStream'

const TOAST_MS = 6000
const MAX_TOASTS = 3
const RETRY_STEPS_MS = [1000, 2000, 5000, 10000, 30000]

let controller = null
let retryTimer = null
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
      this.runStream(controller, 0)
    },

    disconnect() {
      clearTimeout(retryTimer)
      controller?.abort()
      controller = null
      this.connected = false
      this.toasts = []
    },

    async runStream(mine, attempt) {
      let nextAttempt = attempt + 1
      try {
        const opened = openNotificationStream({
          signal: mine.signal,
          onUnread: (count) => { this.connected = true; this.unreadCount = count; nextAttempt = 0 },
          onNotification: (n) => this.receive(n),
        })
        await opened
        nextAttempt = 0       // 서버가 시간이 다 돼서 끊은 것: 바로 다시
      } catch (e) {
        if (mine.signal.aborted) return
        // 토큰이 만료됐으면 axios 요청으로 새 토큰을 받은 뒤 다시 (못 받으면 axios가 로그인 화면으로 보낸다)
        if (e.status === 401) await this.refreshUnread()
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
      const key = ++toastSeq
      this.toasts = [...this.toasts, { key, notification }].slice(-MAX_TOASTS)
      setTimeout(() => this.dismiss(key), TOAST_MS)
    },

    dismiss(key) {
      this.toasts = this.toasts.filter((t) => t.key !== key)
    },
  },
})
