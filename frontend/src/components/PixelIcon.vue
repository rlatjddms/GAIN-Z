<template>
  <!-- 픽셀 아이콘 (pixel/icons.js). 색은 글자색(currentColor)을 따른다 -->
  <svg
    class="pixel-icon"
    viewBox="0 0 12 12"
    :width="size"
    :height="size"
    shape-rendering="crispEdges"
    :role="label ? 'img' : null"
    :aria-label="label || null"
    :aria-hidden="label ? null : 'true'"
  >
    <rect v-for="([x, y], i) in cells" :key="i" :x="x" :y="y" width="1" height="1" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { iconCells } from '../pixel/icons'

const props = defineProps({
  name: { type: String, required: true },
  // 픽셀이 고르게 보이게 12의 배수를 권장 (24, 36, 48)
  size: { type: Number, default: 24 },
  label: { type: String, default: '' },
})
const cells = computed(() => iconCells(props.name))
</script>

<style scoped>
.pixel-icon { display: inline-block; flex: none; fill: currentColor; vertical-align: middle; }
</style>
