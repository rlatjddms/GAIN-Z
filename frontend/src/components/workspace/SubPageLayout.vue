<template>
  <div class="sub-app">
    <AppHeader>
      <div class="page-pill">{{ pill }}</div>
    </AppHeader>

    <main class="sub-body">
      <router-link :to="backTo" class="back"><ArrowLeft :size="15" :stroke-width="2.4" /> {{ backLabel }}</router-link>
      <PageHeader v-if="title" :title="title" :name="name" :subtitle="subtitle" class="head">
        <template v-if="$slots.actions" #actions><slot name="actions" /></template>
        <template v-if="$slots.below" #below><slot name="below" /></template>
      </PageHeader>
      <slot />
    </main>
  </div>
</template>

<script setup>
// 홈에서 들어가는 상세 페이지 공통 틀 (#134): 헤더 + 뒤로 가기 + 페이지 머리말. 폭은 모든 페이지가 1040px
import { ArrowLeft } from 'lucide-vue-next'
import AppHeader from '../AppHeader.vue'
import PageHeader from '../ui/PageHeader.vue'

defineProps({
  // 헤더 가운데 표시 (SOLO 기능, 마이페이지 MY …)
  pill: { type: String, default: 'SOLO' },
  // 뒤로 가기: 기본은 홈. 밴드 화면은 합주실(홈 BAND 탭), 악보 상세는 악보 보관함
  backTo: { type: [String, Object], default: '/home' },
  backLabel: { type: String, default: '홈' },
  // 페이지 머리말 (components/ui/PageHeader.vue). title이 없으면 그리지 않는다 (불러오는 중 등)
  title: { type: String, default: '' },
  name: { type: String, default: '' },
  subtitle: { type: String, default: '' },
})
</script>

<style scoped>
* { box-sizing: border-box; }

.sub-app {
  width: 100%; min-height: 100vh;
  background: var(--c-bg); color: var(--c-text);
  font-family: var(--font-sans);
}
.page-pill {
  background: var(--c-line-soft); color: var(--c-primary-text); font-weight: 800; font-size: 0.8rem;
  padding: 8px 18px; border-radius: var(--radius-md); letter-spacing: 0.5px;
}
.sub-body { max-width: 1040px; margin: 0 auto; padding: 24px 20px 48px; }
.back {
  display: inline-flex; align-items: center; gap: 4px; margin-bottom: 16px; font-size: 0.85rem; font-weight: 800;
  color: var(--c-muted); text-decoration: none; transition: color 0.15s;
}
.back:hover { color: var(--c-primary-text); }
.head { margin-bottom: 20px; }

@media (max-width: 860px) {
  .sub-body { padding: 16px 16px 40px; }
}
@media (max-width: 640px) {
  .page-pill { padding: 6px 12px; font-size: 0.72rem; }
  .back { margin-bottom: 12px; }
  .head { margin-bottom: 16px; }
}
</style>
