<template>
  <WsCard :title="title">
    <template #actions>
      <span class="week-total">합계 {{ durationText(weekTotal) }}</span>
    </template>

    <div class="days">
      <button
        v-for="d in days"
        :key="d.key"
        class="day"
        :class="[`lv${d.level}`, { selected: d.key === date, today: d.key === todayKey }]"
        :disabled="d.future"
        :title="d.future ? '' : `${dateLabel(d.key)} · ${durationText(d.seconds)}`"
        @click="emit('select', d.key)"
      >
        <span class="dow">{{ d.dow }}</span>
        <span class="cell">
          <i v-if="d.key === todayKey && timer.running" class="rec"></i>
        </span>
        <span class="time">{{ d.future ? '' : shortDuration(d.seconds) }}</span>
        <span class="date">{{ d.date }}</span>
      </button>
    </div>
    <p class="hint">날짜를 누르면 그날 연습 시간과 루틴을 볼 수 있어요.</p>
  </WsCard>
</template>

<script setup>
// 고른 날짜가 들어 있는 주(월~일)의 연습량. 날짜를 누르면 그 날짜로 이동
import { computed } from 'vue'
import WsCard from './WsCard.vue'
import { usePracticeStore } from '../../stores/practice'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { addDays, dateKey, dateLabel, dowLabel, durationText, parseKey, practiceLevel, shortDuration, weekStart } from '../../constants/practice'

const props = defineProps({ date: { type: String, required: true } })
const emit = defineEmits(['select'])
const practice = usePracticeStore()
const timer = usePracticeTimerStore()
const todayKey = dateKey()

const monday = computed(() => weekStart(parseKey(props.date)))
const days = computed(() => Array.from({ length: 7 }, (_, i) => {
  const d = addDays(monday.value, i)
  const key = dateKey(d)
  const seconds = practice.totalOf(key)
  return { key, dow: dowLabel(d), date: d.getDate(), seconds, level: practiceLevel(seconds), future: key > todayKey }
}))
const weekTotal = computed(() => days.value.reduce((s, d) => s + d.seconds, 0))

const title = computed(() => {
  if (days.value.some((d) => d.key === todayKey)) return '이번 주'
  const first = monday.value, last = addDays(first, 6)
  return first.getMonth() === last.getMonth()
    ? `${first.getMonth() + 1}월 ${first.getDate()}일 ~ ${last.getDate()}일`
    : `${first.getMonth() + 1}월 ${first.getDate()}일 ~ ${last.getMonth() + 1}월 ${last.getDate()}일`
})
</script>

<style scoped>
.week-total { font-size: 0.75rem; font-weight: 800; color: #2d5bff; }

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
.cell {
  position: relative; width: 100%; max-width: 52px; aspect-ratio: 1; border-radius: 10px; background: #f1f3f5;
}
.lv1 .cell { background: #c9d6ff; }
.lv2 .cell { background: #8ea8ff; }
.lv3 .cell { background: #5a7dff; }
.lv4 .cell { background: #2d5bff; }
.time { min-height: 1em; font-size: 0.75rem; font-weight: 800; color: #495057; font-variant-numeric: tabular-nums; }
.lv0 .time { color: #ced4da; }
.date { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }
.day.today .dow, .day.today .date { color: #2d5bff; }
.day.selected .dow, .day.selected .date { color: #2d5bff; font-weight: 900; }
.rec {
  position: absolute; top: 6px; right: 6px; width: 7px; height: 7px; border-radius: 50%;
  background: #e03131; animation: blink 1s infinite;
}
@keyframes blink { 50% { opacity: 0.2; } }

.hint { margin: 12px 0 0; font-size: 0.75rem; color: #adb5bd; }
</style>
