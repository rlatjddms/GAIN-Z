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
        <text v-if="s.t === 'text'" v-show="editing?.index !== i" :x="s.x" :y="s.y" :fill="s.c" :font-size="s.s" class="note">
          <tspan v-for="(line, k) in linesOf(s.text)" :key="k" :x="s.x" :dy="k ? s.s * LINE_HEIGHT : 0">{{ line || NBSP }}</tspan>
        </text>
        <path v-else :d="pathOf(s.p)" :stroke="s.c" :stroke-width="s.s" :class="s.t === 'hl' ? 'hl' : 'pen'" />
      </template>
      <path v-if="current" :d="pathOf(current.p)" :stroke="current.c" :stroke-width="current.s" :class="current.t === 'hl' ? 'hl' : 'pen'" />
    </svg>

    <!-- 글자 입력칸: 누른 자리에 뜨고, Enter나 다른 곳을 누르면 붙는다.
         휴대폰 자판은 누르는 순간 바로 포커스해야 떠서, 글자 도구일 때는 늘 만들어 두고 숨겨 둔다.
         편집 도구처럼 테두리를 끌면 옮기고, 오른쪽 아래 손잡이를 끌면 크기, 오른쪽 위 ×는 지우기 -->
    <div
      v-if="active && store.tool === 'text'"
      ref="editorEl"
      class="note-editor"
      :class="{ idle: !editing }"
      :style="editorStyle"
      title="끌어서 옮기기"
      @pointerdown.stop="startDrag($event, 'move')"
    >
      <!-- Enter는 줄바꿈, 다른 곳을 누르거나 Ctrl(⌘)+Enter로 붙이기 -->
      <textarea
        ref="input"
        :value="editing ? editing.text : ''"
        class="note-input"
        :style="inputStyle"
        :tabindex="editing ? 0 : -1"
        :cols="inputSize.cols"
        :rows="inputSize.rows"
        wrap="off"
        maxlength="300"
        aria-label="필기 글자"
        @pointerdown.stop
        @input="editing && (editing.text = $event.target.value)"
        @keydown.enter.ctrl.prevent="commitText"
        @keydown.enter.meta.prevent="commitText"
        @keydown.esc.prevent="editing = null"
        @blur="onBlur"
      ></textarea>
      <template v-if="editing">
        <button type="button" class="note-del" aria-label="이 글자 지우기" title="이 글자 지우기"
                @pointerdown.stop.prevent="toolPress = true" @click="removeText"><X :size="10" :stroke-width="3" /></button>
        <span class="note-resize" title="끌어서 크기 조절" @pointerdown.stop="startDrag($event, 'resize')"></span>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { X } from 'lucide-vue-next'
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

// 글자: 여러 줄 (줄 간격은 글자 크기의 1.25배)
const LINE_HEIGHT = 1.25
const NBSP = String.fromCharCode(160) // 빈 줄도 줄 간격을 차지하게
const linesOf = (text) => (text || '').split(String.fromCharCode(10))
const isWide = (ch) => ch.charCodeAt(0) > 0x2e80

// 글자 칸 대략 크기 (한글·한자는 글자 크기만큼, 영문·숫자는 그 0.6배). 가장 긴 줄 × 줄 수
const textBox = (s) => {
  const lines = linesOf(s.text)
  const width = Math.max(...lines.map((line) => [...line].reduce((w, ch) => w + (isWide(ch) ? s.s : s.s * 0.6), 0)))
  return { left: s.x, right: s.x + width, top: s.y - s.s, bottom: s.y + (lines.length - 1) * s.s * LINE_HEIGHT + s.s * 0.25 }
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
const editorStyle = computed(() => {
  const e = editing.value
  if (!e) return null
  return { left: `${(e.x / SURFACE_WIDTH) * 100}%`, top: `${((e.y - e.s) / height.value) * 100}%` }
})
const inputStyle = computed(() => {
  const e = editing.value
  if (!e) return null
  return { fontSize: `${Math.max((e.s * layerWidth.value) / SURFACE_WIDTH, 12)}px`, color: e.c }
})

// 쓴 만큼 늘어나는 입력칸 (한글은 두 칸으로 셈)
const inputSize = computed(() => {
  const lines = linesOf(editing.value?.text)
  const cols = Math.max(...lines.map((line) => [...line].reduce((n, ch) => n + (isWide(ch) ? 2 : 1), 0)))
  return { cols: Math.max(cols + 1, 4), rows: lines.length }
})

// 이 글자 지우기 (새로 쓰던 글자면 그냥 닫기)
const removeText = () => {
  const e = editing.value
  toolPress.value = false
  editing.value = null
  if (!e || e.index === -1) return
  const list = [...strokes.value]
  list.splice(e.index, 1)
  surfaceChange(list)
}

// 상자를 끌거나 손잡이를 누르는 동안 입력칸이 포커스를 잃어도 붙이지 않는다
const toolPress = ref(false)
const onBlur = () => {
  if (toolPress.value) return
  commitText()
}

// 끌기: move = 테두리를 끌어 옮기기, resize = 오른쪽 아래 손잡이로 크기 (왼쪽 위는 그대로)
const MIN_TEXT = 12
const MAX_TEXT = 160
const editorEl = ref(null)
const clamp = (v, lo, hi) => Math.min(Math.max(v, lo), hi)
const startDrag = (ev, mode) => {
  const e = editing.value
  if (!e || (ev.pointerType === 'mouse' && ev.button !== 0)) return
  ev.preventDefault() // 입력칸 포커스는 그대로
  toolPress.value = true
  const handle = ev.currentTarget
  handle.setPointerCapture(ev.pointerId)
  const unit = SURFACE_WIDTH / layerWidth.value // 화면 1px → 면 좌표
  const start = { x: e.x, y: e.y, s: e.s, cx: ev.clientX, cy: ev.clientY, w: editorEl.value.offsetWidth, h: editorEl.value.offsetHeight }
  const onMove = (m) => {
    if (m.pointerId !== ev.pointerId) return
    const dx = m.clientX - start.cx
    const dy = m.clientY - start.cy
    if (mode === 'move') {
      e.x = round(clamp(start.x + dx * unit, 0, SURFACE_WIDTH - 20))
      e.y = round(clamp(start.y + dy * unit, e.s, height.value))
    } else {
      const ratio = (start.w + start.h + dx + dy) / (start.w + start.h)
      e.s = round(clamp(start.s * ratio, MIN_TEXT, MAX_TEXT))
      e.y = round(start.y - start.s + e.s)
    }
  }
  const onUp = (m) => {
    if (m.pointerId !== ev.pointerId) return
    handle.removeEventListener('pointermove', onMove)
    handle.removeEventListener('pointerup', onUp)
    handle.removeEventListener('pointercancel', onUp)
    toolPress.value = false
    if (mode === 'resize') store.textSize = e.s
    input.value?.focus()
  }
  handle.addEventListener('pointermove', onMove)
  handle.addEventListener('pointerup', onUp)
  handle.addEventListener('pointercancel', onUp)
}

const startText = (e, at) => {
  layerWidth.value = e.currentTarget.getBoundingClientRect().width
  // 이미 쓴 글자를 누르면 그 글자를 고친다
  const index = strokes.value.findIndex((s) => s.t === 'text' && hits({ ...s }, at))
  if (index !== -1) editing.value = { ...strokes.value[index], index }
  else {
    const size = store.textSize
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
    const before = list[e.index]
    if (text === before.text && e.s === before.s && e.x === before.x && e.y === before.y) return
    if (text) list[e.index] = { ...before, text, s: e.s, x: e.x, y: e.y }
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

.note-editor {
  position: absolute; max-width: 92%; margin: -6px 0 0 -8px; padding: 4px 6px;
  border: 1.5px dashed var(--c-primary); border-radius: 6px; background: rgba(255, 255, 255, 0.8);
  cursor: move; touch-action: none;
}
.note-editor.idle { left: 0; top: 0; width: 1px; margin: 0; padding: 0; border: 0; opacity: 0; pointer-events: none; }
.note-editor.idle .note-input { min-width: 0; width: 1px; }
/* 오른쪽 위 지우기 동그라미 */
.note-del {
  position: absolute; top: -8px; right: -8px; width: 16px; height: 16px; padding: 0;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--c-line); border-radius: 50%; background: #fff; color: #111827; cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}
.note-del:hover { color: var(--c-danger); border-color: var(--c-danger); }
/* 오른쪽 아래 크기 손잡이 */
.note-resize {
  position: absolute; right: -5px; bottom: -5px; width: 8px; height: 8px;
  background: #fff; border: 1.5px solid var(--c-primary); border-radius: 2px; cursor: nwse-resize; touch-action: none;
}
/* 손가락 화면: 작게 보여도 누르기 쉽게 누르는 범위만 넓힌다 */
@media (pointer: coarse) {
  .note-del::before, .note-resize::before { content: ''; position: absolute; inset: -10px; }
}
.note-input {
  display: block; max-width: 100%; padding: 0; margin: 0; cursor: text;
  font-family: 'Pretendard', sans-serif; font-weight: 800; line-height: 1.25;
  background: transparent; border: 0; outline: none; resize: none; overflow: hidden; white-space: pre;
}
</style>
