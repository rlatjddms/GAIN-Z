<template>
  <!-- 마디 악보(TabData) 보기: 타브·코드 전환, 보는 키로 옮기기, BPM 따라가기 커서 -->
  <div ref="root" class="measure-sheet">
    <div class="bar">
      <div v-if="canTab && hasChords" class="modes" role="tablist" aria-label="악보 종류">
        <button role="tab" :aria-selected="mode === 'tab'" :class="{ on: mode === 'tab' }" @click="mode = 'tab'">타브</button>
        <button role="tab" :aria-selected="mode === 'chord'" :class="{ on: mode === 'chord' }" @click="mode = 'chord'">코드</button>
      </div>
      <span class="info">{{ infoText }}</span>

      <div class="play">
        <label class="bpm">
          <input v-model.number="tempo" type="number" min="20" max="300" step="1" aria-label="BPM">
          <span>BPM</span>
        </label>
        <button class="btn" :class="{ on: playing }" @click="playing ? stop() : play()">
          {{ playing ? '■ 멈추기' : '▶ 따라가기' }}
        </button>
      </div>
    </div>

    <TabSheet v-if="mode === 'tab'" :measures="shown" :tuning="tuning" :cursor="cursor" />
    <ChordChart v-else :measures="shown" :cursor="cursor" />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import TabSheet from './TabSheet.vue'
import ChordChart from './ChordChart.vue'
import { readMeasures, transposeMeasures, linkTechniques, locateBeat, TAB_TUNINGS } from '../../music/noteData'
import { transposeContext } from '../../music/transpose'

const props = defineProps({
  // 백엔드 TabView 목록 [{ measureNumber, noteData, recommendedFingering }]
  tabs: { type: Array, required: true },
  instrumentType: { type: String, default: null },
  originalKey: { type: String, default: null },
  // 보는 키 (없으면 원래 키)
  viewKey: { type: String, default: null },
  bpm: { type: Number, default: null },
})

const measures = computed(() => readMeasures(props.tabs))
const tuning = computed(() => TAB_TUNINGS[props.instrumentType] || null)
const canTab = computed(() => Boolean(tuning.value) && measures.value.some((m) => m.notes.length))
const hasChords = computed(() => measures.value.some((m) => m.chords.length))

const mode = ref('tab')
watch(canTab, (can) => { mode.value = can ? 'tab' : 'chord' }, { immediate: true })

const ctx = computed(() => transposeContext(props.originalKey, props.viewKey || props.originalKey))
const shown = computed(() => linkTechniques(transposeMeasures(measures.value, ctx.value)))

const infoText = computed(() => {
  const parts = [`${measures.value.length}마디`]
  if (mode.value === 'tab' && tuning.value) parts.push(`조율 ${[...tuning.value].reverse().join(' ')}`)
  if (ctx.value.semitones) parts.push(`${props.viewKey} 키로 옮김`)
  return parts.join(' · ')
})

/* ---------- 따라가기: BPM 박자대로 커서를 옮긴다 (소리는 없음) ---------- */
const DEFAULT_BPM = 80
const tempo = ref(props.bpm || DEFAULT_BPM)
const playing = ref(false)
const cursor = ref(null)
let frame = 0
let startedAt = 0
let startBeat = 0

const clampTempo = () => Math.min(Math.max(Number(tempo.value) || DEFAULT_BPM, 20), 300)

const tick = (now) => {
  const elapsed = startBeat + ((now - startedAt) / 60000) * clampTempo()
  const at = locateBeat(measures.value, elapsed)
  if (!at) { stop(); return }
  cursor.value = at
  frame = requestAnimationFrame(tick)
}

const play = () => {
  if (!measures.value.length) return
  playing.value = true
  startedAt = performance.now()
  startBeat = 0
  frame = requestAnimationFrame(tick)
}

const stop = () => {
  cancelAnimationFrame(frame)
  playing.value = false
  cursor.value = null
}

// 따라가는 중에 BPM을 바꾸면 지금 자리에서 새 빠르기로 이어 간다
watch(tempo, () => {
  if (!playing.value || !cursor.value) return
  const before = measures.value.slice(0, cursor.value.index).reduce((sum, m) => sum + m.length, 0)
  startBeat = before + cursor.value.beat
  startedAt = performance.now()
})

// 커서가 다음 마디로 넘어가면 그 마디가 보이게 스크롤
const root = ref(null)
watch(() => cursor.value?.index, async (index) => {
  if (index === undefined) return
  await nextTick()
  root.value?.querySelector(`[data-measure-index="${index}"]`)?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
})

watch(() => props.tabs, stop)
onBeforeUnmount(stop)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.modes { display: flex; background: var(--c-line-soft); border-radius: 10px; padding: 3px; }
.modes button {
  border: none; background: none; font-family: inherit; font-size: 0.75rem; font-weight: 800; color: var(--c-muted);
  padding: 6px 12px; border-radius: 8px; cursor: pointer;
}
.modes button.on { background: #fff; color: var(--c-primary-text); box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); }
.info { font-size: 0.72rem; font-weight: 700; color: var(--c-muted); }

.play { margin-left: auto; display: flex; align-items: center; gap: 6px; }
.bpm { display: flex; align-items: center; gap: 4px; font-size: 0.65rem; font-weight: 800; color: var(--c-faint); }
.bpm input {
  width: 52px; border: 1px solid var(--c-line); border-radius: 8px; padding: 6px; font-family: inherit;
  font-size: 0.78rem; font-weight: 800; color: var(--c-text-2); text-align: center; outline: none;
}
.bpm input:focus { border-color: var(--c-primary); }
.btn.on { background: var(--c-primary); border-color: var(--c-primary); color: #fff; }
</style>
