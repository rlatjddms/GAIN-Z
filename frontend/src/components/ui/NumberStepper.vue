<template>
  <!-- 숫자 고르기 (#230): −/+ 버튼(누르고 있으면 계속), 숫자를 위아래로 밀기, 마우스 휠, 직접 입력 -->
  <div class="stepper" :class="{ dragging }">
    <button type="button" class="step" :disabled="disabled || modelValue <= min" :aria-label="`${label} 줄이기`"
            @pointerdown.prevent="hold(-1, $event)" @keydown.enter.prevent="change(-1)" @keydown.space.prevent="change(-1)">
      <Minus :size="16" :stroke-width="2.6" />
    </button>
    <input
      ref="input" class="value" type="text" inputmode="numeric" :value="draft" :disabled="disabled"
      :aria-label="label" role="spinbutton" :aria-valuemin="min" :aria-valuemax="max" :aria-valuenow="modelValue"
      title="위아래로 밀거나 휠을 굴려서 바꿀 수 있어요"
      @input="onInput" @blur="commit" @keydown.up.prevent="change(1)" @keydown.down.prevent="change(-1)"
      @pointerdown="dragStart" @wheel="onWheel"
    >
    <span v-if="unit" class="unit">{{ unit }}</span>
    <button type="button" class="step" :disabled="disabled || modelValue >= max" :aria-label="`${label} 늘리기`"
            @pointerdown.prevent="hold(1, $event)" @keydown.enter.prevent="change(1)" @keydown.space.prevent="change(1)">
      <Plus :size="16" :stroke-width="2.6" />
    </button>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { Minus, Plus } from 'lucide-vue-next'

const props = defineProps({
  modelValue: { type: Number, default: 1 },
  min: { type: Number, default: 1 },
  max: { type: Number, default: 99 },
  label: { type: String, default: '숫자' },
  unit: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const input = ref(null)
const draft = ref(String(props.modelValue))
watch(() => props.modelValue, (v) => { draft.value = String(v) })

const clamp = (n) => Math.min(props.max, Math.max(props.min, Math.round(n)))
const set = (n) => {
  const next = clamp(n)
  draft.value = String(next)
  if (next !== props.modelValue) emit('update:modelValue', next)
}
const change = (delta) => set(props.modelValue + delta)

// 직접 입력: 숫자만 남기고, 칸을 벗어날 때 범위 안으로
const onInput = (e) => {
  draft.value = e.target.value.replace(/\D/g, '').slice(0, String(props.max).length)
  e.target.value = draft.value
  if (draft.value) emit('update:modelValue', Number(draft.value))
}
const commit = () => set(Number(draft.value) || props.min)

// −/+를 누르고 있으면 조금 뒤부터 빠르게 바뀐다
let holdTimer = null
const stopHold = () => { clearTimeout(holdTimer); clearInterval(holdTimer); holdTimer = null }
const hold = (delta, e) => {
  if (e.button !== 0) return
  change(delta)
  stopHold()
  holdTimer = setTimeout(() => { holdTimer = setInterval(() => change(delta), 80) }, 400)
  window.addEventListener('pointerup', stopHold, { once: true })
  window.addEventListener('pointercancel', stopHold, { once: true })
}

// 숫자를 위아래로 밀기: 위로 16px마다 +1. 조금만 움직이면 그냥 눌러서 입력하는 것으로 본다
const STEP_PX = 16
const dragging = ref(false)
let drag = null
const dragStart = (e) => {
  if (props.disabled || e.button !== 0) return
  drag = { y: e.clientY, from: props.modelValue, moved: false, id: e.pointerId }
  window.addEventListener('pointermove', dragMove)
  window.addEventListener('pointerup', dragEnd, { once: true })
  window.addEventListener('pointercancel', dragEnd, { once: true })
}
const dragMove = (e) => {
  if (!drag || e.pointerId !== drag.id) return
  const dy = drag.y - e.clientY
  if (!drag.moved && Math.abs(dy) < 6) return
  if (!drag.moved) { drag.moved = true; dragging.value = true; input.value?.blur() }
  e.preventDefault()
  set(drag.from + Math.trunc(dy / STEP_PX))
}
const dragEnd = () => {
  window.removeEventListener('pointermove', dragMove)
  dragging.value = false
  drag = null
}

// 마우스 휠: 위로 굴리면 +1 (이 칸 위에서만, 페이지는 스크롤하지 않는다)
const onWheel = (e) => {
  if (props.disabled || !e.deltaY) return
  e.preventDefault()
  change(e.deltaY < 0 ? 1 : -1)
}

onBeforeUnmount(() => { stopHold(); dragEnd() })
</script>

<style scoped>
.stepper {
  display: inline-flex; align-self: flex-start; align-items: center; gap: 4px; padding: 3px;
  border: 1px solid var(--c-line); border-radius: 12px; background: var(--c-bg);
}
.stepper:focus-within { border-color: var(--c-primary); background: #fff; }
.step {
  flex: none; display: flex; align-items: center; justify-content: center; width: 36px; height: 36px;
  border: none; border-radius: 9px; background: var(--c-surface); color: var(--c-text-2); cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08); touch-action: manipulation; user-select: none;
}
.step:hover:not(:disabled) { color: var(--c-primary-text); }
.step:disabled { opacity: 0.4; cursor: default; }
.value {
  width: 48px; height: 36px; padding: 0; border: none; outline: none; background: none;
  text-align: center; font: inherit; font-size: 1.05rem; font-weight: 800; color: var(--c-text);
  cursor: ns-resize; touch-action: none;
}
.dragging .value { color: var(--c-primary-text); }
.unit { margin-right: 4px; font-size: 0.85rem; font-weight: 700; color: var(--c-muted); }
</style>
