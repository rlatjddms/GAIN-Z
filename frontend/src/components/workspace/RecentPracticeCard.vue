<template>
  <WsCard title="최근 7일">
    <template #actions>
      <span class="week-total">합계 {{ durationText(weekTotal) }}</span>
    </template>

    <div class="days">
      <button
        v-for="d in days"
        :key="d.key"
        class="day"
        :class="[`lv${d.level}`, { today: d.isToday }]"
        :title="`${dateLabel(d.key)} · ${durationText(d.seconds)} (눌러서 고치기)`"
        @click="editing = d.key"
      >
        <span class="dow">{{ d.dow }}</span>
        <span class="cell">
          <i v-if="d.isToday && timer.running" class="rec"></i>
        </span>
        <span class="time">{{ shortDuration(d.seconds) }}</span>
        <span class="date">{{ d.date }}</span>
      </button>
    </div>
    <p class="hint">날짜를 누르면 그날 연습 시간을 고칠 수 있어요. (최근 7일까지)</p>

    <EditDayModal :date="editing" @close="editing = null" @saved="onSaved" />
    <LevelUpModal :result="levelUp" @close="levelUp = null" />
  </WsCard>
</template>

<script setup>
import { ref, computed } from 'vue'
import WsCard from './WsCard.vue'
import EditDayModal from './EditDayModal.vue'
import LevelUpModal from './LevelUpModal.vue'
import { usePracticeStore } from '../../stores/practice'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { addDays, dateKey, dateLabel, dowLabel, durationText, shortDuration, practiceLevel, EDITABLE_DAYS } from '../../constants/practice'

const practice = usePracticeStore()
const timer = usePracticeTimerStore()

const days = computed(() => {
  const today = new Date()
  return Array.from({ length: EDITABLE_DAYS }, (_, i) => {
    const date = addDays(today, i - (EDITABLE_DAYS - 1))
    const key = dateKey(date)
    const seconds = practice.totalOf(key)
    return { key, dow: dowLabel(date), date: date.getDate(), seconds, level: practiceLevel(seconds), isToday: i === EDITABLE_DAYS - 1 }
  })
})
const weekTotal = computed(() => days.value.reduce((s, d) => s + d.seconds, 0))

const editing = ref(null)
const levelUp = ref(null)
const onSaved = (result) => {
  const exp = result?.experience
  if (exp && ((exp.isLeveledUp ?? exp.leveledUp) || exp.unlockedOutfits?.length)) {
    levelUp.value = { ...exp, expGained: result.expGained }
  }
}
</script>

<style scoped>
.week-total { font-size: 0.75rem; font-weight: 800; color: #2d5bff; }

.days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  border: none; background: none; padding: 6px 0; border-radius: 12px; cursor: pointer; font-family: inherit;
  transition: background 0.15s;
}
.day:hover { background: #f8f9fa; }
.dow { font-size: 0.7rem; font-weight: 800; color: #adb5bd; }
.cell {
  position: relative; width: 100%; max-width: 52px; aspect-ratio: 1; border-radius: 10px; background: #f1f3f5;
}
.lv1 .cell { background: #c9d6ff; }
.lv2 .cell { background: #8ea8ff; }
.lv3 .cell { background: #5a7dff; }
.lv4 .cell { background: #2d5bff; }
.time { font-size: 0.75rem; font-weight: 800; color: #495057; font-variant-numeric: tabular-nums; }
.lv0 .time { color: #ced4da; }
.date { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }
.day.today .dow, .day.today .date { color: #2d5bff; }
.rec {
  position: absolute; top: 6px; right: 6px; width: 7px; height: 7px; border-radius: 50%;
  background: #e03131; animation: blink 1s infinite;
}
@keyframes blink { 50% { opacity: 0.2; } }

.hint { margin: 12px 0 0; font-size: 0.75rem; color: #adb5bd; }
</style>
