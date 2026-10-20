<template>
  <WsCard>
    <!-- 머리: 기간 이름과 이동(위 날짜도 같이 바뀜), 주·월 전환 -->
    <div class="head">
      <div class="period">
        <button class="arrow" :aria-label="mode === 'WEEK' ? '지난주' : '지난달'" @click="shift(-1)">‹</button>
        <h3>{{ title }}</h3>
        <button class="arrow" :disabled="!canNext" :aria-label="mode === 'WEEK' ? '다음 주' : '다음 달'" @click="shift(1)">›</button>
      </div>
      <div class="mode" role="tablist" aria-label="보기">
        <button role="tab" :aria-selected="mode === 'WEEK'" :class="{ on: mode === 'WEEK' }" @click="emit('update:mode', 'WEEK')">주</button>
        <button role="tab" :aria-selected="mode === 'MONTH'" :class="{ on: mode === 'MONTH' }" @click="emit('update:mode', 'MONTH')">월</button>
      </div>
    </div>

    <div class="body">
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
      <!-- 주 7칸 / 월 달력. 칸마다 연습 시간과 루틴 체크 -->
      <div class="grid" :class="mode === 'WEEK' ? 'week' : 'month'">
        <span v-for="w in WEEKDAYS" :key="w" class="wd">{{ w }}</span>
        <template v-for="d in gridCells" :key="d.key">
          <span v-if="d.blank" class="blank"></span>
          <button v-else class="cell" :class="[`lv${d.level}`, { selected: d.key === date, today: d.key === todayKey }]"
                  :disabled="d.future" :title="cellTitle(d)" @click="emit('select', d.key)">
            <span class="num">{{ d.day }}</span>
            <i v-if="d.key === todayKey && timer.running" class="rec"></i>
            <span v-if="!d.future" class="time" :class="{ zero: !d.seconds }">{{ shortDuration(d.seconds) }}</span>
            <span v-if="d.routine" class="routine" :class="{ done: d.routine.done }">{{ d.routine.done ? '✓' : `${d.routine.completed}/${d.routine.total}` }}</span>
          </button>
        </template>
      </div>

      <!-- 색 기준 -->
      <div class="legend">
        <span class="scale">
          <template v-for="l in LEGEND" :key="l.level"><i :class="`lv${l.level}`"></i><small>{{ l.label }}</small></template>
        </span>
        <span class="rt"><b>2/3</b> 루틴 체크 <b class="done">✓</b> 모두 완료</span>
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

const canNext = computed(() => dateKey(end.value) < todayKey)
// 지난·다음 주(7일) 또는 지난·다음 달(같은 날, 그 달에 없으면 마지막 날). 오늘을 넘지 않는다
const shift = (dir) => {
  let next
  if (props.mode === 'WEEK') next = addDays(selectedDate.value, dir * 7)
  else {
    const d = selectedDate.value
    const lastDay = new Date(d.getFullYear(), d.getMonth() + dir + 1, 0).getDate()
    next = new Date(d.getFullYear(), d.getMonth() + dir, Math.min(d.getDate(), lastDay))
  }
  const key = dateKey(next)
  emit('select', key > todayKey ? todayKey : key)
}

/* ---------- 칸 ---------- */
const cellOf = (d) => {
  const key = dateKey(d)
  const seconds = practice.totalOf(key)
  const r = routineRows.value[key]
  const routine = r && r.total > 0 ? { completed: r.completed, total: r.total, done: r.completed >= r.total } : null
  return { key, day: d.getDate(), seconds, level: practiceLevel(seconds), future: key > todayKey, routine }
}
const cells = computed(() => Array.from({ length: 7 }, (_, i) => cellOf(addDays(start.value, i))))
const monthCells = computed(() => {
  const lead = (start.value.getDay() + 6) % 7
  const out = Array.from({ length: lead }, (_, i) => ({ key: `blank-${i}`, blank: true }))
  for (let d = new Date(start.value); d <= end.value; d = addDays(d, 1)) out.push(cellOf(d))
  return out
})
const gridCells = computed(() => (props.mode === 'WEEK' ? cells.value : monthCells.value))
const cellTitle = (d) => {
  if (d.future) return ''
  const parts = [dateLabel(d.key), durationText(d.seconds)]
  if (d.routine) parts.push(`루틴 ${d.routine.completed}/${d.routine.total}`)
  return parts.join(' · ')
}

// 색 단계: 연습 안 함 / 30분 미만 / 1시간 미만 / 2시간 미만 / 2시간 이상 (practiceLevel과 같은 기준)
const LEGEND = [
  { level: 0, label: '0' },
  { level: 1, label: '~30분' },
  { level: 2, label: '~1시간' },
  { level: 3, label: '~2시간' },
  { level: 4, label: '2시간+' },
]

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
.period { display: flex; align-items: center; gap: 6px; }
.period h3 { margin: 0 4px; font-size: 0.95rem; font-weight: 900; color: #343a40; min-width: 64px; text-align: center; }
.arrow {
  width: 28px; height: 28px; border: none; border-radius: 9px; background: #fff; color: #495057;
  font-size: 1rem; font-weight: 900; cursor: pointer; box-shadow: inset 0 0 0 1px #e9ecef;
}
.arrow:hover:not(:disabled) { color: var(--c-primary-text); box-shadow: inset 0 0 0 1px var(--c-primary-line); }
.arrow:disabled { opacity: 0.35; cursor: default; }
.mode { display: inline-flex; background: #f1f3f5; border-radius: 10px; padding: 3px; gap: 2px; }
.mode button {
  border: none; background: none; border-radius: 8px; padding: 4px 14px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #868e96;
}
.mode button.on { background: #fff; color: var(--c-primary-text); box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1); }

/* 위 요약 한 줄, 아래 칸 */
.body { display: flex; flex-direction: column; gap: 16px; }

.grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 6px; }
.wd { text-align: center; font-size: 0.68rem; font-weight: 800; color: #adb5bd; }
.cell {
  position: relative; width: 100%; height: 54px; border: none; border-radius: 9px; background: #f1f3f5; cursor: pointer;
  font-family: inherit; padding: 5px 6px; display: flex; flex-direction: column; justify-content: space-between; align-items: flex-start;
  transition: box-shadow 0.15s; min-width: 0;
}
.week .cell { height: 64px; padding: 7px 8px; }
.cell:hover:not(:disabled) { box-shadow: 0 0 0 2px var(--c-primary-line); }
.cell:disabled { cursor: default; opacity: 0.4; }
.cell.selected { box-shadow: 0 0 0 2px var(--c-primary); }
.num { font-size: 0.72rem; font-weight: 800; color: #495057; line-height: 1; }
.cell.today .num { color: var(--c-primary-text); text-decoration: underline; text-underline-offset: 2px; }
.time { align-self: stretch; text-align: right; font-size: 0.68rem; font-weight: 800; color: #343a40; font-variant-numeric: tabular-nums; line-height: 1; white-space: nowrap; overflow: hidden; }
.week .time { font-size: 0.78rem; }
.time.zero { color: #ced4da; }
.routine {
  position: absolute; top: 5px; right: 6px; font-size: 0.6rem; font-weight: 900; line-height: 1;
  color: #868e96; font-variant-numeric: tabular-nums;
}
.routine.done { color: #2f9e44; font-size: 0.7rem; }
.cell.lv1, .legend i.lv1 { background: var(--c-primary-line); }
.cell.lv2, .legend i.lv2 { background: color-mix(in srgb, var(--c-primary) 55%, #fff); }
.cell.lv3, .legend i.lv3 { background: color-mix(in srgb, var(--c-primary) 78%, #fff); }
.cell.lv4, .legend i.lv4 { background: var(--c-primary); }
.cell.lv3 .num, .cell.lv4 .num, .cell.lv3 .time, .cell.lv4 .time, .cell.lv3 .routine, .cell.lv4 .routine { color: #fff; }
.cell.lv3 .routine.done, .cell.lv4 .routine.done { color: #b2f2bb; }

.legend { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; font-size: 0.66rem; font-weight: 800; color: #adb5bd; }
.scale { display: flex; align-items: center; gap: 4px; }
.scale i { width: 10px; height: 10px; border-radius: 3px; background: #f1f3f5; }
.scale small { font-size: inherit; margin-right: 6px; }
.rt b { color: #868e96; margin-right: 2px; }
.rt b.done { color: #2f9e44; margin-left: 6px; }

.rec {
  position: absolute; bottom: 7px; left: 7px; width: 6px; height: 6px; border-radius: 50%;
  background: #e03131; animation: blink 1s infinite;
}
@keyframes blink { 50% { opacity: 0.2; } }

.stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.stat {
  background: #f8f9fa; border-radius: 12px; padding: 9px 12px;
  display: flex; flex-direction: column; gap: 1px; min-width: 0;
}
.stat span { font-size: 0.68rem; font-weight: 800; color: #868e96; }
.stat strong { font-size: 0.95rem; font-weight: 900; color: #1a1a1a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat strong.empty { color: #ced4da; }
.stat small { font-size: 0.72rem; font-weight: 800; color: #adb5bd; }
.stat em { font-style: normal; font-size: 0.68rem; font-weight: 700; color: #adb5bd; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat em.up { color: #2f9e44; }
.stat em.down { color: #e8590c; }

.error { margin: 12px 0 0; font-size: 0.78rem; font-weight: 700; color: #e03131; }

@media (max-width: 640px) {
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
