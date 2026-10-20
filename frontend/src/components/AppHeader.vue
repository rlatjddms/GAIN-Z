<template>
  <header class="main-header">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/home" class="logo" aria-label="GAIN-Z 홈"><AppLogo :pixel="3" /></router-link>
      </div>

      <div class="header-center">
        <slot />
      </div>

      <div class="header-right">
        <NotificationBell v-if="authStore.isAuthenticated" />
        <!-- 내 프로필: 누르면 마이페이지·친구·로그아웃 메뉴 -->
        <div ref="menuRoot" class="profile-menu">
          <button class="user-info" :class="{ on: menuOpen || onMyPages }" :aria-expanded="menuOpen" aria-haspopup="menu" @click="menuOpen = !menuOpen">
            <ProfileAvatar :src="authStore.profile?.profileImageUrl" :size="32" />
            <span class="name">{{ authStore.displayName }}</span>
            <svg class="caret" viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
          </button>
          <transition name="menu">
            <div v-if="menuOpen" class="menu" role="menu">
              <router-link to="/mypage" class="menu-item" active-class="on" role="menuitem">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                마이페이지
              </router-link>
              <router-link to="/friends" class="menu-item" active-class="on" role="menuitem">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                친구 목록
              </router-link>
              <div class="menu-divider"></div>
              <button class="menu-item logout" role="menuitem" @click="handleLogout">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                로그아웃
              </button>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import ProfileAvatar from './ProfileAvatar.vue'
import AppLogo from './AppLogo.vue'
import NotificationBell from './NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

/* ---------- 프로필 메뉴 ---------- */
const menuOpen = ref(false)
const menuRoot = ref(null)
const onMyPages = computed(() => ['/mypage', '/friends'].includes(route.path))

// 메뉴 밖을 누르거나 Esc, 다른 화면으로 가면 닫는다
const onDocClick = (e) => {
  if (menuOpen.value && !menuRoot.value?.contains(e.target)) menuOpen.value = false
}
const onKey = (e) => { if (e.key === 'Escape') menuOpen.value = false }
watch(() => route.fullPath, () => { menuOpen.value = false })
onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})

// 서버에 저장된 이름으로 맞춘다 (소셜 가입자는 그전까지 이메일 앞부분이 보인다)
onMounted(() => {
  if (authStore.isAuthenticated && !authStore.profile) authStore.fetchProfile()
})

const handleLogout = async () => {
  menuOpen.value = false
  await authStore.logout()
  router.push('/')
}
</script>

<style scoped>
button { -webkit-appearance: none; appearance: none; outline: none; }

.main-header {
  background: #fff; border-bottom: 1px solid var(--c-line);
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
.logo { display: inline-flex; align-items: center; text-decoration: none; }

.profile-menu { position: relative; }
.user-info {
  display: flex; align-items: center; gap: 8px; padding: 4px 10px 4px 4px;
  border: none; background: none; cursor: pointer; font-family: inherit;
  border-radius: 999px; transition: background 0.15s;
}
.user-info:hover, .user-info.on { background: var(--c-primary-soft); }
.user-info.on .name, .user-info.on .caret { color: var(--c-primary-text); }
.caret { color: var(--c-faint); transition: transform 0.15s; }
.user-info[aria-expanded="true"] .caret { transform: rotate(180deg); }

.menu {
  position: absolute; top: calc(100% + 8px); right: 0; min-width: 168px; z-index: 1100;
  background: #fff; border: 1px solid var(--c-line); border-radius: 14px; padding: 6px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
}
.menu-item {
  display: flex; align-items: center; gap: 10px; width: 100%; box-sizing: border-box;
  padding: 9px 12px; border: none; background: none; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 0.85rem; font-weight: 700; color: var(--c-text-2); text-decoration: none; text-align: left;
}
.menu-item svg { color: var(--c-faint); flex: none; }
.menu-item:hover, .menu-item.on { background: var(--c-primary-soft); color: var(--c-primary-text); }
.menu-item:hover svg, .menu-item.on svg { color: var(--c-primary-text); }
.menu-item.logout:hover { background: var(--c-danger-soft); color: #fa5252; }
.menu-item.logout:hover svg { color: #fa5252; }
.menu-divider { height: 1px; background: var(--c-line-soft); margin: 4px 6px; }
.menu-enter-active, .menu-leave-active { transition: opacity 0.12s, transform 0.12s; }
.menu-enter-from, .menu-leave-to { opacity: 0; transform: translateY(-4px); }
.name { font-weight: 700; font-size: 0.9rem; color: var(--c-text-2); }
@media (max-width: 640px) {
  .header-inner { padding: 0 16px; }
  .user-info { padding-right: 6px; }
  .name { display: none; }
}
</style>
