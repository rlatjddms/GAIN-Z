<template>
  <WsCard>
    <!-- 머리: 기간 이름과 이동, 주·월 전환 -->
    <div class="head">
      <div class="period">
        <button class="arrow" aria-label="이전 기간" @click="shift(-1)">‹</button>
        <h3>{{ title }}</h3>
        <button class="arrow" :disabled="!canNext" aria-label="다음 기간" @click="shift(1)">›</button>
      </div>
      <div class="mode" role="tablist" aria-label="보기">
        <button role="tab" :aria-selected="mode === 'WEEK'" :class="{ on: mode === 'WEEK' }" @click="emit('update:mode', 'WEEK')">주</button>
        <button role="tab" :aria-selected="mode === 'MONTH'" :class="{ on: mode === 'MONTH' }" @click="emit('update:mode', 'MONTH')">월</button>
      </div>
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
        <strong :class="{ empty: !summary.routineDays }">{{ summary.routineDays ? `${summary.routineDone}` : '—' }}<small v-if="summary.routineDays"> / {{ summary.routineDays }}일</small></strong>
        <em v-if="summary.routineDays">루틴이 있던 날 기준</em>
      </div>
    </div>

    <!-- 주: 7칸 -->
    <div v-if="mode === 'WEEK'" class="days">
      <button v-for="d in cells" :key="d.key" class="day" :class="[`lv${d.level}`, { selected: d.key === date, today: d.key === todayKey }]"
              :disabled="d.future" :title="d.future ? '' : `${dateLabel(d.key)} · ${durationText(d.seconds)}`" @click="emit('select', d.key)">
        <span class="dow">{{ d.dow }}</span>
        <span class="cell"><i v-if="d.key === todayKey && timer.running" class="rec"></i></span>
        <span class="time">{{ d.future ? '' : shortDuration(d.seconds) }}</span>
        <span class="date">{{ d.day }}</span>
      </button>
    </div>

    <!-- 월: 달력 -->
    <div v-else class="month">
      <span v-for="w in WEEKDAYS" :key="w" class="wd">{{ w }}</span>
      <template v-for="d in monthCells" :key="d.key">
        <span v-if="d.blank" class="blank"></span>
        <button v-else class="mday" :class="[`lv${d.level}`, { selected: d.key === date, today: d.key === todayKey }]"
                :disabled="d.future" :title="d.future ? '' : `${dateLabel(d.key)} · ${durationText(d.seconds)}`" @click="emit('select', d.key)">
          <span class="num">{{ d.day }}</span>
          <span class="mtime">{{ d.future || !d.seconds ? '' : shortDuration(d.seconds) }}</span>
          <i v-if="d.key === todayKey && timer.running" class="rec"></i>
        </button>
      </template>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-else class="hint">날짜를 누르면 그날 연습 시간과 루틴을 볼 수 있어요.</p>
  </WsCard>
</template>

<script setup>
// 고른 날짜가 들어 있는 주(월~일) 또는 달의 연습 기록과 요약. 날짜를 누르면 그 날짜로 이동
import { ref, computed, watch } from 'vue'
import WsCard from './WsCard.vue'
import { usePracticeStore } from '../../stores/practice'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { apiErrorMessage } from '../../api/practice'
import { getRoutineSummary } from '../../api/routine'
import { addDays, dateKey, dateLabel, dowLabel, durationText, parseKey, practiceLevel, shortDuration, weekStart } from '../../constants/practice'

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
const canNext = computed(() => dateKey(end.value) < todayKey)

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
  return { key, day: d.getDate(), dow: dowLabel(d), seconds, level: practiceLevel(seconds), future: key > todayKey }
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
  const from = prevStart.value
  const to = lastCounted.value
  getRoutineSummary(dateKey(start.value), dateKey(to))
    .then((rows) => { routineRows.value = Object.fromEntries((rows || []).map((r) => [r.date, r])) })
    .catch(() => { routineRows.value = {} })
  try {
    await practice.loadRange(from, to)
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
.head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.period { display: flex; align-items: center; gap: 6px; }
.period h3 { margin: 0 4px; font-size: 0.95rem; font-weight: 900; color: #343a40; min-width: 64px; text-align: center; }
.arrow {
  width: 28px; height: 28px; border: none; border-radius: 9px; background: #fff; color: #495057;
  font-size: 1rem; font-weight: 900; cursor: pointer; box-shadow: inset 0 0 0 1px #e9ecef;
}
.arrow:hover:not(:disabled) { color: #2d5bff; box-shadow: inset 0 0 0 1px #c9d6ff; }
.arrow:disabled { opacity: 0.35; cursor: default; }
.mode { display: inline-flex; background: #f1f3f5; border-radius: 10px; padding: 3px; gap: 2px; }
.mode button {
  border: none; background: none; border-radius: 8px; padding: 5px 14px; cursor: pointer;
  font-family: inherit; font-size: 0.78rem; font-weight: 800; color: #868e96;
}
.mode button.on { background: #fff; color: #2d5bff; box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1); }

.stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; margin-bottom: 16px; }
.stat {
  background: #f8f9fa; border: 1px solid #f1f3f5; border-radius: 12px; padding: 10px 12px;
  display: flex; flex-direction: column; gap: 3px; min-width: 0;
}
.stat span { font-size: 0.7rem; font-weight: 800; color: #868e96; }
.stat strong { font-size: 1rem; font-weight: 900; color: #1a1a1a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat strong.empty { color: #ced4da; }
.stat small { font-size: 0.75rem; font-weight: 800; color: #adb5bd; }
.stat em { font-style: normal; font-size: 0.68rem; font-weight: 700; color: #adb5bd; }
.stat em.up { color: #2f9e44; }
.stat em.down { color: #e8590c; }

/* 주 */
.days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  border: none; background: none; padding: 6px 0; border-radius: 12px; cursor: pointer; font-family: inherit;
  transition: background 0.15s;
}
.day:hover:not(:disabled) { background: #f8f9fa; }
.day:disabled { cursor: default; opacity: 0.4; }
.day.selected { background: #f0f4ff; }
.dow { font-size: 0.7rem; font-weight: 800; color: #adb5bd; }
.cell { position: relative; width: 100%; max-width: 52px; aspect-ratio: 1; border-radius: 10px; background: #f1f3f5; }
.time { min-height: 1em; font-size: 0.75rem; font-weight: 800; color: #495057; font-variant-numeric: tabular-nums; }
.lv0 .time { color: #ced4da; }
.date { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }
.day.today .dow, .day.today .date { color: #2d5bff; }
.day.selected .dow, .day.selected .date { color: #2d5bff; font-weight: 900; }
.lv1 .cell, .mday.lv1 { background: #c9d6ff; }
.lv2 .cell, .mday.lv2 { background: #8ea8ff; }
.lv3 .cell, .mday.lv3 { background: #5a7dff; }
.lv4 .cell, .mday.lv4 { background: #2d5bff; }

/* 월 */
.month { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 6px; }
.wd { text-align: center; font-size: 0.7rem; font-weight: 800; color: #adb5bd; padding-bottom: 2px; }
.mday {
  position: relative; aspect-ratio: 1.15; border: none; border-radius: 10px; background: #f1f3f5; cursor: pointer;
  font-family: inherit; display: flex; flex-direction: column; align-items: flex-start; justify-content: space-between;
  padding: 6px 7px; transition: box-shadow 0.15s;
}
.mday:hover:not(:disabled) { box-shadow: 0 0 0 2px #c9d6ff; }
.mday:disabled { cursor: default; opacity: 0.4; }
.mday.selected { box-shadow: 0 0 0 2px #2d5bff; }
.num { font-size: 0.72rem; font-weight: 800; color: #495057; }
.mday.today .num { color: #2d5bff; }
.mtime { align-self: flex-end; font-size: 0.68rem; font-weight: 800; color: #343a40; font-variant-numeric: tabular-nums; }
.mday.lv3 .num, .mday.lv4 .num, .mday.lv3 .mtime, .mday.lv4 .mtime { color: #fff; }

.rec {
  position: absolute; top: 6px; right: 6px; width: 7px; height: 7px; border-radius: 50%;
  background: #e03131; animation: blink 1s infinite;
}
@keyframes blink { 50% { opacity: 0.2; } }

.hint { margin: 12px 0 0; font-size: 0.75rem; color: #adb5bd; }
.error { margin: 12px 0 0; font-size: 0.78rem; font-weight: 700; color: #e03131; }

@media (max-width: 640px) {
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .mtime { display: none; }
}
</style>
