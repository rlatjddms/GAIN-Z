<template>
  <!-- 저장·오류 알림 (#267, stores/toast.js): 화면 아래 가운데. 휴대폰 키보드가 떠 있으면 그 위로.
       셋리스트·게시글 창 같은 <dialog>가 열려 있으면 그 바깥은 눌리지 않으므로, 알림을 그 창 안에 그린다 -->
  <Teleport :to="host">
    <div class="toast-zone" :class="{ lifted: updateStore.showBanner }" aria-live="polite">
      <transition name="toast">
        <div v-if="store.current" :key="store.current.key" class="toast" :class="store.current.kind"
             :role="store.current.kind === 'error' ? 'alert' : 'status'">
          <CircleCheck v-if="store.current.kind === 'success'" :size="18" :stroke-width="2.4" aria-hidden="true" />
          <CircleAlert v-else :size="18" :stroke-width="2.4" aria-hidden="true" />
          <span class="text">{{ store.current.text }}</span>
          <button v-if="store.current.kind === 'error'" type="button" class="close" aria-label="알림 닫기" @click="store.dismiss()">×</button>
        </div>
      </transition>
    </div>
  </Teleport>
</template>

<script setup>
import { shallowRef, watch, onBeforeUnmount } from 'vue'
import { CircleAlert, CircleCheck } from 'lucide-vue-next'
import { useToastStore } from '../../stores/toast'
import { useAppUpdateStore } from '../../stores/appUpdate'

const store = useToastStore()
const updateStore = useAppUpdateStore()

// 맨 위에 열린 모달 창 (없으면 body)
const topModal = () => {
  const open = [...document.querySelectorAll('dialog[open]')].filter((d) => {
    try { return d.matches(':modal') } catch { return true }
  })
  return open.at(-1) || 'body'
}
const host = shallowRef('body')
let watched = null
const place = () => {
  watched?.removeEventListener('close', place)
  host.value = topModal()
  watched = host.value === 'body' ? null : host.value
  watched?.addEventListener('close', place)
}
// 새 알림마다 그때 맨 위 창으로 옮기고, 그 창이 닫히면 다시 아래 창이나 화면으로
watch(() => store.current?.key, (key) => { if (key) place() })
onBeforeUnmount(() => watched?.removeEventListener('close', place))
</script>

<style scoped>
.toast-zone {
  position: fixed; left: 50%; transform: translateX(-50%); z-index: 3100; pointer-events: none;
  bottom: calc(24px + env(safe-area-inset-bottom) + var(--kb, 0px));
  width: max-content; max-width: calc(100vw - 32px);
}
/* 새 버전 안내 막대(UpdateBanner)가 떠 있으면 그 위로 */
.toast-zone.lifted { bottom: calc(92px + env(safe-area-inset-bottom) + var(--kb, 0px)); }
.toast {
  pointer-events: auto; display: flex; align-items: center; gap: 8px; box-sizing: border-box;
  padding: 11px 16px; border-radius: 12px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; font-size: 0.85rem; font-weight: 700; line-height: 1.4; text-align: left;
  background: #1f2937; color: #fff;
}
.toast.success svg { color: #69db7c; flex: none; }
.toast.error { background: var(--c-danger, #e03131); }
.toast.error svg { flex: none; }
.text { min-width: 0; overflow-wrap: anywhere; }
.close {
  flex: none; margin: -4px -6px -4px 2px; width: 26px; height: 26px; border: none; border-radius: 6px;
  background: none; color: inherit; font-size: 1.1rem; line-height: 1; cursor: pointer;
}
.close:hover { background: rgba(255, 255, 255, 0.18); }
.toast-enter-active, .toast-leave-active { transition: opacity 0.2s, transform 0.2s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(8px); }
@media (prefers-reduced-motion: reduce) {
  .toast-enter-active, .toast-leave-active { transition: opacity 0.2s; }
  .toast-enter-from, .toast-leave-to { transform: none; }
}
</style>
