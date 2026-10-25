<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!saving && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="band-start-title">
          <div class="segment fill tabs" role="tablist">
            <button type="button" role="tab" :aria-selected="tab === 'create'" :class="{ on: tab === 'create' }"
                    :disabled="saving" @click="tab = 'create'">밴드 만들기</button>
            <button type="button" role="tab" :aria-selected="tab === 'join'" :class="{ on: tab === 'join' }"
                    :disabled="saving" @click="tab = 'join'">초대 코드로 가입</button>
          </div>

          <h2 id="band-start-title">{{ tab === 'create' ? '새 밴드 만들기' : '초대 코드로 가입하기' }}</h2>
          <p class="sub">
            {{ tab === 'create'
              ? '만든 사람이 리더가 돼요. 만든 뒤 초대 코드를 멤버에게 알려 주세요.'
              : '리더에게 받은 8자리 초대 코드를 넣어 주세요.' }}
          </p>

          <form @submit.prevent="submit">
            <template v-if="tab === 'create'">
              <label class="field">
                <span>밴드 이름</span>
                <input v-model="name" type="text" :maxlength="BAND_NAME_MAX" placeholder="밴드명을 입력해주세요" :disabled="saving">
              </label>
              <label class="field">
                <span>소개 <em>(선택)</em></span>
                <textarea v-model="description" rows="2" :maxlength="BAND_DESCRIPTION_MAX"
                          placeholder="밴드 소개를 입력해주세요" :disabled="saving"></textarea>
              </label>
            </template>
            <label v-else class="field">
              <span>초대 코드</span>
              <input :value="code" type="text" class="code-input" :maxlength="INVITE_CODE_LENGTH" placeholder="초대 코드를 입력해주세요"
                     autocomplete="off" spellcheck="false" :disabled="saving" @input="onCodeInput">
            </label>

            <div class="field">
              <span>내 파트</span>
              <div class="parts">
                <button v-for="o in INSTRUMENT_OPTIONS" :key="o.value" type="button" class="part"
                        :class="{ active: part === o.value }" :disabled="saving" @click="part = o.value">
                  <span class="emoji">{{ o.emoji }}</span>{{ o.label }}
                </button>
              </div>
            </div>

            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button type="button" class="btn" :disabled="saving" @click="emit('close')">취소</button>
              <button type="submit" class="btn primary" :disabled="saving || !valid">
                {{ saving ? '잠시만요…' : tab === 'create' ? '만들기' : '가입하기' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { createBand, joinBand, BAND_NAME_MAX, BAND_DESCRIPTION_MAX, INVITE_CODE_LENGTH } from '../../api/band'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 처음 보여 줄 탭: create | join
  initialTab: { type: String, default: 'create' },
  // 내 파트 기본값 (캐릭터 악기)
  defaultPart: { type: String, default: 'GUITAR' },
  // 초대 코드를 미리 채워 둘 때 (친구가 보낸 밴드 초대 알림, #229)
  initialCode: { type: String, default: '' },
})
// done(band): 만들거나 가입한 밴드 상세
const emit = defineEmits(['close', 'done'])

const tab = ref('create')
const name = ref('')
const description = ref('')
const code = ref('')
const part = ref('GUITAR')
const saving = ref(false)
const error = ref('')

watch(() => props.open, (open) => {
  if (!open) return
  tab.value = props.initialTab
  name.value = ''
  description.value = ''
  code.value = props.initialCode.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, INVITE_CODE_LENGTH)
  part.value = props.defaultPart || 'GUITAR'
  error.value = ''
}, { immediate: true }) // 알림으로 들어와 처음부터 열린 채 그려질 때도 탭·코드를 채운다 (#229)
watch(tab, () => { error.value = '' })

// 코드는 대문자·숫자만 (붙여 넣은 공백·하이픈은 뺀다)
const onCodeInput = (e) => {
  code.value = e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, INVITE_CODE_LENGTH)
  e.target.value = code.value
}

const valid = computed(() => {
  if (!part.value) return false
  return tab.value === 'create' ? name.value.trim().length > 0 : code.value.length === INVITE_CODE_LENGTH
})

const submit = async () => {
  if (!valid.value || saving.value) return
  saving.value = true
  error.value = ''
  try {
    const band = tab.value === 'create'
      ? await createBand({ name: name.value.trim(), description: description.value.trim() || null, part: part.value })
      : await joinBand({ inviteCode: code.value, part: part.value })
    emit('done', band)
  } catch (e) {
    error.value = apiErrorMessage(e, tab.value === 'create' ? '밴드를 만들지 못했어요.' : '가입하지 못했어요.')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.tabs { margin-bottom: 18px; }
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 420px; box-sizing: border-box;
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 24px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; }
.sub { margin: 0 0 18px; font-size: 0.8rem; color: var(--c-muted); line-height: 1.5; }

.field { display: flex; flex-direction: column; gap: 8px; margin-top: 18px; }
form > .field:first-child { margin-top: 0; }
.field > span { font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); }
.field em { font-style: normal; font-weight: 600; color: var(--c-faint); }
.field input, .field textarea {
  border: 1px solid var(--c-line); border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: var(--c-bg); resize: none;
}
.field input:focus, .field textarea:focus { border-color: var(--c-primary); background: #fff; }
.code-input { font-family: ui-monospace, Menlo, Consolas, monospace !important; letter-spacing: 4px; font-weight: 800; text-align: center; font-size: 1.1rem !important; }
.code-input::placeholder { font-family: 'Pretendard', sans-serif; letter-spacing: 0; font-weight: 600; font-size: 0.9rem; }

.parts { display: flex; flex-wrap: wrap; gap: 6px; }
.part {
  border: 1px solid var(--c-line); background: #fff; border-radius: 10px; padding: 7px 10px; cursor: pointer;
  font-family: inherit; font-size: 0.78rem; font-weight: 700; color: var(--c-text-2); display: flex; align-items: center; gap: 4px;
}
.part.active { border-color: var(--c-primary); background: var(--c-primary-soft); color: var(--c-primary-text); }
.part .emoji { font-size: 0.95rem; }

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
</style>
