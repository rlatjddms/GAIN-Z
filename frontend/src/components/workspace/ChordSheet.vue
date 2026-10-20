<template>
  <!-- 코드 악보: 코드는 가사 바로 위에. 키를 바꾸면 코드 이름만 옮겨서 보여 준다 -->
  <div class="chord-sheet" :style="{ fontSize: `${fontScale}rem` }">
    <template v-for="(row, i) in rows" :key="i">
      <h4 v-if="row.type === 'section'" class="section">{{ row.text }}</h4>
      <div v-else-if="row.type === 'blank'" class="blank"></div>
      <div v-else class="line">
        <span v-for="(seg, j) in row.segments" :key="j" class="seg">
          <span class="chord">{{ seg.chord ? shift(seg.chord) : ' ' }}</span>
          <span class="lyric">{{ seg.lyric || ' ' }}</span>
        </span>
      </div>
    </template>
    <p v-if="!rows.length" class="empty">{{ emptyText }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { parseChordSheet } from '../../music/chordSheet'
import { transposeChord, transposeContext } from '../../music/transpose'

const props = defineProps({
  content: { type: String, default: '' },
  originalKey: { type: String, default: null },
  // 보는 키 (없으면 원래 키)
  viewKey: { type: String, default: null },
  fontScale: { type: Number, default: 1 },
  emptyText: { type: String, default: '아직 악보 내용이 없어요.' },
})

const rows = computed(() => parseChordSheet(props.content))
const ctx = computed(() => transposeContext(props.originalKey, props.viewKey || props.originalKey))
const shift = (chord) => transposeChord(chord, ctx.value.semitones, ctx.value.preferFlat)
</script>

<style scoped>
.chord-sheet { line-height: 1.35; color: #212529; word-break: keep-all; }
.section {
  margin: 18px 0 6px; font-size: 0.75em; font-weight: 900; letter-spacing: 0.04em; color: var(--c-primary-text);
}
.section:first-child { margin-top: 0; }
.blank { height: 0.9em; }
.line { display: flex; flex-wrap: wrap; align-items: flex-end; margin-bottom: 6px; }
.seg { display: inline-flex; flex-direction: column; white-space: pre; }
.chord {
  font-size: 0.85em; font-weight: 900; color: #e8590c; padding-right: 6px;
  font-family: 'Pretendard', sans-serif; font-variant-numeric: tabular-nums;
}
.lyric { font-weight: 600; }
.empty { margin: 0; color: #adb5bd; font-size: 0.85rem; }
</style>
