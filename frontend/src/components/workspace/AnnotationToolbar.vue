<template>
  <!-- 필기 도구 (#167): 펜·형광펜·지우개, 색, 되돌리기, 모두 지우기. 저장은 쓸 때마다 알아서 -->
  <div class="annotation-toolbar" role="toolbar" aria-label="필기 도구">
    <div class="segment" role="group" aria-label="도구">
      <button v-for="t in toolList" :key="t.key" :class="{ on: store.tool === t.key }" :aria-pressed="store.tool === t.key"
              :title="t.label" @click="store.setTool(t.key)">
        <component :is="t.icon" :size="14" :stroke-width="2.2" /><span class="label">{{ t.label }}</span>
      </button>
    </div>

    <div v-if="store.tool !== 'eraser'" class="colors" role="group" aria-label="색">
      <button v-for="c in TOOLS[store.tool].colors" :key="c" class="swatch" :class="{ on: store.color === c }"
              :style="{ '--swatch': c }" :aria-label="`색 ${c}`" :aria-pressed="store.color === c" @click="store.setColor(c)" />
    </div>

    <div class="actions">
      <button class="btn small ghost" :disabled="!store.canUndo" title="되돌리기" @click="store.undo()">
        <Undo2 :size="14" :stroke-width="2.2" /><span class="label">되돌리기</span>
      </button>
      <button class="btn small ghost danger-text" :disabled="!store.strokeCount" title="모두 지우기" @click="clearAll">
        <Trash2 :size="14" :stroke-width="2.2" /><span class="label">모두 지우기</span>
      </button>
      <span class="status" :class="store.status" aria-live="polite">{{ statusText }}</span>
      <button class="btn small primary" @click="emit('done')">완료</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Pen, Highlighter, Eraser, Undo2, Trash2 } from 'lucide-vue-next'
import { useAnnotationStore, TOOLS } from '../../stores/annotation'
import { useDialogStore } from '../../stores/dialog'

const emit = defineEmits(['done'])
const store = useAnnotationStore()
const dialog = useDialogStore()

const toolList = [
  { key: 'pen', label: '펜', icon: Pen },
  { key: 'hl', label: '형광펜', icon: Highlighter },
  { key: 'eraser', label: '지우개', icon: Eraser },
]

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
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px;
  padding: 8px 10px; border: 1px solid var(--c-primary-line); border-radius: var(--radius-md);
  background: var(--c-surface); box-shadow: var(--shadow-sm);
}
.segment > button { display: inline-flex; align-items: center; gap: 4px; }
.colors { display: flex; gap: 6px; }
.swatch {
  width: 22px; height: 22px; padding: 0; border-radius: 50%; cursor: pointer;
  background: var(--swatch); border: 2px solid var(--c-surface); box-shadow: 0 0 0 1px var(--c-line);
}
.swatch.on { box-shadow: 0 0 0 2px var(--c-text); }
.actions { display: flex; align-items: center; gap: 4px; margin-left: auto; }
.danger-text:hover:not(:disabled) { color: var(--c-danger); }
.status { min-width: 44px; font-size: 0.72rem; font-weight: 700; color: var(--c-faint); text-align: right; margin-right: 4px; }
.status.error { color: var(--c-danger); }

@media (max-width: 640px) {
  .label { display: none; }
  .actions { margin-left: 0; flex: 1; justify-content: flex-end; }
}
</style>
