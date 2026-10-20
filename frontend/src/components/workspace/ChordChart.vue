<template>
  <!-- 코드 악보(마디 표): 한 줄에 4마디, 구간이 바뀌면 새 줄. 코드가 그대로면 % -->
  <div class="chord-chart">
    <template v-for="(row, r) in rows" :key="r">
      <h4 v-if="row.section" class="section">{{ row.section }}</h4>
      <div class="row">
        <div
          v-for="cell in row.cells"
          :key="cell.index"
          class="cell"
          :class="{ current: cursor?.index === cell.index, invalid: cell.measure.invalid }"
          :data-measure-index="cell.index"
        >
          <span class="num">{{ cell.measure.number }}</span>
          <span v-if="cell.index === 0 || cell.measure.timeChanged" class="time">{{ cell.measure.time.num }}/{{ cell.measure.time.den }}</span>
          <span v-if="cell.measure.invalid" class="bad">읽을 수 없는 마디</span>
          <template v-else-if="cell.measure.chords.length">
            <span
              v-for="(c, i) in cell.measure.chords"
              :key="i"
              class="chord"
              :style="{ left: `calc(10px + (100% - 20px) * ${c.beat / cell.measure.length})` }"
            >{{ c.name }}</span>
          </template>
          <span v-else-if="cell.repeat" class="chord repeat" title="앞 마디 코드 그대로">%</span>
          <div
            v-if="cursor?.index === cell.index"
            class="cursor"
            :style="{ left: `calc(10px + (100% - 20px) * ${cursor.beat / cell.measure.length})` }"
          ></div>
        </div>
        <!-- 줄 끝을 맞추는 빈 칸 -->
        <div v-for="n in PER_ROW - row.cells.length" :key="`pad${n}`" class="cell pad"></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // noteData.js로 읽고 키를 옮긴 마디
  measures: { type: Array, required: true },
  cursor: { type: Object, default: null },
})

const PER_ROW = 4

const rows = computed(() => {
  const result = []
  let row = null
  let hadChord = false
  props.measures.forEach((measure, index) => {
    if (!row || row.cells.length >= PER_ROW || measure.section) {
      row = { section: measure.section, cells: [] }
      result.push(row)
    }
    row.cells.push({ measure, index, repeat: hadChord })
    if (measure.chords.length) hadChord = true
  })
  return result
})
</script>

<style scoped>
.chord-chart { display: flex; flex-direction: column; }
.section {
  margin: 16px 0 6px; font-size: 0.75rem; font-weight: 900; letter-spacing: 0.04em; color: var(--c-primary-text);
}
.section:first-child { margin-top: 0; }
.row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border-left: 2px solid var(--c-text-2); margin-bottom: 8px; }
.cell { position: relative; height: 52px; border-right: 2px solid var(--c-text-2); }
.cell.pad { border-right-color: transparent; }
.cell.current { background: var(--c-primary-soft); }
.cell.invalid { background: repeating-linear-gradient(135deg, transparent 0 6px, #fff5f5 6px 12px); }

.num { position: absolute; top: 3px; left: 5px; font-size: 0.58rem; font-weight: 800; color: #ced4da; }
.time { position: absolute; top: 3px; right: 5px; font-size: 0.62rem; font-weight: 900; color: var(--c-text-2); }
.chord {
  position: absolute; top: 50%; transform: translateY(-35%); white-space: nowrap;
  font-size: 1.05rem; font-weight: 900; color: #212529;
}
.chord.repeat { left: 50%; transform: translate(-50%, -35%); color: var(--c-faint); }
.bad { position: absolute; left: 6px; bottom: 5px; font-size: 0.6rem; font-weight: 800; color: #e03131; }
.cursor { position: absolute; top: 4px; bottom: 4px; width: 2px; margin-left: -1px; background: var(--c-primary); border-radius: 1px; }

@media (max-width: 520px) {
  .chord { font-size: 0.85rem; }
}
</style>
