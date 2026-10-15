import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { splitByDay } from '../constants/practice'

// 연습 타이머 (열품타 방식: 시작/정지 한 버튼, 정지할 때마다 구간을 저장)
// 켜 둔 시각과 아직 저장하지 못한 구간을 사용자별 localStorage에 보관해서, 새로고침해도 이어진다
const storageKey = () => `practiceTimer_${useAuthStore().userEmail || 'guest'}`

export const usePracticeTimerStore = defineStore('practiceTimer', {
  state: () => ({
    startedAt: null,   // 켜져 있으면 이번 구간 시작 시각(ms)
    pending: [],       // 저장 실패한 구간 [{ date, seconds }]
    loadedKey: null,
  }),

  getters: {
    running: (state) => state.startedAt != null,
  },

  actions: {
    load() {
      const key = storageKey()
      if (this.loadedKey === key) return
      this.loadedKey = key
      this.startedAt = null
      this.pending = []
      try {
        const saved = JSON.parse(localStorage.getItem(key) || 'null')
        if (saved) {
          this.startedAt = Number.isFinite(saved.startedAt) ? saved.startedAt : null
          this.pending = Array.isArray(saved.pending) ? saved.pending : []
        }
      } catch {
        // 저장값이 깨졌으면 새로 시작
      }
    },
    save() {
      const key = storageKey()
      if (!this.running && !this.pending.length) localStorage.removeItem(key)
      else localStorage.setItem(key, JSON.stringify({ startedAt: this.startedAt, pending: this.pending }))
    },
    start() {
      if (this.running) return
      this.startedAt = Date.now()
      this.save()
    },
    // 정지: 이번 구간을 날짜별로 나눠 돌려준다 (저장은 호출한 쪽에서)
    stop(now = Date.now()) {
      if (!this.running) return []
      const parts = splitByDay(this.startedAt, now)
      this.startedAt = null
      this.save()
      return parts
    },
    discard() {
      this.startedAt = null
      this.save()
    },
    setPending(parts) {
      this.pending = parts
      this.save()
    },
  },
})
