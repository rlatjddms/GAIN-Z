import { defineStore } from 'pinia'
import { DEFAULT_THEME, themeOf } from '../constants/themes'
import { faviconSvg } from '../pixel/logo'

// 테마 색 (#122). 이 기기에만 저장한다 (localStorage). 화면 전체는 <html data-theme>로 바뀐다
const STORAGE_KEY = 'gainz-theme'

function readSaved() {
  try {
    return localStorage.getItem(STORAGE_KEY) || DEFAULT_THEME
  } catch {
    return DEFAULT_THEME
  }
}

// html 속성과 파비콘을 그 테마로
export function applyTheme(key) {
  const theme = themeOf(key)
  document.documentElement.dataset.theme = theme.key
  let link = document.querySelector('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  link.type = 'image/svg+xml'
  link.href = `data:image/svg+xml,${encodeURIComponent(faviconSvg(theme.color))}`
  return theme.key
}

export const useThemeStore = defineStore('theme', {
  state: () => ({ key: readSaved() }),
  actions: {
    init() {
      this.key = applyTheme(this.key)
    },
    set(key) {
      this.key = applyTheme(key)
      try {
        localStorage.setItem(STORAGE_KEY, this.key)
      } catch {
        // 저장이 막힌 브라우저면 이번 방문 동안만
      }
    },
  },
})
