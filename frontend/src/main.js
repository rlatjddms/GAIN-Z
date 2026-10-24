import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/theme.css'
import './styles/ui.css'
import { useThemeStore } from './stores/theme'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 화면이 그려지기 전에 고른 테마 색을 적용 (깜빡임 방지)
useThemeStore(pinia).init()
app.use(router)
app.mount('#app')
hideSplash()

// 시작 화면(#194, index.html)은 열고 1.3초 동안(애니메이션) 또는 앱이 뜰 때까지 보여 주고 걷어 낸다
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
  }, Math.max(0, 1300 - performance.now()))
}

// PWA (#143): 배포한 화면에서만 서비스 워커를 켠다 (개발 중에는 캐시 때문에 고친 화면이 안 보일 수 있어서 끈다)
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js'))
}

