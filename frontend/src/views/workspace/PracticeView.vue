<template>
  <SubPageLayout>
  <div class="page">
    <div class="page-head">
      <div ref="pickerRef" class="date-picker">
        <button class="arrow" aria-label="전날" @click="move(-1)">‹</button>
        <button class="date-title" :aria-expanded="calendarOpen" @click="calendarOpen = !calendarOpen">
          {{ fullDateLabel(selected) }}<span class="caret">▾</span>
        </button>
        <button class="arrow" :disabled="isToday" aria-label="다음 날" @click="move(1)">›</button>
        <PracticeCalendar v-model="selected" :open="calendarOpen" @close="calendarOpen = false" />
      </div>
      <button v-if="!isToday" class="today-btn" @click="selected = todayKey">오늘로</button>
    </div>

    <p v-if="loadError" class="load-error">{{ loadError }}</p>

    <div class="today-grid">
      <PracticeTimerCard :date="selected" @go-today="selected = todayKey" />
      <RoutineCard :date="selected" />
    </div>

    <WeekPracticeCard :date="selected" @select="selected = $event" />
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import PracticeTimerCard from '../../components/workspace/PracticeTimerCard.vue'
import WeekPracticeCard from '../../components/workspace/WeekPracticeCard.vue'
import RoutineCard from '../../components/workspace/RoutineCard.vue'
import PracticeCalendar from '../../components/workspace/PracticeCalendar.vue'
import { usePracticeStore } from '../../stores/practice'
import { apiErrorMessage } from '../../api/practice'
import { addDays, dateKey, fullDateLabel, parseKey, weekStart } from '../../constants/practice'

const route = useRoute()
const router = useRouter()
const practice = usePracticeStore()

// 보고 있는 날짜. 주소(?date=)에 담아 새로고침해도 유지한다 (오늘이면 생략)
const todayKey = dateKey()
const validDate = (v) => typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) && v <= todayKey
const selected = ref(validDate(route.query.date) ? route.query.date : todayKey)
const isToday = computed(() => selected.value === todayKey)

const move = (diff) => {
  const next = dateKey(addDays(parseKey(selected.value), diff))
  if (next <= todayKey) selected.value = next
}

// 고른 날짜가 들어 있는 주(오늘까지)를 불러온다
const loadError = ref('')
const loadWeek = async (key) => {
  const start = weekStart(parseKey(key))
  const end = addDays(start, 6)
  try {
    await practice.loadRange(start, dateKey(end) > todayKey ? new Date() : end)
    loadError.value = ''
  } catch (e) {
    loadError.value = `연습 기록을 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
}

watch(selected, (key) => {
  router.replace({ query: key === todayKey ? {} : { date: key } })
  loadWeek(key)
})

// 달력 바깥을 누르면 닫기
const calendarOpen = ref(false)
const pickerRef = ref(null)
const onDocClick = (e) => {
  if (calendarOpen.value && pickerRef.value && !pickerRef.value.contains(e.target)) calendarOpen.value = false
}
onMounted(() => {
  document.addEventListener('mousedown', onDocClick)
  // 타이머가 쓰는 오늘 합계는 항상 필요하다
  loadWeek(todayKey)
  if (!isToday.value) loadWeek(selected.value)
})
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; flex-wrap: wrap; }

.date-picker { position: relative; display: flex; align-items: center; gap: 4px; }
.arrow {
  width: 32px; height: 32px; border: none; border-radius: 10px; background: #fff; color: #495057;
  font-size: 1.1rem; font-weight: 900; cursor: pointer; box-shadow: inset 0 0 0 1px #e9ecef;
}
.arrow:hover:not(:disabled) { color: #2d5bff; box-shadow: inset 0 0 0 1px #c9d6ff; }
.arrow:disabled { opacity: 0.35; cursor: default; }
.date-title {
  display: flex; align-items: center; gap: 6px; border: none; background: none; cursor: pointer;
  padding: 4px 8px; border-radius: 10px; font-family: inherit; font-size: 1.3rem; font-weight: 900; color: #1a1a1a;
}
.date-title:hover { background: #f1f3f5; }
.caret { font-size: 0.7rem; color: #adb5bd; }
.today-btn {
  border: 1px solid #c9d6ff; background: #f0f4ff; color: #2d5bff; cursor: pointer; font-family: inherit;
  font-size: 0.75rem; font-weight: 800; padding: 6px 12px; border-radius: 9px;
}

.today-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 16px; }

.load-error { margin: 0; padding: 10px 14px; border-radius: 12px; background: #fff5f5; color: #e03131; font-size: 0.8rem; font-weight: 700; }

@media (max-width: 760px) {
  .today-grid { grid-template-columns: minmax(0, 1fr); }
  .date-title { font-size: 1.1rem; }
}
</style>
