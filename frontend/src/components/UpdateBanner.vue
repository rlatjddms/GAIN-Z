<template>
  <transition name="banner">
    <div v-if="store.showBanner" class="update-banner" role="status">
      <RefreshCw class="icon" :size="18" :stroke-width="2.4" aria-hidden="true" />
      <div class="text">
        <strong>새 버전이 있어요</strong>
        <span>업데이트하면 화면을 새로 불러와요.</span>
      </div>
      <button type="button" class="btn primary small" :disabled="applying" @click="apply">{{ applying ? '불러오는 중…' : '업데이트' }}</button>
      <button type="button" class="close" aria-label="나중에" @click="store.dismissed = true">×</button>
    </div>
  </transition>
</template>

<script setup>
// 새 버전 안내 (#223). 화면 아래에 띄우고, 닫으면 다음 새 버전이 나올 때까지 숨긴다 (마이페이지에서는 언제든 업데이트)
import { ref, onMounted } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { useAppUpdateStore } from '../stores/appUpdate'

const store = useAppUpdateStore()
const applying = ref(false)

const apply = () => {
  applying.value = true
  store.apply()
}

onMounted(() => store.start())
</script>

<style scoped>
.update-banner {
  position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom)); transform: translateX(-50%);
  z-index: 1500; width: min(440px, calc(100vw - 32px)); box-sizing: border-box;
  display: flex; align-items: center; gap: 10px; padding: 12px 8px 12px 14px;
  background: #fff; border: 1px solid var(--c-line-soft); border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12), 0 1px 3px rgba(15, 23, 42, 0.06);
  font-family: 'Pretendard', sans-serif;
}
.icon { flex: none; color: var(--c-primary); }
.text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.text strong { font-size: 0.86rem; font-weight: 800; color: var(--c-text); }
.text span { font-size: 0.74rem; color: var(--c-muted); }
.btn { flex: none; }
.close {
  flex: none; width: 26px; height: 26px; border: none; background: none; border-radius: 6px;
  cursor: pointer; color: var(--c-faint); font-size: 1.05rem; line-height: 1;
}
.close:hover { color: var(--c-text-2); background: var(--c-line-soft); }

.banner-enter-active, .banner-leave-active { transition: opacity 0.2s, transform 0.2s; }
.banner-enter-from, .banner-leave-to { opacity: 0; transform: translate(-50%, 12px); }
</style>
