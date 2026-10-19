<template>
  <SubPageLayout pill="NOTIFICATIONS">
    <div class="page">
      <div class="page-head">
        <h2>알림</h2>
        <p>읽은 알림은 30일이 지나면 자동으로 지워져요</p>
        <router-link to="/mypage#notification-settings" class="settings-link" title="알림 설정">
          <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          알림 설정
        </router-link>
      </div>

      <section class="card">
        <!-- 걸러 보기 탭 -->
        <div class="tabs" role="tablist" aria-label="알림 걸러 보기">
          <button v-for="t in TABS" :key="t.key" role="tab" :aria-selected="tab === t.key" :class="{ on: tab === t.key }"
                  @click="tab = t.key">
            {{ t.label }}<span v-if="t.key === 'UNREAD' && store.unreadCount" class="tab-count">{{ store.unreadCount }}</span>
          </button>
        </div>

        <!-- 도구 줄: 평소엔 정리 버튼, 고르는 중엔 선택 동작 -->
        <div class="toolbar">
          <template v-if="!selecting">
            <button class="tool" :disabled="!items.length" @click="startSelect">선택</button>
            <span class="spacer"></span>
            <button class="tool" :disabled="!store.unreadCount || busy" @click="readAll">모두 읽음</button>
            <button class="tool muted" :disabled="busy" @click="clearRead">읽은 알림 지우기</button>
          </template>
          <template v-else>
            <label class="check-all">
              <input type="checkbox" :checked="allChecked" :indeterminate.prop="someChecked" @change="toggleAll">
              <span>{{ selected.size ? `${selected.size}개 선택` : '전체 선택' }}</span>
            </label>
            <span class="spacer"></span>
            <button class="tool" :disabled="!selected.size || busy" @click="readSelected">읽음</button>
            <button class="tool danger" :disabled="!selected.size || busy" @click="deleteSelected">지우기</button>
            <button class="tool muted" :disabled="busy" @click="stopSelect">취소</button>
          </template>
        </div>
        <p v-if="error" class="error">{{ error }}</p>

        <!-- 목록 (날짜별 묶음) -->
        <p v-if="!loaded" class="state">불러오는 중…</p>
        <p v-else-if="!items.length" class="state">{{ tab === 'UNREAD' ? '안 읽은 알림이 없어요' : '알림이 없어요' }}</p>
        <template v-else>
          <section v-for="g in groups" :key="g.label" class="group">
            <h3>{{ g.label }}</h3>
            <ul>
              <li v-for="n in g.items" :key="n.id" class="item" :class="{ unread: !n.read, checked: selected.has(n.id) }">
                <input v-if="selecting" class="check" type="checkbox" :checked="selected.has(n.id)"
                       :aria-label="`${n.content} 고르기`" @change="toggle(n.id)">
                <button class="item-main" @click="selecting ? toggle(n.id) : openItem(n)">
                  <NotificationIcon :type="n.type" :size="36" />
                  <span class="text">
                    <span class="title">{{ n.title }}</span>
                    <span class="content">{{ n.content }}</span>
                  </span>
                  <span class="time">{{ agoText(n.createdAt) }}</span>
                </button>
                <button v-if="!selecting" class="remove" title="알림 지우기" aria-label="알림 지우기" @click="removeOne(n)">×</button>
              </li>
            </ul>
          </section>
          <button v-if="hasNext" class="more" :disabled="loadingMore" @click="loadMore">{{ loadingMore ? '불러오는 중…' : '더 보기' }}</button>
        </template>
      </section>
    </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import SubPageLayout from '../components/workspace/SubPageLayout.vue'
import NotificationIcon from '../components/NotificationIcon.vue'
import { useNotificationStore } from '../stores/notification'
import {
  getNotifications, markNotificationRead, markNotificationsRead, markAllNotificationsRead,
  deleteNotification, deleteNotifications, deleteReadNotifications,
} from '../api/notification'
import { apiErrorMessage } from '../api/practice'
import { NOTIFICATION_CATEGORIES, agoText, groupByDate } from '../constants/notification'

const PAGE_SIZE = 30
const TABS = [{ key: 'ALL', label: '전체' }, { key: 'UNREAD', label: '안 읽음' },
  ...NOTIFICATION_CATEGORIES.map((c) => ({ key: c.value, label: c.label }))]

const router = useRouter()
const store = useNotificationStore()

const tab = ref('ALL')
const items = ref([])
const page = ref(0)
const hasNext = ref(false)
const loaded = ref(false)
const loadingMore = ref(false)
const busy = ref(false)
const error = ref('')

const groups = computed(() => groupByDate(items.value))

/* ---------- 불러오기 ---------- */
const filter = () => ({
  category: ['ALL', 'UNREAD'].includes(tab.value) ? null : tab.value,
  unread: tab.value === 'UNREAD',
})

let token = 0
const reload = async () => {
  const mine = ++token
  loaded.value = false
  error.value = ''
  try {
    const result = await getNotifications({ page: 0, size: PAGE_SIZE, ...filter() })
    if (mine !== token) return
    items.value = result.items
    page.value = 0
    hasNext.value = result.hasNext
  } catch (e) {
    if (mine === token) error.value = apiErrorMessage(e, '알림을 불러오지 못했어요.')
  } finally {
    if (mine === token) loaded.value = true
  }
  store.refreshUnread()
}

const loadMore = async () => {
  loadingMore.value = true
  try {
    const result = await getNotifications({ page: page.value + 1, size: PAGE_SIZE, ...filter() })
    items.value = [...items.value, ...result.items]
    page.value += 1
    hasNext.value = result.hasNext
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 더 불러오지 못했어요.')
  } finally {
    loadingMore.value = false
  }
}


/* ---------- 하나씩 ---------- */
const openItem = (n) => {
  if (!n.read) {
    n.read = true
    store.setUnread(store.unreadCount - 1)
    markNotificationRead(n.id).catch(() => {})
  }
  if (n.redirectUrl) router.push(n.redirectUrl)
}

const removeOne = async (n) => {
  try {
    await deleteNotification(n.id)
    items.value = items.value.filter((x) => x.id !== n.id)
    if (!n.read) store.setUnread(store.unreadCount - 1)
  } catch (e) {
    error.value = apiErrorMessage(e, '알림을 지우지 못했어요.')
  }
}

/* ---------- 골라서 ---------- */
const selecting = ref(false)
const selected = ref(new Set())
const allChecked = computed(() => items.value.length > 0 && selected.value.size === items.value.length)
const someChecked = computed(() => selected.value.size > 0 && !allChecked.value)

const startSelect = () => { selecting.value = true; selected.value = new Set() }
const stopSelect = () => { selecting.value = false; selected.value = new Set() }
const toggle = (id) => {
  const next = new Set(selected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}
const toggleAll = () => {
  selected.value = allChecked.value ? new Set() : new Set(items.value.map((n) => n.id))
}

const run = async (action, fallback) => {
  busy.value = true
  error.value = ''
  try {
    await action()
  } catch (e) {
    error.value = apiErrorMessage(e, fallback)
  } finally {
    busy.value = false
  }
}

const readSelected = () => run(async () => {
  const ids = [...selected.value]
  await markNotificationsRead(ids)
  items.value.forEach((n) => { if (selected.value.has(n.id)) n.read = true })
  if (tab.value === 'UNREAD') items.value = items.value.filter((n) => !n.read)
  stopSelect()
  store.refreshUnread()
}, '읽음으로 바꾸지 못했어요.')

const deleteSelected = () => run(async () => {
  const ids = [...selected.value]
  if (!window.confirm(`알림 ${ids.length}개를 지울까요?`)) return
  await deleteNotifications(ids)
  items.value = items.value.filter((n) => !selected.value.has(n.id))
  stopSelect()
  store.refreshUnread()
}, '알림을 지우지 못했어요.')

/* ---------- 한꺼번에 ---------- */
const readAll = () => run(async () => {
  await markAllNotificationsRead()
  items.value.forEach((n) => { n.read = true })
  if (tab.value === 'UNREAD') items.value = []
  store.setUnread(0)
}, '읽음으로 바꾸지 못했어요.')

const clearRead = () => run(async () => {
  if (!window.confirm('읽은 알림을 모두 지울까요? 안 읽은 알림은 남아요.')) return
  await deleteReadNotifications()
  await reload()
}, '알림을 지우지 못했어요.')

// 탭을 바꾸면 고르기를 끝내고 다시 불러온다 (처음 열 때도). 위의 함수들을 쓰므로 맨 아래에 둔다
watch(tab, () => { stopSelect(); reload() }, { immediate: true })
</script>

<style scoped>
.page { max-width: 720px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }
.settings-link {
  margin-left: auto; align-self: center; display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 11px; border: 1px solid #e9ecef; border-radius: 10px; background: #fff;
  font-size: 0.75rem; font-weight: 800; color: #495057; text-decoration: none;
}
.settings-link:hover { border-color: #2d5bff; color: #2d5bff; }

.card { background: #fff; border: 1px solid #e9ecef; border-radius: 20px; padding: 18px 20px 20px; }

.tabs { display: flex; gap: 4px; overflow-x: auto; padding-bottom: 12px; border-bottom: 1px solid #f1f3f5; }
.tabs button {
  flex: none; display: inline-flex; align-items: center; gap: 5px; border: none; background: none; cursor: pointer;
  padding: 7px 12px; border-radius: 999px; font-family: inherit; font-size: 0.8rem; font-weight: 800; color: #868e96;
}
.tabs button:hover { background: #f8f9fa; color: #495057; }
.tabs button.on { background: #2d5bff; color: #fff; }
.tab-count { min-width: 16px; padding: 0 5px; border-radius: 999px; background: #fa5252; color: #fff; font-size: 0.65rem; line-height: 16px; text-align: center; }
.tabs button.on .tab-count { background: #fff; color: #2d5bff; }

.toolbar { display: flex; align-items: center; gap: 6px; padding: 10px 0; }
.spacer { flex: 1; }
.tool {
  border: 1px solid #e9ecef; background: #fff; border-radius: 10px; padding: 6px 11px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #495057; white-space: nowrap;
}
.tool:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.tool.muted { color: #868e96; }
.tool.danger { color: #e03131; }
.tool.danger:hover:not(:disabled) { border-color: #ffc9c9; background: #fff5f5; color: #e03131; }
.tool:disabled { opacity: 0.45; cursor: not-allowed; }
.check-all { display: inline-flex; align-items: center; gap: 8px; font-size: 0.8rem; font-weight: 800; color: #495057; cursor: pointer; }
.check-all input, .check { width: 16px; height: 16px; accent-color: #2d5bff; cursor: pointer; }

.group + .group { margin-top: 8px; }
.group h3 { margin: 8px 0 4px; padding: 0 4px; font-size: 0.72rem; font-weight: 800; color: #adb5bd; }
ul { list-style: none; margin: 0; padding: 0; }
.item { display: flex; align-items: center; gap: 4px; border-radius: 14px; padding-left: 6px; }
.item:hover { background: #f8f9fa; }
.item.unread { background: #f5f8ff; }
.item.unread:hover { background: #edf2ff; }
.item.checked { background: #e7efff; }
.check { flex: none; margin-left: 4px; }
.item-main {
  flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; padding: 10px 6px; border: none; background: none;
  cursor: pointer; text-align: left; font-family: inherit;
}
.text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.title { font-size: 0.72rem; font-weight: 800; color: #868e96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content { font-size: 0.86rem; font-weight: 700; color: #343a40; line-height: 1.45; overflow-wrap: anywhere; }
.item.unread .content { font-weight: 800; color: #1a1a1a; }
.time { flex: none; align-self: flex-start; margin-top: 2px; font-size: 0.7rem; font-weight: 700; color: #adb5bd; white-space: nowrap; }
.item.unread .time::before { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #2d5bff; margin-right: 5px; vertical-align: 1px; }
.remove {
  flex: none; width: 26px; height: 26px; margin-right: 6px; border: none; background: none; border-radius: 8px;
  cursor: pointer; color: #ced4da; font-size: 1.1rem; line-height: 1; opacity: 0; transition: 0.15s;
}
.item:hover .remove, .remove:focus-visible { opacity: 1; }
.remove:hover { color: #495057; background: #e9ecef; }

.more {
  display: block; width: 100%; margin-top: 10px; padding: 10px; border: 1px dashed #dee2e6; background: none; border-radius: 12px;
  cursor: pointer; font-family: inherit; font-size: 0.8rem; font-weight: 800; color: #868e96;
}
.more:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }

.state { margin: 0; padding: 36px 16px; text-align: center; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }
.error { margin: 0 0 8px; font-size: 0.8rem; color: #e03131; font-weight: 700; }

@media (hover: none) {
  .remove { opacity: 1; }
}
</style>
