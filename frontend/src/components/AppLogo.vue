<template>
  <!-- GAIN-Z 픽셀 로고. 색은 테마 색(--c-primary)을 따라간다 -->
  <svg
    class="app-logo"
    :viewBox="`0 0 ${logo.width} ${logo.height}`"
    :width="logo.width * pixel"
    :height="logo.height * pixel"
    shape-rendering="crispEdges"
    role="img"
    aria-label="GAIN-Z"
  >
    <rect v-for="([x, y], i) in logo.cells" :key="i" :x="x" :y="y" width="1" height="1" />
  </svg>
</template>

<script setup>
import { logoCells } from '../pixel/logo'

defineProps({
  // 픽셀 한 칸 크기(px)
  pixel: { type: Number, default: 3 },
})
const logo = logoCells('GAIN-Z')
</script>

<style scoped>
.app-logo { display: block; fill: var(--c-primary); }
</style>
