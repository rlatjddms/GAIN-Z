<template>
  <div class="sub-app">
    <AppHeader>
      <div class="page-pill">{{ pill }}</div>
    </AppHeader>

    <main class="sub-body">
      <router-link to="/home" class="back">← 홈</router-link>
      <slot />
    </main>
  </div>
</template>

<script setup>
// 홈에서 들어가는 상세 페이지 공통 틀 (헤더 + 홈으로 돌아가기)
import AppHeader from '../AppHeader.vue'

defineProps({
  // 헤더 가운데 표시 (SOLO 기능, 마이페이지 MY …)
  pill: { type: String, default: 'SOLO' },
})
</script>

<style scoped>
* { box-sizing: border-box; }

.sub-app {
  width: 100%; min-height: 100vh;
  background: #f8f9fa; color: #1a1a1a;
  font-family: 'Pretendard', sans-serif;
}
.page-pill {
  background: #f1f3f5; color: #2d5bff; font-weight: 800; font-size: 0.8rem;
  padding: 8px 18px; border-radius: 12px; letter-spacing: 0.5px;
}
.sub-body { max-width: 1040px; margin: 0 auto; padding: 24px 20px 48px; }
.back {
  display: inline-block; margin-bottom: 16px; font-size: 0.85rem; font-weight: 800;
  color: #868e96; text-decoration: none; transition: color 0.15s;
}
.back:hover { color: #2d5bff; }

@media (max-width: 760px) {
  .sub-body { padding: 16px 16px 40px; }
}
</style>
