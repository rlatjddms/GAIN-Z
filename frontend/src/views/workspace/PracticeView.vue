<template>
  <SubPageLayout>
  <div class="page">
    <div class="page-head">
      <h2>오늘의 연습</h2>
      <p>{{ todayLabel }}</p>
    </div>

    <div class="today-grid">
      <PracticeTimerCard />

      <!-- 오늘의 루틴: #46에서 연결 -->
      <WsCard title="오늘의 루틴" soon>
        <template #actions>
          <button class="text-btn" disabled>+ 추가</button>
        </template>
        <ul class="routine-list">
          <li v-for="n in 3" :key="n">
            <span class="check"></span>
            <span class="bar" :style="{ width: `${[70, 52, 60][n - 1]}%` }"></span>
            <span class="count">0 / {{ [10, 3, 5][n - 1] }}</span>
          </li>
        </ul>
        <p class="hint">스케일 10회처럼 매일 할 연습을 정해 두고 체크해요.</p>
      </WsCard>
    </div>

    <!-- 이번 주 연습량: #43·#44에서 연결 -->
    <WsCard title="이번 주" soon>
      <div class="week">
        <div v-for="d in week" :key="d.key" class="day" :class="{ today: d.isToday }">
          <span class="dow">{{ d.dow }}</span>
          <span class="cell"></span>
          <span class="date">{{ d.date }}</span>
        </div>
      </div>
    </WsCard>
  </div>
  </SubPageLayout>
</template>

<script setup>
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import PracticeTimerCard from '../../components/workspace/PracticeTimerCard.vue'

const DOW = ['일', '월', '화', '수', '목', '금', '토']
const now = new Date()
const todayLabel = `${now.getMonth() + 1}월 ${now.getDate()}일 ${DOW[now.getDay()]}요일`

// 월요일부터 일요일까지
const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - ((now.getDay() + 6) % 7))
const week = Array.from({ length: 7 }, (_, i) => {
  const d = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + i)
  return { key: i, dow: DOW[d.getDay()], date: d.getDate(), isToday: d.toDateString() === now.toDateString() }
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.today-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr); gap: 16px; }

.hint { margin: 14px 0 0; font-size: 0.75rem; color: #adb5bd; line-height: 1.5; }

.text-btn {
  border: none; background: none; padding: 0; font: inherit; font-size: 0.75rem; font-weight: 800;
  color: #2d5bff; text-decoration: none; cursor: pointer;
}
.text-btn:disabled { color: #ced4da; cursor: not-allowed; }

.routine-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.routine-list li {
  display: flex; align-items: center; gap: 10px;
  background: #f8f9fa; border-radius: 12px; padding: 12px 14px;
}
.check { flex: none; width: 18px; height: 18px; border: 2px solid #dee2e6; border-radius: 6px; }
.bar { height: 8px; background: #e9ecef; border-radius: 4px; }
.count { margin-left: auto; font-size: 0.75rem; font-weight: 800; color: #ced4da; font-variant-numeric: tabular-nums; }

.week { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.dow { font-size: 0.7rem; font-weight: 800; color: #adb5bd; }
.cell { width: 100%; max-width: 56px; aspect-ratio: 1; background: #f1f3f5; border-radius: 10px; }
.date { font-size: 0.75rem; font-weight: 700; color: #868e96; }
.day.today .dow, .day.today .date { color: #2d5bff; }
.day.today .cell { box-shadow: inset 0 0 0 2px #2d5bff; }

@media (max-width: 760px) {
  .today-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
