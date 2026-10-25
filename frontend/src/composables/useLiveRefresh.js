import { watch, onMounted, onBeforeUnmount } from 'vue'
import { useNotificationStore } from '../stores/notification'

// 다른 사람이 바꾼 내용이 새로고침 없이 보이게 (#224). 화면이 이미 불러온 내용을 다시 불러오는 때:
// - 앱(탭)으로 돌아왔을 때: 잠깐 다녀온 건 빼고 HIDDEN_MS 넘게 다른 데 있었을 때
// - 실시간 알림 연결이 끊겼다가 다시 붙었을 때 (그동안 놓친 알림이 있을 수 있음)
// - types에 든 종류의 새 알림이 왔을 때 (예: 내 글에 댓글 → 댓글 목록)
// refresh는 화면을 비우지 않고 조용히 다시 불러와야 한다. 이미 불러오는 중이면 한 번 건너뛴다
const HIDDEN_MS = 15_000

export function useLiveRefresh(refresh, { types = [] } = {}) {
  const store = useNotificationStore()
  let hiddenAt = 0
  let running = false

  const run = async () => {
    if (running) return
    running = true
    try { await refresh() } catch { /* 화면이 알아서 알린다 */ } finally { running = false }
  }
  const onVisibility = () => {
    if (document.visibilityState === 'hidden') hiddenAt = Date.now()
    else if (hiddenAt && Date.now() - hiddenAt >= HIDDEN_MS) { hiddenAt = 0; run() }
  }

  watch(() => store.resync, run)
  watch(() => store.latest, (n) => { if (n && types.includes(n.type)) run() })
  onMounted(() => document.addEventListener('visibilitychange', onVisibility))
  onBeforeUnmount(() => document.removeEventListener('visibilitychange', onVisibility))
}
