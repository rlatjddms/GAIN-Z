<template>
  <!-- 바꿔 칠 코드표 (#192): 연습 키를 바꾸면 악보 위 구석에 떠 있는 작은 창. 접었다 펼 수 있고, 표에 없는 코드는 입력해서 바꿔 본다 -->
  <aside class="chord-float" :class="{ open }" aria-label="바꿔 칠 코드표">
    <button v-if="!open" type="button" class="pill" :aria-expanded="false" @click="setOpen(true)">
      <span class="keys">{{ originalKey }}<ArrowRight :size="12" :stroke-width="2.6" />{{ viewKey }}</span>
      <span class="pill-label">코드표</span>
      <ChevronUp :size="14" :stroke-width="2.4" />
    </button>

    <div v-else class="panel">
      <header class="head">
        <strong>{{ originalKey }} <ArrowRight :size="12" :stroke-width="2.6" /> {{ viewKey }}</strong>
        <span class="offset">{{ offsetLabel }}</span>
        <button type="button" class="fold" aria-label="코드표 접기" :aria-expanded="true" @click="setOpen(false)"><ChevronDown :size="16" :stroke-width="2.4" /></button>
      </header>

      <ul class="map">
        <li v-for="(row, i) in rows" :key="row.degree">
          <span class="from">{{ row.chord }}</span>
          <ArrowRight class="to-arrow" :size="11" :stroke-width="2.6" />
          <span class="to">{{ target[i].chord }}</span>
        </li>
      </ul>

      <form class="ask" @submit.prevent>
        <input
          v-model="typed"
          type="text"
          inputmode="text"
          autocapitalize="characters"
          autocomplete="off"
          spellcheck="false"
          maxlength="40"
          placeholder="표에 없는 코드 (예: Bb E7)"
          aria-label="바꿔 볼 코드"
        >
      </form>
      <p v-if="answers.length" class="answers">
        <span v-for="(a, i) in answers" :key="i" :class="{ bad: !a.to }">
          <template v-if="a.to">{{ a.from }} <ArrowRight :size="11" :stroke-width="2.6" /> <b>{{ a.to }}</b></template>
          <template v-else>{{ a.from }}?</template>
        </span>
      </p>
      <p v-else class="tip">7·sus4·/베이스 같은 꼬리는 그대로 붙여요</p>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowRight, ChevronDown, ChevronUp } from 'lucide-vue-next'
import { diatonicChords, normalizeChord, transposeChord, transposeContext } from '../../music/transpose'

const props = defineProps({
  originalKey: { type: String, required: true },
  viewKey: { type: String, required: true },
})

// 펼침 상태는 이 기기에 기억한다 (처음엔 펼침)
const STORE_KEY = 'chordMapOpen'
const readOpen = () => { try { return localStorage.getItem(STORE_KEY) !== '0' } catch { return true } }
const open = ref(readOpen())
const setOpen = (v) => {
  open.value = v
  try { localStorage.setItem(STORE_KEY, v ? '1' : '0') } catch { /* 기억 못 해도 화면은 그대로 */ }
}

const context = computed(() => transposeContext(props.originalKey, props.viewKey))
const offsetLabel = computed(() => `${context.value.semitones > 0 ? '+' : ''}${context.value.semitones}반음`)
const rows = computed(() => diatonicChords(props.originalKey))
const target = computed(() => diatonicChords(props.viewKey))

// 띄어 쓰거나 쉼표로 여러 개를 한 번에
const typed = ref('')
const answers = computed(() => typed.value.split(/[\s,]+/).filter(Boolean).slice(0, 6).map((raw) => {
  const chord = normalizeChord(raw)
  return { from: chord || raw, to: chord ? transposeChord(chord, context.value.semitones, context.value.preferFlat) : null }
}))
</script>

<style scoped>
.chord-float {
  position: fixed; right: 16px; bottom: calc(16px + env(safe-area-inset-bottom)); z-index: 40;
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
.pill {
  display: flex; align-items: center; gap: 8px; height: 40px; padding: 0 14px;
  border: 1px solid var(--c-line); border-radius: 999px; background: #fff; box-shadow: 0 8px 24px rgba(15, 23, 42, 0.14);
  font: inherit; font-size: 0.8rem; font-weight: 800; color: var(--c-text-2); cursor: pointer;
}
.pill:hover { border-color: var(--c-primary); }
.keys { display: inline-flex; align-items: center; gap: 3px; color: #e8590c; font-weight: 900; }
.panel {
  width: 236px; box-sizing: border-box; padding: 12px 12px 10px;
  background: #fff; border: 1px solid var(--c-line); border-radius: 16px; box-shadow: 0 12px 32px rgba(15, 23, 42, 0.16);
}
.head { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.head strong { display: inline-flex; align-items: center; gap: 4px; font-size: 0.9rem; font-weight: 900; }
.offset { font-size: 0.7rem; font-weight: 700; color: var(--c-muted); }
.fold { margin-left: auto; display: flex; padding: 4px; border: none; border-radius: 8px; background: none; color: var(--c-muted); cursor: pointer; }
.fold:hover { background: var(--c-line-soft); }
.map { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 4px 10px; }
.map li { display: flex; align-items: center; gap: 4px; padding: 3px 6px; border-radius: 8px; background: var(--c-bg); font-size: 0.82rem; font-weight: 800; }
.from { color: var(--c-text-2); }
.to-arrow { color: #ced4da; flex: none; }
.to { color: #e8590c; font-weight: 900; }
.ask { margin-top: 10px; }
.ask input {
  width: 100%; box-sizing: border-box; height: 34px; padding: 0 10px; border: 1px solid var(--c-line); border-radius: 10px;
  background: var(--c-bg); font: inherit; font-size: 16px; outline: none;
}
.ask input:focus { border-color: var(--c-primary); background: #fff; }
.answers { display: flex; flex-wrap: wrap; gap: 4px 10px; margin: 8px 2px 0; font-size: 0.8rem; font-weight: 700; color: var(--c-text-2); }
.answers span { display: inline-flex; align-items: center; gap: 3px; }
.answers b { color: #e8590c; font-weight: 900; }
.answers .bad { color: var(--c-faint); }
.tip { margin: 6px 2px 0; font-size: 0.68rem; color: var(--c-faint); }

@media (max-width: 640px) {
  .chord-float { right: 12px; bottom: calc(12px + env(safe-area-inset-bottom)); }
  .panel { width: min(236px, calc(100vw - 24px)); }
}
</style>
