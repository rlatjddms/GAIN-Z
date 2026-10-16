import { ref, watch, onBeforeUnmount } from 'vue'

// 만료 시각에 화면을 갱신한다. 긴 기간은 타이머 최대값 대신 하루씩 확인한다.
export function useNoticeClock(source, expired = () => {}) {
  const now = ref(Date.now())
  let timer
  function schedule() {
    clearTimeout(timer)
    now.value = Date.now()
    const ends = source().filter(p => p.notice && p.noticeExpiresAt).map(p => Date.parse(p.noticeExpiresAt)).filter(t => t > now.value)
    if (!ends.length) return
    const next = Math.min(...ends)
    timer = setTimeout(() => {
      now.value = Date.now()
      if (now.value >= next) expired()
      schedule()
    }, Math.min(next - now.value + 20, 86400000))
  }
  watch(() => source().map(p => [p.id, p.notice, p.noticeExpiresAt]), schedule, { immediate: true })
  onBeforeUnmount(() => clearTimeout(timer))
  return now
}
