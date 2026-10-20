<template>
  <!-- 키 바꿔 치기 도우미: 이미지 악보는 글자를 바꿀 수 없어서, 원래 키 코드 → 연습 키 코드 대응표를 보여 준다 -->
  <div class="key-helper">
    <div class="key-row">
      <div class="key-box">
        <span class="label">원래 키</span>
        <strong>{{ originalKey }}</strong>
      </div>
      <span class="arrow">→</span>
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

    <p v-if="capo" class="capo">🎸 카포 {{ capo }}프렛에 끼우고 악보 코드 그대로 치면 {{ current }} 키예요.</p>

    <table v-if="changed" class="map">
      <thead>
        <tr><th></th><th>악보</th><th></th><th>바꿔 칠 코드</th></tr>
      </thead>
      <tbody>
        <tr v-for="(row, i) in rows" :key="row.degree">
          <td class="degree">{{ row.degree }}</td>
          <td>{{ row.chord }}</td>
          <td class="arrow">→</td>
          <td class="to">{{ target[i].chord }}</td>
        </tr>
      </tbody>
    </table>
    <p v-if="changed" class="note">7, sus4, /베이스 같은 꼬리는 그대로 붙이면 돼요. (예: {{ rows[4].chord }}7 → {{ target[4].chord }}7)</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { transposeKey, semitonesBetween, diatonicChords, capoFor } from '../../music/transpose'

const props = defineProps({
  originalKey: { type: String, required: true },
  viewKey: { type: String, default: null },
  instrumentType: { type: String, default: null },
  status: { type: String, default: '' },
})
// change: 새 연습 키
const emit = defineEmits(['change'])

const current = computed(() => props.viewKey || props.originalKey)
const changed = computed(() => current.value !== props.originalKey)
const offset = computed(() => semitonesBetween(props.originalKey, current.value))
const offsetLabel = computed(() => `${offset.value > 0 ? '+' : ''}${offset.value}반음`)
const rows = computed(() => diatonicChords(props.originalKey))
const target = computed(() => diatonicChords(current.value))
const capo = computed(() => (props.instrumentType === 'GUITAR' && changed.value ? capoFor(props.originalKey, current.value) : null))
</script>

<style scoped>
.key-row { display: flex; align-items: center; gap: 8px; }
.key-control { display: flex; align-items: center; gap: 4px; }
.key-box { min-width: 56px; display: flex; flex-direction: column; align-items: center; line-height: 1.1; }
.key-box .label { font-size: 0.6rem; font-weight: 800; color: #adb5bd; }
.key-box strong { font-size: 1.2rem; font-weight: 900; color: #495057; }
.key-box.now strong { color: #e8590c; }
.arrow { color: #ced4da; font-weight: 900; }
.step {
  width: 34px; height: 34px; border: 1px solid #e9ecef; background: #fff; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 1.05rem; font-weight: 900; color: #495057;
}
.step:hover { border-color: var(--c-primary); color: var(--c-primary-text); }

.info { margin: 10px 0 0; font-size: 0.75rem; font-weight: 700; color: #868e96; display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.saved { color: #2f9e44; font-weight: 800; }
.text-btn { border: none; background: none; padding: 0; color: var(--c-primary-text); font-weight: 800; cursor: pointer; font-family: inherit; font-size: inherit; }
.capo { margin: 10px 0 0; padding: 8px 10px; border-radius: 10px; background: #fff4e6; color: #d9480f; font-size: 0.75rem; font-weight: 700; line-height: 1.5; }

.map { width: 100%; margin-top: 12px; border-collapse: collapse; font-size: 0.85rem; }
.map th { font-size: 0.65rem; font-weight: 800; color: #adb5bd; text-align: left; padding: 0 4px 6px; }
.map td { padding: 5px 4px; border-top: 1px solid #f1f3f5; font-weight: 800; color: #495057; }
.map .degree { font-size: 0.7rem; color: #adb5bd; width: 34px; }
.map .arrow { width: 18px; text-align: center; }
.map .to { color: #e8590c; font-weight: 900; }
.note { margin: 8px 0 0; font-size: 0.7rem; color: #adb5bd; line-height: 1.5; }
</style>
