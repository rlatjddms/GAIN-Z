import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/theme.css'
import './styles/ui.css'
import { useThemeStore } from './stores/theme'
import { watchKeyboardInset } from './components/ui/keyboardInset'
import { watchImeInput } from './components/ui/imeInput'

// 시작 화면을 걷어 내는 시각: 로고가 다 그려지고(약 1.75초) 잠깐 보인 뒤 (#291에서 1.3초 → 2.1초).
// hideSplash()가 아래에서 바로 불리므로 그보다 위에 둔다 (const는 선언 전에 쓰면 오류)
const SPLASH_MS = 2100

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 휴대폰 키보드가 아래 팝업을 가리지 않게 (#226)
watchKeyboardInset()
// 한글 조합 중인 글자도 입력 칸 값으로 바로 (#263)
watchImeInput()
// 화면이 그려지기 전에 고른 테마 색을 적용 (깜빡임 방지)
useThemeStore(pinia).init()
app.use(router)
app.mount('#app')
hideSplash()

// 시작 화면(#194, index.html)은 열고 SPLASH_MS 동안(애니메이션) 또는 앱이 뜰 때까지 보여 주고 걷어 낸다
function hideSplash() {
  const splash = document.getElementById('splash')
  if (!splash) return
  if (getComputedStyle(splash).display === 'none') {
    splash.remove()
    return
  }
  setTimeout(() => {
    splash.classList.add('done')
    setTimeout(() => splash.remove(), 400)
  }, Math.max(0, SPLASH_MS - performance.now()))
}

// PWA (#143): 배포한 화면에서만 서비스 워커를 켠다 (개발 중에는 캐시 때문에 고친 화면이 안 보일 수 있어서 끈다)
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js'))
}

