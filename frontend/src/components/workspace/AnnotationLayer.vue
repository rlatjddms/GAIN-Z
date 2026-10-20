<template>
  <!-- 악보 한 면(사진 한 장, PDF 한 쪽) 위 내 필기 (#167). 필기 중일 때만 그리기를 받는다 -->
  <svg
    class="annotation-layer"
    :class="{ drawing: active, erasing: active && store.tool === 'eraser' }"
    :viewBox="`0 0 ${SURFACE_WIDTH} ${height}`"
    preserveAspectRatio="none"
    @pointerdown="down"
    @pointermove="move"
    @pointerup="up"
    @pointercancel="up"
  >
    <path
      v-for="(s, i) in shown"
      :key="i"
      :d="pathOf(s.p)"
      :stroke="s.c"
      :stroke-width="s.s"
      :class="s.t === 'hl' ? 'hl' : 'pen'"
    />
    <path v-if="current" :d="pathOf(current.p)" :stroke="current.c" :stroke-width="current.s" :class="current.t === 'hl' ? 'hl' : 'pen'" />
  </svg>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAnnotationStore, surfaceKey, SURFACE_WIDTH, TOOLS } from '../../stores/annotation'

const props = defineProps({
  pageId: { type: Number, required: true },
  pdfPage: { type: Number, default: 1 },
  // 면의 세로/가로 비율 (그림·PDF 쪽 크기에서)
  aspect: { type: Number, required: true },
})

const store = useAnnotationStore()
const key = computed(() => surfaceKey(props.pageId, props.pdfPage))
const height = computed(() => Math.round(SURFACE_WIDTH * props.aspect))
const strokes = computed(() => store.strokesOf(key.value))
const active = computed(() => store.enabled && store.loaded)

const current = ref(null)  // 그리는 중인 획
let pointerId = null
const erased = ref(null)   // 이번에 지우개로 지운 뒤 남은 획 (끝날 때 한 번에 기록)
const shown = computed(() => erased.value || strokes.value)

const round = (v) => Math.round(v * 10) / 10

// 화면 좌표 → 면 좌표 (너비 1000 기준)
const point = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const scale = SURFACE_WIDTH / rect.width
  return [round((e.clientX - rect.left) * scale), round((e.clientY - rect.top) * scale)]
}

// 점들을 부드러운 선으로 (가운데 점을 지나는 2차 곡선)
const pathOf = (p) => {
  if (!p || p.length < 2) return ''
  if (p.length === 2) return `M${p[0]} ${p[1]}l0.1 0`
  let d = `M${p[0]} ${p[1]}`
  for (let i = 2; i < p.length - 2; i += 2) {
    const mx = round((p[i] + p[i + 2]) / 2)
    const my = round((p[i + 1] + p[i + 3]) / 2)
    d += `Q${p[i]} ${p[i + 1]} ${mx} ${my}`
  }
  return `${d}L${p[p.length - 2]} ${p[p.length - 1]}`
}

// 지우개: 닿은 획을 통째로 지운다
const ERASER_RADIUS = 14
const hits = (stroke, [x, y]) => {
  const r = ERASER_RADIUS + stroke.s / 2
  const p = stroke.p
  for (let i = 0; i < p.length; i += 2) {
    const dx = p[i] - x
    const dy = p[i + 1] - y
    if (dx * dx + dy * dy <= r * r) return true
  }
  return false
}
const erase = (at) => {
  const list = shown.value
  const rest = list.filter((s) => !hits(s, at))
  if (rest.length !== list.length) erased.value = rest
}

const down = (e) => {
  if (!active.value || pointerId !== null) return
  if (e.pointerType === 'mouse' && e.button !== 0) return
  e.preventDefault()
  pointerId = e.pointerId
  e.currentTarget.setPointerCapture(e.pointerId)
  const at = point(e)
  if (store.tool === 'eraser') {
    erased.value = null
    erase(at)
    return
  }
  const tool = TOOLS[store.tool]
  current.value = { t: store.tool, c: store.color, s: tool.width, p: at }
}

const move = (e) => {
  if (e.pointerId !== pointerId) return
  e.preventDefault()
  const at = point(e)
  if (store.tool === 'eraser') {
    erase(at)
    return
  }
  const p = current.value.p
  const dx = at[0] - p[p.length - 2]
  const dy = at[1] - p[p.length - 1]
  // 너무 가까운 점은 버려서 저장 크기를 줄인다
  if (dx * dx + dy * dy >= 4) current.value.p = [...p, ...at]
}

const up = (e) => {
  if (e.pointerId !== pointerId) return
  pointerId = null
  const change = { pageId: props.pageId, pdfPage: props.pdfPage, h: height.value }
  if (store.tool === 'eraser') {
    if (erased.value) store.change([{ ...change, strokes: erased.value }])
    erased.value = null
    return
  }
  const stroke = current.value
  current.value = null
  if (stroke) store.change([{ ...change, strokes: [...strokes.value, stroke] }])
}
</script>

<style scoped>
.annotation-layer {
  position: absolute; inset: 0; width: 100%; height: 100%;
  pointer-events: none; fill: none; stroke-linecap: round; stroke-linejoin: round;
}
.annotation-layer.drawing { pointer-events: auto; touch-action: none; cursor: crosshair; }
.annotation-layer.erasing { cursor: cell; }
.pen { stroke-opacity: 0.95; }
.hl { stroke-opacity: 0.38; mix-blend-mode: multiply; stroke-linecap: butt; }
</style>
