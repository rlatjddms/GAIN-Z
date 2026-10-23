<template>
  <section class="ws-card">
    <header v-if="title || $slots.actions" class="ws-card-head">
      <h3>
        {{ title }}
        <span v-if="soon" class="soon">준비 중</span>
      </h3>
      <div class="actions"><slot name="actions" /></div>
    </header>
    <slot />
  </section>
</template>

<script setup>
// 워크스페이스 공통 카드. soon: 아직 연결되지 않은 기능 표시
defineProps({
  title: { type: String, default: '' },
  soon: { type: Boolean, default: false },
})
</script>

<style scoped>
.ws-card {
  background: #fff; border: 1px solid var(--c-line); border-radius: 20px;
  padding: var(--pad-card); min-width: 0;
}
.ws-card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
h3 { margin: 0; font-size: 0.9rem; font-weight: 800; color: #343a40; display: flex; align-items: center; gap: 8px; }
.soon {
  font-size: 0.65rem; font-weight: 800; color: #ff922b; background: #fff4e6;
  padding: 3px 8px; border-radius: 6px;
}
.actions { display: flex; align-items: center; gap: 8px; }
</style>
