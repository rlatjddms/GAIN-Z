<template>
  <div class="data-card">
    <div class="card-title">
      PRACTICE LOG
      <span class="grass-legend">
        적음
        <span v-for="lv in 5" :key="lv" class="g" :class="`lv${lv - 1}`"></span>
        많음
      </span>
    </div>

    <div class="log-stats">
      <div class="log-stat streak-stat">
        <span>연속 연습</span>
        <strong>{{ streak }}일 🔥</strong>
      </div>
      <div class="log-stat">
        <span>{{ month }}월 연습 시간</span>
        <strong :class="{ empty: !monthSeconds }">{{ monthSeconds ? durationText(monthSeconds) : '—' }}</strong>
      </div>
      <div class="log-stat">
        <span>{{ month }}월 연습한 날</span>
        <strong :class="{ empty: !monthDays }">{{ monthDays ? `${monthDays}일` : '—' }}</strong>
      </div>
    </div>

    <!-- 연습 잔디: 세로 7칸(월~일) × 가로 주, 맨 오른쪽이 이번 주 -->
    <div class="grass" :class="{ loading: !loaded }">
      <span
        v-for="cell in cells"
        :key="cell.key"
        class="g"
        :class="[`lv${cell.level}`, { future: cell.future, today: cell.isToday }]"
        :title="cell.future ? '' : `${dateLabel(cell.key)} · ${cell.seconds ? durationText(cell.seconds) : '연습 안 함'}`"
      ></span>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { usePracticeStore } from '../../stores/practice'
import { apiErrorMessage } from '../../api/practice'
import { addDays, dateKey, dateLabel, durationText, practiceLevel, startOfDay } from '../../constants/practice'

const WEEKS = 30
const practice = usePracticeStore()

const today = startOfDay()
const todayKey = dateKey(today)
const month = today.getMonth() + 1
// 이번 주 월요일에서 (WEEKS - 1)주 전 월요일부터
const thisMonday = addDays(today, -((today.getDay() + 6) % 7))
const firstDay = addDays(thisMonday, -(WEEKS - 1) * 7)

const loaded = ref(false)
const error = ref('')
onMounted(async () => {
  try {
    await practice.loadRange(firstDay, today)
  } catch (e) {
    error.value = `연습 기록을 불러오지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    loaded.value = true
  }
})

const cells = computed(() => Array.from({ length: WEEKS * 7 }, (_, i) => {
  const key = dateKey(addDays(firstDay, i))
  const seconds = practice.totalOf(key)
  return { key, seconds, level: practiceLevel(seconds), future: key > todayKey, isToday: key === todayKey }
}))

// 연속 연습: 오늘부터 거꾸로 연습한 날을 센다. 오늘 아직 안 했으면 어제부터 (오늘 하면 이어짐)
const streak = computed(() => {
  let d = practice.totalOf(todayKey) > 0 ? today : addDays(today, -1)
  let count = 0
  while (d >= firstDay && practice.totalOf(dateKey(d)) > 0) {
    count++
    d = addDays(d, -1)
  }
  return count
})

const monthKeys = computed(() => cells.value.filter((c) => !c.future && Number(c.key.slice(5, 7)) === month && Number(c.key.slice(0, 4)) === today.getFullYear()))
const monthSeconds = computed(() => monthKeys.value.reduce((s, c) => s + c.seconds, 0))
const monthDays = computed(() => monthKeys.value.filter((c) => c.seconds > 0).length)
</script>

<style scoped>
.data-card { background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 24px; }
.card-title { font-weight: 800; font-size: 0.85rem; margin-bottom: 18px; display: flex; justify-content: space-between; color: #495057; }

.log-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 14px; }
.log-stat {
  background: #f8f9fa; border: 1px solid #f1f3f5; border-radius: 12px; padding: 10px 12px;
  display: flex; flex-direction: column; gap: 4px; min-width: 0;
}
.log-stat span { font-size: 0.7rem; font-weight: 700; color: #868e96; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.log-stat strong { font-size: 1rem; font-weight: 900; color: #1a1a1a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.log-stat strong.empty { color: #ced4da; }
.streak-stat { background: #fff4e6; border-color: #ffe8cc; }
.streak-stat strong { color: #ff922b; }

.grass {
  display: grid; grid-auto-flow: column;
  grid-template-rows: repeat(7, auto); grid-template-columns: repeat(30, minmax(0, 1fr)); gap: 3px;
  transition: opacity 0.2s;
}
.grass.loading { opacity: 0.4; }
.g { display: block; aspect-ratio: 1; border-radius: 3px; background: #f1f3f5; }
.g.future { visibility: hidden; }
.g.today { box-shadow: inset 0 0 0 1.5px #1a1a1a; }
.grass-legend { display: flex; align-items: center; gap: 4px; font-size: 0.7rem; font-weight: 700; color: #adb5bd; }
.grass-legend .g { width: 10px; }
.g.lv1 { background: #c9d6ff; }
.g.lv2 { background: #8ea8ff; }
.g.lv3 { background: #5a7dff; }
.g.lv4 { background: #2d5bff; }

.error { margin: 10px 0 0; font-size: 0.75rem; font-weight: 700; color: #e03131; }
</style>
