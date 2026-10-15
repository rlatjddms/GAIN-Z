import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

// 연습 타이머. 시작 시각과 누적 시간만 저장해서, 새로고침하거나 다른 화면에 다녀와도 이어서 잰다
// 사용자별로 localStorage에 보관한다 (practiceTimer_<이메일>)
const storageKey = () => `practiceTimer_${useAuthStore().userEmail || 'guest'}`

export const usePracticeTimerStore = defineStore('practiceTimer', {
  state: () => ({
    status: 'idle',      // idle | running | paused
    startedAt: null,     // running일 때 이번 구간을 시작한 시각(ms)
    accumulatedMs: 0,    // 일시정지 전까지 쌓인 시간
    loadedKey: null,
  }),

  actions: {
    load() {
      const key = storageKey()
      if (this.loadedKey === key) return
      this.loadedKey = key
      try {
        const saved = JSON.parse(localStorage.getItem(key) || 'null')
        if (saved && ['running', 'paused'].includes(saved.status)) {
          this.status = saved.status
          this.startedAt = saved.startedAt
          this.accumulatedMs = saved.accumulatedMs || 0
          return
        }
      } catch {
        // 저장값이 깨졌으면 새로 시작
      }
      this.status = 'idle'; this.startedAt = null; this.accumulatedMs = 0
    },
    save() {
      const key = storageKey()
      if (this.status === 'idle') localStorage.removeItem(key)
      else localStorage.setItem(key, JSON.stringify({ status: this.status, startedAt: this.startedAt, accumulatedMs: this.accumulatedMs }))
    },
    elapsedMs(now = Date.now()) {
      return this.accumulatedMs + (this.status === 'running' && this.startedAt ? Math.max(0, now - this.startedAt) : 0)
    },
    start() {
      if (this.status === 'running') return
      this.startedAt = Date.now()
      this.status = 'running'
      this.save()
    },
    pause() {
      if (this.status !== 'running') return
      this.accumulatedMs = this.elapsedMs()
      this.startedAt = null
      this.status = 'paused'
      this.save()
    },
    reset() {
      this.status = 'idle'; this.startedAt = null; this.accumulatedMs = 0
      this.save()
    },
  },
})
