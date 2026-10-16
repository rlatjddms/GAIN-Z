<template>
  <!-- 타브 악보: 화면 너비에 맞춰 마디를 줄로 나누고, 줄 맨 앞에 줄 이름을 적는다 -->
  <div ref="root" class="tab-sheet">
    <div v-for="(row, r) in rows" :key="r" class="row">
      <div class="tuning" :style="{ height: `${staffHeight}px` }" aria-hidden="true">
        <span v-for="(name, s) in tuning" :key="s" :style="{ top: `${stringY(s + 1)}px` }">{{ name }}</span>
      </div>

      <div class="measures">
        <div
          v-for="item in row"
          :key="item.index"
          class="measure"
          :class="{ current: cursor?.index === item.index, invalid: item.measure.invalid }"
          :style="{ flexGrow: item.width }"
          :data-measure-index="item.index"
        >
          <div class="head">
            <span v-if="item.measure.section" class="section">{{ item.measure.section }}</span>
            <span class="num">{{ item.measure.number }}</span>
            <span v-if="item.index === 0 || item.measure.timeChanged" class="time">{{ item.measure.time.num }}/{{ item.measure.time.den }}</span>
            <span v-if="item.measure.invalid" class="bad">읽을 수 없는 마디</span>
          </div>

          <div class="chords">
            <span v-for="(c, i) in item.measure.chords" :key="i" :style="{ left: x(c.beat, item.measure) }">{{ c.name }}</span>
          </div>

          <div class="staff" :style="{ height: `${staffHeight}px` }">
            <div v-for="s in tuning.length" :key="s" class="line" :style="{ top: `${stringY(s)}px` }"></div>

            <template v-for="(n, i) in visibleNotes(item.measure)" :key="i">
              <span
                v-if="techniqueMark(n).before"
                class="tech"
                :style="{ left: x(n.beat, item.measure), top: `${stringY(n.string)}px` }"
              >{{ techniqueMark(n).before }}</span>
              <span class="fret" :style="{ left: x(n.beat, item.measure), top: `${stringY(n.string)}px` }">
                {{ fretLabel(n) }}<small v-if="techniqueMark(n).after">{{ techniqueMark(n).after }}</small>
              </span>
            </template>

            <div
              v-if="cursor?.index === item.index"
              class="cursor"
              :style="{ left: x(cursor.beat, item.measure) }"
            ></div>
          </div>

          <p v-if="item.measure.fingering" class="fingering" :title="item.measure.fingering">{{ item.measure.fingering }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { techniqueMark, fretLabel } from '../../music/noteData'

const props = defineProps({
  // noteData.js로 읽고(readMeasures) 키를 옮긴(transposeMeasures, linkTechniques) 마디
  measures: { type: Array, required: true },
  // 줄 이름 (1번 줄부터)
  tuning: { type: Array, required: true },
  // 따라가기 커서 { index, beat }
  cursor: { type: Object, default: null },
})

const STRING_GAP = 14
const STAFF_PAD = 8
const TUNING_WIDTH = 20
const MAX_PER_ROW = 4

const staffHeight = computed(() => (props.tuning.length - 1) * STRING_GAP + STAFF_PAD * 2)
const stringY = (string) => STAFF_PAD + (string - 1) * STRING_GAP

// 마디 안 가로 위치: 양쪽 여백 14px을 빼고 박 비율대로
const x = (beat, measure) => `calc(14px + (100% - 28px) * ${beat / measure.length})`

const visibleNotes = (measure) => measure.notes.filter((n) => n.string <= props.tuning.length)

/* ---------- 줄 나누기: 화면 너비가 바뀌면 다시 나눈다 ---------- */
const root = ref(null)
const width = ref(0)
let observer = null
onMounted(() => {
  observer = new ResizeObserver(([entry]) => { width.value = entry.contentRect.width })
  observer.observe(root.value)
})
onBeforeUnmount(() => observer?.disconnect())

// 마디 최소 너비: 박 수와 음이 나오는 자리 수에 맞춘다
const minWidth = (m) => {
  const columns = new Set([...m.notes.map((n) => n.beat), ...m.chords.map((c) => c.beat)]).size
  return Math.max(m.length * 34, columns * 26 + 28, 120)
}

const rows = computed(() => {
  const available = Math.max(width.value - TUNING_WIDTH, 120)
  const result = []
  let row = []
  let used = 0
  props.measures.forEach((measure, index) => {
    const w = minWidth(measure)
    if (row.length && (used + w > available || row.length >= MAX_PER_ROW)) {
      result.push(row)
      row = []
      used = 0
    }
    row.push({ measure, index, width: w })
    used += w
  })
  if (row.length) result.push(row)
  return result
})
</script>

<style scoped>
.tab-sheet { display: flex; flex-direction: column; gap: 18px; }
.row { display: flex; align-items: flex-start; }

.tuning { position: relative; flex: none; width: 20px; margin-top: 36px; }
.tuning span {
  position: absolute; left: 0; transform: translateY(-50%);
  font-size: 0.6rem; font-weight: 800; color: #adb5bd; line-height: 1;
}

.measures { flex: 1; display: flex; min-width: 0; }
.measure { flex-basis: 0; min-width: 0; position: relative; border-radius: 6px; }
.measure.current { background: #f0f4ff; }

.head { height: 16px; display: flex; align-items: center; gap: 6px; padding-left: 4px; white-space: nowrap; overflow: hidden; }
.section {
  font-size: 0.65rem; font-weight: 900; color: #fff; background: #2d5bff; border-radius: 4px; padding: 1px 5px;
}
.num { font-size: 0.6rem; font-weight: 800; color: #ced4da; font-variant-numeric: tabular-nums; }
.time { font-size: 0.65rem; font-weight: 900; color: #495057; }
.bad { font-size: 0.6rem; font-weight: 800; color: #e03131; }

.chords { position: relative; height: 20px; }
.chords span {
  position: absolute; bottom: 2px; white-space: nowrap;
  font-size: 0.8rem; font-weight: 900; color: #e8590c;
}

/* 줄과 마디줄: 마디마다 왼쪽에 마디줄, 줄 마지막 마디는 오른쪽에도 */
.staff { position: relative; border-left: 1px solid #868e96; }
.measure:last-child .staff { border-right: 1px solid #868e96; }
.line { position: absolute; left: 0; right: 0; height: 1px; background: #ced4da; }

.fret {
  position: absolute; transform: translate(-50%, -50%);
  padding: 0 2px; background: #fff; border-radius: 3px; line-height: 1;
  font-size: 0.72rem; font-weight: 800; color: #212529; white-space: nowrap;
  font-family: ui-monospace, 'SFMono-Regular', Consolas, monospace;
}
.measure.current .fret { background: #f0f4ff; }
.fret small { font-size: 0.6rem; color: #868e96; margin-left: 1px; }
.tech {
  position: absolute; transform: translate(-190%, -120%);
  font-size: 0.58rem; font-weight: 800; font-style: italic; color: #2d5bff; line-height: 1;
}

.cursor { position: absolute; top: -4px; bottom: -4px; width: 2px; margin-left: -1px; background: #2d5bff; border-radius: 1px; }

.fingering {
  margin: 4px 0 0; padding: 0 4px; font-size: 0.62rem; font-weight: 700; color: #868e96;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.measure.invalid .staff { background: repeating-linear-gradient(135deg, transparent 0 6px, #fff5f5 6px 12px); }
</style>
