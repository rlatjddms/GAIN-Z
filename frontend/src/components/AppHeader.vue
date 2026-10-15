<template>
  <header class="main-header">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/home" class="logo">GAIN-Z</router-link>
      </div>

      <div class="header-center">
        <slot />
      </div>

      <div class="header-right">
        <button class="noti-btn" title="알림">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
          </svg>
        </button>
        <div class="user-info">
          <span class="avatar"></span>
          <span class="name">{{ authStore.displayName }}</span>
        </div>
        <button class="logout-icon-btn" title="로그아웃" @click="handleLogout">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = async () => {
  await authStore.logout()
  router.push('/')
}
</script>

<style scoped>
button { -webkit-appearance: none; appearance: none; outline: none; }

.main-header {
  background: #fff; border-bottom: 1px solid #e9ecef;
  position: sticky; top: 0; z-index: 1000;
}
.header-inner {
  max-width: 1040px; margin: 0 auto; padding: 0 24px;
  height: 64px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
}
.header-left { display: flex; justify-content: flex-start; }
.header-center { display: flex; justify-content: center; }
.header-right { display: flex; justify-content: flex-end; align-items: center; gap: 12px; }
.logo { font-size: 1.4rem; font-weight: 900; color: #2d5bff; letter-spacing: -1px; text-decoration: none; }

.user-info { display: flex; align-items: center; gap: 8px; padding-right: 12px; border-right: 1px solid #eee; }
.avatar { width: 30px; height: 30px; background: #e9ecef; border-radius: 50%; border: 1.5px solid #fff; box-shadow: 0 0 0 1px #eee; }
.name { font-weight: 700; font-size: 0.9rem; color: #495057; }
.noti-btn, .logout-icon-btn {
  background: none; border: none; color: #adb5bd; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  padding: 6px; border-radius: 8px; transition: 0.2s;
}
.noti-btn:hover { color: #2d5bff; background: #f0f4ff; }
.logout-icon-btn:hover { color: #fa5252; background: #fff5f5; }

@media (max-width: 640px) {
  .header-inner { padding: 0 16px; }
  .user-info { display: none; }
}
</style>
