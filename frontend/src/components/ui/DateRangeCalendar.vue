<template>
  <!-- 날짜·기간 고르기 달력 (#188). 한 번 누르면 그 하루, 다른 날을 한 번 더 누르면 그 사이 기간 -->
  <div class="range-calendar" role="dialog" aria-label="날짜 고르기" @keydown.esc="emit('close')">
    <div class="cal-head">
      <button type="button" class="nav" aria-label="이전 달" @click="moveMonth(-1)">‹</button>
      <strong>{{ view.getFullYear() }}년 {{ view.getMonth() + 1 }}월</strong>
      <button type="button" class="nav" :disabled="isCurrentMonth" aria-label="다음 달" @click="moveMonth(1)">›</button>
    </div>

    <div class="dows"><span v-for="d in DOWS" :key="d">{{ d }}</span></div>
    <div class="days">
      <button
        v-for="cell in cells"
        :key="cell.key"
        type="button"
        class="day"
        :class="{
          other: !cell.inMonth,
          today: cell.key === todayKey,
          edge: cell.key === from || cell.key === to,
          inside: from && to && cell.key > from && cell.key < to,
        }"
        :disabled="cell.key > todayKey"
        :aria-pressed="cell.key === from || cell.key === to"
        :aria-label="dateLabel(cell.key)"
        @click="pick(cell.key)"
      >{{ cell.date }}</button>
    </div>

    <p class="hint">{{ hint }}</p>

    <div class="cal-foot">
      <button type="button" class="chip" @click="preset(0)">오늘</button>
      <button type="button" class="chip" @click="preset(6)">최근 7일</button>
      <button type="button" class="chip" @click="preset(29)">최근 30일</button>
      <button v-if="from" type="button" class="link" @click="emit('change', { from: null, to: null })">날짜 지우기</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { addDays, dateKey, dateLabel, parseKey, weekStart } from '../../constants/practice'

const props = defineProps({
  from: { type: String, default: null },   // 'YYYY-MM-DD'
  to: { type: String, default: null },
})
// change: { from, to } (하루면 둘이 같다)
const emit = defineEmits(['change', 'close'])

const DOWS = ['월', '화', '수', '목', '금', '토', '일']
const todayKey = dateKey()

const startMonth = (key) => { const d = key ? parseKey(key) : new Date(); return new Date(d.getFullYear(), d.getMonth(), 1) }
const view = ref(startMonth(props.to || props.from))
const isCurrentMonth = computed(() => view.value.getFullYear() === new Date().getFullYear() && view.value.getMonth() === new Date().getMonth())
const moveMonth = (diff) => { view.value = new Date(view.value.getFullYear(), view.value.getMonth() + diff, 1) }

// 월요일부터 6주
const cells = computed(() => {
  const start = weekStart(view.value)
  return Array.from({ length: 42 }, (_, i) => {
    const d = addDays(start, i)
    return { key: dateKey(d), date: d.getDate(), inMonth: d.getMonth() === view.value.getMonth() }
  })
})

// 하루만 골라 둔 상태에서 다른 날을 누르면 기간, 아니면 새로 하루
const pick = (key) => {
  if (props.from && props.from === props.to && key !== props.from) {
    emit('change', key < props.from ? { from: key, to: props.from } : { from: props.from, to: key })
  } else {
    emit('change', { from: key, to: key })
  }
}
const preset = (days) => emit('change', { from: dateKey(addDays(new Date(), -days)), to: todayKey })

const hint = computed(() => (props.from && props.from === props.to ? '다른 날을 누르면 그 사이 기간으로 찾아요' : '날짜를 눌러 고르세요'))
</script>

<style scoped>
.range-calendar {
  width: 296px; box-sizing: border-box;
  background: #fff; border: 1px solid var(--c-line); border-radius: 18px; padding: 14px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}
.cal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.cal-head strong { font-size: 0.9rem; font-weight: 900; }
.nav {
  width: 28px; height: 28px; border: none; border-radius: 8px; background: var(--c-line-soft); color: var(--c-text-2);
  font-size: 1rem; font-weight: 900; cursor: pointer;
}
.nav:disabled { opacity: 0.35; cursor: default; }
.dows, .days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.dows span { text-align: center; font-size: 0.7rem; font-weight: 800; color: var(--c-faint); padding-bottom: 4px; }
.day {
  aspect-ratio: 1; border: none; border-radius: 9px; background: var(--c-bg); cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); font-variant-numeric: tabular-nums;
}
.day:hover:not(:disabled) { outline: 2px solid var(--c-primary-line); }
.day.other { opacity: 0.4; }
.day.today { color: var(--c-primary-text); }
.day.inside { background: var(--c-primary-soft); color: var(--c-primary-text); }
.day.edge { background: var(--c-primary); color: var(--c-on-primary); opacity: 1; }
.day:disabled { background: transparent; color: #dee2e6; cursor: default; }
.hint { margin: 10px 2px 0; font-size: 0.72rem; color: var(--c-faint); }
.cal-foot { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.chip {
  padding: 5px 10px; border: 1px solid var(--c-line); border-radius: 999px; background: #fff;
  font: inherit; font-size: 0.72rem; font-weight: 700; color: var(--c-text-2); cursor: pointer;
}
.chip:hover { border-color: var(--c-primary-line); color: var(--c-primary-text); }
.link { margin-left: auto; border: none; background: none; padding: 0; font: inherit; font-size: 0.72rem; font-weight: 800; color: var(--c-muted); cursor: pointer; }
</style>
