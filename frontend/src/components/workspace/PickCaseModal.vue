<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="close">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="pick-case-title">
          <header class="modal-head">
            <div>
              <h2 id="pick-case-title">MY PICK CASE <span class="soon">준비 중</span></h2>
              <p>업적을 달성하면 피크를 얻어요</p>
            </div>
            <button class="close" aria-label="닫기" @click="close">✕</button>
          </header>

          <div class="rarity-legend">
            <span v-for="r in RARITIES" :key="r.key" class="r" :style="{ '--c': r.color }">{{ r.label }}</span>
          </div>

          <!-- 피크 도감: #47(지급 API)·#48에서 연결 -->
          <div class="pick-grid">
            <div v-for="(r, i) in SLOTS" :key="i" class="slot" :style="{ '--c': r.color }">
              <span class="lock">🔒</span>
            </div>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { watch, onBeforeUnmount } from 'vue'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['close'])
const close = () => emit('close')

// 백엔드 PickRarity와 같은 순서
const RARITIES = [
  { key: 'COMMON', label: '일반', color: '#ced4da' },
  { key: 'RARE', label: '레어', color: '#5a7dff' },
  { key: 'LEGENDARY', label: '전설', color: '#f59e0b' },
]
// 실제 피크 목록이 연결되기 전까지 보여 줄 빈 칸 (일반 6 · 레어 4 · 전설 2)
const SLOTS = [...Array(6).fill(RARITIES[0]), ...Array(4).fill(RARITIES[1]), ...Array(2).fill(RARITIES[2])]

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

.rarity-legend { display: flex; gap: 12px; margin-bottom: 12px; }
.r { display: flex; align-items: center; gap: 5px; font-size: 0.7rem; font-weight: 800; color: #868e96; }
.r::before { content: ''; width: 8px; height: 8px; border-radius: 2px; background: var(--c); }

.pick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.slot {
  aspect-ratio: 1; border-radius: 14px; background: #f8f9fa;
  border: 2px solid color-mix(in srgb, var(--c) 45%, #fff);
  display: flex; align-items: center; justify-content: center;
}
.lock { font-size: 1.1rem; opacity: 0.35; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-active .modal, .modal-leave-active .modal { transition: transform 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal, .modal-leave-to .modal { transform: translateY(12px) scale(0.98); }
</style>
