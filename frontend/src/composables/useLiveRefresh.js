import { watch, onMounted, onBeforeUnmount } from 'vue'
import { useNotificationStore } from '../stores/notification'

// 다른 사람이 바꾼 내용이 새로고침 없이 보이게 (#224, #259). 화면이 이미 불러온 내용을 다시 불러오는 때:
// - 앱(탭)으로 돌아왔을 때: 잠깐 다녀온 건 빼고 HIDDEN_MS 넘게 다른 데 있었을 때
// - 실시간 알림 연결이 끊겼다가 다시 붙었을 때 (그동안 놓친 알림이 있을 수 있음)
// - types에 든 종류의 새 알림이 왔을 때 (예: 내 글에 댓글 → 댓글 목록)
// - 서버가 보낸 change 신호를 changes(신호)가 true로 받을 때 (예: 같은 밴드에 새 글)
// refresh는 화면을 비우지 않고 조용히 다시 불러와야 한다.
// 신호가 연달아 오면 WAIT_MS 동안 모았다가 한 번만, 불러오는 중에 또 오면 끝난 뒤 한 번 더 불러온다.
// 다른 앱에 가 있는 동안 온 신호는 돌아왔을 때 불러온다
const HIDDEN_MS = 15_000
const WAIT_MS = 400

export function useLiveRefresh(refresh, { types = [], changes = null } = {}) {
  const store = useNotificationStore()
  let hiddenAt = 0
  let running = false
  let again = false
  let missed = false
  let timer = null
  let alive = true

  const run = async () => {
    if (!alive) return
    if (running) { again = true; return }
    running = true
    try { await refresh() } catch { /* 화면이 알아서 알린다 */ } finally { running = false }
    if (again) { again = false; run() }
  }
  const soon = () => {
    if (document.visibilityState === 'hidden') { missed = true; return }
    clearTimeout(timer)
    timer = setTimeout(run, WAIT_MS)
  }
  const onVisibility = () => {
    if (document.visibilityState === 'hidden') { hiddenAt = Date.now(); return }
    const long = hiddenAt && Date.now() - hiddenAt >= HIDDEN_MS
    hiddenAt = 0
    if (long || missed) { missed = false; run() }
  }

  watch(() => store.resync, run)
  watch(() => store.latest, (n) => { if (n && types.includes(n.type)) soon() })
  watch(() => store.change, (c) => { if (c && changes?.(c)) soon() })
  onMounted(() => document.addEventListener('visibilitychange', onVisibility))
  onBeforeUnmount(() => {
    alive = false
    clearTimeout(timer)
    document.removeEventListener('visibilitychange', onVisibility)
  })
}
