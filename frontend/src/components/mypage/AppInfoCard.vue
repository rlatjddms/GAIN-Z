<template>
  <WsCard title="앱 정보">
    <div class="row">
      <div class="version">
        <span class="label">지금 버전</span>
        <strong>{{ buildLabel(CURRENT_BUILD_ID) }}</strong>
      </div>
      <button v-if="store.hasUpdate" type="button" class="btn primary small" :disabled="applying" @click="apply">
        {{ applying ? '불러오는 중…' : '업데이트' }}
      </button>
      <button v-else type="button" class="btn small" :disabled="store.checking" @click="check">
        {{ store.checking ? '확인 중…' : '업데이트 확인' }}
      </button>
    </div>
    <p v-if="store.hasUpdate" class="note new">새 버전이 있어요. 업데이트하면 화면을 새로 불러와요.</p>
    <p v-else-if="message" class="note">{{ message }}</p>
  </WsCard>
</template>

<script setup>
// 앱 정보 (#223): 지금 버전과 업데이트 확인. 새 버전이 있으면 바로 업데이트
import { ref } from 'vue'
import WsCard from '../workspace/WsCard.vue'
import { CURRENT_BUILD_ID, buildLabel, useAppUpdateStore } from '../../stores/appUpdate'

const store = useAppUpdateStore()
const message = ref('')
const applying = ref(false)

const check = async () => {
  message.value = ''
  if (import.meta.env.DEV) {
    message.value = '개발 서버에서는 확인하지 않아요.'
    return
  }
  const found = await store.check()
  if (!found) message.value = '최신 버전이에요.'
}

const apply = () => {
  applying.value = true
  store.apply()
}
</script>

<style scoped>
.row { display: flex; align-items: center; gap: 12px; }
.version { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.label { font-size: 0.72rem; font-weight: 700; color: var(--c-faint); }
.version strong { font-size: 0.88rem; font-weight: 800; color: var(--c-text); }
.btn { flex: none; }
.note { margin: 10px 0 0; font-size: 0.78rem; font-weight: 700; color: var(--c-muted); }
.note.new { color: var(--c-primary-text); }
</style>
