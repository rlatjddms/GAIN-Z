<script setup>
import { watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useNotificationStore } from './stores/notification'
import NotificationToasts from './components/NotificationToasts.vue'
import RecordAcquiredModal from './components/RecordAcquiredModal.vue'
import PracticeTimerGuard from './components/workspace/PracticeTimerGuard.vue'
import AppDialog from './components/ui/AppDialog.vue'
import AppToast from './components/ui/AppToast.vue'
import UpdateBanner from './components/UpdateBanner.vue'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()

// 로그인한 동안 실시간 알림 연결을 하나만 열어 둔다 (화면을 옮겨도 끊지 않는다)
watch(() => authStore.isAuthenticated, (loggedIn) => {
  if (loggedIn) notificationStore.connect()
  else notificationStore.disconnect()
}, { immediate: true })
</script>

<template>
  <router-view />
  <NotificationToasts v-if="authStore.isAuthenticated" />
  <RecordAcquiredModal v-if="authStore.isAuthenticated" />
  <PracticeTimerGuard v-if="authStore.isAuthenticated" />
  <AppDialog />
  <AppToast />
  <UpdateBanner />
</template>


<style scoped>

header {
  background-color: var(--c-bg);
  padding: 20px;
  text-align: center;
}

main {
  padding: 20px;
  text-align: center;
}
</style>

<style>
html, body {
  margin: 0 !important;
  padding: 0 !important;
  background-color: #f8f9fb;
  width: 100%;
  height: 100%;
}
/* 휴대폰에서 흰 화면인 페이지(로그인 등)는 바탕도 흰색 (#226, components/ui/useWhitePage.js) */
@media (max-width: 640px) {
  html.white-page, html.white-page body { background-color: #fff; }
}

#app {
  width: 100%;
  height: 100%;
}

input::placeholder, textarea::placeholder {
  color: var(--c-faint);
  opacity: 1;
  font-weight: 400;
}
</style>
