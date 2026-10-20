<template>
  <!-- 새 레코드 획득 연출. 실시간 알림으로 '레코드' 알림이 오면 띄운다 (한 번에 하나씩, 여러 개면 차례로) -->
  <Teleport to="body">
    <transition name="pop">
      <div v-if="current" class="backdrop" @click.self="next">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="record-acquired-title"
                 :style="{ '--c': rarityOf(current.rarity).border }">
          <div class="rays" aria-hidden="true"></div>
          <div class="badge">NEW RECORD!</div>
          <div class="disc">
            <RecordIcon :type="rarityOf(current.rarity).type" :color="current.color" :size="112" />
          </div>
          <span class="rarity">{{ rarityOf(current.rarity).label }} · {{ rarityOf(current.rarity).rarity }}</span>
          <h2 id="record-acquired-title">{{ current.name }}</h2>
          <p class="desc">{{ current.description }}</p>
          <p class="condition">{{ current.condition }}</p>
          <div class="actions">
            <button class="btn ghost" @click="showInCollection">도감에서 보기</button>
            <button class="btn primary" @click="next">{{ queue.length > 1 ? `다음 (${queue.length - 1})` : '확인' }}</button>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import RecordIcon from './workspace/RecordIcon.vue'
import { useNotificationStore } from '../stores/notification'
import { useRecordsStore } from '../stores/records'
import { markNotificationRead } from '../api/notification'
import { rarityOf } from '../constants/records'

const router = useRouter()
const notifications = useNotificationStore()
const records = useRecordsStore()

// 보여 줄 레코드 차례 [레코드]
const queue = ref([])
const current = computed(() => queue.value[0] || null)
// 이미 보여 준 레코드 키 (같은 레코드 알림이 여러 탭·재연결로 와도 한 번만)
const shown = new Set()
// 연출을 보면 그 알림은 읽은 것으로
const pendingNotificationIds = []

watch(() => notifications.latest, async (n) => {
  if (!n || n.type !== 'RECORD') return
  pendingNotificationIds.push(n.id)
  try {
    const before = new Set(records.owned.map((r) => r.key))
    await records.load()
    // 방금 얻은 것: 이전에 없던 것, 처음 불러온 거라면 10분 안에 얻은 것
    const fresh = records.owned.filter((r) => !shown.has(r.key)
      && (before.size ? !before.has(r.key) : Date.now() - new Date(r.acquiredAt).getTime() < 10 * 60000))
    fresh.forEach((r) => shown.add(r.key))
    queue.value = [...queue.value, ...fresh.reverse()]
  } catch {
    // 도감을 못 불러와도 알림함에는 남아 있다
  }
})

const markShownRead = () => {
  while (pendingNotificationIds.length) {
    const id = pendingNotificationIds.shift()
    notifications.setUnread(notifications.unreadCount - 1)
    markNotificationRead(id).catch(() => {})
  }
}

const next = () => {
  queue.value = queue.value.slice(1)
  if (!queue.value.length) markShownRead()
}

const showInCollection = () => {
  const key = current.value.key
  queue.value = []
  markShownRead()
  router.push({ path: '/home', query: { records: key } })
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2500; background: rgba(15, 23, 42, 0.55);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  position: relative; overflow: hidden; width: 100%; max-width: 340px; box-sizing: border-box; text-align: center;
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 28px 24px 22px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
.rays {
  position: absolute; top: -120px; left: 50%; width: 360px; height: 360px; transform: translateX(-50%);
  background: radial-gradient(circle, color-mix(in srgb, var(--c) 45%, transparent) 0%, transparent 65%);
  pointer-events: none;
}
.badge {
  position: relative; display: inline-block; padding: 5px 12px; border-radius: 999px;
  background: var(--c-text); color: #fff; font-size: 0.72rem; font-weight: 900; letter-spacing: 1px;
}
.disc { position: relative; display: flex; justify-content: center; margin: 18px 0 12px; animation: spin-in 0.7s ease-out; }
.rarity { position: relative; font-size: 0.72rem; font-weight: 800; color: var(--c-muted); }
h2 { position: relative; margin: 4px 0 8px; font-size: 1.3rem; font-weight: 900; }
.desc { margin: 0; font-size: 0.85rem; font-weight: 700; color: var(--c-text-2); line-height: 1.5; }
.condition { margin: 6px 0 0; font-size: 0.75rem; font-weight: 700; color: var(--c-faint); }
.actions { display: flex; gap: 8px; margin-top: 20px; }
.btn { flex: 1; min-height: 40px; font-size: 0.85rem; }
@keyframes spin-in {
  from { transform: scale(0.4) rotate(-180deg); opacity: 0; }
  to { transform: scale(1) rotate(0); opacity: 1; }
}
.pop-enter-active, .pop-leave-active { transition: opacity 0.2s; }
.pop-enter-active .modal { transition: transform 0.25s cubic-bezier(0.2, 1.4, 0.4, 1); }
.pop-enter-from, .pop-leave-to { opacity: 0; }
.pop-enter-from .modal { transform: scale(0.85); }
</style>
