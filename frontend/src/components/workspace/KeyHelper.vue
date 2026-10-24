<template>
  <!-- 키 바꿔 치기 도우미: 이미지 악보는 글자를 바꿀 수 없어서, 원래 키 코드 → 연습 키 코드 대응표를 보여 준다 -->
  <div class="key-helper">
    <div class="key-row">
      <div class="key-box">
        <span class="label">원래 키</span>
        <strong>{{ originalKey }}</strong>
      </div>
      <ArrowRight class="arrow" :size="16" :stroke-width="2.4" />
      <div class="key-control" role="group" aria-label="연습 키 바꾸기">
        <button class="step" aria-label="반음 내리기" @click="emit('change', transposeKey(current, -1))">−</button>
        <div class="key-box now">
          <span class="label">연습 키</span>
          <strong>{{ current }}</strong>
        </div>
        <button class="step" aria-label="반음 올리기" @click="emit('change', transposeKey(current, 1))">+</button>
      </div>
    </div>

    <p class="info">
      <template v-if="changed">{{ offsetLabel }} · <button class="text-btn" @click="emit('change', originalKey)">원래 키로</button></template>
      <template v-else>−/+로 연습할 키를 고르면 코드 대응표가 나와요.</template>
      <span v-if="status" class="saved">{{ status }}</span>
    </p>

    <p v-if="capo" class="capo">카포 {{ capo }}프렛에 끼우고 악보 코드 그대로 치면 {{ current }} 키예요.</p>

    <table v-if="changed" class="map">
      <thead>
        <tr><th></th><th>악보</th><th></th><th>바꿔 칠 코드</th></tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in rows" :key="row.degree">
          <td class="degree">{{ row.degree }}</td>
          <td>{{ row.chord }}</td>
          <td class="arrow"><ArrowRight :size="14" :stroke-width="2.4" /></td>
          <td class="to">{{ target[i].chord }}</td>
        </tr>
        <!-- 내가 추가한 코드 (#192): 이 악보에 나오는 키 밖 코드. 이 기기에 악보마다 기억 -->
        <tr v-for="chord in custom" :key="chord" class="custom">
          <td class="degree">
            <button type="button" class="remove" :aria-label="`${chord} 빼기`" @click="removeChord(chord)"><X :size="12" :stroke-width="2.6" /></button>
          </td>
          <td>{{ chord }}</td>
          <td class="arrow"><ArrowRight :size="14" :stroke-width="2.4" /></td>
          <td class="to">{{ moved(chord) }}</td>
        </tr>
      </tbody>
    </table>
    <form v-if="changed" class="add-chord" @submit.prevent="addChord">
      <input
        v-model="typed"
        type="text"
        autocomplete="off"
        autocapitalize="characters"
        spellcheck="false"
        maxlength="40"
        placeholder="악보에 있는 코드 추가 (예: Bb, E7)"
        aria-label="표에 추가할 코드"
      >
      <button type="submit" class="add-btn" :disabled="!typed.trim()">추가</button>
    </form>
    <p v-if="addError" class="add-error">{{ addError }}</p>
    <p v-if="changed" class="note">7, sus4, /베이스 같은 꼬리는 그대로 붙이면 돼요. (예: {{ rows[4].chord }}7 → {{ target[4].chord }}7)</p>
  </div>
</template>

<script setup>
import { ArrowRight, X } from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'
import { transposeKey, semitonesBetween, diatonicChords, capoFor, normalizeChord, transposeChord, transposeContext } from '../../music/transpose'

const props = defineProps({
  originalKey: { type: String, required: true },
  viewKey: { type: String, default: null },
  instrumentType: { type: String, default: null },
  status: { type: String, default: '' },
  // 내가 추가한 코드를 기억할 이름 (악보마다 다르게, 예: sheetChords:12). 없으면 기억하지 않는다
  storageKey: { type: String, default: '' },
})
// change: 새 연습 키
const emit = defineEmits(['change'])

const current = computed(() => props.viewKey || props.originalKey)
const changed = computed(() => current.value !== props.originalKey)
const offset = computed(() => semitonesBetween(props.originalKey, current.value))
const offsetLabel = computed(() => `${offset.value > 0 ? '+' : ''}${offset.value}반음`)
const rows = computed(() => diatonicChords(props.originalKey))
const target = computed(() => diatonicChords(current.value))
// 내가 추가한 코드 (#192): 쉼표·띄어쓰기로 여러 개를 한 번에, 겹치면 하나만, 20개까지
const MAX_CUSTOM = 20
const readCustom = () => {
  if (!props.storageKey) return []
  try { return JSON.parse(localStorage.getItem(props.storageKey)) || [] } catch { return [] }
}
const custom = ref(readCustom())
watch(() => props.storageKey, () => { custom.value = readCustom() })
const persist = () => {
  if (!props.storageKey) return
  try { localStorage.setItem(props.storageKey, JSON.stringify(custom.value)) } catch { /* 기억 못 해도 표는 그대로 */ }
}
const typed = ref('')
const addError = ref('')
const addChord = () => {
  const parts = typed.value.split(/[\s,]+/).filter(Boolean)
  const chords = parts.map(normalizeChord)
  const bad = parts.filter((_, i) => !chords[i])
  const known = new Set([...rows.value.map(r => r.chord), ...custom.value])
  const fresh = [...new Set(chords.filter(c => c && !known.has(c)))]
  custom.value = [...custom.value, ...fresh].slice(0, MAX_CUSTOM)
  persist()
  addError.value = bad.length ? `코드로 읽지 못했어요: ${bad.join(', ')}` : ''
  typed.value = bad.join(' ')
}
const removeChord = (chord) => {
  custom.value = custom.value.filter(c => c !== chord)
  persist()
}
const moved = (chord) => {
  const { semitones, preferFlat } = transposeContext(props.originalKey, current.value)
  return transposeChord(chord, semitones, preferFlat)
}
const capo = computed(() => (props.instrumentType === 'GUITAR' && changed.value ? capoFor(props.originalKey, current.value) : null))
</script>

<style scoped>
.key-row { display: flex; align-items: center; gap: 8px; }
.key-control { display: flex; align-items: center; gap: 4px; }
.key-box { min-width: 56px; display: flex; flex-direction: column; align-items: center; line-height: 1.1; }
.key-box .label { font-size: 0.6rem; font-weight: 800; color: var(--c-faint); }
.key-box strong { font-size: 1.2rem; font-weight: 900; color: var(--c-text-2); }
.key-box.now strong { color: #e8590c; }
.arrow { color: #ced4da; font-weight: 900; }
.step {
  width: 34px; height: 34px; border: 1px solid var(--c-line); background: #fff; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 1.05rem; font-weight: 900; color: var(--c-text-2);
}
.step:hover { border-color: var(--c-primary); color: var(--c-primary-text); }

.info { margin: 10px 0 0; font-size: 0.75rem; font-weight: 700; color: var(--c-muted); display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.saved { font-size: 0.78rem; font-weight: 800; color: var(--c-success); }
.text-btn { border: none; background: none; padding: 0; color: var(--c-primary-text); font-weight: 800; cursor: pointer; font-family: inherit; font-size: inherit; }
.capo { margin: 10px 0 0; padding: 8px 10px; border-radius: 10px; background: #fff4e6; color: #d9480f; font-size: 0.75rem; font-weight: 700; line-height: 1.5; }

.map { width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 0.85rem; }
.map th { font-size: 0.65rem; font-weight: 800; color: var(--c-faint); text-align: left; padding: 0 4px 6px; }
.map td { padding: 5px 4px; border-top: 1px solid var(--c-line-soft); font-weight: 800; color: var(--c-text-2); }
.map .degree { font-size: 0.7rem; color: var(--c-faint); width: 34px; }
.map .arrow { width: 18px; text-align: center; }
.map .to { color: #e8590c; font-weight: 900; }
.map .custom td { background: #fffaf3; }
.remove { display: flex; padding: 2px; border: none; border-radius: 6px; background: none; color: var(--c-faint); cursor: pointer; }
.remove:hover { color: var(--c-danger); background: var(--c-danger-soft); }
.add-chord { display: flex; gap: 6px; margin-top: 8px; }
.add-chord input {
  flex: 1; min-width: 0; height: 34px; padding: 0 10px; border: 1px solid var(--c-line); border-radius: 10px;
  background: var(--c-bg); font: inherit; font-size: 16px; outline: none;
}
.add-chord input:focus { border-color: var(--c-primary); background: #fff; }
.add-btn {
  flex: none; height: 34px; padding: 0 12px; border: 1px solid var(--c-primary); border-radius: 10px; background: #fff;
  font: inherit; font-size: 0.8rem; font-weight: 800; color: var(--c-primary-text); cursor: pointer;
}
.add-btn:disabled { border-color: var(--c-line); color: var(--c-faint); cursor: default; }
.add-error { margin: 6px 2px 0; font-size: 0.72rem; font-weight: 700; color: #e8590c; }
.note { margin: 8px 0 0; font-size: 0.7rem; color: var(--c-faint); line-height: 1.5; }
</style>
