import { defineStore } from 'pinia'

// 앱 업데이트 (#223). 설치한 앱(PWA)은 켜 둔 채로 두면 예전 화면 코드가 계속 돈다.
// 배포한 버전(/version.json)을 가끔 확인해서 지금 화면과 다르면 안내하고, 누르면 새로 불러온다
// (페이지 이동은 서비스 워커가 네트워크 먼저라 새로 불러오면 새 버전을 받는다)
export const CURRENT_BUILD_ID = __BUILD_ID__
const CHECK_INTERVAL_MS = 10 * 60 * 1000

// "2026-10-25T05:30:00.000Z+abc1234" → "2026.10.25 14:30 · abc1234" (이 기기 시간대)
export function buildLabel(buildId) {
  const [iso, sha] = String(buildId || '').split('+')
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '개발 버전'
  const pad = (n) => String(n).padStart(2, '0')
  const date = `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return sha ? `${date} · ${sha}` : date
}

export const useAppUpdateStore = defineStore('appUpdate', {
  state: () => ({
    latestBuildId: null,
    checking: false,
    dismissed: false,
    timer: null,
  }),
  getters: {
    hasUpdate: (state) => !!state.latestBuildId && state.latestBuildId !== CURRENT_BUILD_ID,
    showBanner() { return this.hasUpdate && !this.dismissed },
  },
  actions: {
    // 배포한 버전 확인. 개발 서버에는 version.json이 없어서 확인하지 않는다. 결과: 새 버전이 있으면 true
    async check() {
      if (import.meta.env.DEV || this.checking) return this.hasUpdate
      this.checking = true
      try {
        const response = await fetch(`/version.json?t=${Date.now()}`, { cache: 'no-store' })
        if (response.ok) {
          const { buildId } = await response.json()
          if (buildId && buildId !== this.latestBuildId) {
            this.latestBuildId = buildId
            this.dismissed = false
          }
        }
      } catch {
        // 오프라인 등: 다음에 다시 확인
      } finally {
        this.checking = false
      }
      return this.hasUpdate
    },

    // 앱으로 돌아올 때와 10분마다 확인
    start() {
      if (this.timer) return
      this.check()
      this.timer = setInterval(() => this.check(), CHECK_INTERVAL_MS)
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') this.check()
      })
    },

    // 서비스 워커도 새로 받아 두고 화면을 다시 불러온다
    async apply() {
      try {
        const registration = await navigator.serviceWorker?.getRegistration()
        await registration?.update()
      } catch {
        // 서비스 워커가 없어도 새로 불러오면 된다
      }
      window.location.reload()
    },
  },
})
