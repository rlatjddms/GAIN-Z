<template>
  <!-- 로그인 방법 (#189): 이메일 비밀번호와 구글·카카오·네이버를 한 계정에 연결. 마지막 하나는 해제할 수 없다 -->
  <WsCard title="로그인 방법">
    <ul v-if="profile" class="methods">
      <li class="method">
        <span class="icon email" aria-hidden="true">@</span>
        <span class="name">
          이메일
          <span class="detail">{{ profile.hasPassword ? '비밀번호로 로그인할 수 있어요' : '비밀번호를 만들면 이메일로도 로그인할 수 있어요' }}</span>
        </span>
        <button type="button" class="btn small" @click="emit('password', profile.hasPassword ? 'change' : 'create')">
          {{ profile.hasPassword ? '비밀번호 바꾸기' : '비밀번호 만들기' }}
        </button>
      </li>
      <li v-for="p in SOCIAL_PROVIDERS" :key="p.value" class="method">
        <span class="icon" :class="p.value.toLowerCase()" aria-hidden="true">{{ p.mark }}</span>
        <span class="name">
          {{ p.label }}
          <span class="detail" :class="{ on: linked(p.value) }">{{ linked(p.value) ? '연결됨' : '연결 안 됨' }}</span>
        </span>
        <button v-if="linked(p.value)" type="button" class="btn small" :disabled="busy !== '' || onlyOne"
                :title="onlyOne ? '로그인 방법이 하나뿐이라 해제할 수 없어요' : ''" @click="unlink(p)">
          {{ busy === p.value ? '해제 중…' : '해제' }}
        </button>
        <button v-else type="button" class="btn small primary" :disabled="busy !== ''" @click="link(p)">
          {{ busy === p.value ? '이동 중…' : '연결' }}
        </button>
      </li>
    </ul>
    <p v-if="profile && onlyOne && profile.socialLinks?.length" class="muted small">로그인 방법이 하나뿐이라 해제할 수 없어요. 다른 방법을 먼저 연결하거나 비밀번호를 만들어 주세요.</p>
  </WsCard>
</template>

<script setup>
import { computed, ref } from 'vue'
import WsCard from '../workspace/WsCard.vue'
import { useAuthStore } from '../../stores/auth'
import { useDialogStore } from '../../stores/dialog'
import { startSocialLink, unlinkSocial, SOCIAL_PROVIDERS } from '../../api/user'
import { API_BASE_URL } from '../../api/axios'
import { apiErrorMessage } from '../../api/practice'
import { useToastStore } from '../../stores/toast'

const toast = useToastStore()

// password: 'create' | 'change' (비밀번호 창 열기)
const emit = defineEmits(['password'])

const authStore = useAuthStore()
const dialog = useDialogStore()
const profile = computed(() => authStore.profile)
const busy = ref('')

const linked = (provider) => profile.value?.socialLinks?.includes(provider)
const methodCount = computed(() => (profile.value?.hasPassword ? 1 : 0) + (profile.value?.socialLinks?.length ?? 0))
const onlyOne = computed(() => methodCount.value <= 1)

// 서버에서 5분짜리 연결 표를 받아 소셜 로그인으로 보낸다. 돌아오면 마이페이지에서 결과를 알려 준다
const link = async (p) => {
  busy.value = p.value
  try {
    const token = await startSocialLink(p.value)
    // 돌아왔을 때 지금과 같은 로그인 유지 방식으로 토큰을 저장하게
    sessionStorage.setItem('keepLoggedInPreference', localStorage.getItem('accessToken') ? 'true' : 'false')
    window.location.href = `${API_BASE_URL}/oauth2/authorization/${p.value.toLowerCase()}?link=${encodeURIComponent(token)}`
  } catch (e) {
    toast.error(apiErrorMessage(e, '연결을 시작하지 못했어요. 잠시 후 다시 시도해 주세요.'))
    busy.value = ''
  }
}

const unlink = async (p) => {
  if (!await dialog.confirm({ title: `${p.label} 연결을 해제할까요?`, text: `다음부터 ${p.label}로는 로그인할 수 없어요.`, okText: '해제' })) return
  busy.value = p.value
  try {
    authStore.profile = await unlinkSocial(p.value)
  } catch (e) {
    toast.error(apiErrorMessage(e, '연결을 해제하지 못했어요. 잠시 후 다시 시도해 주세요.'))
  } finally {
    busy.value = ''
  }
}
</script>

<style scoped>
.methods { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.method { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--c-line-soft); }
.method:last-child { border-bottom: none; }
.icon {
  flex: 0 0 auto; width: 32px; height: 32px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem; font-weight: 900; background: var(--c-bg); color: var(--c-text-2);
}
.icon.google { background: #e7f5ff; color: #1971c2; }
.icon.kakao { background: #fee500; color: #3c1e1e; }
.icon.naver { background: #03c75a; color: #fff; }
.name { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; font-size: 0.9rem; font-weight: 800; color: var(--c-text); }
.detail { font-size: 0.75rem; font-weight: 600; color: var(--c-muted); }
.detail.on { color: var(--c-primary-text); }
.btn.small { flex: 0 0 auto; min-height: 32px; padding: 0 12px; font-size: 0.78rem; white-space: nowrap; }
.muted { margin: 8px 0 0; color: var(--c-muted); line-height: 1.5; }
.small { font-size: 0.75rem; }
</style>
