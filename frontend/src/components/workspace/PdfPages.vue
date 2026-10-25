<template>
  <!-- PDF를 쪽마다 그림으로 그린다 (#167). 쪽마다 필기 면을 얹을 수 있다 -->
  <div ref="root" class="pdf-pages">
    <div v-if="error" class="state error">PDF를 열지 못했어요.</div>
    <div v-else-if="!pages.length" class="state">PDF 여는 중…</div>
    <div
      v-for="p in pages"
      :key="p.n"
      class="pdf-page"
      :style="{ aspectRatio: `${p.w} / ${p.h}` }"
    >
      <canvas :ref="(el) => (canvases[p.n] = el)" :aria-label="`${alt} ${p.n}쪽`" role="img"></canvas>
      <AnnotationLayer v-if="annotate" :page-id="pageId" :pdf-page="p.n" :aspect="p.h / p.w" />
      <span v-if="pages.length > 1" class="page-no">{{ p.n }} / {{ pages.length }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import AnnotationLayer from './AnnotationLayer.vue'
import { loadPdfjs } from './pdfjs'

const props = defineProps({
  url: { type: String, required: true },
  pageId: { type: Number, required: true },
  alt: { type: String, default: '악보' },
  annotate: { type: Boolean, default: false },
  // 이 쪽만 그린다 (크게 보기에서 쪽마다 한 화면, #231). null이면 모든 쪽
  only: { type: Number, default: null },
})
// loaded(쪽 수): PDF를 열었을 때
const emit = defineEmits(['loaded'])

const root = ref(null)
const pages = ref([])     // [{ n, w, h }] (1배 크기)
const error = ref(false)
const canvases = {}
const tasks = {}           // 쪽마다 그리는 중인 작업 (다시 그릴 때 멈춤)
let doc = null
let loading = null          // 연 PDF의 불러오기 작업 (닫을 때 이걸로 정리, pdf.js 6에는 doc.destroy가 없음)
// 닫다가 오류가 나도 화면 이동은 막지 않는다
const close = (task) => { try { task?.destroy() } catch { /* 이미 닫힘 */ } }
let renderedWidth = 0
let token = 0

const MAX_CANVAS_WIDTH = 2400

// 지금 너비에 맞춰 모든 쪽을 다시 그린다 (선명하게: 화면 배율만큼 크게)
const renderAll = async () => {
  if (!doc || !root.value) return
  const width = root.value.clientWidth
  if (!width || Math.abs(width - renderedWidth) < 8) return
  renderedWidth = width
  const mine = ++token
  const ratio = Math.min(window.devicePixelRatio || 1, 2)
  for (const p of pages.value) {
    if (mine !== token) return
    const page = await doc.getPage(p.n)
    const canvas = canvases[p.n]
    if (!canvas || mine !== token) continue
    const scale = Math.min((width * ratio) / p.w, MAX_CANVAS_WIDTH / p.w)
    const viewport = page.getViewport({ scale })
    tasks[p.n]?.cancel()
    canvas.width = Math.floor(viewport.width)
    canvas.height = Math.floor(viewport.height)
    const task = page.render({ canvas, viewport })
    tasks[p.n] = task
    await task.promise.catch(() => {})
  }
}

const open = async () => {
  const mine = ++token
  error.value = false
  pages.value = []
  renderedWidth = 0
  try {
    // 파일은 이미 받아 둔 blob: 주소라 바이트로 넘긴다 (pdf.js는 blob: 주소를 XHR로 다시 읽음)
    const [pdfjs, bytes] = await Promise.all([loadPdfjs(), fetch(props.url).then((r) => r.arrayBuffer())])
    if (mine !== token) return
    const task = pdfjs.getDocument({ data: new Uint8Array(bytes), isEvalSupported: false })
    const opened = await task.promise.catch((e) => { close(task); throw e })
    if (mine !== token) { close(task); return }
    close(loading)
    loading = task
    doc = opened
    const list = []
    for (let n = 1; n <= doc.numPages; n++) {
      const viewport = (await doc.getPage(n)).getViewport({ scale: 1 })
      list.push({ n, w: viewport.width, h: viewport.height })
    }
    if (mine !== token) return
    pages.value = props.only ? list.filter((p) => p.n === props.only) : list
    emit('loaded', list.length)
    await nextTick()
    renderAll()
  } catch {
    if (mine === token) error.value = true
  }
}

let observer = null
let resizeTimer = null
onMounted(() => {
  observer = new ResizeObserver(() => {
    clearTimeout(resizeTimer)
    resizeTimer = setTimeout(renderAll, 150)
  })
  observer.observe(root.value)
})
watch(() => props.url, open, { immediate: true })
onBeforeUnmount(() => {
  token++
  observer?.disconnect()
  clearTimeout(resizeTimer)
  close(loading)
  loading = null
  doc = null
})
</script>

<style scoped>
.pdf-pages { display: flex; flex-direction: column; gap: 12px; background: var(--c-bg); }
.pdf-page { position: relative; width: 100%; background: #fff; }
canvas { display: block; width: 100%; height: 100%; }
.page-no {
  position: absolute; right: 8px; bottom: 6px; padding: 2px 6px; border-radius: 6px;
  font-size: 0.7rem; font-weight: 700; color: var(--c-text-2); background: rgba(255, 255, 255, 0.85);
  pointer-events: none;
}
.state {
  display: flex; align-items: center; justify-content: center; min-height: 240px;
  font-size: 0.8rem; font-weight: 700; color: var(--c-faint); background: var(--c-bg);
}
.state.error { color: var(--c-danger); }
</style>
