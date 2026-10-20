<template>
  <header class="page-header">
    <div class="titles">
      <h1>{{ title }}</h1>
      <p v-if="name || subtitle" class="subtitle">
        <strong v-if="name">{{ name }}</strong>
        <span v-if="name && subtitle" class="dot" aria-hidden="true">·</span>
        <span v-if="subtitle">{{ subtitle }}</span>
      </p>
      <slot name="below" />
    </div>
    <div v-if="$slots.actions" class="actions"><slot name="actions" /></div>
  </header>
</template>

<script setup>
// 페이지 머리말 (#134): 영어 대문자 제목 + 한글 이름·설명. 모든 페이지가 같은 크기·색을 쓴다
defineProps({
  title: { type: String, required: true },   // 영어 대문자 (PRACTICE LOG)
  name: { type: String, default: '' },       // 한글 페이지 이름 (연습 기록장)
  subtitle: { type: String, default: '' },   // 한 줄 설명
})
</script>

<style scoped>
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px 16px; flex-wrap: wrap; }
.titles { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
h1 {
  margin: 0; font-size: 1.35rem; font-weight: 900; letter-spacing: 0.04em; line-height: 1.2;
  color: var(--c-text); overflow-wrap: anywhere;
}
.subtitle { margin: 0; font-size: 0.85rem; font-weight: 600; line-height: 1.5; color: var(--c-muted); overflow-wrap: anywhere; }
.subtitle strong { font-weight: 800; color: var(--c-text-2); }
.dot { margin: 0 6px; color: var(--c-faint); }
.actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

@media (max-width: 560px) {
  .page-header { align-items: flex-start; flex-direction: column; }
}
</style>
