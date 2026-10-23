<template>
  <!-- 헤더 알림 종: 안 읽은 개수 배지, 누르면 최근 알림 미리보기 (정리는 알림 페이지에서) -->
  <div ref="root" class="noti">
    <button class="noti-btn" :class="{ on: open }" :title="unread ? `안 읽은 알림 ${unread}개` : '알림'"
            aria-haspopup="dialog" :aria-expanded="open" @click="toggle">
      <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
      </svg>
      <span v-if="unread" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
    </button>

    <transition name="panel">
      <section v-if="open" class="panel" role="dialog" aria-label="알림">
        <header class="panel-head">
          <strong>알림<span v-if="unread" class="head-count">{{ unread }}</span></strong>
          <button class="text-btn" :disabled="!unread || busy" @click="readAll">모두 읽음</button>
        </header>

        <div class="list">
          <p v-if="error" class="state error">{{ error }}</p>
          <p v-else-if="!loaded" class="state">불러오는 중…</p>
          <p v-else-if="!items.length" class="state">새 알림이 없어요</p>
          <section v-for="g in groups" v-else :key="g.label" class="group">
            <h3>{{ g.label }}</h3>
            <ul>
              <li v-for="n in g.items" :key="n.id" class="item" :class="{ unread: !n.read }">
                <button class="item-main" @click="openItem(n)">
                  <NotificationIcon :type="n.type" :size="30" />
                  <span class="text">
                    <span class="title">{{ n.title }}</span>
                    <span class="content">{{ n.content }}</span>
                    <span class="time">{{ agoText(n.createdAt) }}</span>
                  </span>
                </button>
                <button class="remove" title="알림 지우기" aria-label="알림 지우기" @click="remove(n)">×</button>
              </li>
            </ul>
          </section>
        </div>

        <router-link to="/notifications" class="see-all">
          전체 보기{{ hasMore ? ' · 더 있어요' : '' }}
          <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
        </router-link>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import NotificationIcon from './NotificationIcon.vue'
import { useNotificationStore } from '../stores/notification'
import { getNotifications, markNotificationRead, markAllNotificationsRead, deleteNotification } from '../api/notification'
import { apiErrorMessage } from '../api/practice'
import { agoText, groupByDate } from '../constants/notification'

// 미리보기는 최근 몇 개만 (나머지는 알림 페이지)
const PREVIEW_SIZE = 8
// 실시간 연결이 끊긴 동안만 쓰는 예비 확인 간격
const POLL_MS = 60000

const router = useRouter()
const route = useRoute()
const store = useNotificationStore()
const unread = computed(() => store.unreadCount)

const root = ref(null)
const open = ref(false)
const items = ref([])
const hasMore = ref(false)
const loaded = ref(false)
const busy = ref(false)
const error = ref('')

const groups = computed(() => groupByDate(items.value))

const toggle = async () => {
  open.value = !open.value
  if (!open.value) return
  loaded.value = false
  error.value = ''
  try {
    const result = await getNotifications({ page: 0, size: PREVIEW_SIZE })
    items.value = result.items
    hasMore.value = result.hasNext
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 불러오지 못했어요.')
  } finally {
    loaded.value = true
  }
  store.refreshUnread()
}

const openItem = (n) => {
  if (!n.read) {
    n.read = true
    store.setUnread(store.unreadCount - 1)
    markNotificationRead(n.id).catch(() => {})
  }
  open.value = false
  if (n.redirectUrl) router.push(n.redirectUrl)
}

const remove = async (n) => {
  try {
    await deleteNotification(n.id)
    items.value = items.value.filter((x) => x.id !== n.id)
    if (!n.read) store.setUnread(store.unreadCount - 1)
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 지우지 못했어요.')
  }
}

const readAll = async () => {
  busy.value = true
  try {
    await markAllNotificationsRead()
    items.value.forEach((n) => { n.read = true })
    store.setUnread(0)
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 읽음으로 바꾸지 못했어요.')
  } finally {
    busy.value = false
  }
}

/* ---------- 열고 닫기·배지 갱신 ---------- */
const onDocClick = (e) => { if (open.value && !root.value?.contains(e.target)) open.value = false }
const onKey = (e) => { if (e.key === 'Escape') open.value = false }

// 새 알림은 실시간 연결(App.vue)로 바로 온다. 알림함이 열려 있으면 맨 위에 붙인다
watch(() => store.latest, (n) => {
  if (!n || !open.value || items.value.some((x) => x.id === n.id)) return
  items.value = [n, ...items.value].slice(0, PREVIEW_SIZE)
})

// 연결이 끊긴 동안에는 화면을 옮길 때와 탭이 보이는 동안 1분마다 개수를 다시 센다
let timer = null
const poll = () => { if (!store.connected && document.visibilityState === 'visible') store.refreshUnread() }
watch(() => route.fullPath, () => { open.value = false; if (!store.connected) store.refreshUnread() })

onMounted(() => {
  if (!store.connected) store.refreshUnread()
  timer = setInterval(poll, POLL_MS)
  document.addEventListener('visibilitychange', poll)
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  document.removeEventListener('visibilitychange', poll)
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.noti { position: relative; }
/* 크기만 프로필 버튼(사진 32px + 둘레 4px = 40px)과 같게. 모양은 투명 바탕·연한 글자색 그대로 */
.noti-btn {
  position: relative; background: none; border: none; color: var(--c-faint); cursor: pointer;
  display: flex; align-items: center; justify-content: center; box-sizing: border-box;
  width: 40px; height: 40px; padding: 0; border-radius: 8px; transition: 0.2s;
}
.noti-btn:hover, .noti-btn.on { color: var(--c-primary-text); background: var(--c-primary-soft); }
.badge {
  position: absolute; top: 0; right: 0; transform: translate(30%, -20%);
  min-width: 16px; height: 16px; box-sizing: border-box; padding: 0 4px; border-radius: 999px;
  background: #fa5252; color: #fff; font-size: 0.6rem; font-weight: 900; line-height: 16px; text-align: center;
  box-shadow: 0 0 0 2px #fff;
}

.panel {
  position: absolute; top: calc(100% + 8px); right: -8px; z-index: 1100; width: 360px; max-width: calc(100vw - 24px);
  background: #fff; border: 1px solid var(--c-line); border-radius: 18px; box-shadow: 0 16px 40px rgba(15, 23, 42, 0.16);
  overflow: hidden; font-family: 'Pretendard', sans-serif;
}
.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px 10px; }
.panel-head strong { display: inline-flex; align-items: center; gap: 6px; font-size: 0.95rem; font-weight: 900; color: var(--c-text); }
.head-count { min-width: 18px; padding: 0 6px; border-radius: 999px; background: #fa5252; color: #fff; font-size: 0.65rem; line-height: 18px; text-align: center; }
.text-btn { border: none; background: none; padding: 0; cursor: pointer; font-family: inherit; font-size: 0.75rem; font-weight: 800; color: var(--c-primary-text); }
.text-btn:disabled { opacity: 0.4; cursor: default; }

.list { max-height: 420px; overflow-y: auto; padding: 0 6px 6px; }
.group h3 { margin: 6px 0 2px; padding: 0 10px; font-size: 0.68rem; font-weight: 800; color: var(--c-faint); }
ul { list-style: none; margin: 0; padding: 0; }
.item { display: flex; align-items: flex-start; border-radius: 12px; }
.item:hover { background: var(--c-bg); }
.item.unread { background: var(--c-primary-soft); }
.item.unread:hover { background: var(--c-primary-soft); }
.item-main {
  flex: 1; min-width: 0; display: flex; gap: 10px; padding: 10px 4px 10px 10px; border: none; background: none;
  cursor: pointer; text-align: left; font-family: inherit;
}
.text { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.title { font-size: 0.7rem; font-weight: 800; color: var(--c-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content {
  font-size: 0.82rem; font-weight: 700; color: #343a40; line-height: 1.4; overflow-wrap: anywhere;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.item.unread .content { font-weight: 800; color: var(--c-text); }
.time { font-size: 0.68rem; font-weight: 700; color: var(--c-faint); }
.item.unread .time::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: var(--c-primary); margin-right: 5px; vertical-align: 1px; }
.remove {
  flex: none; margin: 8px 6px 0 0; width: 22px; height: 22px; border: none; background: none; border-radius: 6px;
  cursor: pointer; color: #ced4da; font-size: 1rem; line-height: 1; opacity: 0; transition: 0.15s;
}
.item:hover .remove, .remove:focus-visible { opacity: 1; }
.remove:hover { color: var(--c-text-2); background: var(--c-line); }

.see-all {
  display: flex; align-items: center; justify-content: center; gap: 4px; padding: 12px; border-top: 1px solid var(--c-line-soft);
  font-size: 0.8rem; font-weight: 800; color: var(--c-text-2); text-decoration: none;
}
.see-all:hover { background: var(--c-bg); color: var(--c-primary-text); }

.state { margin: 0; padding: 28px 16px; text-align: center; font-size: 0.8rem; font-weight: 700; color: var(--c-faint); }
.state.error { color: var(--c-danger); }

.panel-enter-active, .panel-leave-active { transition: opacity 0.12s, transform 0.12s; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-4px); }

@media (hover: none) {
  .remove { opacity: 1; }
}
/* 휴대폰: 종 아래가 아니라 화면 폭에 맞춰 헤더 바로 아래에 */
@media (max-width: 640px) {
  /* 휴대폰: 헤더 오른쪽 버튼은 프로필 버튼과 같은 34px */
  .noti-btn { width: 34px; height: 34px; }
  .noti-btn svg { width: 19px; height: 19px; }
  .panel { position: fixed; top: 64px; left: 12px; right: 12px; width: auto; max-width: none; max-height: calc(100dvh - 80px); }
}
</style>
