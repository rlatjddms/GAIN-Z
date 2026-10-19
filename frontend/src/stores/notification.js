import { defineStore } from 'pinia'
import { getUnreadCount } from '../api/notification'

// 헤더 알림 배지의 안 읽은 개수 (화면 이동할 때와 일정 간격으로 다시 센다)
export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
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
  },
})
