<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="userId" class="backdrop" @click.self="!busy && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="profile-title">
          <button class="close" :disabled="busy" aria-label="닫기" @click="emit('close')">×</button>

          <p v-if="loadError" class="error">{{ loadError }}</p>
          <div v-else-if="!profile" class="loading">불러오는 중…</div>

          <template v-else>
            <!-- 사진·이름·닉네임, 오른쪽에 친구 관계 버튼 -->
            <header class="head">
              <ProfileAvatar :src="profile.profileImageUrl" :size="56" :alt="`${profile.name} 프로필 사진`" />
              <div class="who">
                <h2 id="profile-title">{{ profile.name }}</h2>
                <p v-if="profile.character">{{ profile.character.nickname }} · Lv.{{ profile.character.level ?? 1 }}</p>
                <p v-else-if="profile.visible">아직 캐릭터가 없어요</p>
              </div>
              <button v-if="profile.relation === 'FRIEND'" class="head-btn danger" :disabled="busy" @click="emit('remove', profile)">
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
              <span class="lock" aria-hidden="true">🔒</span>
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
                    <span class="band-name">
                      <span v-if="b.position === 'LEADER'" title="리더">👑 </span>{{ b.name }}
                    </span>
                    <span class="part">{{ partLabel(b.part) }}</span>
                    <span v-if="b.together" class="badge">같은 밴드</span>
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
import { ref, watch } from 'vue'
import ProfileAvatar from './ProfileAvatar.vue'
import ProfileMusic from './ProfileMusic.vue'
import CharacterStage from './CharacterStage.vue'
import PracticeLogCard from './workspace/PracticeLogCard.vue'
import { getUserProfile } from '../api/user'
import { sendFriendRequestToBandMate, acceptFriendRequest, getReceivedRequests } from '../api/friend'
import { apiErrorMessage } from '../api/practice'
import { INSTRUMENT_OPTIONS } from '../constants/characterOptions'

const props = defineProps({
  // 볼 사람의 userId. null이면 닫힘
  userId: { type: Number, default: null },
  // 친구 신청 버튼을 보일지 (같은 밴드 멤버에게만 코드 없이 신청할 수 있다)
  canRequest: { type: Boolean, default: false },
})
// remove: 친구 삭제를 눌렀을 때 (확인·삭제는 부르는 쪽에서)
// changed: 친구 신청·수락으로 관계가 바뀌었을 때
const emit = defineEmits(['close', 'remove', 'changed'])

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
  background: #f8f9fa; border-radius: 24px; padding: 22px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a;
  display: flex; flex-direction: column; gap: 14px;
}
.close {
  position: absolute; top: 14px; right: 16px; border: none; background: none; cursor: pointer;
  font-size: 1.5rem; line-height: 1; color: #adb5bd;
}
.close:hover:not(:disabled) { color: #495057; }

.head { display: flex; align-items: center; gap: 14px; padding-right: 28px; }
.who { flex: 1; min-width: 0; }
.who h2 { margin: 0; font-size: 1.15rem; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.who p { margin: 3px 0 0; font-size: 0.82rem; font-weight: 700; color: #495057; }

.head-btn {
  flex: none; border: 1px solid #e9ecef; background: #fff; border-radius: 10px; padding: 7px 11px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #868e96; white-space: nowrap;
}
.head-btn.danger:hover:not(:disabled) { color: #e03131; border-color: #ffc9c9; background: #fff5f5; }
.head-btn.primary { color: #2d5bff; border-color: #d0dcff; background: #f0f4ff; }
.head-btn.primary:hover:not(:disabled) { background: #2d5bff; color: #fff; border-color: #2d5bff; }
.head-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.head-tag { flex: none; font-size: 0.72rem; font-weight: 800; color: #868e96; background: #f1f3f5; padding: 5px 10px; border-radius: 999px; }

.hidden-box {
  background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 36px 20px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.hidden-box .lock { font-size: 1.6rem; }
.hidden-box p { margin: 0; font-size: 0.85rem; font-weight: 800; color: #868e96; }

.bands-card { background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 20px 24px; }
h3 { margin: 0 0 12px; font-size: 0.85rem; font-weight: 800; color: #495057; }
.count { color: #adb5bd; }
.bands { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.band { display: flex; align-items: center; gap: 10px; padding: 10px 2px; border-top: 1px solid #f1f3f5; }
.band:first-child { border-top: none; padding-top: 0; }
.band-name { flex: 1; min-width: 0; font-size: 0.88rem; font-weight: 900; color: #343a40; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.part { font-size: 0.75rem; font-weight: 700; color: #868e96; }
.badge { font-size: 0.68rem; font-weight: 800; color: #2f9e44; background: #ebfbee; padding: 3px 8px; border-radius: 999px; }

.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading, .empty { margin: 0; padding: 8px 0; font-size: 0.85rem; color: #adb5bd; font-weight: 700; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
