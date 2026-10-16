<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!saving && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="withdraw-title">
          <h2 id="withdraw-title">회원 탈퇴</h2>
          <p class="sub">탈퇴하면 되돌릴 수 없어요. 아래 내용을 꼭 확인해 주세요.</p>

          <ul class="notice">
            <li>캐릭터, 레벨·경험치, 옷장 아이템이 모두 사라져요.</li>
            <li>연습 기록, 루틴, 악보 보관함도 다시 볼 수 없어요.</li>
            <li>같은 이메일로 다시 가입하면 처음부터 시작해요.</li>
          </ul>

          <form @submit.prevent="submit">
            <label v-if="needsPassword" class="field">
              <span>비밀번호 확인</span>
              <input v-model="password" type="password" autocomplete="current-password" :disabled="saving">
            </label>
            <label class="field">
              <span>확인을 위해 <b>{{ CONFIRM_WORD }}</b>라고 입력해 주세요</span>
              <input v-model="typed" type="text" :placeholder="CONFIRM_WORD" :disabled="saving">
            </label>

            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button type="button" class="btn" :disabled="saving" @click="emit('close')">취소</button>
              <button type="submit" class="btn danger" :disabled="saving || !valid">{{ saving ? '탈퇴하는 중…' : '탈퇴하기' }}</button>
            </div>
          </form>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { withdraw } from '../../api/user'
import { apiErrorMessage } from '../../api/practice'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 이메일 가입자는 비밀번호로 한 번 더 확인
  needsPassword: { type: Boolean, default: false },
})
// done: 탈퇴 완료
const emit = defineEmits(['close', 'done'])

const CONFIRM_WORD = '탈퇴'

const password = ref('')
const typed = ref('')
const saving = ref(false)
const error = ref('')

const valid = computed(() => typed.value.trim() === CONFIRM_WORD && (!props.needsPassword || password.value))

watch(() => props.open, (open) => {
  if (!open) return
  password.value = ''
  typed.value = ''
  error.value = ''
})

const submit = async () => {
  if (!valid.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await withdraw(props.needsPassword ? password.value : null)
    emit('done')
  } catch (e) {
    error.value = apiErrorMessage(e, '탈퇴하지 못했어요. 잠시 후 다시 시도해 주세요.')
    saving.value = false
  }
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
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; color: #e03131; }
.sub { margin: 0 0 14px; font-size: 0.8rem; color: #868e96; }

.notice {
  margin: 0 0 16px; padding: 12px 14px 12px 30px; background: #fff5f5; border-radius: 12px;
  font-size: 0.78rem; font-weight: 700; color: #c92a2a; line-height: 1.7;
}

.field { display: flex; flex-direction: column; gap: 6px; margin-top: 12px; }
.field:first-of-type { margin-top: 0; }
.field span { font-size: 0.75rem; font-weight: 800; color: #495057; }
.field b { color: #e03131; }
.field input {
  border: 1px solid #e9ecef; border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: #f8f9fa;
}
.field input:focus { border-color: #e03131; background: #fff; }
.error { margin: 12px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }

.actions { display: flex; gap: 8px; margin-top: 20px; }
.btn {
  flex: 1; border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer;
  font-weight: 800; font-size: 0.9rem; padding: 12px; border-radius: 12px; font-family: inherit;
}
.btn.danger { background: #e03131; border-color: #e03131; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
