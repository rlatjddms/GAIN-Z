<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="friend" class="backdrop" @click.self="!busy && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="friend-title">
          <button class="close" :disabled="busy" aria-label="닫기" @click="emit('close')">×</button>

          <div class="stage">
            <PixelCharacter :character="friend.character" :outfit="friend.character?.currentOutfitSpriteKey || 'DEFAULT'" :scale="5" />
          </div>

          <div class="who">
            <ProfileAvatar :src="friend.profileImageUrl" :size="40" :alt="`${friend.name} 프로필 사진`" />
            <div>
              <h2 id="friend-title">{{ friend.name }}</h2>
              <p v-if="friend.character">{{ friend.character.nickname }} · Lv.{{ friend.character.level ?? 1 }}</p>
              <p v-else>아직 캐릭터가 없어요</p>
            </div>
          </div>

          <dl class="info">
            <div v-if="instrumentLabel" class="row">
              <dt>악기</dt>
              <dd>{{ instrumentLabel }}</dd>
            </div>
            <div v-if="friend.character" class="row">
              <dt>연속 연습</dt>
              <dd>{{ friend.character.currentStreak ?? 0 }}일 (최고 {{ friend.character.maxStreak ?? 0 }}일)</dd>
            </div>
            <div v-if="sinceText" class="row">
              <dt>친구가 된 날</dt>
              <dd>{{ sinceText }}</dd>
            </div>
          </dl>

          <p v-if="error" class="error">{{ error }}</p>
          <button class="unfriend" :disabled="busy" @click="unfriend">{{ busy ? '끊는 중…' : '친구 끊기' }}</button>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 친구 목록에서 누른 친구 정보 (프로필 카드가 생기면 그 카드로 바꾼다 #109)
import { ref, computed, watch } from 'vue'
import PixelCharacter from '../PixelCharacter.vue'
import ProfileAvatar from '../ProfileAvatar.vue'
import { removeFriend } from '../../api/friend'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'

const props = defineProps({
  // 친구 목록의 항목. null이면 닫힘
  friend: { type: Object, default: null },
})
// removed: 친구 끊기 성공 (userId)
const emit = defineEmits(['close', 'removed'])

const busy = ref(false)
const error = ref('')

watch(() => props.friend, () => { error.value = '' })

const instrumentLabel = computed(() => {
  const type = props.friend?.character?.instrumentType
  const option = INSTRUMENT_OPTIONS.find((o) => o.value === type)
  return option ? `${option.emoji} ${option.label}` : ''
})

const sinceText = computed(() => {
  const at = props.friend?.since
  if (!at) return ''
  const d = new Date(at)
  return Number.isNaN(d.getTime()) ? '' : `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
})

const unfriend = async () => {
  if (busy.value) return
  if (!window.confirm(`${props.friend.name} 님과 친구를 끊을까요?`)) return
  busy.value = true
  error.value = ''
  try {
    await removeFriend(props.friend.userId)
    emit('removed', props.friend.userId)
  } catch (e) {
    error.value = apiErrorMessage(e, '친구를 끊지 못했어요.')
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  position: relative; width: 100%; max-width: 340px; box-sizing: border-box;
  background: #fff; border-radius: 24px; padding: 20px 22px 22px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a;
}
.close {
  position: absolute; top: 12px; right: 14px; border: none; background: none; cursor: pointer;
  font-size: 1.4rem; line-height: 1; color: #adb5bd;
}
.close:hover:not(:disabled) { color: #495057; }
.stage {
  display: flex; justify-content: center; align-items: flex-end; height: 190px; margin-bottom: 14px;
  border-radius: 16px; background: linear-gradient(#eef2ff, #f8f9fa);
}
.who { display: flex; align-items: center; gap: 12px; }
h2 { margin: 0; font-size: 1.05rem; font-weight: 900; }
.who p { margin: 2px 0 0; font-size: 0.78rem; font-weight: 700; color: #868e96; }

.info { margin: 14px 0 0; }
.row { display: grid; grid-template-columns: 84px minmax(0, 1fr); gap: 10px; padding: 9px 0; border-top: 1px solid #f1f3f5; }
dt { font-size: 0.72rem; font-weight: 800; color: #868e96; }
dd { margin: 0; font-size: 0.82rem; font-weight: 700; color: #343a40; }

.error { margin: 10px 0 0; font-size: 0.78rem; color: #e03131; font-weight: 700; }
.unfriend {
  margin-top: 14px; border: none; background: none; padding: 0; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 700; color: #adb5bd; text-decoration: underline;
}
.unfriend:hover:not(:disabled) { color: #e03131; }
.unfriend:disabled { cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
