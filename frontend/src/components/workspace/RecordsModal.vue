<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="close">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="records-title">
          <header class="modal-head">
            <div>
              <h2 id="records-title">MY RECORDS <span class="soon">준비 중</span></h2>
              <p>업적을 달성하면 음반이 하나씩 쌓여요</p>
            </div>
            <button class="close" aria-label="닫기" @click="close">✕</button>
          </header>

          <div class="legend">
            <span v-for="r in RECORD_RARITIES" :key="r.key" class="legend-item">
              <RecordIcon :type="r.type" :color="LEGEND_COLORS[r.key]" :size="20" />
              {{ r.label }} <small>{{ r.rarity }}</small>
            </span>
          </div>

          <!-- 레코드 도감: #47(지급 API)·#48에서 연결 -->
          <div class="record-grid">
            <div v-for="(r, i) in PLACEHOLDER_RECORDS" :key="i" class="slot" :style="{ '--c': r.border }">
              <RecordIcon :type="r.type" locked :size="44" />
              <span class="slot-name">???</span>
            </div>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { watch, onBeforeUnmount } from 'vue'
import RecordIcon from './RecordIcon.vue'
import { RECORD_RARITIES, PLACEHOLDER_RECORDS } from '../../constants/records'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['close'])
const close = () => emit('close')

const LEGEND_COLORS = { COMMON: '#5a7dff', RARE: '#e8590c', LEGENDARY: '#7c3aed' }

// 열려 있는 동안 Esc로 닫기
const onKey = (e) => { if (e.key === 'Escape') close() }
watch(() => props.open, (v) => {
  if (v) window.addEventListener('keydown', onKey)
  else window.removeEventListener('keydown', onKey)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 560px; max-height: calc(100vh - 32px); overflow-y: auto;
  background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a; box-sizing: border-box;
}
.modal-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.modal-head h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; display: flex; align-items: center; gap: 8px; }
.modal-head p { margin: 0; font-size: 0.8rem; color: #868e96; }
.soon { font-size: 0.65rem; font-weight: 800; color: #ff922b; background: #fff4e6; padding: 3px 8px; border-radius: 6px; }
.close {
  flex: none; width: 32px; height: 32px; border: none; border-radius: 10px; background: #f1f3f5;
  color: #868e96; font-size: 0.9rem; cursor: pointer;
}
.close:hover { background: #e9ecef; color: #1a1a1a; }

.legend { display: flex; gap: 16px; margin-bottom: 14px; flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 0.75rem; font-weight: 800; color: #495057; }
.legend-item small { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }

.record-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.slot {
  border-radius: 14px; background: #f8f9fa; padding: 14px 6px 10px;
  border: 2px solid color-mix(in srgb, var(--c) 55%, #fff);
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.slot-name { font-size: 0.7rem; font-weight: 800; color: #ced4da; letter-spacing: 1px; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-active .modal, .modal-leave-active .modal { transition: transform 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal, .modal-leave-to .modal { transform: translateY(12px) scale(0.98); }

@media (max-width: 480px) {
  .record-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
