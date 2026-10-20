<template>
  <SubPageLayout>
  <div class="page">
    <div class="page-head">
      <h2>연습 기록장</h2>
      <div class="head-actions">
      <div ref="pickerRef" class="date-picker">
        <button class="arrow" aria-label="전날" @click="move(-1)">‹</button>
        <button class="date-title" :aria-expanded="calendarOpen" @click="calendarOpen = !calendarOpen">
          {{ fullDateLabel(selected) }}
        </button>
        <button class="arrow" :disabled="isToday" aria-label="다음 날" @click="move(1)">›</button>
        <PracticeCalendar v-model="selected" :open="calendarOpen" @close="calendarOpen = false" />
      </div>
      <button v-if="!isToday" class="today-btn" @click="selected = todayKey">오늘로</button>
      </div>
    </div>

    <p v-if="loadError" class="load-error">{{ loadError }}</p>

    <div class="today-grid">
      <PracticeTimerCard :date="selected" @go-today="selected = todayKey" />
      <RoutineCard :date="selected" />
    </div>

    <PracticeRecordCard v-model:mode="viewMode" :date="selected" @select="selected = $event" />
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import PracticeTimerCard from '../../components/workspace/PracticeTimerCard.vue'
import PracticeRecordCard from '../../components/workspace/PracticeRecordCard.vue'
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
// 기록 보기: 주(WEEK) / 월(MONTH). 주소(?view=month)에 담아 새로고침해도 유지
const viewMode = ref(route.query.view === 'month' ? 'MONTH' : 'WEEK')

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

const syncQuery = () => {
  const query = {}
  if (selected.value !== todayKey) query.date = selected.value
  if (viewMode.value === 'MONTH') query.view = 'month'
  router.replace({ query })
}

// 기록 카드는 보고 있는 주·달을 스스로 불러온다. 여기서는 타이머가 쓰는 오늘과 고른 날짜의 주를 챙긴다
watch(selected, (key) => {
  syncQuery()
  loadWeek(key)
})
watch(viewMode, syncQuery)

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
/* 제목 아래에 날짜 고르기 */
.page-head { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.head-actions { display: flex; align-items: center; gap: 10px; margin-left: -4px; }

.date-picker { position: relative; display: flex; align-items: center; gap: 4px; }
.arrow {
  width: 32px; height: 32px; border: none; border-radius: 10px; background: #fff; color: #495057;
  font-size: 1.1rem; font-weight: 900; cursor: pointer; box-shadow: inset 0 0 0 1px #e9ecef;
}
.arrow:hover:not(:disabled) { color: var(--c-primary-text); box-shadow: inset 0 0 0 1px var(--c-primary-line); }
.arrow:disabled { opacity: 0.35; cursor: default; }
.date-title {
  display: flex; align-items: center; gap: 6px; border: none; background: none; cursor: pointer;
  padding: 4px 8px; border-radius: 10px; font-family: inherit; font-size: 1.1rem; font-weight: 900; color: #1a1a1a;
}
.date-title:hover { background: #f1f3f5; }
.today-btn {
  border: 1px solid var(--c-primary-line); background: var(--c-primary-soft); color: var(--c-primary-text); cursor: pointer; font-family: inherit;
  font-size: 0.75rem; font-weight: 800; padding: 6px 12px; border-radius: 9px;
}

.today-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 16px; }

.load-error { margin: 0; padding: 10px 14px; border-radius: 12px; background: #fff5f5; color: #e03131; font-size: 0.8rem; font-weight: 700; }

@media (max-width: 760px) {
  .today-grid { grid-template-columns: minmax(0, 1fr); }
  .date-title { font-size: 1rem; }
}
</style>
