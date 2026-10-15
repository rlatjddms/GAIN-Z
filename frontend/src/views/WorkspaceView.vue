<template>
  <div class="workspace-app">
    <AppHeader>
      <div class="page-pill">SOLO · WORKSPACE</div>
    </AppHeader>

    <div class="ws-layout">
      <!-- 왼쪽 메뉴 (좁은 화면에서는 위쪽 가로 메뉴) -->
      <aside class="ws-side">
        <nav class="ws-nav">
          <router-link
            v-for="item in NAV"
            :key="item.to"
            :to="item.to"
            class="nav-item"
            :class="{ active: isActive(item) }"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.caption }}</small>
            </span>
          </router-link>
        </nav>

        <div class="level-chip" :style="{ '--accent': tier.accent }">
          <div class="level-row">
            <strong>Lv.{{ level }}</strong>
            <span>{{ tier.name }}</span>
          </div>
          <div class="exp-bar"><div class="exp-fill" :style="{ width: `${progress}%` }"></div></div>
        </div>
      </aside>

      <main class="ws-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppHeader from '../components/AppHeader.vue'
import { clampLevel, getLevelTier, getExpProgress } from '../constants/levelTier'

// 각 메뉴는 /workspace 아래 자식 라우트. 기능 이슈들이 각 화면을 채운다
const NAV = [
  { to: '/workspace', label: '오늘', caption: '연습 · 루틴', icon: '🎸', exact: true },
  { to: '/workspace/log', label: '기록', caption: '연습 잔디', icon: '🌱' },
  { to: '/workspace/sheets', label: '악보', caption: '보관함 · AI 악보', icon: '📂' },
  { to: '/workspace/picks', label: '피크', caption: '컬렉션', icon: '🔺' },
]

const route = useRoute()
const authStore = useAuthStore()
const isActive = (item) => (item.exact ? route.path === item.to : route.path.startsWith(item.to))

const level = computed(() => clampLevel(authStore.character?.level ?? 1))
const tier = computed(() => getLevelTier(level.value))
const progress = computed(() => getExpProgress(level.value, authStore.character?.exp))
</script>

<style scoped>
* { box-sizing: border-box; }

.workspace-app {
  width: 100%; min-height: 100vh;
  background: #f8f9fa; color: #1a1a1a;
  font-family: 'Pretendard', sans-serif;
}
.page-pill {
  background: #f1f3f5; color: #2d5bff; font-weight: 800; font-size: 0.8rem;
  padding: 8px 18px; border-radius: 12px; letter-spacing: 0.5px; white-space: nowrap;
}

.ws-layout {
  max-width: 1040px; margin: 0 auto; padding: 32px 20px 48px;
  display: grid; grid-template-columns: 200px minmax(0, 1fr); gap: 24px; align-items: start;
}

/* 왼쪽 메뉴 */
.ws-side { position: sticky; top: 88px; display: flex; flex-direction: column; gap: 12px; }
.ws-nav {
  background: #fff; border: 1px solid #e9ecef; border-radius: 20px; padding: 8px;
  display: flex; flex-direction: column; gap: 2px;
}
.nav-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 14px;
  text-decoration: none; color: #495057; transition: background 0.15s, color 0.15s;
}
.nav-item:hover { background: #f8f9fa; }
.nav-item.active { background: #f0f4ff; color: #2d5bff; }
.nav-icon {
  flex: none; width: 34px; height: 34px; border-radius: 10px; background: #f1f3f5;
  display: flex; align-items: center; justify-content: center; font-size: 1rem;
}
.nav-item.active .nav-icon { background: #fff; }
.nav-text { display: flex; flex-direction: column; min-width: 0; }
.nav-text strong { font-size: 0.9rem; font-weight: 800; }
.nav-text small { font-size: 0.7rem; color: #adb5bd; white-space: nowrap; }
.nav-item.active .nav-text small { color: #7b96ff; }

.level-chip { background: #fff; border: 1px solid #e9ecef; border-radius: 16px; padding: 12px 14px; }
.level-row { display: flex; align-items: baseline; gap: 8px; margin-bottom: 8px; }
.level-row strong { font-size: 0.85rem; font-weight: 900; color: var(--accent); }
.level-row span { font-size: 0.7rem; font-weight: 700; color: #868e96; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.exp-bar { height: 6px; background: #f1f3f5; border-radius: 4px; overflow: hidden; }
.exp-fill { height: 100%; background: var(--accent); border-radius: 4px; }

.ws-main { min-width: 0; }

/* 좁은 화면: 메뉴를 위쪽 가로 탭으로 */
@media (max-width: 760px) {
  .ws-layout { grid-template-columns: minmax(0, 1fr); padding: 16px 16px 40px; gap: 16px; }
  .ws-side { position: static; }
  .ws-nav { flex-direction: row; padding: 6px; }
  .nav-item { flex: 1; flex-direction: column; gap: 4px; padding: 8px 4px; }
  .nav-icon { width: 28px; height: 28px; font-size: 0.9rem; }
  .nav-text { align-items: center; }
  .nav-text strong { font-size: 0.8rem; }
  .nav-text small, .level-chip { display: none; }
}
</style>
