<script setup>
import { watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useNotificationStore } from './stores/notification'
import NotificationToasts from './components/NotificationToasts.vue'
import RecordAcquiredModal from './components/RecordAcquiredModal.vue'
import PracticeTimerGuard from './components/workspace/PracticeTimerGuard.vue'

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
</template>


<style scoped>

header {
  background-color: #f8f9fa;
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

#app {
  width: 100%;
  height: 100%;
}

input::placeholder, textarea::placeholder {
  color: #adb5bd;
  opacity: 1;
  font-weight: 400;
}
</style>
