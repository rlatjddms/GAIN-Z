import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // pdf.js 워커(#167)는 ES 모듈이라 워커도 ES 형식으로 묶는다
  worker: { format: 'es' },
})