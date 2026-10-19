<template>
  <div v-if="open" class="calendar" role="dialog" aria-label="날짜 고르기" @keydown.esc="emit('close')">
    <div class="cal-head">
      <button class="nav" aria-label="이전 달" @click="moveMonth(-1)">‹</button>
      <strong>{{ view.getFullYear() }}년 {{ view.getMonth() + 1 }}월</strong>
      <button class="nav" :disabled="isCurrentMonth" aria-label="다음 달" @click="moveMonth(1)">›</button>
    </div>

    <div class="dows">
      <span v-for="d in DOWS" :key="d">{{ d }}</span>
    </div>
    <div class="days" :class="{ loading }">
      <button
        v-for="cell in cells"
        :key="cell.key"
        class="day"
        :class="[`lv${cell.level}`, { other: !cell.inMonth, selected: cell.key === modelValue, today: cell.key === todayKey }]"
        :disabled="cell.future"
        :title="cell.future ? '' : `${dateLabel(cell.key)} · ${cell.seconds ? durationText(cell.seconds) : '연습 안 함'}`"
        @click="select(cell.key)"
      >{{ cell.date }}</button>
    </div>

    <div class="cal-foot">
      <button class="link" @click="select(todayKey)">오늘</button>
    </div>
  </div>
</template>

<script setup>
// 연습 날짜 고르기 달력 (날짜별 연습량 색, 미래 날짜는 고를 수 없음)
import { ref, computed, watch } from 'vue'
import { usePracticeStore } from '../../stores/practice'
import { addDays, dateKey, dateLabel, durationText, parseKey, practiceLevel, weekStart } from '../../constants/practice'

const props = defineProps({
  open: { type: Boolean, default: false },
  modelValue: { type: String, required: true },   // 'YYYY-MM-DD'
})
const emit = defineEmits(['update:modelValue', 'close'])

const DOWS = ['월', '화', '수', '목', '금', '토', '일']
const practice = usePracticeStore()
const todayKey = dateKey()

// 보고 있는 달 (1일)
const view = ref(new Date())
const loading = ref(false)
const isCurrentMonth = computed(() => view.value.getFullYear() === new Date().getFullYear() && view.value.getMonth() === new Date().getMonth())

const loadMonth = async () => {
  const first = new Date(view.value.getFullYear(), view.value.getMonth(), 1)
  const last = new Date(view.value.getFullYear(), view.value.getMonth() + 1, 0)
  const to = dateKey(last) > todayKey ? new Date() : last
  loading.value = true
  try {
    await practice.loadRange(first, to)
  } catch {
    // 색만 못 칠할 뿐 날짜 고르기는 된다
  } finally {
    loading.value = false
  }
}

// 열 때마다 고른 날짜의 달로 맞춘다
watch(() => props.open, (v) => {
  if (!v) return
  const d = parseKey(props.modelValue)
  view.value = new Date(d.getFullYear(), d.getMonth(), 1)
  loadMonth()
})

const moveMonth = (diff) => {
  view.value = new Date(view.value.getFullYear(), view.value.getMonth() + diff, 1)
  loadMonth()
}

// 월요일부터 6주
const cells = computed(() => {
  const start = weekStart(view.value)
  return Array.from({ length: 42 }, (_, i) => {
    const d = addDays(start, i)
    const key = dateKey(d)
    const seconds = practice.totalOf(key)
    return {
      key, date: d.getDate(), seconds, level: practiceLevel(seconds),
      inMonth: d.getMonth() === view.value.getMonth(), future: key > todayKey,
    }
  })
})

const select = (key) => {
  emit('update:modelValue', key)
  emit('close')
}
</script>

<style scoped>
.calendar {
  position: absolute; top: calc(100% + 8px); right: 0; z-index: 50; width: 296px; box-sizing: border-box;
  background: #fff; border: 1px solid #e9ecef; border-radius: 18px; padding: 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}
.cal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.cal-head strong { font-size: 0.9rem; font-weight: 900; }
.nav {
  width: 28px; height: 28px; border: none; border-radius: 8px; background: #f1f3f5; color: #495057;
  font-size: 1rem; font-weight: 900; cursor: pointer;
}
.nav:disabled { opacity: 0.35; cursor: default; }

.dows, .days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.dows span { text-align: center; font-size: 0.7rem; font-weight: 800; color: #adb5bd; padding-bottom: 4px; }
.days { transition: opacity 0.2s; }
.days.loading { opacity: 0.5; }
.day {
  aspect-ratio: 1; border: none; border-radius: 9px; background: #f8f9fa; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #495057; font-variant-numeric: tabular-nums;
}
.day:hover:not(:disabled) { outline: 2px solid #c9d6ff; }
.day.lv1 { background: #c9d6ff; }
.day.lv2 { background: #8ea8ff; color: #fff; }
.day.lv3 { background: #5a7dff; color: #fff; }
.day.lv4 { background: #2d5bff; color: #fff; }
.day.other { opacity: 0.4; }
.day.today { color: #2d5bff; }
.day.today.lv2, .day.today.lv3, .day.today.lv4 { color: #fff; text-decoration: underline; }
.day.selected { outline: 2px solid #2d5bff; outline-offset: 1px; }
.day:disabled { background: transparent; color: #dee2e6; cursor: default; }

.cal-foot { display: flex; justify-content: flex-end; margin-top: 10px; }
.link { border: none; background: none; padding: 0; font: inherit; font-size: 0.75rem; font-weight: 800; color: #2d5bff; cursor: pointer; }
/* 날짜 고르기는 넓은 화면에선 오른쪽 끝, 좁은 화면에선 제목 아래 왼쪽에 있다 */
@media (max-width: 640px) {
  .calendar { right: auto; left: 0; }
}
</style>
