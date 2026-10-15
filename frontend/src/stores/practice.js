import { defineStore } from 'pinia'
import { getDailyPractice, recordPracticeSession, updateDailyPractice } from '../api/practice'
import { useAuthStore } from './auth'
import { dateKey, addDays } from '../constants/practice'

// 날짜별 연습 합계 캐시와 저장·수정 동작
export const usePracticeStore = defineStore('practice', {
  state: () => ({
    days: {},   // 'YYYY-MM-DD' → { totalSeconds, expGained }
  }),

  getters: {
    totalOf: (state) => (key) => state.days[key]?.totalSeconds ?? 0,
  },

  actions: {
    // from~to(Date) 구간을 불러오고, 기록이 없는 날은 0으로 채운다
    async loadRange(from, to) {
      const rows = await getDailyPractice(dateKey(from), dateKey(to))
      for (let d = new Date(from); dateKey(d) <= dateKey(to); d = addDays(d, 1)) {
        this.days[dateKey(d)] = { totalSeconds: 0, expGained: 0 }
      }
      for (const r of rows || []) this.days[r.date] = { totalSeconds: r.totalSeconds, expGained: r.expGained }
    },

    // 저장·수정 응답을 반영하고, 경험치가 바뀌었으면 캐릭터(레벨·경험치)를 다시 불러온다
    async applyResult(result) {
      const prev = this.days[result.practiceDate]
      this.days[result.practiceDate] = {
        totalSeconds: result.dayTotalSeconds,
        expGained: (prev?.expGained ?? 0) + (result.expGained ?? 0),
      }
      if (result.experience) await useAuthStore().fetchCharacter()
      return result
    },

    async recordSession(date, seconds) {
      return this.applyResult(await recordPracticeSession({ durationSeconds: seconds, practiceDate: date }))
    },

    async updateDay(date, totalSeconds) {
      return this.applyResult(await updateDailyPractice(date, totalSeconds))
    },
  },
})
