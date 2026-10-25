import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 앱 업데이트 (#223): 빌드마다 버전을 정해 화면 코드에 넣고, 같은 값을 dist/version.json으로도 낸다.
// 앱은 version.json을 다시 받아 보고 값이 다르면 새 버전이 배포된 것으로 본다
const BUILD_ID = [new Date().toISOString(), process.env.VERCEL_GIT_COMMIT_SHA?.slice(0, 7)].filter(Boolean).join('+')

const versionFile = {
  name: 'gainz-version-file',
  apply: 'build',
  generateBundle() {
    this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ buildId: BUILD_ID }) })
  },
}

export default defineConfig({
  plugins: [vue(), versionFile],
  define: { __BUILD_ID__: JSON.stringify(BUILD_ID) },
  // pdf.js 워커(#167)는 ES 모듈이라 워커도 ES 형식으로 묶는다
  worker: { format: 'es' },
})
