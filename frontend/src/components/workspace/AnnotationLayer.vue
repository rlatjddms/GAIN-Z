<template>
  <!-- 악보 한 면(사진 한 장, PDF 한 쪽) 위 내 필기 (#167). 필기 중일 때만 그리기·글자 쓰기를 받는다 -->
  <div
    class="annotation-layer"
    :class="{ drawing: active, erasing: active && store.tool === 'eraser', typing: active && store.tool === 'text' }"
    @pointerdown="down"
    @pointermove="move"
    @pointerup="up"
    @pointercancel="up"
  >
    <svg :viewBox="`0 0 ${SURFACE_WIDTH} ${height}`" preserveAspectRatio="none">
      <template v-for="(s, i) in shown" :key="i">
        <text v-if="s.t === 'text'" v-show="editing?.index !== i" :x="s.x" :y="s.y" :fill="s.c" :font-size="s.s" class="note">{{ s.text }}</text>
        <path v-else :d="pathOf(s.p)" :stroke="s.c" :stroke-width="s.s" :class="s.t === 'hl' ? 'hl' : 'pen'" />
      </template>
      <path v-if="current" :d="pathOf(current.p)" :stroke="current.c" :stroke-width="current.s" :class="current.t === 'hl' ? 'hl' : 'pen'" />
    </svg>

    <!-- 글자 입력칸: 누른 자리에 뜨고, Enter나 다른 곳을 누르면 붙는다.
         휴대폰 자판은 누르는 순간 바로 포커스해야 떠서, 글자 도구일 때는 늘 만들어 두고 숨겨 둔다 -->
    <input
      v-if="active && store.tool === 'text'"
      ref="input"
      :value="editing ? editing.text : ''"
      class="note-input"
      :class="{ idle: !editing }"
      :style="inputStyle"
      :tabindex="editing ? 0 : -1"
      @input="editing && (editing.text = $event.target.value)"
      maxlength="80"
      enterkeyhint="done"
      aria-label="필기 글자"
      @pointerdown.stop
      @keydown.enter.prevent="commitText"
      @keydown.esc.prevent="editing = null"
      @blur="commitText"
    >
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
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
const surfaceChange = (list) => store.change([{ pageId: props.pageId, pdfPage: props.pdfPage, h: height.value, strokes: list }])

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

// 글자 칸 대략 크기 (한글·한자는 글자 크기만큼, 영문·숫자는 그 0.6배)
const textBox = (s) => {
  const width = [...s.text].reduce((w, ch) => w + (ch.charCodeAt(0) > 0x2e80 ? s.s : s.s * 0.6), 0)
  return { left: s.x, right: s.x + width, top: s.y - s.s, bottom: s.y + s.s * 0.25 }
}

// 지우개: 닿은 획·글자를 통째로 지운다
const ERASER_RADIUS = 14
const hits = (stroke, [x, y]) => {
  if (stroke.t === 'text') {
    const b = textBox(stroke)
    return x >= b.left - ERASER_RADIUS && x <= b.right + ERASER_RADIUS && y >= b.top - ERASER_RADIUS && y <= b.bottom + ERASER_RADIUS
  }
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

/* ---------- 글자 ---------- */
const editing = ref(null)  // { index: 고치는 글자 자리(-1이면 새 글자), x, y, s, c, text }
const input = ref(null)
const layerWidth = ref(0)

// 입력칸을 글자가 그려질 자리·크기에 맞춘다
const inputStyle = computed(() => {
  const e = editing.value
  if (!e) return null
  const scale = layerWidth.value / SURFACE_WIDTH
  return {
    left: `${(e.x / SURFACE_WIDTH) * 100}%`,
    top: `${((e.y - e.s) / height.value) * 100}%`,
    fontSize: `${Math.max(e.s * scale, 12)}px`,
    color: e.c,
  }
})

const startText = (e, at) => {
  layerWidth.value = e.currentTarget.getBoundingClientRect().width
  // 이미 쓴 글자를 누르면 그 글자를 고친다
  const index = strokes.value.findIndex((s) => s.t === 'text' && hits({ ...s }, at))
  if (index !== -1) editing.value = { ...strokes.value[index], index }
  else {
    const size = TOOLS.text.size
    editing.value = { index: -1, x: at[0], y: round(at[1] + size / 2), s: size, c: store.color, text: '' }
  }
  input.value?.focus()
}

const commitText = () => {
  const e = editing.value
  if (!e) return
  editing.value = null
  const text = e.text.trim()
  const list = [...strokes.value]
  if (e.index !== -1) {
    if (text === list[e.index].text) return
    if (text) list[e.index] = { ...list[e.index], text }
    else list.splice(e.index, 1)
  } else if (text) {
    list.push({ t: 'text', c: e.c, s: e.s, x: e.x, y: e.y, text })
  } else {
    return
  }
  surfaceChange(list)
}

// 필기를 끝내거나 도구를 바꾸면 쓰던 글자를 붙인다
watch(() => [store.enabled, store.tool], commitText)

/* ---------- 포인터 ---------- */
const down = (e) => {
  if (!active.value || pointerId !== null) return
  if (e.pointerType === 'mouse' && e.button !== 0) return
  e.preventDefault()
  pointerId = e.pointerId
  e.currentTarget.setPointerCapture(e.pointerId)
  const at = point(e)
  if (store.tool === 'text') return  // 글자는 손을 뗄 때 (그때 바로 자판을 띄움)
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
  if (!current.value) return
  const p = current.value.p
  const dx = at[0] - p[p.length - 2]
  const dy = at[1] - p[p.length - 1]
  // 너무 가까운 점은 버려서 저장 크기를 줄인다
  if (dx * dx + dy * dy >= 4) current.value.p = [...p, ...at]
}

const up = (e) => {
  if (e.pointerId !== pointerId) return
  pointerId = null
  if (store.tool === 'text') {
    commitText()
    startText(e, point(e))
    return
  }
  if (store.tool === 'eraser') {
    if (erased.value) surfaceChange(erased.value)
    erased.value = null
    return
  }
  const stroke = current.value
  current.value = null
  if (stroke) surfaceChange([...strokes.value, stroke])
}
</script>

<style scoped>
.annotation-layer { position: absolute; inset: 0; pointer-events: none; }
.annotation-layer.drawing { pointer-events: auto; touch-action: none; cursor: crosshair; }
.annotation-layer.erasing { cursor: cell; }
.annotation-layer.typing { cursor: text; }
svg {
  display: block; width: 100%; height: 100%;
  fill: none; stroke-linecap: round; stroke-linejoin: round;
}
.pen { stroke-opacity: 0.95; }
.hl { stroke-opacity: 0.38; mix-blend-mode: multiply; stroke-linecap: butt; }
/* 글자: 악보 줄 위에서도 읽히게 흰 테두리 */
.note { font-family: 'Pretendard', sans-serif; font-weight: 800; stroke: #fff; stroke-width: 4; paint-order: stroke; }

.note-input.idle { left: 0; top: 0; width: 1px; min-width: 0; opacity: 0; pointer-events: none; }
.note-input {
  position: absolute; min-width: 6em; max-width: 90%; padding: 0 2px; margin: 0;
  font-family: 'Pretendard', sans-serif; font-weight: 800; line-height: 1.25;
  background: rgba(255, 255, 255, 0.9); border: 1px dashed currentColor; border-radius: 4px; outline: none;
}
</style>
