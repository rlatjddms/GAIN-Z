<template>
  <!-- 헤더 알림 종: 안 읽은 개수 배지, 누르면 알림함 -->
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
          <strong>알림</strong>
          <div class="head-actions">
            <button class="text-btn" :disabled="!unread || busy" @click="readAll">모두 읽음</button>
            <button class="text-btn muted" :disabled="!items.length || busy" @click="clearAll">모두 지우기</button>
          </div>
        </header>

        <div class="list" @scroll="onScroll">
          <p v-if="error" class="state error">{{ error }}</p>
          <p v-else-if="!loaded" class="state">불러오는 중…</p>
          <p v-else-if="!items.length" class="state">새 알림이 없어요</p>
          <ul v-else>
            <li v-for="n in items" :key="n.id" class="item" :class="{ unread: !n.read }">
              <button class="item-main" @click="openItem(n)">
                <span class="icon" :class="categoryOf(n.type)" aria-hidden="true">
                  <svg v-if="categoryOf(n.type) === 'friend'" viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><line x1="19" y1="8" x2="19" y2="14"></line><line x1="22" y1="11" x2="16" y2="11"></line></svg>
                  <svg v-else-if="categoryOf(n.type) === 'community'" viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                  <svg v-else-if="categoryOf(n.type) === 'jam'" viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>
                  <svg v-else viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                </span>
                <span class="text">
                  <span class="title">{{ n.title }}</span>
                  <span class="content">{{ n.content }}</span>
                  <span class="time">{{ agoText(n.createdAt) }}</span>
                </span>
              </button>
              <button class="remove" title="알림 지우기" aria-label="알림 지우기" @click="remove(n)">×</button>
            </li>
          </ul>
          <p v-if="loadingMore" class="state">불러오는 중…</p>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useNotificationStore } from '../stores/notification'
import {
  getNotifications, markNotificationRead, markAllNotificationsRead, deleteNotification, deleteAllNotifications,
} from '../api/notification'
import { apiErrorMessage } from '../api/practice'

const PAGE_SIZE = 20
const POLL_MS = 30000

const router = useRouter()
const route = useRoute()
const store = useNotificationStore()
const unread = computed(() => store.unreadCount)

const root = ref(null)
const open = ref(false)
const items = ref([])
const page = ref(0)
const hasNext = ref(false)
const loaded = ref(false)
const loadingMore = ref(false)
const busy = ref(false)
const error = ref('')

const CATEGORY = {
  FRIEND_REQUEST: 'friend', FRIEND_ACCEPT: 'friend',
  NOTICE: 'community', COMMENT: 'community', REPLY: 'community', MENTION: 'community',
  SETLIST_ASSIGN: 'jam', JAM_NEW: 'jam', JAM_TRACK: 'jam',
}
const categoryOf = (type) => CATEGORY[type] || 'band'

const agoText = (at) => {
  const d = new Date(at)
  if (Number.isNaN(d.getTime())) return ''
  const minutes = Math.floor((Date.now() - d.getTime()) / 60000)
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${minutes}분 전`
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}시간 전`
  if (minutes < 60 * 24 * 7) return `${Math.floor(minutes / (60 * 24))}일 전`
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

/* ---------- 목록 ---------- */
const loadPage = async (next) => {
  const result = await getNotifications({ page: next, size: PAGE_SIZE })
  items.value = next === 0 ? result.items : [...items.value, ...result.items]
  page.value = next
  hasNext.value = result.hasNext
}

const toggle = async () => {
  open.value = !open.value
  if (!open.value) return
  loaded.value = false
  error.value = ''
  try {
    await loadPage(0)
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 불러오지 못했어요.')
  } finally {
    loaded.value = true
  }
  store.refreshUnread()
}

// 끝까지 내리면 다음 페이지
const onScroll = async (e) => {
  const el = e.target
  if (!hasNext.value || loadingMore.value || el.scrollTop + el.clientHeight < el.scrollHeight - 40) return
  loadingMore.value = true
  try {
    await loadPage(page.value + 1)
  } catch {
    hasNext.value = false
  } finally {
    loadingMore.value = false
  }
}

/* ---------- 읽기·지우기 ---------- */
const openItem = async (n) => {
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

const clearAll = async () => {
  if (!window.confirm('알림을 모두 지울까요?')) return
  busy.value = true
  try {
    await deleteAllNotifications()
    items.value = []
    hasNext.value = false
    store.setUnread(0)
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 지우지 못했어요.')
  } finally {
    busy.value = false
  }
}

/* ---------- 열고 닫기·배지 갱신 ---------- */
const onDocClick = (e) => { if (open.value && !root.value?.contains(e.target)) open.value = false }
const onKey = (e) => { if (e.key === 'Escape') open.value = false }

// 화면을 옮길 때, 그리고 탭이 보이는 동안 30초마다 안 읽은 개수를 다시 센다
let timer = null
const poll = () => { if (document.visibilityState === 'visible') store.refreshUnread() }
watch(() => route.fullPath, () => { open.value = false; store.refreshUnread() })

onMounted(() => {
  store.refreshUnread()
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
.noti-btn {
  position: relative; background: none; border: none; color: #adb5bd; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  padding: 6px; border-radius: 8px; transition: 0.2s;
}
.noti-btn:hover, .noti-btn.on { color: #2d5bff; background: #f0f4ff; }
.badge {
  position: absolute; top: 0; right: 0; transform: translate(30%, -20%);
  min-width: 16px; height: 16px; box-sizing: border-box; padding: 0 4px; border-radius: 999px;
  background: #fa5252; color: #fff; font-size: 0.6rem; font-weight: 900; line-height: 16px; text-align: center;
  box-shadow: 0 0 0 2px #fff;
}

.panel {
  position: absolute; top: calc(100% + 8px); right: -8px; z-index: 1100; width: 340px; max-width: calc(100vw - 24px);
  background: #fff; border: 1px solid #e9ecef; border-radius: 16px; box-shadow: 0 12px 32px rgba(15, 23, 42, 0.14);
  overflow: hidden; font-family: 'Pretendard', sans-serif;
}
.panel-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid #f1f3f5; }
.panel-head strong { font-size: 0.9rem; font-weight: 900; color: #343a40; }
.head-actions { display: flex; gap: 10px; }
.text-btn { border: none; background: none; padding: 0; cursor: pointer; font-family: inherit; font-size: 0.72rem; font-weight: 800; color: #2d5bff; }
.text-btn.muted { color: #adb5bd; }
.text-btn.muted:hover:not(:disabled) { color: #e03131; }
.text-btn:disabled { opacity: 0.4; cursor: default; }

.list { max-height: 420px; overflow-y: auto; }
ul { list-style: none; margin: 0; padding: 4px; }
.item { position: relative; display: flex; align-items: flex-start; border-radius: 12px; }
.item:hover { background: #f8f9fa; }
.item.unread { background: #f5f8ff; }
.item.unread:hover { background: #edf2ff; }
.item-main {
  flex: 1; min-width: 0; display: flex; gap: 10px; padding: 10px 6px 10px 10px; border: none; background: none;
  cursor: pointer; text-align: left; font-family: inherit;
}
.icon {
  flex: none; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: #f1f3f5; color: #868e96;
}
.icon.friend { background: #ebfbee; color: #2f9e44; }
.icon.band { background: #f0f4ff; color: #2d5bff; }
.icon.community { background: #fff4e6; color: #e8590c; }
.icon.jam { background: #f3f0ff; color: #7048e8; }
.text { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.title { font-size: 0.7rem; font-weight: 800; color: #868e96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content { font-size: 0.82rem; font-weight: 700; color: #343a40; line-height: 1.4; word-break: keep-all; overflow-wrap: anywhere; }
.item.unread .content { font-weight: 800; color: #1a1a1a; }
.time { font-size: 0.68rem; font-weight: 700; color: #adb5bd; }
.item.unread .time::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #2d5bff; margin-right: 5px; vertical-align: 1px; }
.remove {
  flex: none; margin: 8px 6px 0 0; width: 22px; height: 22px; border: none; background: none; border-radius: 6px;
  cursor: pointer; color: #ced4da; font-size: 1rem; line-height: 1; opacity: 0; transition: 0.15s;
}
.item:hover .remove, .remove:focus-visible { opacity: 1; }
.remove:hover { color: #495057; background: #e9ecef; }

.state { margin: 0; padding: 28px 16px; text-align: center; font-size: 0.8rem; font-weight: 700; color: #adb5bd; }
.state.error { color: #e03131; }

.panel-enter-active, .panel-leave-active { transition: opacity 0.12s, transform 0.12s; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-4px); }

@media (hover: none) {
  .remove { opacity: 1; }
}
</style>
