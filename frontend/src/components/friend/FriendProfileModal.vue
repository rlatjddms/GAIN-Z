<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="userId" class="backdrop" @click.self="!removing && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="friend-profile-title">
          <button class="close" :disabled="removing" aria-label="닫기" @click="emit('close')">×</button>

          <p v-if="loadError" class="error">{{ loadError }}</p>
          <div v-else-if="!profile" class="loading">불러오는 중…</div>

          <template v-else>
            <!-- 사진·이름·닉네임 -->
            <header class="head">
              <ProfileAvatar :src="profile.profileImageUrl" :size="56" :alt="`${profile.name} 프로필 사진`" />
              <div class="who">
                <h2 id="friend-profile-title">{{ profile.name }}</h2>
                <p v-if="profile.character">{{ profile.character.nickname }} · Lv.{{ profile.character.level ?? 1 }}</p>
                <p v-else>아직 캐릭터가 없어요</p>
              </div>
              <button class="unfriend" :disabled="removing" @click="unfriend">{{ removing ? '삭제하는 중…' : '친구 삭제' }}</button>
            </header>
            <p v-if="actionError" class="error">{{ actionError }}</p>

            <!-- 꾸민 캐릭터와 무대 배경 그대로 -->
            <CharacterStage v-if="profile.character" :character="profile.character" />

            <!-- 깃허브 잔디처럼 최근 연습 -->
            <PracticeLogCard :daily="profile.dailyPractice" :total-minutes="profile.character?.totalPracticeTime ?? 0" />

            <!-- 함께하는 밴드 -->
            <section class="bands-card">
              <h3>함께하는 밴드 <span class="count">{{ profile.bands.length }}</span></h3>
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
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 친구 목록에서 누른 친구의 프로필 팝업 (꾸민 캐릭터·무대, 연습 잔디, 함께하는 밴드)
import { ref, watch } from 'vue'
import ProfileAvatar from '../ProfileAvatar.vue'
import CharacterStage from '../CharacterStage.vue'
import PracticeLogCard from '../workspace/PracticeLogCard.vue'
import { getFriendProfile } from '../../api/friend'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'

const props = defineProps({
  // 볼 친구의 userId. null이면 닫힘
  userId: { type: Number, default: null },
})
// remove: 친구 삭제를 눌렀을 때 (확인·삭제는 목록에서 한 곳으로 처리)
const emit = defineEmits(['close', 'remove'])

const profile = ref(null)
const loadError = ref('')
const actionError = ref('')
const removing = ref(false)

let token = 0
watch(() => props.userId, async (userId) => {
  const mine = ++token
  profile.value = null
  loadError.value = ''
  actionError.value = ''
  removing.value = false
  if (!userId) return
  try {
    const result = await getFriendProfile(userId)
    if (mine === token) profile.value = result
  } catch (e) {
    if (mine === token) loadError.value = apiErrorMessage(e, '친구 프로필을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}, { immediate: true })

const partLabel = (type) => {
  const option = INSTRUMENT_OPTIONS.find((o) => o.value === type)
  return option ? `${option.emoji} ${option.label}` : type
}

const unfriend = () => {
  emit('remove', profile.value)
}

// 목록에서 삭제하다 실패하면 팝업에 보여 준다
defineExpose({
  setRemoving: (value) => { removing.value = value },
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

.bands-card { background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 20px 24px; }
h3 { margin: 0 0 12px; font-size: 0.85rem; font-weight: 800; color: #495057; }
.count { color: #adb5bd; }
.bands { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.band { display: flex; align-items: center; gap: 10px; padding: 10px 2px; border-top: 1px solid #f1f3f5; }
.band:first-child { border-top: none; padding-top: 0; }
.band-name { flex: 1; min-width: 0; font-size: 0.88rem; font-weight: 900; color: #343a40; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.part { font-size: 0.75rem; font-weight: 700; color: #868e96; }
.badge { font-size: 0.68rem; font-weight: 800; color: #2f9e44; background: #ebfbee; padding: 3px 8px; border-radius: 999px; }

.unfriend {
  flex: none; border: 1px solid #e9ecef; background: #fff; border-radius: 10px; padding: 7px 11px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #868e96; white-space: nowrap;
}
.unfriend:hover:not(:disabled) { color: #e03131; border-color: #ffc9c9; background: #fff5f5; }
.unfriend:disabled { opacity: 0.5; }
.unfriend:disabled { cursor: not-allowed; }

.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading, .empty { margin: 0; padding: 8px 0; font-size: 0.85rem; color: #adb5bd; font-weight: 700; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
