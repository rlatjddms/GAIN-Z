<template>
  <WsCard>
    <!-- 머리: 기간 이름(위 날짜를 따라감)과 주·월 전환 -->
    <div class="head">
      <h3>{{ title }}</h3>
      <div class="mode" role="tablist" aria-label="보기">
        <button role="tab" :aria-selected="mode === 'WEEK'" :class="{ on: mode === 'WEEK' }" @click="emit('update:mode', 'WEEK')">주</button>
        <button role="tab" :aria-selected="mode === 'MONTH'" :class="{ on: mode === 'MONTH' }" @click="emit('update:mode', 'MONTH')">월</button>
      </div>
    </div>

    <div class="body">
      <!-- 주: 7칸 -->
      <div v-if="mode === 'WEEK'" class="grid week">
        <span v-for="w in WEEKDAYS" :key="w" class="wd">{{ w }}</span>
        <button v-for="d in cells" :key="d.key" class="cell" :class="[`lv${d.level}`, { selected: d.key === date, today: d.key === todayKey }]"
                :disabled="d.future" :title="d.future ? '' : `${dateLabel(d.key)} · ${durationText(d.seconds)}`" @click="emit('select', d.key)">
          <span class="num">{{ d.day }}</span>
          <i v-if="d.key === todayKey && timer.running" class="rec"></i>
        </button>
        <span v-for="d in cells" :key="`t-${d.key}`" class="time" :class="{ zero: !d.seconds }">{{ d.future ? '' : shortDuration(d.seconds) }}</span>
      </div>

      <!-- 월: 달력 -->
      <div v-else class="grid">
        <span v-for="w in WEEKDAYS" :key="w" class="wd">{{ w }}</span>
        <template v-for="d in monthCells" :key="d.key">
          <span v-if="d.blank" class="blank"></span>
          <button v-else class="cell" :class="[`lv${d.level}`, { selected: d.key === date, today: d.key === todayKey }]"
                  :disabled="d.future" :title="d.future ? '' : `${dateLabel(d.key)} · ${durationText(d.seconds)}`" @click="emit('select', d.key)">
            <span class="num">{{ d.day }}</span>
            <i v-if="d.key === todayKey && timer.running" class="rec"></i>
          </button>
        </template>
      </div>

      <!-- 기간 요약 -->
      <div class="stats">
        <div class="stat">
          <span>총 연습</span>
          <strong>{{ durationText(summary.total) }}</strong>
          <em v-if="compareText" :class="{ up: summary.total > summary.prevTotal, down: summary.total < summary.prevTotal }">{{ compareText }}</em>
        </div>
        <div class="stat">
          <span>연습한 날</span>
          <strong>{{ summary.practicedDays }}<small> / {{ summary.elapsedDays }}일</small></strong>
        </div>
        <div class="stat">
          <span>하루 평균</span>
          <strong :class="{ empty: !summary.practicedDays }">{{ summary.practicedDays ? durationText(summary.total / summary.practicedDays) : '—' }}</strong>
          <em>연습한 날 기준</em>
        </div>
        <div class="stat">
          <span>루틴 모두 완료</span>
          <strong :class="{ empty: !summary.routineDays }">{{ summary.routineDays ? summary.routineDone : '—' }}<small v-if="summary.routineDays"> / {{ summary.routineDays }}일</small></strong>
          <em v-if="summary.routineDays">루틴이 있던 날 기준</em>
        </div>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
  </WsCard>
</template>

<script setup>
// 고른 날짜가 들어 있는 주(월~일) 또는 달의 연습 기록과 요약. 기간은 화면 위 날짜를 따라가고, 칸을 누르면 그 날짜로 이동
import { ref, computed, watch } from 'vue'
import WsCard from './WsCard.vue'
import { usePracticeStore } from '../../stores/practice'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { apiErrorMessage } from '../../api/practice'
import { getRoutineSummary } from '../../api/routine'
import { addDays, dateKey, dateLabel, durationText, parseKey, practiceLevel, shortDuration, weekStart } from '../../constants/practice'

const props = defineProps({
  date: { type: String, required: true },
  // WEEK | MONTH
  mode: { type: String, default: 'WEEK' },
})
const emit = defineEmits(['select', 'update:mode'])

const WEEKDAYS = ['월', '화', '수', '목', '금', '토', '일']
const practice = usePracticeStore()
const timer = usePracticeTimerStore()
const todayKey = dateKey()

/* ---------- 기간 ---------- */
const selectedDate = computed(() => parseKey(props.date))
const start = computed(() => (props.mode === 'WEEK'
  ? weekStart(selectedDate.value)
  : new Date(selectedDate.value.getFullYear(), selectedDate.value.getMonth(), 1)))
const end = computed(() => (props.mode === 'WEEK'
  ? addDays(start.value, 6)
  : new Date(start.value.getFullYear(), start.value.getMonth() + 1, 0)))
// 기간 중 오늘까지 (지난 기간이면 끝까지)
const lastCounted = computed(() => (dateKey(end.value) > todayKey ? parseKey(todayKey) : end.value))

const title = computed(() => {
  const s = start.value, e = end.value
  const containsToday = dateKey(s) <= todayKey && todayKey <= dateKey(e)
  if (props.mode === 'MONTH') {
    if (containsToday) return '이번 달'
    return s.getFullYear() === new Date().getFullYear() ? `${s.getMonth() + 1}월` : `${s.getFullYear()}년 ${s.getMonth() + 1}월`
  }
  if (containsToday) return '이번 주'
  return s.getMonth() === e.getMonth()
    ? `${s.getMonth() + 1}월 ${s.getDate()}일 ~ ${e.getDate()}일`
    : `${s.getMonth() + 1}월 ${s.getDate()}일 ~ ${e.getMonth() + 1}월 ${e.getDate()}일`
})

/* ---------- 칸 ---------- */
const cellOf = (d) => {
  const key = dateKey(d)
  const seconds = practice.totalOf(key)
  return { key, day: d.getDate(), seconds, level: practiceLevel(seconds), future: key > todayKey }
}
const cells = computed(() => Array.from({ length: 7 }, (_, i) => cellOf(addDays(start.value, i))))
const monthCells = computed(() => {
  const lead = (start.value.getDay() + 6) % 7
  const out = Array.from({ length: lead }, (_, i) => ({ key: `blank-${i}`, blank: true }))
  for (let d = new Date(start.value); d <= end.value; d = addDays(d, 1)) out.push(cellOf(d))
  return out
})

/* ---------- 불러오기 (이번 기간 + 비교할 지난 기간) ---------- */
const prevStart = computed(() => (props.mode === 'WEEK'
  ? addDays(start.value, -7)
  : new Date(start.value.getFullYear(), start.value.getMonth() - 1, 1)))
const routineRows = ref({})
const error = ref('')

const load = async () => {
  error.value = ''
  const to = lastCounted.value
  getRoutineSummary(dateKey(start.value), dateKey(to))
    .then((rows) => { routineRows.value = Object.fromEntries((rows || []).map((r) => [r.date, r])) })
    .catch(() => { routineRows.value = {} })
  try {
    await practice.loadRange(prevStart.value, to)
  } catch (e) {
    error.value = `연습 기록을 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
}
watch(() => [props.mode, dateKey(start.value)], load, { immediate: true })

/* ---------- 요약 ---------- */
const summary = computed(() => {
  let total = 0, practicedDays = 0, elapsedDays = 0
  for (let d = new Date(start.value); d <= lastCounted.value; d = addDays(d, 1)) {
    const s = practice.totalOf(dateKey(d))
    total += s
    elapsedDays += 1
    if (s > 0) practicedDays += 1
  }
  // 지난 기간의 같은 날수만큼 (이번 달 1~19일이면 지난달 1~19일)
  let prevTotal = 0
  const prevEndLimit = addDays(start.value, -1)
  for (let i = 0, d = new Date(prevStart.value); i < elapsedDays && d <= prevEndLimit; i++, d = addDays(d, 1)) {
    prevTotal += practice.totalOf(dateKey(d))
  }
  const rows = Object.values(routineRows.value).filter((r) => r.total > 0)
  return {
    total, practicedDays, elapsedDays, prevTotal,
    routineDays: rows.length,
    routineDone: rows.filter((r) => r.completed >= r.total).length,
  }
})

const compareText = computed(() => {
  const { total, prevTotal } = summary.value
  if (!total && !prevTotal) return ''
  const label = props.mode === 'WEEK' ? '지난주' : '지난달'
  const diff = total - prevTotal
  if (Math.abs(diff) < 60) return `${label}와 비슷해요`
  return `${label}보다 ${diff > 0 ? '+' : '-'}${durationText(Math.abs(diff))}`
})
</script>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.head h3 { margin: 0; font-size: 0.9rem; font-weight: 800; color: #343a40; }
.mode { display: inline-flex; background: #f1f3f5; border-radius: 10px; padding: 3px; gap: 2px; }
.mode button {
  border: none; background: none; border-radius: 8px; padding: 4px 14px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #868e96;
}
.mode button.on { background: #fff; color: #2d5bff; box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1); }

/* 왼쪽 칸, 오른쪽 요약 */
.body { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 24px; align-items: start; }

.grid { display: grid; grid-template-columns: repeat(7, 34px); gap: 5px; justify-content: start; }
.wd { text-align: center; font-size: 0.68rem; font-weight: 800; color: #adb5bd; }
.cell {
  position: relative; width: 34px; height: 34px; border: none; border-radius: 8px; background: #f1f3f5; cursor: pointer;
  font-family: inherit; display: flex; align-items: center; justify-content: center; padding: 0;
  transition: box-shadow 0.15s;
}
.cell:hover:not(:disabled) { box-shadow: 0 0 0 2px #c9d6ff; }
.cell:disabled { cursor: default; opacity: 0.4; }
.cell.selected { box-shadow: 0 0 0 2px #2d5bff; }
.num { font-size: 0.7rem; font-weight: 800; color: #495057; }
.cell.today .num { color: #2d5bff; text-decoration: underline; text-underline-offset: 2px; }
.cell.lv1 { background: #c9d6ff; }
.cell.lv2 { background: #8ea8ff; }
.cell.lv3 { background: #5a7dff; }
.cell.lv4 { background: #2d5bff; }
.cell.lv3 .num, .cell.lv4 .num { color: #fff; }
.time { text-align: center; font-size: 0.65rem; font-weight: 800; color: #495057; font-variant-numeric: tabular-nums; }
.time.zero { color: #ced4da; }

.rec {
  position: absolute; top: 3px; right: 3px; width: 6px; height: 6px; border-radius: 50%;
  background: #e03131; animation: blink 1s infinite;
}
@keyframes blink { 50% { opacity: 0.2; } }

.stats { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.stat {
  background: #f8f9fa; border: 1px solid #f1f3f5; border-radius: 12px; padding: 10px 12px;
  display: flex; flex-direction: column; gap: 2px; min-width: 0;
}
.stat span { font-size: 0.7rem; font-weight: 800; color: #868e96; }
.stat strong { font-size: 1rem; font-weight: 900; color: #1a1a1a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat strong.empty { color: #ced4da; }
.stat small { font-size: 0.75rem; font-weight: 800; color: #adb5bd; }
.stat em { font-style: normal; font-size: 0.68rem; font-weight: 700; color: #adb5bd; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat em.up { color: #2f9e44; }
.stat em.down { color: #e8590c; }

.error { margin: 12px 0 0; font-size: 0.78rem; font-weight: 700; color: #e03131; }

@media (max-width: 640px) {
  .body { grid-template-columns: minmax(0, 1fr); }
  .grid { justify-content: center; }
}
</style>
