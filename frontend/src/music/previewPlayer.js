// 30초 미리듣기 재생기 (#187). 화면 어디서 눌러도 한 번에 한 곡만 소리가 난다
import { ref } from 'vue'

const playingUrl = ref(null)
let audio = null

function stop() {
  if (audio) {
    audio.pause()
    audio = null
  }
  playingUrl.value = null
}

// 같은 곡을 다시 누르면 멈추고, 다른 곡을 누르면 바꿔 튼다
async function toggle(url) {
  if (!url) return
  if (playingUrl.value === url) { stop(); return }
  stop()
  audio = new Audio(url)
  audio.volume = 0.6
  const current = audio
  audio.addEventListener('ended', () => { if (audio === current) stop() })
  playingUrl.value = url
  try {
    await audio.play()
  } catch {
    // 브라우저가 막았거나 주소가 사라진 경우: 조용히 멈춘다
    if (audio === current) stop()
  }
}

export function usePreviewPlayer() {
  return { playingUrl, toggle, stop }
}
