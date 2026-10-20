<template>
  <!-- 새 알림 팝업: 화면 오른쪽 위에 잠깐 떴다 사라진다. 누르면 그 화면으로 -->
  <div class="toasts" aria-live="polite">
    <transition-group name="toast">
      <div v-for="t in store.toasts" :key="t.key" class="toast" role="status">
        <button class="toast-main" @click="open(t)">
          <NotificationIcon :type="t.notification.type" :size="34" />
          <span class="text">
            <span class="title">{{ t.notification.title }}</span>
            <span class="content">{{ t.notification.content }}</span>
          </span>
        </button>
        <button class="close" aria-label="알림 닫기" @click="store.dismiss(t.key)">×</button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import NotificationIcon from './NotificationIcon.vue'
import { useNotificationStore } from '../stores/notification'
import { markNotificationRead } from '../api/notification'

const router = useRouter()
const store = useNotificationStore()

const open = (t) => {
  store.dismiss(t.key)
  const n = t.notification
  if (!n.read) {
    n.read = true
    store.setUnread(store.unreadCount - 1)
    markNotificationRead(n.id).catch(() => {})
  }
  if (n.redirectUrl) router.push(n.redirectUrl)
}
</script>

<style scoped>
.toasts {
  position: fixed; top: 76px; right: 20px; z-index: 3000; width: 340px; max-width: calc(100vw - 32px);
  display: flex; flex-direction: column; gap: 10px; pointer-events: none;
  font-family: 'Pretendard', sans-serif;
}
.toast {
  pointer-events: auto; display: flex; align-items: flex-start; background: #fff; border: 1px solid #f1f3f5;
  border-radius: 16px; box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10), 0 1px 3px rgba(15, 23, 42, 0.06);
  overflow: hidden; transition: box-shadow 0.15s, transform 0.15s;
}
.toast:hover { transform: translateY(-1px); box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14), 0 1px 3px rgba(15, 23, 42, 0.06); }
.toast-main {
  flex: 1; min-width: 0; display: flex; gap: 12px; padding: 12px 6px 12px 12px; border: none; background: none;
  cursor: pointer; text-align: left; font-family: inherit;
}
.toast-main:hover .content { color: var(--c-primary-text); }
.text { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.title { font-size: 0.72rem; font-weight: 800; color: #868e96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content {
  font-size: 0.85rem; font-weight: 800; color: #1a1a1a; line-height: 1.4; overflow-wrap: anywhere;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.close {
  flex: none; margin: 8px 8px 0 0; width: 22px; height: 22px; border: none; background: none; border-radius: 6px;
  cursor: pointer; color: #adb5bd; font-size: 1rem; line-height: 1;
}
.close:hover { color: #495057; background: #f1f3f5; }

.toast-enter-active, .toast-leave-active { transition: opacity 0.2s, transform 0.2s; }
.toast-enter-from { opacity: 0; transform: translateX(24px); }
.toast-leave-to { opacity: 0; transform: translateX(24px); }

@media (max-width: 640px) {
  .toasts { top: 72px; right: 16px; left: 16px; width: auto; max-width: none; }
}
</style>
