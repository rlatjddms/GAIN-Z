<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="userId" class="backdrop" @click.self="!busy && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="profile-title">
          <ModalClose :disabled="busy" @close="emit('close')" />

          <p v-if="loadError" class="error">{{ loadError }}</p>
          <div v-else-if="!profile" class="loading">불러오는 중…</div>

          <template v-else>
            <!-- 사진·닉네임·레벨, 오른쪽에 친구 관계 버튼 -->
            <header class="head">
              <ProfileAvatar :src="profile.profileImageUrl" :size="56" :alt="`${profile.name} 프로필 사진`" />
              <div class="who">
                <h2 id="profile-title">{{ profile.name }}</h2>
                <p v-if="profile.character">Lv.{{ profile.character.level ?? 1 }}</p>
                <p v-else-if="profile.visible">아직 캐릭터가 없어요</p>
              </div>
              <!-- 친구 목록에서 열면 밴드 초대 버튼, 친구 삭제는 ⋯ 메뉴로 (#229) -->
              <template v-if="profile.relation === 'FRIEND' && canInvite">
                <button class="head-btn primary" :disabled="busy" @click="emit('invite', { userId: profile.userId, name: profile.name })">밴드 초대</button>
                <FriendMenu :name="profile.name" :can-invite="false" :disabled="busy" @remove="emit('remove', profile)" />
              </template>
              <button v-else-if="profile.relation === 'FRIEND'" class="head-btn danger" :disabled="busy" @click="emit('remove', profile)">
                {{ busy ? '삭제하는 중…' : '친구 삭제' }}
              </button>
              <button v-else-if="profile.relation === 'RECEIVED'" class="head-btn primary" :disabled="busy" @click="acceptRequest">친구 수락</button>
              <span v-else-if="profile.relation === 'SENT'" class="head-tag">신청함</span>
              <button v-else-if="profile.relation === 'NONE' && canRequest" class="head-btn primary" :disabled="busy" @click="sendRequest">
                {{ busy ? '보내는 중…' : '친구 신청' }}
              </button>
            </header>
            <p v-if="actionError" class="error">{{ actionError }}</p>

            <!-- 공개 범위 때문에 못 보는 프로필 -->
            <div v-if="!profile.visible" class="hidden-box">
              <span class="lock"><PixelIcon name="lock" :size="36" /></span>
              <p>공개되지 않은 프로필이에요</p>
            </div>

            <template v-else>
              <!-- 요즘 듣는 음악 -->
              <ProfileMusic v-if="profile.profileMusic" :music="profile.profileMusic" />

              <!-- 꾸민 캐릭터와 무대 배경 그대로 -->
              <CharacterStage v-if="profile.character" :character="profile.character" />

              <!-- 깃허브 잔디처럼 최근 연습 -->
              <PracticeLogCard :daily="profile.dailyPractice" :total-minutes="profile.character?.totalPracticeTime ?? 0" />

              <!-- 가입한 밴드 (나도 있는 밴드는 같은 밴드 표시) -->
              <section class="bands-card">
                <h3>가입한 밴드 <span class="count">{{ profile.bands.length }}</span></h3>
                <p v-if="!profile.bands.length" class="empty">아직 들어간 밴드가 없어요</p>
                <ul v-else class="bands">
                  <li v-for="b in profile.bands" :key="b.bandId" class="band">
                    <span class="band-title">
                      <span class="band-name"><Crown v-if="b.position === 'LEADER'" class="crown" :size="12" :stroke-width="2.4" aria-label="리더" />{{ b.name }}</span>
                      <span v-if="b.together" class="badge">같은 밴드</span>
                    </span>
                    <span class="part">{{ partLabel(b.part) }}</span>
                  </li>
                </ul>
              </section>
            </template>
          </template>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 다른 사람 프로필 팝업 (친구 목록·밴드 멤버에서 연다). 공개 범위에 따라 이름·사진만 보일 수 있다
import { Crown } from 'lucide-vue-next'
import PixelIcon from '../components/PixelIcon.vue'
import { ref, watch } from 'vue'
import ProfileAvatar from './ProfileAvatar.vue'
import ProfileMusic from './ProfileMusic.vue'
import FriendMenu from './friend/FriendMenu.vue'
import CharacterStage from './CharacterStage.vue'
import PracticeLogCard from './workspace/PracticeLogCard.vue'
import { getUserProfile } from '../api/user'
import { sendFriendRequestToBandMate, acceptFriendRequest, getReceivedRequests } from '../api/friend'
import { apiErrorMessage } from '../api/practice'
import { INSTRUMENT_OPTIONS } from '../constants/characterOptions'
import ModalClose from './ui/ModalClose.vue'

const props = defineProps({
  // 볼 사람의 userId. null이면 닫힘
  userId: { type: Number, default: null },
  // 친구 신청 버튼을 보일지 (같은 밴드 멤버에게만 코드 없이 신청할 수 있다)
  canRequest: { type: Boolean, default: false },
  // 친구면 밴드 초대 버튼을 보일지 (친구 목록에서 열 때, #229)
  canInvite: { type: Boolean, default: false },
})
// remove: 친구 삭제를 눌렀을 때 (확인·삭제는 부르는 쪽에서)
// changed: 친구 신청·수락으로 관계가 바뀌었을 때
// invite({ userId, name }): 밴드 초대를 눌렀을 때 (초대 창은 부르는 쪽에서)
const emit = defineEmits(['close', 'remove', 'changed', 'invite'])

const profile = ref(null)
const loadError = ref('')
const actionError = ref('')
const busy = ref(false)

let token = 0
const load = async (userId) => {
  const mine = ++token
  try {
    const result = await getUserProfile(userId)
    if (mine === token) profile.value = result
  } catch (e) {
    if (mine === token) loadError.value = apiErrorMessage(e, '프로필을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

watch(() => props.userId, (userId) => {
  token++
  profile.value = null
  loadError.value = ''
  actionError.value = ''
  busy.value = false
  if (userId) load(userId)
}, { immediate: true })

const partLabel = (type) => INSTRUMENT_OPTIONS.find((o) => o.value === type)?.label || type

// 신청·수락 뒤에는 관계가 바뀌니 다시 불러온다 (친구가 되면 더 많이 보일 수 있다)
const act = async (action, fallback) => {
  busy.value = true
  actionError.value = ''
  try {
    await action()
    emit('changed')
    await load(props.userId)
  } catch (e) {
    actionError.value = apiErrorMessage(e, fallback)
  } finally {
    busy.value = false
  }
}

const sendRequest = () => act(() => sendFriendRequestToBandMate(profile.value.userId), '친구 신청을 보내지 못했어요.')

const acceptRequest = () => act(async () => {
  const request = (await getReceivedRequests()).find((r) => r.user.userId === profile.value.userId)
  if (!request) throw new Error()
  await acceptFriendRequest(request.requestId)
}, '친구 신청을 수락하지 못했어요.')

// 부르는 쪽에서 친구 삭제를 처리할 때 버튼 상태·실패 메시지를 맞춘다
defineExpose({
  setRemoving: (value) => { busy.value = value },
  setError: (message) => { actionError.value = message },
})
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: flex-start; justify-content: center; padding: 40px 16px; overflow-y: auto;
}
.modal {
  position: relative; width: 100%; max-width: 560px; box-sizing: border-box;
  background: var(--c-bg); border-radius: var(--radius-xl); padding: 22px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
  display: flex; flex-direction: column; gap: 14px;
}
/* 높이가 모자라도 무대·잔디가 찌그러지지 않고 팝업 안에서 스크롤 (#229) */
.modal > * { flex-shrink: 0; }

.head { display: flex; align-items: center; gap: 14px; padding-right: 28px; }
.who { flex: 1; min-width: 0; }
.who h2 { margin: 0; font-size: 1.15rem; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.who p { margin: 3px 0 0; font-size: 0.82rem; font-weight: 700; color: var(--c-text-2); }

.head-btn {
  flex: none; border: 1px solid var(--c-line); background: #fff; border-radius: 10px; padding: 7px 11px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: var(--c-muted); white-space: nowrap;
}
.head-btn.danger:hover:not(:disabled) { color: var(--c-danger); border-color: #ffc9c9; background: var(--c-danger-soft); }
.head-btn.primary { color: var(--c-primary-text); border-color: var(--c-primary-line); background: var(--c-primary-soft); }
.head-btn.primary:hover:not(:disabled) { background: var(--c-primary); color: #fff; border-color: var(--c-primary); }
.head-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.head-tag { flex: none; font-size: 0.72rem; font-weight: 800; color: var(--c-muted); background: var(--c-line-soft); padding: 5px 10px; border-radius: 999px; }

.hidden-box {
  background: #fff; border: 1px solid var(--c-line); border-radius: 24px; padding: 36px 20px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.hidden-box .lock { font-size: 1.6rem; }
.hidden-box p { margin: 0; font-size: 0.85rem; font-weight: 800; color: var(--c-muted); }

.bands-card { background: #fff; border: 1px solid var(--c-line); border-radius: 24px; padding: 20px 24px; }
h3 { margin: 0 0 14px; font-size: 0.85rem; font-weight: 800; color: var(--c-text-2); }
.count { color: var(--c-faint); }
/* 밴드마다 연한 칸으로 (#229): 제목 바로 밑에 줄이 붙어 보이던 것을 칸으로 나눈다 */
.bands { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.band { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 14px; background: var(--c-bg); }
.band-title { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; }
.band-name { min-width: 0; font-size: 0.88rem; font-weight: 900; color: #343a40; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* 리더 왕관: 홈 밴드 무대와 같은 노란 왕관, 이름과 띄움 */
.crown { margin-right: 6px; color: #f08c00; vertical-align: -1px; }
.part { flex: none; font-size: 0.75rem; font-weight: 700; color: var(--c-muted); }
.badge { flex: none; font-size: 0.68rem; font-weight: 800; color: var(--c-success); background: #ebfbee; padding: 3px 8px; border-radius: 999px; }

.error { margin: 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }
.loading, .empty { margin: 0; padding: 32px 16px; text-align: center; font-size: 0.85rem; font-weight: 700; color: var(--c-faint); }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
/* 휴대폰: 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .modal {
    width: 100%; max-width: none; max-height: 92dvh; overflow-y: auto; overscroll-behavior: contain;
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    padding-bottom: calc(24px + env(safe-area-inset-bottom));
  }
}
</style>
