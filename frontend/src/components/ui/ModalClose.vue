<template>
  <!-- 팝업 오른쪽 위 닫기 버튼 (#292). 내용이 있는 팝업은 모두 이걸 쓴다 ("삭제할까요?" 같은 짧은 확인창은 빼고).
       팝업 상자에 position: relative가 있어야 한다. 저장 중처럼 닫으면 안 될 때는 disabled -->
  <button type="button" class="modal-close" :aria-label="label" :disabled="disabled" @click="emit('close')">
    <X :size="18" :stroke-width="2.4" aria-hidden="true" />
  </button>
</template>

<script setup>
import { X } from 'lucide-vue-next'

defineProps({
  disabled: Boolean,
  label: { type: String, default: '닫기' },
})
const emit = defineEmits(['close'])
</script>

<style scoped>
.modal-close {
  position: absolute; top: 14px; right: 14px; z-index: 2;
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; padding: 0; border: none; border-radius: 10px;
  background: var(--c-line-soft); color: var(--c-muted); cursor: pointer;
}
.modal-close:hover:not(:disabled) { background: var(--c-line); color: var(--c-text); }
.modal-close:disabled { opacity: 0.5; cursor: default; }
</style>
