<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!busy && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="music-title">
          <h2 id="music-title">요즘 듣는 음악</h2>
          <p class="sub">친구와 밴드 멤버가 내 프로필에서 볼 수 있어요.</p>

          <form @submit.prevent="save">
            <label class="field">
              <span>곡 제목</span>
              <input ref="titleInput" v-model="title" type="text" :maxlength="PROFILE_MUSIC_TITLE_MAX" placeholder="예) Hysteria" :disabled="busy">
            </label>
            <label class="field">
              <span>아티스트</span>
              <input v-model="artist" type="text" :maxlength="PROFILE_MUSIC_ARTIST_MAX" placeholder="예) Muse" :disabled="busy">
            </label>
            <label class="field">
              <span>링크 <em>선택</em></span>
              <input v-model="url" type="url" :maxlength="PROFILE_MUSIC_URL_MAX" placeholder="유튜브·스포티파이 링크" :disabled="busy">
            </label>
            <p v-if="url.trim() && !urlValid" class="hint bad">http:// 또는 https://로 시작하는 링크만 넣을 수 있어요.</p>
            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button v-if="current" type="button" class="btn ghost" :disabled="busy" @click="remove">지우기</button>
              <span class="spacer"></span>
              <button type="button" class="btn" :disabled="busy" @click="emit('close')">취소</button>
              <button type="submit" class="btn primary" :disabled="busy || !valid">{{ busy ? '저장 중…' : '저장' }}</button>
            </div>
          </form>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 요즘 듣는 음악 등록·수정 팝업 (홈 음악 칩에서 연다). 저장하면 내 정보(authStore.profile)를 바로 바꾼다
import { ref, computed, watch, nextTick } from 'vue'
import { useAuthStore } from '../stores/auth'
import {
  updateProfileMusic, deleteProfileMusic,
  PROFILE_MUSIC_TITLE_MAX, PROFILE_MUSIC_ARTIST_MAX, PROFILE_MUSIC_URL_MAX,
} from '../api/user'
import { apiErrorMessage } from '../api/practice'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const authStore = useAuthStore()
const current = computed(() => authStore.profile?.profileMusic || null)

const title = ref('')
const artist = ref('')
const url = ref('')
const busy = ref(false)
const error = ref('')
const titleInput = ref(null)

const urlValid = computed(() => /^https?:\/\/\S+$/i.test(url.value.trim()))
const valid = computed(() => title.value.trim() && artist.value.trim() && (!url.value.trim() || urlValid.value))

watch(() => props.open, async (open) => {
  if (!open) return
  title.value = current.value?.title || ''
  artist.value = current.value?.artist || ''
  url.value = current.value?.url || ''
  error.value = ''
  await nextTick()
  titleInput.value?.focus()
})

const run = async (action, fallback) => {
  busy.value = true
  error.value = ''
  try {
    authStore.profile = await action()
    emit('close')
  } catch (e) {
    error.value = apiErrorMessage(e, fallback)
  } finally {
    busy.value = false
  }
}

const save = () => {
  if (!valid.value || busy.value) return
  run(() => updateProfileMusic({ title: title.value.trim(), artist: artist.value.trim(), url: url.value.trim() || null }),
    '음악을 저장하지 못했어요.')
}

const remove = () => {
  if (busy.value || !window.confirm('요즘 듣는 음악을 지울까요?')) return
  run(() => deleteProfileMusic(), '음악을 지우지 못했어요.')
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 380px; box-sizing: border-box;
  background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a;
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; }
.sub { margin: 0 0 18px; font-size: 0.8rem; color: #868e96; }
.field { display: flex; flex-direction: column; gap: 6px; margin-top: 12px; }
.field:first-of-type { margin-top: 0; }
.field span { font-size: 0.75rem; font-weight: 800; color: #495057; }
.field em { font-style: normal; font-weight: 700; color: #adb5bd; margin-left: 4px; }
.field input {
  border: 1px solid #e9ecef; border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: #f8f9fa;
}
.field input:focus { border-color: #2d5bff; background: #fff; }
.hint { margin: 6px 0 0; font-size: 0.72rem; font-weight: 700; line-height: 1.5; }
.hint.bad { color: #e8590c; }
.error { margin: 12px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.actions { display: flex; align-items: center; gap: 8px; margin-top: 20px; }
.spacer { flex: 1; }
.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer;
  font-weight: 800; font-size: 0.85rem; padding: 10px 14px; border-radius: 12px; font-family: inherit;
}
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn.ghost { border-color: transparent; color: #adb5bd; padding-left: 4px; padding-right: 4px; }
.btn.ghost:hover:not(:disabled) { color: #e03131; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
