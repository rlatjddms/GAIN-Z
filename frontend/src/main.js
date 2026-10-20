import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/theme.css'
import { useThemeStore } from './stores/theme'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 화면이 그려지기 전에 고른 테마 색을 적용 (깜빡임 방지)
useThemeStore(pinia).init()
app.use(router)
app.mount('#app')
