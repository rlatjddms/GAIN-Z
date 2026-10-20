<template>
  <!-- 알림 영역 아이콘 (친구·밴드·커뮤니티·합주) -->
  <span class="icon" :class="category.toLowerCase()" :style="{ width: `${size}px`, height: `${size}px` }" aria-hidden="true">
    <svg v-if="category === 'FRIEND'" viewBox="0 0 24 24" :width="glyph" :height="glyph" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><line x1="19" y1="8" x2="19" y2="14"></line><line x1="22" y1="11" x2="16" y2="11"></line></svg>
    <svg v-else-if="category === 'COMMUNITY'" viewBox="0 0 24 24" :width="glyph" :height="glyph" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
    <svg v-else-if="category === 'RECORD'" viewBox="0 0 24 24" :width="glyph" :height="glyph" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"></circle><circle cx="12" cy="12" r="2.5"></circle></svg>
    <svg v-else-if="category === 'JAM'" viewBox="0 0 24 24" :width="glyph" :height="glyph" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>
    <svg v-else viewBox="0 0 24 24" :width="glyph" :height="glyph" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { iconOf } from '../constants/notification'

const props = defineProps({
  type: { type: String, required: true },
  size: { type: Number, default: 30 },
})

const category = computed(() => iconOf(props.type))
const glyph = computed(() => Math.round(props.size / 2))
</script>

<style scoped>
.icon {
  flex: none; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: var(--c-line-soft); color: var(--c-muted);
}
.icon.friend { background: #ebfbee; color: var(--c-success); }
.icon.band { background: var(--c-primary-soft); color: var(--c-primary-text); }
.icon.community { background: #fff4e6; color: #e8590c; }
.icon.jam { background: #f3f0ff; color: #7048e8; }
.icon.record { background: #fff9db; color: #e67700; }
</style>
