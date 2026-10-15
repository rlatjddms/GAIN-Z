<template>
  <div class="page">
    <div class="page-head">
      <h2>연습 기록</h2>
      <p>최근 1년</p>
    </div>

    <!-- 요약 수치: #44에서 연결 -->
    <div class="stats">
      <div v-for="s in STATS" :key="s.label" class="stat">
        <span class="stat-label">{{ s.label }}</span>
        <strong>—</strong>
      </div>
    </div>

    <!-- 연습 잔디: #43(조회 API)·#44에서 연결 -->
    <WsCard title="연습 잔디" soon>
      <div class="grass-scroll">
        <div class="grass">
          <span v-for="n in WEEKS * 7" :key="n" class="g"></span>
        </div>
      </div>
      <div class="legend">
        적음
        <span v-for="lv in 5" :key="lv" class="g" :class="`lv${lv - 1}`"></span>
        많음
      </div>
    </WsCard>
  </div>
</template>

<script setup>
import WsCard from '../../components/workspace/WsCard.vue'

const WEEKS = 53
const STATS = [
  { label: '연속 연습' },
  { label: '총 연습 시간' },
  { label: '이번 달 연습한 날' },
]
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.stat {
  background: #fff; border: 1px solid #e9ecef; border-radius: 16px; padding: 16px 18px;
  display: flex; flex-direction: column; gap: 6px;
}
.stat-label { font-size: 0.75rem; font-weight: 700; color: #868e96; }
.stat strong { font-size: 1.4rem; font-weight: 900; color: #ced4da; }

/* 잔디: 세로 7칸(요일) × 가로 53주, 왼쪽이 오래된 주 */
.grass-scroll { overflow: hidden; }
.grass {
  display: grid; grid-auto-flow: column;
  grid-template-rows: repeat(7, auto); grid-template-columns: repeat(53, minmax(0, 1fr)); gap: 3px;
}
.g { display: block; aspect-ratio: 1; border-radius: 3px; background: #f1f3f5; }
.legend {
  display: flex; align-items: center; justify-content: flex-end; gap: 4px;
  margin-top: 12px; font-size: 0.7rem; font-weight: 700; color: #adb5bd;
}
.legend .g { width: 11px; }
.g.lv1 { background: #c9d6ff; }
.g.lv2 { background: #8ea8ff; }
.g.lv3 { background: #5a7dff; }
.g.lv4 { background: #2d5bff; }

@media (max-width: 760px) {
  .stats { grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
  .stat { padding: 12px; }
  .stat strong { font-size: 1.1rem; }
  /* 좁은 화면에서는 칸이 너무 작아지지 않게 가로 스크롤 */
  .grass-scroll { overflow-x: auto; padding-bottom: 4px; }
  .grass { min-width: 600px; }
}
</style>
