<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!saving && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="password-title">
          <h2 id="password-title">비밀번호 바꾸기</h2>
          <p class="sub">지금 비밀번호를 확인한 뒤 새 비밀번호로 바꿔요.</p>

          <form @submit.prevent="submit">
            <label class="field">
              <span>지금 비밀번호</span>
              <input v-model="current" type="password" autocomplete="current-password" :disabled="saving">
            </label>
            <label class="field">
              <span>새 비밀번호</span>
              <input v-model="next" type="password" autocomplete="new-password" placeholder="8~16자 영문, 숫자, 특수문자" :disabled="saving">
            </label>
            <p v-if="next && !nextValid" class="hint bad">8~16자로 영문, 숫자, 특수문자를 모두 넣어 주세요. (공백 없이)</p>
            <p v-else-if="next && next === current" class="hint bad">지금 비밀번호와 다르게 정해 주세요.</p>
            <label class="field">
              <span>새 비밀번호 확인</span>
              <input v-model="confirm" type="password" autocomplete="new-password" :disabled="saving">
            </label>
            <p v-if="confirm && confirm !== next" class="hint bad">새 비밀번호와 같지 않아요.</p>

            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button type="button" class="btn" :disabled="saving" @click="emit('close')">취소</button>
              <button type="submit" class="btn primary" :disabled="saving || !valid">{{ saving ? '바꾸는 중…' : '바꾸기' }}</button>
            </div>
          </form>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { changePassword } from '../../api/user'
import { apiErrorMessage } from '../../api/practice'

const props = defineProps({
  open: { type: Boolean, default: false },
})
// done: 바꾸기 성공
const emit = defineEmits(['close', 'done'])

// 백엔드 SignupRequest·PasswordChangeRequest와 같은 규칙
const PASSWORD_PATTERN = /^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\W)(?=\S+$).{8,16}$/

const current = ref('')
const next = ref('')
const confirm = ref('')
const saving = ref(false)
const error = ref('')

const nextValid = computed(() => PASSWORD_PATTERN.test(next.value))
const valid = computed(() => current.value && nextValid.value && next.value !== current.value && confirm.value === next.value)

watch(() => props.open, (open) => {
  if (!open) return
  current.value = ''
  next.value = ''
  confirm.value = ''
  error.value = ''
})

const submit = async () => {
  if (!valid.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    await changePassword({ currentPassword: current.value, newPassword: next.value })
    emit('done')
  } catch (e) {
    error.value = apiErrorMessage(e, '비밀번호를 바꾸지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
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
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 24px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; }
.sub { margin: 0 0 18px; font-size: 0.8rem; color: var(--c-muted); }

.field { display: flex; flex-direction: column; gap: 6px; margin-top: 12px; }
.field:first-of-type { margin-top: 0; }
.field span { font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); }
.field input {
  border: 1px solid var(--c-line); border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: var(--c-bg);
}
.field input:focus { border-color: var(--c-primary); background: #fff; }
.hint { margin: 6px 0 0; font-size: 0.72rem; font-weight: 700; line-height: 1.5; }
.hint.bad { color: #e8590c; }
.error { margin: 12px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }

.actions { display: flex; gap: 8px; margin-top: 20px; }
.btn { flex: 1; min-height: 44px; font-size: 0.88rem; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
