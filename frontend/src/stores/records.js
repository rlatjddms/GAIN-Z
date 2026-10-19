import { defineStore } from 'pinia'
import { getRecords } from '../api/records'

// 도감 (홈 선반·도감 팝업·획득 연출이 함께 쓴다)
export const useRecordsStore = defineStore('records', {
  state: () => ({
    records: [],
    ownedCount: 0,
    total: 0,
    loaded: false,
  }),

  getters: {
    // 얻은 것 (최근에 얻은 순)
    owned: (state) => state.records.filter((r) => r.owned)
      .sort((a, b) => new Date(b.acquiredAt) - new Date(a.acquiredAt)),
  },

  actions: {
    async load() {
      const result = await getRecords()
      this.records = result.records
      this.ownedCount = result.ownedCount
      this.total = result.total
      this.loaded = true
      return result
    },
  },
})
