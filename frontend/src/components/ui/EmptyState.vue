<template>
  <div class="empty-state" :class="{ compact }">
    <PixelIcon v-if="icon" :name="icon" :size="compact ? 36 : 48" class="icon" />
    <strong>{{ title }}</strong>
    <p v-if="text || $slots.default"><slot>{{ text }}</slot></p>
    <div v-if="$slots.actions" class="actions"><slot name="actions" /></div>
  </div>
</template>

<script setup>
// 빈 화면 (#134): 픽셀 아이콘 + 제목 + 설명 + 버튼. 점선 상자 모양을 모든 화면이 같이 쓴다
import PixelIcon from '../PixelIcon.vue'

defineProps({
  icon: { type: String, default: '' },     // pixel/icons.js 이름
  title: { type: String, required: true },
  text: { type: String, default: '' },
  compact: { type: Boolean, default: false },
})
</script>

<style scoped>
.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  padding: 44px 20px; text-align: center;
  border: 2px dashed var(--c-line); border-radius: var(--radius-lg); background: var(--c-surface);
}
.empty-state.compact { padding: 28px 16px; }
.icon { color: var(--c-faint); margin-bottom: 4px; }
strong { font-size: 0.95rem; font-weight: 900; color: var(--c-text); }
p { margin: 0; max-width: 420px; font-size: 0.82rem; font-weight: 600; line-height: 1.6; color: var(--c-muted); }
.actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; margin-top: 8px; }
</style>
