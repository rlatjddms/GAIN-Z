<template>
  <div class="page">
    <div class="page-head">
      <h2>피크 컬렉션</h2>
      <p>업적을 달성하면 피크를 얻어요</p>
    </div>

    <!-- 피크 도감: #47(지급 API)·#48에서 연결 -->
    <WsCard title="도감" soon>
      <template #actions>
        <div class="rarity-legend">
          <span v-for="r in RARITIES" :key="r.key" class="r" :style="{ '--c': r.color }">{{ r.label }}</span>
        </div>
      </template>
      <div class="pick-grid">
        <div v-for="(r, i) in SLOTS" :key="i" class="slot" :style="{ '--c': r.color }">
          <span class="lock">🔒</span>
        </div>
      </div>
    </WsCard>
  </div>
</template>

<script setup>
import WsCard from '../../components/workspace/WsCard.vue'

// 백엔드 PickRarity와 같은 순서
const RARITIES = [
  { key: 'COMMON', label: '일반', color: '#ced4da' },
  { key: 'RARE', label: '레어', color: '#5a7dff' },
  { key: 'LEGENDARY', label: '전설', color: '#f59e0b' },
]
// 실제 피크 목록이 연결되기 전까지 보여 줄 빈 칸 (일반 6 · 레어 4 · 전설 2)
const SLOTS = [...Array(6).fill(RARITIES[0]), ...Array(4).fill(RARITIES[1]), ...Array(2).fill(RARITIES[2])]
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.rarity-legend { display: flex; gap: 10px; }
.r { display: flex; align-items: center; gap: 5px; font-size: 0.7rem; font-weight: 800; color: #868e96; }
.r::before { content: ''; width: 8px; height: 8px; border-radius: 2px; background: var(--c); }

.pick-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; }
.slot {
  aspect-ratio: 1; border-radius: 14px; background: #f8f9fa;
  border: 2px solid color-mix(in srgb, var(--c) 45%, #fff);
  display: flex; align-items: center; justify-content: center;
}
.lock { font-size: 1.1rem; opacity: 0.35; }

@media (max-width: 760px) {
  .pick-grid { grid-template-columns: repeat(4, 1fr); }
}
</style>
