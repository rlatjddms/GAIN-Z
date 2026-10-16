<template>
  <SubPageLayout pill="MY">
    <div class="page">
      <div class="page-head">
        <h2>마이페이지</h2>
        <p>내 캐릭터와 계정 정보를 확인하고 바꿀 수 있어요</p>
      </div>

      <div class="layout">
        <!-- 캐릭터 + 연습 요약 -->
        <div class="col">
          <CharacterStage :character="character">
            <router-link to="/wardrobe" class="wardrobe-btn" title="옷장: 외형·의상·배경 바꾸기">
              <svg viewBox="0 0 24 24" width="13" height="13" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 3l3 2 3-2 5 3-2 4-2-1v12H8V9L6 10 4 6z"></path>
              </svg>
              옷장
            </router-link>
          </CharacterStage>

          <WsCard title="연습 요약">
            <div class="stats">
              <div class="stat">
                <span>총 연습 시간</span>
                <strong>{{ durationText((character?.totalPracticeTime || 0) * 60) }}</strong>
              </div>
              <div class="stat">
                <span>연속 연습</span>
                <strong>{{ character?.currentStreak || 0 }}일</strong>
              </div>
              <div class="stat">
                <span>최장 연속</span>
                <strong>{{ character?.maxStreak || 0 }}일</strong>
              </div>
            </div>
          </WsCard>
        </div>

        <!-- 계정 -->
        <div class="col">
          <WsCard title="내 정보">
            <p v-if="loadError" class="error">{{ loadError }}</p>
            <div v-else-if="!profile" class="loading">불러오는 중…</div>

            <dl v-else class="info">
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
            <p v-if="nameStatus" class="saved">{{ nameStatus }}</p>
          </WsCard>

          <WsCard title="계정 설정">
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
      </div>
    </div>

    <PasswordModal :open="passwordOpen" @close="passwordOpen = false" @done="onPasswordChanged" />
    <WithdrawModal :open="withdrawOpen" :needs-password="isLocal" @close="withdrawOpen = false" @done="onWithdrawn" />
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SubPageLayout from '../components/workspace/SubPageLayout.vue'
import WsCard from '../components/workspace/WsCard.vue'
import CharacterStage from '../components/CharacterStage.vue'
import PasswordModal from '../components/mypage/PasswordModal.vue'
import WithdrawModal from '../components/mypage/WithdrawModal.vue'
import { useAuthStore } from '../stores/auth'
import { updateMyName } from '../api/user'
import { apiErrorMessage } from '../api/practice'
import { durationText } from '../constants/practice'

const router = useRouter()
const authStore = useAuthStore()

const character = computed(() => authStore.character)
const profile = computed(() => authStore.profile)
const loadError = ref('')

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
  // 들어올 때마다 최신 정보로 (다른 기기에서 이름을 바꿨을 수도 있다)
  const result = await authStore.fetchProfile()
  if (!result && !authStore.profile) loadError.value = '내 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
})

/* ---------- 이름 수정 ---------- */
const editingName = ref(false)
const nameDraft = ref('')
const nameInput = ref(null)
const savingName = ref(false)
const nameError = ref('')
const nameStatus = ref('')
const nameValid = computed(() => {
  const v = nameDraft.value.trim()
  return v.length >= 1 && v.length <= 20 && v !== profile.value?.name
})

const startEditName = async () => {
  nameDraft.value = profile.value.name
  nameError.value = ''
  nameStatus.value = ''
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
    nameStatus.value = '이름을 바꿨어요'
  } catch (e) {
    nameError.value = apiErrorMessage(e, '이름을 바꾸지 못했어요.')
  } finally {
    savingName.value = false
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
  if (!window.confirm('로그아웃할까요?')) return
  await authStore.logout()
  router.push('/')
}

const withdrawOpen = ref(false)
const onWithdrawn = async () => {
  withdrawOpen.value = false
  await authStore.clearAfterWithdraw()
  window.alert('탈퇴했어요. 그동안 GAIN-Z를 이용해 주셔서 고마워요.')
  router.replace('/')
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.col { display: flex; flex-direction: column; gap: 16px; min-width: 0; }

.wardrobe-btn {
  position: absolute; bottom: 16px; right: 20px;
  display: flex; align-items: center; gap: 5px; padding: 6px 12px; border-radius: 8px;
  background: #fff; border: 1px solid #e9ecef; color: #495057; font-size: 0.75rem; font-weight: 700; text-decoration: none;
  transition: all 0.2s;
}
.wardrobe-btn:hover { background: #f1f3f5; color: #2d5bff; border-color: #2d5bff; }

.stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.stat { background: #f8f9fa; border-radius: 14px; padding: 12px 14px; display: flex; flex-direction: column; gap: 4px; }
.stat span { font-size: 0.7rem; font-weight: 800; color: #868e96; }
.stat strong { font-size: 1.05rem; font-weight: 900; color: #212529; }

.info { margin: 0; display: flex; flex-direction: column; }
.row { display: grid; grid-template-columns: 76px minmax(0, 1fr); gap: 12px; align-items: center; padding: 11px 0; border-top: 1px solid #f1f3f5; }
.row:first-child { border-top: none; padding-top: 0; }
dt { font-size: 0.75rem; font-weight: 800; color: #868e96; }
dd { margin: 0; font-size: 0.88rem; font-weight: 700; color: #343a40; word-break: break-all; }
.name-view { display: flex; align-items: center; gap: 10px; }
.name-view strong { font-weight: 900; }
.name-edit form { display: flex; gap: 6px; }
.name-edit input {
  flex: 1; min-width: 0; border: 1px solid #2d5bff; border-radius: 10px; padding: 7px 10px;
  font-family: inherit; font-size: 0.85rem; font-weight: 700; outline: none;
}
.field-error { margin: 6px 0 0; font-size: 0.72rem; font-weight: 700; color: #e03131; }

.provider {
  display: inline-block; font-size: 0.72rem; font-weight: 800; padding: 3px 9px; border-radius: 999px;
  background: #f1f3f5; color: #495057;
}
.provider.google { background: #e7f5ff; color: #1971c2; }
.provider.kakao { background: #fff9db; color: #946200; }
.provider.naver { background: #ebfbee; color: #2b8a3e; }

.settings { display: flex; flex-direction: column; }
.setting {
  display: flex; justify-content: space-between; align-items: center; width: 100%;
  border: none; background: none; padding: 12px 2px; border-top: 1px solid #f1f3f5; cursor: pointer;
  font-family: inherit; font-size: 0.88rem; font-weight: 800; color: #343a40; text-align: left;
}
.setting:first-child { border-top: none; padding-top: 0; }
.setting:hover { color: #2d5bff; }
.chev { color: #ced4da; font-size: 1.1rem; }
.muted { margin: 0 0 12px; font-size: 0.78rem; color: #868e96; line-height: 1.6; }
.withdraw-link {
  margin-top: 14px; border: none; background: none; padding: 0; cursor: pointer;
  font-family: inherit; font-size: 0.72rem; font-weight: 700; color: #adb5bd; text-decoration: underline;
}
.withdraw-link:hover:not(:disabled) { color: #e03131; }
.withdraw-link:disabled { cursor: not-allowed; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit; cursor: pointer;
  font-weight: 800; font-size: 0.8rem; padding: 8px 12px; border-radius: 10px; white-space: nowrap;
}
.btn.small { padding: 7px 10px; font-size: 0.75rem; }
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }
.text-btn { border: none; background: none; padding: 0; color: #2d5bff; font-weight: 800; cursor: pointer; font-family: inherit; font-size: 0.75rem; }

.saved { margin: 10px 0 0; font-size: 0.75rem; font-weight: 800; color: #2f9e44; }
.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading { padding: 16px 0; font-size: 0.85rem; color: #adb5bd; }

@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
}
/* 좁은 화면: 무대 아래 경험치와 겹치지 않게 옷장 버튼을 무대 밑으로 (홈과 같게) */
@media (max-width: 640px) {
  .wardrobe-btn { position: static; margin: 0 auto 12px; width: fit-content; }
  .stats { grid-template-columns: 1fr 1fr 1fr; gap: 6px; }
  .stat { padding: 10px; }
  .stat strong { font-size: 0.9rem; }
}
</style>
