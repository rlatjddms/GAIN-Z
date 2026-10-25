<template>
  <!-- 악보 크게 보기 (#231): 화면 가득, 사진 한 장·PDF 한 쪽이 한 화면. 옆으로 밀거나 ‹ › ← → 로 넘긴다 -->
  <Teleport to="body">
    <div v-if="open" class="viewer" role="dialog" aria-modal="true" :aria-label="`${title} 크게 보기`">
      <header class="bar">
        <strong class="title">{{ title }}</strong>
        <span class="count" aria-live="polite">{{ slides.length ? `${index + 1} / ${slides.length}` : '' }}</span>
        <div class="zoom" role="group" aria-label="확대·축소">
          <button type="button" :disabled="zoom <= ZOOMS[0]" aria-label="축소" @click="setZoom(-1)"><Minus :size="16" :stroke-width="2.4" /></button>
          <span>{{ zoom }}%</span>
          <button type="button" :disabled="zoom >= ZOOMS[ZOOMS.length - 1]" aria-label="확대" @click="setZoom(1)"><Plus :size="16" :stroke-width="2.4" /></button>
        </div>
        <button ref="closeBtn" type="button" class="close" aria-label="크게 보기 닫기" @click="emit('close')"><X :size="20" :stroke-width="2.4" /></button>
      </header>

      <div ref="track" class="track" @scroll="onScroll">
        <section v-for="(s, i) in slides" :key="s.key" class="slide" :aria-label="`${i + 1}쪽`">
          <!-- 지금 쪽과 양옆만 그린다 (쪽이 많아도 가볍게) -->
          <div v-if="Math.abs(i - index) <= 1" class="paper" :style="{ width: `${paperWidth(s)}px` }">
            <div v-if="!urls[s.pageId]" class="state">{{ failed[s.pageId] ? '이 장을 불러오지 못했어요.' : '불러오는 중…' }}</div>
            <PdfPages v-else-if="s.pdf" :url="urls[s.pageId]" :page-id="s.pageId" :only="s.n" :alt="`${title} ${i + 1}쪽`" annotate
                      @loaded="(count) => onPdfLoaded(s.pageId, count)" />
            <div v-else class="image">
              <img :src="urls[s.pageId]" :alt="`${title} ${i + 1}쪽`" draggable="false" @load="onImageLoad(s.pageId, $event)">
              <AnnotationLayer v-if="aspects[s.pageId]" :page-id="s.pageId" :aspect="aspects[s.pageId]" />
            </div>
          </div>
        </section>
      </div>

      <button v-if="index > 0" type="button" class="nav prev" aria-label="이전 쪽" @click="go(index - 1)"><ChevronLeft :size="28" :stroke-width="2.4" /></button>
      <button v-if="index < slides.length - 1" type="button" class="nav next" aria-label="다음 쪽" @click="go(index + 1)"><ChevronRight :size="28" :stroke-width="2.4" /></button>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ChevronLeft, ChevronRight, Minus, Plus, X } from 'lucide-vue-next'
import { loadSheetPageUrl } from '../../api/sheet'
import { isPdf } from '../../constants/sheet'
import AnnotationLayer from './AnnotationLayer.vue'
import PdfPages from './PdfPages.vue'
import { pdfPageRatios } from './pdfjs'

const props = defineProps({
  open: { type: Boolean, default: false },
  sheetId: { type: Number, required: true },
  // 악보의 장 [{ id, contentType }] (올린 순서)
  pages: { type: Array, default: () => [] },
  // 처음 보여 줄 장 (pages 순서)
  startPage: { type: Number, default: 0 },
  title: { type: String, default: '악보' },
})
const emit = defineEmits(['close'])

const ZOOMS = [100, 150, 200]
const zoom = ref(100)
const setZoom = (d) => { zoom.value = ZOOMS[Math.min(Math.max(ZOOMS.indexOf(zoom.value) + d, 0), ZOOMS.length - 1)] }

// 장마다 받은 파일 주소 (한 번만 받는다), PDF 쪽 수, 그림 세로/가로 비율 (필기 면 크기)
const urls = reactive({})
const failed = reactive({})
const pdfCounts = reactive({})
const aspects = reactive({})
const pdfRatios = reactive({})

// 100%는 한 쪽 전체가 화면에 들어오게 (넓은 화면은 높이에, 휴대폰 세로는 폭에 맞춰진다), 150·200%는 그보다 크게
const room = reactive({ w: 0, h: 0 })
let roomObserver = null
const ratioOf = (s) => (s.pdf ? pdfRatios[s.pageId]?.[s.n - 1] : aspects[s.pageId])
const paperWidth = (s) => {
  const ratio = ratioOf(s)
  const fit = ratio ? Math.min(room.w, room.h / ratio) : room.w
  return Math.max(Math.round(fit * zoom.value / 100), 0)
}

// 사진은 한 장이 한 쪽, PDF는 쪽마다 한 쪽 (쪽 수를 알기 전에는 첫 쪽만)
const slides = computed(() => props.pages.flatMap((p) => {
  if (!isPdf(p.contentType)) return [{ key: `${p.id}`, pageId: p.id, pdf: false }]
  const count = pdfCounts[p.id] || 1
  return Array.from({ length: count }, (_, k) => ({ key: `${p.id}-${k + 1}`, pageId: p.id, pdf: true, n: k + 1 }))
}))

const index = ref(0)
const track = ref(null)
const closeBtn = ref(null)

const go = (i) => {
  const el = track.value
  if (!el) return
  index.value = Math.min(Math.max(i, 0), slides.value.length - 1)
  el.scrollTo({ left: index.value * el.clientWidth, behavior: 'smooth' })
}
// 옆으로 민 만큼 지금 쪽을 맞춘다
const onScroll = () => {
  const el = track.value
  if (el?.clientWidth) index.value = Math.round(el.scrollLeft / el.clientWidth)
}
const onKey = (e) => {
  if (e.key === 'ArrowRight') { e.preventDefault(); go(index.value + 1) }
  else if (e.key === 'ArrowLeft') { e.preventDefault(); go(index.value - 1) }
  else if (e.key === 'Escape') emit('close')
}

const onImageLoad = (pageId, e) => {
  const { naturalWidth: w, naturalHeight: h } = e.target
  if (w && h) aspects[pageId] = h / w
}
// PDF 쪽 수를 알면 쪽을 펼친다
const onPdfLoaded = (pageId, count) => { if (pdfCounts[pageId] !== count) pdfCounts[pageId] = count }
// 앞쪽 PDF가 펼쳐져 쪽 번호가 바뀌어도 보던 쪽을 그대로 보여 준다
watch(slides, (next, prev) => {
  const key = prev?.[index.value]?.key
  const i = key ? next.findIndex((s) => s.key === key) : -1
  if (i < 0 || i === index.value) return
  index.value = i
  nextTick(() => track.value?.scrollTo({ left: i * track.value.clientWidth }))
})

const release = () => {
  Object.keys(urls).forEach((id) => { URL.revokeObjectURL(urls[id]); delete urls[id] })
  Object.keys(failed).forEach((id) => delete failed[id])
}
let token = 0
const loadAll = async () => {
  const mine = ++token
  await Promise.all(props.pages.map(async (p) => {
    try {
      const url = await loadSheetPageUrl(props.sheetId, p.id)
      if (mine !== token) { URL.revokeObjectURL(url); return }
      urls[p.id] = url
      // PDF는 쪽 수·비율을 먼저 알아 둔다 (멀리 있는 PDF도 "n / 전체"가 처음부터 맞고, 쪽을 화면에 맞출 수 있게)
      if (isPdf(p.contentType)) {
        const ratios = await pdfPageRatios(url)
        if (mine === token && ratios) { pdfRatios[p.id] = ratios; onPdfLoaded(p.id, ratios.length) }
      }
    } catch {
      if (mine === token) failed[p.id] = true
    }
  }))
}

// 쪽 위를 눌러 포커스가 빠져도 ← → Esc가 되게 창 전체에서 받는다
const listen = (on) => (on ? window.addEventListener('keydown', onKey) : window.removeEventListener('keydown', onKey))

watch(() => props.open, async (open) => {
  listen(open)
  if (!open) { token++; release(); roomObserver?.disconnect(); document.body.style.overflow = ''; return }
  zoom.value = 100
  // 처음 보여 줄 장의 첫 쪽 (앞에 있는 장들의 쪽 수만큼)
  index.value = slides.value.findIndex((s) => s.pageId === props.pages[props.startPage]?.id)
  if (index.value < 0) index.value = 0
  document.body.style.overflow = 'hidden'
  loadAll()
  await nextTick()
  // 쪽 칸의 크기 (안쪽 여백을 뺀 만큼)
  roomObserver = new ResizeObserver(([entry]) => {
    const slide = track.value?.querySelector('.slide')
    const pad = slide ? parseFloat(getComputedStyle(slide).paddingTop) + parseFloat(getComputedStyle(slide).paddingBottom) : 24
    room.w = entry.contentRect.width - 24
    room.h = entry.contentRect.height - pad
  })
  roomObserver.observe(track.value)
  track.value?.scrollTo({ left: index.value * track.value.clientWidth })
  closeBtn.value?.focus()
})
onBeforeUnmount(() => { listen(false); token++; release(); roomObserver?.disconnect(); document.body.style.overflow = '' })
</script>

<style scoped>
.viewer {
  position: fixed; inset: 0; z-index: 2500; display: flex; flex-direction: column;
  background: #1f2328; color: #fff; font-family: 'Pretendard', sans-serif;
}
.bar {
  flex: none; display: flex; align-items: center; gap: 10px;
  padding: calc(8px + env(safe-area-inset-top)) 12px 8px 16px; background: rgba(0, 0, 0, 0.35);
}
.title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.9rem; font-weight: 800; }
.count { flex: none; font-size: 0.82rem; font-weight: 700; color: #ced4da; font-variant-numeric: tabular-nums; }
.zoom { flex: none; display: flex; align-items: center; gap: 2px; font-size: 0.75rem; font-weight: 700; color: #ced4da; }
.zoom span { min-width: 40px; text-align: center; font-variant-numeric: tabular-nums; }
.bar button {
  display: flex; align-items: center; justify-content: center; width: 36px; height: 36px;
  border: none; border-radius: 10px; background: transparent; color: #fff; cursor: pointer;
}
.bar button:hover:not(:disabled) { background: rgba(255, 255, 255, 0.12); }
.bar button:disabled { opacity: 0.35; cursor: default; }

/* 옆으로 넘기기: 한 쪽이 화면 하나, 밀면 쪽 단위로 멈춘다 */
.track {
  flex: 1; min-height: 0; display: flex; overflow-x: auto; overflow-y: hidden;
  scroll-snap-type: x mandatory; scrollbar-width: none; overscroll-behavior-x: contain;
}
.track::-webkit-scrollbar { display: none; }
.slide {
  flex: 0 0 100%; min-width: 0; height: 100%; overflow: auto; scroll-snap-align: center; scroll-snap-stop: always;
  display: flex; justify-content: center; align-items: flex-start; padding: 12px 12px calc(12px + env(safe-area-inset-bottom)); box-sizing: border-box;
}
/* 화면보다 크게 확대하면 쪽 안에서 위아래·옆으로 움직인다 */
.paper { flex: none; min-width: 0; margin: auto; }
.image { position: relative; background: #fff; }
.image img { display: block; width: 100%; height: auto; user-select: none; }
.state { display: flex; align-items: center; justify-content: center; min-height: 50vh; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.nav {
  position: absolute; top: 50%; transform: translateY(-50%); display: flex; align-items: center; justify-content: center;
  width: 44px; height: 64px; border: none; border-radius: 12px; background: rgba(0, 0, 0, 0.35); color: #fff; cursor: pointer;
}
.nav:hover { background: rgba(0, 0, 0, 0.55); }
.prev { left: 8px; }
.next { right: 8px; }
/* 휴대폰은 밀어서 넘기는 게 주라 화살표를 작게 */
@media (max-width: 640px) {
  .nav { width: 34px; height: 52px; background: rgba(0, 0, 0, 0.25); }
  .zoom span { min-width: 34px; }
}
</style>
