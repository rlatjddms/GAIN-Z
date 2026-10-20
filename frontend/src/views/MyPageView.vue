<template>
  <SubPageLayout pill="MY" title="MY PAGE" name="마이페이지" subtitle="회원 정보와 계정 설정을 바꿀 수 있어요">
    <!-- 넓은 화면: 왼쪽 회원 정보·계정, 오른쪽 꾸미기·알림 설정 -->
    <div class="page">
      <div class="col">
      <WsCard title="회원 정보">
        <p v-if="loadError" class="error">{{ loadError }}</p>
        <div v-else-if="!profile" class="loading">불러오는 중…</div>

        <template v-else>
          <!-- 프로필 사진 -->
          <div class="photo">
            <ProfileAvatar :src="profile.profileImageUrl" :size="88" :alt="`${profile.name} 프로필 사진`" />
            <div class="photo-actions">
              <button class="btn small" :disabled="photoBusy" @click="fileInput.click()">
                {{ photoBusy ? '올리는 중…' : profile.profileImageUrl ? '사진 바꾸기' : '사진 올리기' }}
              </button>
              <button v-if="profile.profileImageUrl" class="btn small ghost" :disabled="photoBusy" @click="removePhoto">기본 이미지로</button>
              <p class="photo-hint">PNG · JPG · WEBP, {{ MAX_PROFILE_IMAGE_MB }}MB까지</p>
            </div>
            <input ref="fileInput" type="file" :accept="PROFILE_IMAGE_ACCEPT" hidden @change="onPhotoPicked">
          </div>
          <p v-if="photoError" class="field-error">{{ photoError }}</p>

          <dl class="info">
            <div class="row">
              <dt>이름</dt>
              <dd v-if="!editingName" class="name-view">
                <strong>{{ profile.name }}</strong>
                <button class="text-btn" @click="startEditName">수정</button>
              </dd>
              <dd v-else class="name-edit">
                <form @submit.prevent="saveName">
                  <input
                    ref="nameInput"
                    v-model="nameDraft"
                    type="text"
                    maxlength="20"
                    :disabled="savingName"
                    aria-label="새 이름"
                    @keydown.esc="editingName = false"
                  >
                  <button type="button" class="btn small" :disabled="savingName" @click="editingName = false">취소</button>
                  <button type="submit" class="btn small primary" :disabled="savingName || !nameValid">{{ savingName ? '저장 중…' : '저장' }}</button>
                </form>
                <p v-if="nameError" class="field-error">{{ nameError }}</p>
              </dd>
            </div>
            <div class="row">
              <dt>이메일</dt>
              <dd>{{ profile.email }}</dd>
            </div>
            <div class="row">
              <dt>가입 방식</dt>
              <dd><span class="provider" :class="profile.provider.toLowerCase()">{{ providerLabel }}</span></dd>
            </div>
            <div v-if="joinedText" class="row">
              <dt>가입일</dt>
              <dd>{{ joinedText }}</dd>
            </div>
          </dl>
          <p v-if="status" class="saved">{{ status }}</p>
        </template>
      </WsCard>

      <WsCard title="프로필 공개 범위">
        <p class="muted">다른 사람이 내 프로필을 열었을 때 캐릭터·연습 기록·가입한 밴드·요즘 듣는 음악을 보여 줄지 정해요. 이름과 사진은 항상 보여요.</p>
        <div class="segment" role="radiogroup" aria-label="프로필 공개 범위">
          <button v-for="o in PROFILE_VISIBILITY_OPTIONS" :key="o.value" type="button" role="radio"
                  :aria-checked="currentVisibility === o.value" :class="{ on: currentVisibility === o.value }"
                  :disabled="!profile || visibilityBusy" @click="changeVisibility(o.value)">{{ o.label }}</button>
        </div>
        <p class="muted small">{{ visibilityDesc }}</p>
        <p v-if="visibilityError" class="field-error">{{ visibilityError }}</p>
      </WsCard>

      <WsCard title="계정 설정" class="account-card">
        <div class="settings">
          <button v-if="isLocal" class="setting" @click="passwordOpen = true">
            <span>비밀번호 바꾸기</span><span class="chev">›</span>
          </button>
          <p v-else-if="profile" class="muted">{{ providerLabel }} 계정으로 가입해서 비밀번호는 {{ providerLabel }}에서 관리해요.</p>
          <p v-if="passwordStatus" class="saved">{{ passwordStatus }}</p>

          <button class="setting" @click="logout">
            <span>로그아웃</span><span class="chev">›</span>
          </button>
        </div>
        <button class="withdraw-link" :disabled="!profile" @click="withdrawOpen = true">회원 탈퇴</button>
      </WsCard>
      </div>

      <div class="col">
      <ThemeCard />

      <div id="notification-settings" ref="notificationSettingsRef">
        <NotificationSettingsCard @loaded="scrollToHash" />
      </div>
      </div>
    </div>

    <PasswordModal :open="passwordOpen" @close="passwordOpen = false" @done="onPasswordChanged" />
    <WithdrawModal :open="withdrawOpen" :needs-password="isLocal" @close="withdrawOpen = false" @done="onWithdrawn" />
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SubPageLayout from '../components/workspace/SubPageLayout.vue'
import WsCard from '../components/workspace/WsCard.vue'
import ProfileAvatar from '../components/ProfileAvatar.vue'
import PasswordModal from '../components/mypage/PasswordModal.vue'
import WithdrawModal from '../components/mypage/WithdrawModal.vue'
import NotificationSettingsCard from '../components/mypage/NotificationSettingsCard.vue'
import ThemeCard from '../components/mypage/ThemeCard.vue'
import { useAuthStore } from '../stores/auth'
import { useDialogStore } from '../stores/dialog'
import {
  updateMyName, uploadProfileImage, deleteProfileImage,
  updateProfileVisibility,
  PROFILE_IMAGE_ACCEPT, PROFILE_IMAGE_TYPES, MAX_PROFILE_IMAGE_MB,
  PROFILE_VISIBILITY_OPTIONS,
} from '../api/user'
import { apiErrorMessage } from '../api/practice'

const router = useRouter()
const dialog = useDialogStore()
const route = useRoute()

// 알림 페이지의 ⚙에서 오면(#notification-settings) 알림 설정으로 내려 준다
const notificationSettingsRef = ref(null)
const scrollToHash = async () => {
  if (route.hash !== '#notification-settings') return
  await nextTick()
  notificationSettingsRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
const authStore = useAuthStore()

const profile = computed(() => authStore.profile)
const loadError = ref('')
const status = ref('')

const PROVIDER_LABELS = { LOCAL: '이메일', GOOGLE: '구글', KAKAO: '카카오', NAVER: '네이버' }
const providerLabel = computed(() => PROVIDER_LABELS[profile.value?.provider] || profile.value?.provider || '')
const isLocal = computed(() => profile.value?.provider === 'LOCAL')
const joinedText = computed(() => {
  const at = profile.value?.createdAt
  if (!at) return ''
  const d = new Date(at)
  return Number.isNaN(d.getTime()) ? '' : `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
})

onMounted(async () => {
  // 들어올 때마다 최신 정보로 (다른 기기에서 바꿨을 수도 있다)
  const result = await authStore.fetchProfile()
  if (!result && !authStore.profile) loadError.value = '내 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
})

/* ---------- 프로필 사진 ---------- */
const fileInput = ref(null)
const photoBusy = ref(false)
const photoError = ref('')

const onPhotoPicked = async (e) => {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  photoError.value = ''
  status.value = ''
  if (!PROFILE_IMAGE_TYPES.includes(file.type)) {
    photoError.value = 'PNG · JPG · WEBP 이미지만 올릴 수 있어요.'
    return
  }
  if (file.size > MAX_PROFILE_IMAGE_MB * 1024 * 1024) {
    photoError.value = `${MAX_PROFILE_IMAGE_MB}MB보다 작은 사진을 골라 주세요.`
    return
  }
  photoBusy.value = true
  try {
    authStore.profile = await uploadProfileImage(file)
    status.value = '프로필 사진을 바꿨어요'
  } catch (err) {
    photoError.value = apiErrorMessage(err, '사진을 올리지 못했어요.')
  } finally {
    photoBusy.value = false
  }
}

const removePhoto = async () => {
  if (!await dialog.confirm({ title: '프로필 사진을 지울까요?', text: '기본 이미지로 바뀌어요.', okText: '지우기' })) return
  photoBusy.value = true
  photoError.value = ''
  status.value = ''
  try {
    authStore.profile = await deleteProfileImage()
    status.value = '기본 이미지로 바꿨어요'
  } catch (err) {
    photoError.value = apiErrorMessage(err, '사진을 지우지 못했어요.')
  } finally {
    photoBusy.value = false
  }
}

/* ---------- 이름 수정 ---------- */
const editingName = ref(false)
const nameDraft = ref('')
const nameInput = ref(null)
const savingName = ref(false)
const nameError = ref('')
const nameValid = computed(() => {
  const v = nameDraft.value.trim()
  return v.length >= 1 && v.length <= 20 && v !== profile.value?.name
})

const startEditName = async () => {
  nameDraft.value = profile.value.name
  nameError.value = ''
  status.value = ''
  editingName.value = true
  await nextTick()
  nameInput.value?.select()
}

const saveName = async () => {
  if (!nameValid.value || savingName.value) return
  savingName.value = true
  nameError.value = ''
  try {
    const result = await updateMyName(nameDraft.value.trim())
    authStore.setUserName(result.name)
    editingName.value = false
    status.value = '이름을 바꿨어요'
  } catch (e) {
    nameError.value = apiErrorMessage(e, '이름을 바꾸지 못했어요.')
  } finally {
    savingName.value = false
  }
}

/* ---------- 공개 범위 ---------- */
const visibilityBusy = ref(false)
const visibilityError = ref('')
const currentVisibility = computed(() => profile.value?.profileVisibility || 'PUBLIC')
const visibilityDesc = computed(() => PROFILE_VISIBILITY_OPTIONS.find((o) => o.value === currentVisibility.value)?.desc || '')

const changeVisibility = async (value) => {
  if (value === currentVisibility.value || visibilityBusy.value) return
  visibilityBusy.value = true
  visibilityError.value = ''
  try {
    authStore.profile = await updateProfileVisibility(value)
  } catch (e) {
    visibilityError.value = apiErrorMessage(e, '공개 범위를 바꾸지 못했어요.')
  } finally {
    visibilityBusy.value = false
  }
}

/* ---------- 비밀번호·로그아웃·탈퇴 ---------- */
const passwordOpen = ref(false)
const passwordStatus = ref('')
const onPasswordChanged = () => {
  passwordOpen.value = false
  passwordStatus.value = '비밀번호를 바꿨어요. 다음 로그인부터 새 비밀번호를 써 주세요.'
}

const logout = async () => {
  if (!await dialog.confirm({ title: '로그아웃할까요?', okText: '로그아웃' })) return
  await authStore.logout()
  router.push('/')
}

const withdrawOpen = ref(false)
const onWithdrawn = async () => {
  withdrawOpen.value = false
  await authStore.clearAfterWithdraw()
  await dialog.alert({ title: '탈퇴했어요', text: '그동안 GAIN-Z를 이용해 주셔서 고마워요.' })
  router.replace('/')
}
</script>

<style scoped>
/* 헤더(64px)에 가리지 않게 */
#notification-settings { scroll-margin-top: 80px; }
.page { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.col { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
/* 한 줄로 쌓일 때: 계정 설정(비밀번호·로그아웃·탈퇴)은 맨 아래로 */
@media (max-width: 860px) {
  .page { display: flex; flex-direction: column; }
  .col { display: contents; }
  .account-card { order: 1; }
}

.photo { display: flex; align-items: center; gap: 18px; padding-bottom: 16px; border-bottom: 1px solid var(--c-line-soft); }
.photo-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.photo-hint { flex-basis: 100%; margin: 2px 0 0; font-size: 0.7rem; font-weight: 700; color: var(--c-faint); }

.info { margin: 0; display: flex; flex-direction: column; }
.row { display: grid; grid-template-columns: 76px minmax(0, 1fr); gap: 12px; align-items: center; padding: 12px 0; border-top: 1px solid var(--c-line-soft); }
.row:first-child { border-top: none; }
dt { font-size: 0.75rem; font-weight: 800; color: var(--c-muted); }
dd { margin: 0; font-size: 0.88rem; font-weight: 700; color: #343a40; word-break: break-all; }
.name-view { display: flex; align-items: center; gap: 10px; }
.name-view strong { font-weight: 900; }
.name-edit form { display: flex; gap: 6px; }
.name-edit input {
  flex: 1; min-width: 0; border: 1px solid var(--c-primary); border-radius: 10px; padding: 7px 10px;
  font-family: inherit; font-size: 0.85rem; font-weight: 700; outline: none;
}
.field-error { margin: 8px 0 0; font-size: 0.75rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }

.provider {
  display: inline-block; font-size: 0.72rem; font-weight: 800; padding: 3px 9px; border-radius: 999px;
  background: var(--c-line-soft); color: var(--c-text-2);
}
.provider.google { background: #e7f5ff; color: #1971c2; }
.provider.kakao { background: #fff9db; color: #946200; }
.provider.naver { background: #ebfbee; color: #2b8a3e; }

.settings { display: flex; flex-direction: column; }
.setting {
  display: flex; justify-content: space-between; align-items: center; width: 100%;
  border: none; background: none; padding: 12px 2px; border-top: 1px solid var(--c-line-soft); cursor: pointer;
  font-family: inherit; font-size: 0.88rem; font-weight: 800; color: #343a40; text-align: left;
}
.setting:first-child { border-top: none; padding-top: 0; }
.setting:hover { color: var(--c-primary-text); }
.chev { color: #ced4da; font-size: 1.1rem; }
.muted { margin: 0 0 12px; font-size: 0.78rem; color: var(--c-muted); line-height: 1.6; }
.withdraw-link {
  margin-top: 14px; border: none; background: none; padding: 0; cursor: pointer;
  font-family: inherit; font-size: 0.72rem; font-weight: 700; color: var(--c-faint); text-decoration: underline;
}
.withdraw-link:hover:not(:disabled) { color: var(--c-danger); }
.withdraw-link:disabled { cursor: not-allowed; }

.text-btn { border: none; background: none; padding: 0; color: var(--c-primary-text); font-weight: 800; cursor: pointer; font-family: inherit; font-size: 0.75rem; }

.muted.small { margin: 8px 0 0; font-size: 0.72rem; }
.saved { margin: 10px 0 0; font-size: 0.78rem; font-weight: 800; color: var(--c-success); }
.error { margin: 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }
.loading { padding: 32px 16px; text-align: center; font-size: 0.85rem; font-weight: 700; color: var(--c-faint); }
</style>
