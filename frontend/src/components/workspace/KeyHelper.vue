<template>
  <!-- 키 바꿔 치기 도우미: 연습 키 고르기와 카포 안내. 바꿔 칠 코드표는 악보 위에 떠 있는 창(ChordMapFloat, #192)으로 -->
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
      <template v-else>−/+로 연습할 키를 고르면 악보 위에 바꿔 칠 코드표가 떠요.</template>
      <span v-if="status" class="saved">{{ status }}</span>
    </p>

    <p v-if="capo" class="capo">카포 {{ capo }}프렛에 끼우고 악보 코드 그대로 치면 {{ current }} 키예요.</p>

  </div>
</template>

<script setup>
import { ArrowRight } from 'lucide-vue-next'
import { computed } from 'vue'
import { transposeKey, semitonesBetween, capoFor } from '../../music/transpose'

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

</style>
