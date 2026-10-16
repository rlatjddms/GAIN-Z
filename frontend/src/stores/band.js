import { defineStore } from 'pinia'
import { getMyBands } from '../api/band'

// 홈 BAND 탭: 내 밴드 목록과 고른 밴드. 다른 화면에 다녀와도 BAND 탭과 고른 밴드를 그대로 둔다
export const useBandStore = defineStore('band', {
  state: () => ({
    bands: [],
    loaded: false,
    loading: false,
    error: '',
    selectedBandId: null,
    isBandMode: false,
  }),

  getters: {
    selectedBand: (state) => state.bands.find((b) => b.id === state.selectedBandId) || state.bands[0] || null,
  },

  actions: {
    async fetchMyBands() {
      this.loading = true
      this.error = ''
      try {
        this.bands = await getMyBands()
        this.loaded = true
        if (!this.bands.some((b) => b.id === this.selectedBandId)) {
          this.selectedBandId = this.bands[0]?.id ?? null
        }
      } catch (e) {
        this.error = '밴드 목록을 불러오지 못했어요.'
      } finally {
        this.loading = false
      }
    },

    // API가 돌려준 밴드 상세로 목록 안의 그 밴드를 바꾸거나 새로 넣는다
    upsertBand(band, select = false) {
      const i = this.bands.findIndex((b) => b.id === band.id)
      if (i >= 0) this.bands.splice(i, 1, band)
      else this.bands.push(band)
      if (select) this.selectedBandId = band.id
    },

    // 나가거나 없앤 밴드를 목록에서 뺀다
    removeBand(bandId) {
      this.bands = this.bands.filter((b) => b.id !== bandId)
      if (this.selectedBandId === bandId) this.selectedBandId = this.bands[0]?.id ?? null
    },

    reset() {
      this.$reset()
    },
  },
})
