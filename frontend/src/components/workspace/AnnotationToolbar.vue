<template>
  <!-- 필기 도구 (#167): 펜·형광펜·글자·지우개, 색(정해 둔 색 + 마음대로), 되돌리기, 모두 지우기. 저장은 쓸 때마다 알아서 -->
  <div class="annotation-toolbar" role="toolbar" aria-label="필기 도구">
    <div class="segment" role="group" aria-label="도구">
      <button v-for="t in toolList" :key="t.key" :class="{ on: store.tool === t.key }" :aria-pressed="store.tool === t.key"
              :title="t.label" @click="store.setTool(t.key)">
        <component :is="t.icon" :size="14" :stroke-width="2.2" /><span class="label">{{ t.label }}</span>
      </button>
    </div>

    <!-- 색: 넓은 화면은 한 줄로, 휴대폰은 지금 색 버튼을 누르면 펼쳐진다 -->
    <div v-if="store.tool !== 'eraser'" class="colors-wrap">
      <button class="swatch on color-toggle" :style="{ '--swatch': store.color }" aria-label="색 고르기" :aria-expanded="colorsOpen"
              @click="colorsOpen = !colorsOpen" />
      <div class="colors" :class="{ open: colorsOpen }" role="group" aria-label="색">
      <button v-for="c in TOOLS[store.tool].colors" :key="c" class="swatch" :class="{ on: store.color === c }"
              :style="{ '--swatch': c }" :aria-label="`색 ${c}`" :aria-pressed="store.color === c" @click="pick(c)" />
      <!-- 마음대로 고르는 색: 고른 색이 정해 둔 색 밖이면 그 색으로 보인다 -->
      <label class="swatch custom" :class="{ on: customColor }" :style="customColor ? { '--swatch': store.color } : null" title="다른 색 고르기">
        <input type="color" :value="store.color" aria-label="다른 색 고르기" @input="store.setColor($event.target.value)" @change="colorsOpen = false">
      </label>
      </div>
    </div>

    <div class="edit" role="group" aria-label="고치기">
      <button class="icon-btn" :disabled="!store.canUndo" title="되돌리기" aria-label="되돌리기" @click="store.undo()">
        <Undo2 :size="16" :stroke-width="2.2" />
      </button>
      <button class="icon-btn danger" :disabled="!store.strokeCount" title="모두 지우기" aria-label="모두 지우기" @click="clearAll">
        <Trash2 :size="16" :stroke-width="2.2" />
      </button>
    </div>

    <div class="end">
      <span class="status" :class="store.status" aria-live="polite">{{ statusText }}</span>
      <button class="btn small primary" @click="emit('done')">완료</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Pen, Highlighter, Type, Eraser, Undo2, Trash2 } from 'lucide-vue-next'
import { useAnnotationStore, TOOLS } from '../../stores/annotation'
import { useDialogStore } from '../../stores/dialog'

const emit = defineEmits(['done'])
const store = useAnnotationStore()
const dialog = useDialogStore()

const toolList = [
  { key: 'pen', label: '펜', icon: Pen },
  { key: 'hl', label: '형광펜', icon: Highlighter },
  { key: 'text', label: '글자', icon: Type },
  { key: 'eraser', label: '지우개', icon: Eraser },
]

const colorsOpen = ref(false)
const pick = (c) => {
  store.setColor(c)
  colorsOpen.value = false
}
watch(() => store.tool, () => { colorsOpen.value = false })

const customColor = computed(() => store.tool !== 'eraser' && !TOOLS[store.tool].colors.includes(store.color))

const statusText = computed(() => {
  if (store.status === 'error') return store.message || '저장 못 함 · 다시 시도해요'
  return { saving: '저장 중…', saved: '저장됨' }[store.status] || ''
})

const clearAll = async () => {
  const ok = await dialog.confirm({ title: '필기 모두 지우기', text: '이 악보에 쓴 내 필기를 모두 지울까요? 되돌리기로 다시 살릴 수 있어요.', okText: '모두 지우기', danger: true })
  if (ok) store.clearAll()
}
</script>

<style scoped>
.annotation-toolbar {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px 10px;
  padding: 6px 8px; border: 1px solid var(--c-primary-line); border-radius: var(--radius-md);
  background: var(--c-surface); box-shadow: var(--shadow-sm);
}
.segment > button { display: inline-flex; align-items: center; gap: 4px; }
.colors-wrap { position: relative; }
.colors { display: flex; align-items: center; gap: 5px; }
.color-toggle { display: none; }
.swatch {
  position: relative; width: 22px; height: 22px; padding: 0; border-radius: 50%; cursor: pointer; flex: none;
  background: var(--swatch); border: 2px solid var(--c-surface); box-shadow: 0 0 0 1px var(--c-line);
}
.swatch.on { box-shadow: 0 0 0 2px var(--c-text); }
.swatch.custom:not(.on) {
  background: conic-gradient(#ef4444, #f59e0b, #facc15, #22c55e, #3b82f6, #8b5cf6, #ec4899, #ef4444);
}
.swatch.custom input { position: absolute; inset: 0; width: 100%; height: 100%; opacity: 0; cursor: pointer; border: 0; padding: 0; }

.edit { display: flex; gap: 2px; padding-left: 8px; border-left: 1px solid var(--c-line-soft); }
.icon-btn {
  display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; padding: 0;
  border: 0; border-radius: 8px; background: transparent; color: var(--c-text-2); cursor: pointer;
}
.icon-btn:hover:not(:disabled) { background: var(--c-line-soft); }
.icon-btn.danger:hover:not(:disabled) { color: var(--c-danger); }
.icon-btn:disabled { opacity: 0.35; cursor: default; }

.end { display: flex; align-items: center; gap: 6px; margin-left: auto; }
.status { font-size: 0.72rem; font-weight: 700; color: var(--c-faint); white-space: nowrap; }
.status.error { color: var(--c-danger); white-space: normal; }

/* 휴대폰: 도구·색·되돌리기·지우기·완료를 한 줄에 */
@media (max-width: 640px) {
  .annotation-toolbar { gap: 8px; }
  .label { display: none; }
  .segment > button { padding-left: 9px; padding-right: 9px; }
  .color-toggle { display: block; }
  .colors {
    display: none; position: absolute; top: calc(100% + 8px); left: -8px; z-index: 10;
    padding: 8px; gap: 8px; background: var(--c-surface); border: 1px solid var(--c-line); border-radius: 12px; box-shadow: var(--shadow-sm);
  }
  .colors.open { display: flex; }
  .colors .swatch { width: 28px; height: 28px; }
  .edit { padding-left: 6px; }
  .status:not(.error) { display: none; }
}
</style>
