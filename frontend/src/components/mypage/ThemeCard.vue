<template>
  <WsCard title="테마 색">
    <p class="muted">로고·버튼·탭·연습 잔디 색을 골라요. 이 기기에만 저장돼요.</p>
    <div class="themes" role="radiogroup" aria-label="테마 색">
      <button
        v-for="t in THEMES" :key="t.key" type="button" role="radio" class="theme"
        :class="{ on: theme.key === t.key }" :aria-checked="theme.key === t.key"
        @click="theme.set(t.key)"
      >
        <span class="swatch" :style="{ background: t.color }"><i v-if="theme.key === t.key">✓</i></span>
        <span class="name">{{ t.name }}</span>
      </button>
    </div>
  </WsCard>
</template>

<script setup>
// 테마 색 고르기 (#122). 고르면 바로 화면 전체·파비콘에 적용된다
import WsCard from '../workspace/WsCard.vue'
import { THEMES } from '../../constants/themes'
import { useThemeStore } from '../../stores/theme'

const theme = useThemeStore()
</script>

<style scoped>
.muted { margin: 0 0 12px; font-size: 0.8rem; color: var(--c-muted); }
.themes { display: grid; grid-template-columns: repeat(auto-fill, minmax(84px, 1fr)); gap: 8px; }
.theme {
  display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 10px 4px;
  border: 1px solid var(--c-line); border-radius: var(--radius-md); background: var(--c-surface);
  cursor: pointer; font-family: inherit;
}
.theme.on { border-color: var(--c-primary); background: var(--c-primary-soft); }
.swatch {
  width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 0.8rem; font-weight: 900;
}
.swatch i { font-style: normal; }
.name { font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); }
.theme.on .name { color: var(--c-primary-text); }
</style>
