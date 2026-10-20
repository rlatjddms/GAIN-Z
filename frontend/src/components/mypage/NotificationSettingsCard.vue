<template>
  <!-- 알림 설정: 종류별 켜기·끄기 (누르면 바로 저장). 꺼 둔 종류는 알림이 만들어지지 않는다 -->
  <WsCard title="알림 설정">
    <p class="muted">꺼 둔 알림은 받지 않아요. 다시 켜도 꺼 둔 동안의 알림은 오지 않아요.</p>
    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="!settings" class="loading">불러오는 중…</div>

    <template v-else>
      <section v-for="g in NOTIFICATION_SETTING_GROUPS" :key="g.category" class="group">
        <header class="group-head">
          <NotificationIcon :type="sampleType(g.category)" :size="26" />
          <strong>{{ g.label }}</strong>
          <span class="spacer"></span>
          <button class="text-btn" :disabled="busy" @click="setGroup(g, !groupAllOn(g))">
            {{ groupAllOn(g) ? '모두 끄기' : '모두 켜기' }}
          </button>
        </header>
        <ul>
          <li v-for="item in g.items" :key="item.key" class="row">
            <div class="label">
              <span class="name">{{ item.label }}</span>
              <span class="desc">{{ item.desc }}</span>
            </div>
            <button class="switch" role="switch" :aria-checked="settings[item.key]" :aria-label="`${item.label} 알림`"
                    :class="{ on: settings[item.key] }" :disabled="busy" @click="save({ [item.key]: !settings[item.key] })">
              <span class="knob"></span>
            </button>
          </li>
        </ul>
      </section>
      <p v-if="error" class="error">{{ error }}</p>
    </template>
  </WsCard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import WsCard from '../workspace/WsCard.vue'
import NotificationIcon from '../NotificationIcon.vue'
import { getNotificationSettings, updateNotificationSettings } from '../../api/notification'
import { apiErrorMessage } from '../../api/practice'
import { NOTIFICATION_SETTING_GROUPS } from '../../constants/notification'

// 다 불러온 뒤 알려 준다 (마이페이지가 #notification-settings로 스크롤할 때)
const emit = defineEmits(['loaded'])

const settings = ref(null)
const loadError = ref('')
const error = ref('')
const busy = ref(false)

// 영역 아이콘을 그리려고 그 영역의 대표 종류 하나
const SAMPLE = { FRIEND: 'FRIEND_REQUEST', BAND: 'BAND_JOIN', COMMUNITY: 'NOTICE', JAM: 'JAM_NEW', RECORD: 'RECORD' }
const sampleType = (category) => SAMPLE[category]

const groupAllOn = (g) => g.items.every((item) => settings.value[item.key])

onMounted(async () => {
  try {
    settings.value = (await getNotificationSettings()).settings
  } catch (e) {
    loadError.value = apiErrorMessage(e, '알림 설정을 불러오지 못했어요.')
  } finally {
    emit('loaded')
  }
})

// 화면을 먼저 바꾸고, 실패하면 되돌린다
const save = async (changes) => {
  const before = { ...settings.value }
  settings.value = { ...settings.value, ...changes }
  busy.value = true
  error.value = ''
  try {
    settings.value = (await updateNotificationSettings(changes)).settings
  } catch (e) {
    settings.value = before
    error.value = apiErrorMessage(e, '알림 설정을 바꾸지 못했어요.')
  } finally {
    busy.value = false
  }
}

const setGroup = (g, on) => save(Object.fromEntries(g.items.map((item) => [item.key, on])))
</script>

<style scoped>
.muted { margin: 0 0 12px; font-size: 0.78rem; color: #868e96; line-height: 1.6; }
.group + .group { margin-top: 14px; padding-top: 14px; border-top: 1px solid #f1f3f5; }
.group-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.group-head strong { font-size: 0.85rem; font-weight: 900; color: #343a40; }
.spacer { flex: 1; }
.text-btn { border: none; background: none; padding: 0; cursor: pointer; font-family: inherit; font-size: 0.72rem; font-weight: 800; color: var(--c-primary-text); }
.text-btn:disabled { opacity: 0.4; cursor: default; }

ul { list-style: none; margin: 0; padding: 0; }
.row { display: flex; align-items: center; gap: 12px; padding: 8px 0 8px 34px; }
.label { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.name { font-size: 0.85rem; font-weight: 800; color: #343a40; }
.desc { font-size: 0.72rem; font-weight: 700; color: #adb5bd; }

.switch {
  flex: none; position: relative; width: 40px; height: 22px; border: none; border-radius: 999px; cursor: pointer;
  background: #dee2e6; transition: background 0.15s; padding: 0;
}
.switch.on { background: var(--c-primary); }
.switch:disabled { cursor: wait; }
.knob {
  position: absolute; top: 3px; left: 3px; width: 16px; height: 16px; border-radius: 50%; background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.2); transition: transform 0.15s;
}
.switch.on .knob { transform: translateX(18px); }

.error { margin: 8px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading { padding: 12px 0; font-size: 0.85rem; color: #adb5bd; }

@media (max-width: 480px) {
  .row { padding-left: 0; }
}
</style>
