<template>
  <SubPageLayout>
  <div class="page">
    <div class="page-head">
      <h2>오늘의 연습</h2>
      <p>{{ todayLabel }}</p>
    </div>

    <p v-if="loadError" class="load-error">{{ loadError }}</p>

    <div class="today-grid">
      <PracticeTimerCard />

      <RoutineCard />
    </div>

    <RecentPracticeCard />
  </div>
  </SubPageLayout>
</template>

<script setup>
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import PracticeTimerCard from '../../components/workspace/PracticeTimerCard.vue'
import RecentPracticeCard from '../../components/workspace/RecentPracticeCard.vue'
import RoutineCard from '../../components/workspace/RoutineCard.vue'
import { ref, onMounted } from 'vue'
import { usePracticeStore } from '../../stores/practice'
import { apiErrorMessage } from '../../api/practice'
import { addDays, EDITABLE_DAYS } from '../../constants/practice'

const DOW = ['일', '월', '화', '수', '목', '금', '토']
const now = new Date()
const todayLabel = `${now.getMonth() + 1}월 ${now.getDate()}일 ${DOW[now.getDay()]}요일`

// 오늘 합계와 최근 7일 기록 불러오기
const practice = usePracticeStore()
const loadError = ref('')
onMounted(async () => {
  try {
    await practice.loadRange(addDays(now, -(EDITABLE_DAYS - 1)), now)
  } catch (e) {
    loadError.value = `연습 기록을 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.today-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 16px; }

.load-error { margin: 0; padding: 10px 14px; border-radius: 12px; background: #fff5f5; color: #e03131; font-size: 0.8rem; font-weight: 700; }

@media (max-width: 760px) {
  .today-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
