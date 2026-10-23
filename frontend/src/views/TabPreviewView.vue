<template>
  <div class="tab-preview">
    <header class="head">
      <h1>마디 악보 렌더러 미리보기</h1>
      <p>개발용 페이지. 예시 데이터는 <code>src/music/sampleNoteData.js</code>, 포맷은 <code>docs/note-data-format.md</code>.</p>
    </header>

    <section class="panel controls">
      <label>예시
        <select v-model="sampleName">
          <option value="guitar">기타 (12마디)</option>
          <option value="bass">베이스 (4마디)</option>
          <option value="broken">잘못된 데이터</option>
        </select>
      </label>
      <label>보는 키
        <select v-model="viewKey">
          <option v-for="k in MAJOR_KEYS" :key="k" :value="k">{{ k }}{{ k === SAMPLE_KEY ? ' (원래 키)' : '' }}</option>
        </select>
      </label>
      <label>폭
        <select v-model="width">
          <option value="100%">넓게</option>
          <option value="560px">중간</option>
          <option value="340px">좁게 (휴대폰)</option>
        </select>
      </label>
    </section>

    <section class="panel" :style="{ maxWidth: width }">
      <MeasureSheet
        :tabs="sample.tabs"
        :instrument-type="sample.instrument"
        :original-key="SAMPLE_KEY"
        :view-key="viewKey"
        :bpm="SAMPLE_BPM"
      />
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import MeasureSheet from '../components/workspace/MeasureSheet.vue'
import { MAJOR_KEYS } from '../music/transpose'
import { GUITAR_SAMPLE, BASS_SAMPLE, BROKEN_SAMPLE, SAMPLE_KEY, SAMPLE_BPM } from '../music/sampleNoteData'

const SAMPLES = {
  guitar: { tabs: GUITAR_SAMPLE, instrument: 'GUITAR' },
  bass: { tabs: BASS_SAMPLE, instrument: 'BASS' },
  broken: { tabs: BROKEN_SAMPLE, instrument: 'GUITAR' },
}

const sampleName = ref('guitar')
const sample = computed(() => SAMPLES[sampleName.value])
const viewKey = ref(SAMPLE_KEY)
const width = ref('100%')
</script>

<style scoped>
.tab-preview { max-width: 1000px; margin: 0 auto; padding: 32px 16px; display: flex; flex-direction: column; gap: 16px; }
.head h1 { margin: 0 0 6px; font-size: 1.3rem; font-weight: 900; }
.head p { margin: 0; font-size: 0.85rem; color: #868e96; }
.panel { background: #fff; border: 1px solid #e9ecef; border-radius: 16px; padding: 20px; box-sizing: border-box; width: 100%; }
.controls { display: flex; gap: 16px; flex-wrap: wrap; }
.controls label { display: flex; flex-direction: column; gap: 4px; font-size: 0.75rem; font-weight: 800; color: #495057; }
.controls select { padding: 6px 8px; border: 1px solid #dee2e6; border-radius: 8px; font-family: inherit; }
</style>
