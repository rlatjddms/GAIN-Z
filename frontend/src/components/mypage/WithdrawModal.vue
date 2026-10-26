<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!saving && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="withdraw-title">
          <ModalClose :disabled="saving" @close="emit('close')" />
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
              <!-- 한글은 조합이 끝나야 v-model이 값을 받아 버튼이 늦게 켜진다 (#215) → 입력할 때마다 직접 받는다 -->
              <input :value="typed" type="text" :placeholder="CONFIRM_WORD" :disabled="saving" @input="typed = $event.target.value">
            </label>

            <!-- 다른 멤버가 있는 밴드의 리더 (#215): 평소 밴드 나가기처럼 리더를 넘긴 뒤 탈퇴 -->
            <div v-if="leaderBands.length" class="leader-box">
              <p>리더인 밴드가 있어요. 탈퇴하기 전에 다른 멤버에게 리더를 넘겨 주세요.</p>
              <ul>
                <li v-for="b in leaderBands" :key="b.id">
                  <router-link :to="`/bands/${b.id}/settings`" @click="emit('close')">{{ b.name }} 설정에서 리더 넘기기 ›</router-link>
                </li>
              </ul>
            </div>
            <p v-else-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button type="button" class="btn" :disabled="saving" @click="emit('close')">취소</button>
              <button type="submit" class="btn danger solid" :disabled="saving || !valid">{{ saving ? '탈퇴하는 중…' : '탈퇴하기' }}</button>
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
import ModalClose from '../ui/ModalClose.vue'

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
// 리더를 넘겨야 하는 밴드 [{ id, name }]
const leaderBands = ref([])
// 백엔드 ErrorCode.BAND_LEADER_MUST_HAND_OVER
const BAND_LEADER_MUST_HAND_OVER = 'B039'

const valid = computed(() => typed.value.trim() === CONFIRM_WORD && (!props.needsPassword || password.value))

watch(() => props.open, (open) => {
  if (!open) return
  password.value = ''
  typed.value = ''
  error.value = ''
  leaderBands.value = []
})

const submit = async () => {
  if (!valid.value || saving.value) return
  saving.value = true
  error.value = ''
  leaderBands.value = []
  try {
    await withdraw(props.needsPassword ? password.value : null)
    emit('done')
  } catch (e) {
    const body = e.response?.data
    if (body?.code === BAND_LEADER_MUST_HAND_OVER) leaderBands.value = body.data?.bands || []
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
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 24px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; color: var(--c-danger); }
.sub { margin: 0 0 14px; font-size: 0.8rem; color: var(--c-muted); }

.notice {
  margin: 0 0 16px; padding: 12px 14px 12px 30px; background: var(--c-danger-soft); border-radius: 12px;
  font-size: 0.78rem; font-weight: 700; color: #c92a2a; line-height: 1.7;
}

.field { display: flex; flex-direction: column; gap: 6px; margin-top: 12px; }
.field:first-of-type { margin-top: 0; }
.field span { font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); }
.field b { color: var(--c-danger); }
.field input {
  border: 1px solid var(--c-line); border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: var(--c-bg);
}
.field input:focus { border-color: var(--c-danger); background: #fff; }
.leader-box { margin: 12px 0 0; padding: 12px 14px; border-radius: 12px; background: var(--c-danger-soft); font-size: 0.8rem; line-height: 1.5; }
.leader-box p { margin: 0 0 6px; font-weight: 700; color: var(--c-danger); }
.leader-box ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 4px; }
.leader-box a { font-weight: 800; color: var(--c-text); text-decoration: none; }
.error { margin: 12px 0 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }

.actions { display: flex; gap: 8px; margin-top: 20px; }
.btn { flex: 1; min-height: 40px; font-size: 0.85rem; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
/* 휴대폰: 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .modal {
    width: 100%; max-width: none; max-height: 92dvh;
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    padding-bottom: calc(24px + env(safe-area-inset-bottom));
  }
}
/* 닫기 버튼 자리 (#292) */
.modal { position: relative; }
h2 { padding-right: 40px; }

</style>
