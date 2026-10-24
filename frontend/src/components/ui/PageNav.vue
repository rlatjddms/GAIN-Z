<template>
  <!-- 페이지 번호 (#188). page는 0부터, 화면에는 1부터 보인다 -->
  <nav v-if="totalPages > 1" class="page-nav" aria-label="페이지">
    <button type="button" class="step" :disabled="page === 0" aria-label="이전 페이지" @click="go(page - 1)">‹</button>
    <template v-for="item in items" :key="item.key">
      <span v-if="item.gap" class="gap" aria-hidden="true">…</span>
      <button
        v-else
        type="button"
        class="num"
        :class="{ current: item.page === page }"
        :aria-current="item.page === page ? 'page' : null"
        @click="go(item.page)"
      >{{ item.page + 1 }}</button>
    </template>
    <button type="button" class="step" :disabled="page >= totalPages - 1" aria-label="다음 페이지" @click="go(page + 1)">›</button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
})
const emit = defineEmits(['change'])

// 처음·끝과 지금 쪽 양옆 한 칸씩, 사이가 비면 …
const items = computed(() => {
  const last = props.totalPages - 1
  const pages = new Set([0, last, props.page - 1, props.page, props.page + 1].filter(p => p >= 0 && p <= last))
  const sorted = [...pages].sort((a, b) => a - b)
  const result = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push({ key: `gap-${p}`, gap: true })
    result.push({ key: p, page: p })
  })
  return result
})
const go = (p) => { if (p !== props.page && p >= 0 && p < props.totalPages) emit('change', p) }
</script>

<style scoped>
.page-nav { display: flex; justify-content: center; align-items: center; gap: 4px; margin-top: 16px; }
.num, .step {
  min-width: 34px; height: 34px; padding: 0 8px; border: 1px solid transparent; border-radius: 10px; background: none;
  font: inherit; font-size: 0.82rem; font-weight: 700; color: var(--c-text-2); cursor: pointer; font-variant-numeric: tabular-nums;
}
.num:hover, .step:hover:not(:disabled) { background: var(--c-line-soft); }
.num.current { border-color: var(--c-primary-line); background: var(--c-primary-soft); color: var(--c-primary-text); }
.step { font-size: 1rem; font-weight: 900; }
.step:disabled { opacity: 0.3; cursor: default; }
.gap { min-width: 20px; text-align: center; color: var(--c-faint); font-size: 0.8rem; }
</style>
