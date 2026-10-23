<template>
  <div class="swatches" role="radiogroup" :aria-label="label">
    <button
      v-for="c in presets" :key="c" type="button" role="radio"
      class="swatch" :class="{ on: same(c, modelValue) }" :style="{ background: c }"
      :aria-checked="same(c, modelValue)" :aria-label="c"
      @click="emit('update:modelValue', c)"
    ></button>
    <!-- 프리셋에 없는 색은 직접 고르기 -->
    <label class="swatch custom" :class="{ on: isCustom }" :style="isCustom ? { background: modelValue } : null" title="직접 고르기">
      <span v-if="!isCustom">+</span>
      <input type="color" :value="modelValue" @input="emit('update:modelValue', $event.target.value)">
    </label>
  </div>
</template>

<script setup>
// 색 고르기: 프리셋 몇 개 + 직접 고르기
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '#000000' },
  presets: { type: Array, required: true },
  label: { type: String, default: '색 고르기' },
})
const emit = defineEmits(['update:modelValue'])

const same = (a, b) => (a || '').toLowerCase() === (b || '').toLowerCase()
const isCustom = computed(() => !props.presets.some((c) => same(c, props.modelValue)))
</script>

<style scoped>
.swatches { display: flex; flex-wrap: wrap; gap: 8px; }
.swatch {
  position: relative; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; padding: 0;
  border: 2px solid #fff; box-shadow: 0 0 0 1px #dee2e6;
}
.swatch.on { box-shadow: 0 0 0 2px var(--c-primary); }
.custom {
  display: flex; align-items: center; justify-content: center; overflow: hidden;
  background: conic-gradient(#ef4444, #f59e0b, #22c55e, #3b82f6, #a855f7, #ef4444);
  color: #fff; font-weight: 900; text-shadow: 0 0 3px rgba(0, 0, 0, 0.5);
}
.custom input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
</style>
