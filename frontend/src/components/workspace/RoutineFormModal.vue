<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!saving && emit('close')">
        <form class="modal" role="dialog" aria-modal="true" aria-labelledby="routine-form-title" @submit.prevent="submit">
          <h2 id="routine-form-title">{{ routine ? '루틴 수정' : '루틴 추가' }}</h2>

          <label class="field">
            <span class="label">이름</span>
            <input ref="nameInput" v-model="form.content" class="input" maxlength="50" placeholder="예: 스케일 연습">
          </label>

          <label class="field">
            <span class="label">목표 횟수</span>
            <span class="inline">
              <input v-model.number="form.targetCount" class="input count" type="number" min="1" max="99">회
            </span>
          </label>

          <div class="field">
            <span class="label">반복</span>
            <div class="segment" role="radiogroup" aria-label="반복 방식">
              <button
                v-for="t in REPEAT_TYPES" :key="t.key" type="button" role="radio"
                :aria-checked="form.repeatType === t.key" :class="{ on: form.repeatType === t.key }"
                @click="form.repeatType = t.key"
              >{{ t.label }}</button>
            </div>
          </div>

          <div v-if="form.repeatType === 'WEEKLY'" class="field">
            <span class="label">요일</span>
            <div class="days">
              <button
                v-for="d in WEEK_DAYS" :key="d.key" type="button"
                :aria-pressed="form.repeatDays.includes(d.key)" :class="{ on: form.repeatDays.includes(d.key) }"
                @click="toggleDay(d.key)"
              >{{ d.label }}</button>
            </div>
          </div>

          <div class="field">
            <span class="label">{{ form.repeatType === 'NONE' ? '날짜' : '기간' }}</span>
            <div class="period">
              <input v-model="form.startDate" class="input" type="date" aria-label="시작일">
              <template v-if="form.repeatType !== 'NONE'">
                <span>~</span>
                <input v-model="form.endDate" class="input" type="date" :min="form.startDate" aria-label="종료일">
                <button v-if="form.endDate" type="button" class="clear" @click="form.endDate = ''">계속</button>
              </template>
            </div>
            <span v-if="form.repeatType !== 'NONE' && !form.endDate" class="help">종료일을 비우면 계속 반복해요.</span>
          </div>

          <p v-if="invalidReason" class="help warn">{{ invalidReason }}</p>
          <p v-if="error" class="error">{{ error }}</p>

          <div class="actions">
            <button v-if="routine" type="button" class="btn danger" :disabled="saving" @click="stop">그만하기</button>
            <button type="button" class="btn" :disabled="saving" @click="emit('close')">취소</button>
            <button type="submit" class="btn primary" :disabled="saving || !!invalidReason">{{ saving ? '저장 중…' : '저장' }}</button>
          </div>
        </form>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { reactive, ref, computed, watch, nextTick } from 'vue'
import { createRoutine, updateRoutine, deleteRoutine } from '../../api/routine'
import { apiErrorMessage } from '../../api/practice'
import { dateKey } from '../../constants/practice'
import { REPEAT_TYPES, WEEK_DAYS } from '../../constants/routine'

// routine: 수정할 루틴 (null이면 새로 추가)
const props = defineProps({
  open: { type: Boolean, default: false },
  routine: { type: Object, default: null },
})
// saved: 추가·수정·그만하기 후 목록을 다시 불러오게 알린다
const emit = defineEmits(['close', 'saved'])

const form = reactive({ content: '', targetCount: 10, repeatType: 'DAILY', repeatDays: [], startDate: '', endDate: '' })
const saving = ref(false)
const error = ref('')
const nameInput = ref(null)

watch(() => props.open, async (v) => {
  if (!v) return
  const r = props.routine
  Object.assign(form, r
    ? { content: r.content, targetCount: r.targetCount, repeatType: r.repeatType || 'DAILY', repeatDays: [...(r.repeatDays || [])], startDate: r.startDate || dateKey(), endDate: r.endDate || '' }
    : { content: '', targetCount: 10, repeatType: 'DAILY', repeatDays: [], startDate: dateKey(), endDate: '' })
  error.value = ''
  saving.value = false
  await nextTick()
  nameInput.value?.focus()
})

const toggleDay = (key) => {
  form.repeatDays = form.repeatDays.includes(key) ? form.repeatDays.filter((d) => d !== key) : [...form.repeatDays, key]
}

// 저장 전에 알려 줄 문제 (서버 검증과 같은 규칙)
const invalidReason = computed(() => {
  if (!form.content.trim()) return ''
  if (!(form.targetCount >= 1 && form.targetCount <= 99)) return '목표 횟수는 1~99회로 정해 주세요.'
  if (!form.startDate) return '날짜를 골라 주세요.'
  if (form.repeatType === 'WEEKLY' && !form.repeatDays.length) return '반복할 요일을 하나 이상 골라 주세요.'
  if (form.repeatType !== 'NONE' && form.endDate && form.endDate < form.startDate) return '종료일은 시작일 이후로 골라 주세요.'
  return ''
})

const submit = async () => {
  if (!form.content.trim() || invalidReason.value) return
  saving.value = true
  error.value = ''
  const body = {
    content: form.content.trim(),
    targetCount: form.targetCount,
    repeatType: form.repeatType,
    repeatDays: form.repeatType === 'WEEKLY' ? form.repeatDays : [],
    startDate: form.startDate,
    endDate: form.repeatType === 'NONE' ? null : form.endDate,
  }
  try {
    if (props.routine) await updateRoutine(props.routine.id, body)
    else await createRoutine(body)
    emit('saved')
    emit('close')
  } catch (e) {
    error.value = `저장하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

const stop = async () => {
  if (!window.confirm(`'${props.routine.content}' 루틴을 오늘부터 그만할까요?\n지난 체크 기록은 그대로 남아요.`)) return
  saving.value = true
  error.value = ''
  try {
    await deleteRoutine(props.routine.id)
    emit('saved')
    emit('close')
  } catch (e) {
    error.value = `그만하지 못했어요. ${apiErrorMessage(e)}`
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
  width: 100%; max-width: 400px; max-height: calc(100vh - 32px); overflow-y: auto; box-sizing: border-box;
  background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a;
  display: flex; flex-direction: column; gap: 14px;
}
h2 { margin: 0 0 2px; font-size: 1.05rem; font-weight: 900; }

.field { display: flex; flex-direction: column; gap: 6px; }
.label { font-size: 0.75rem; font-weight: 800; color: #868e96; }
.input {
  border: 1px solid #dee2e6; border-radius: 10px; padding: 9px 11px; font: inherit; font-size: 0.9rem; font-weight: 700;
  outline: none; min-width: 0; box-sizing: border-box;
}
.input:focus { border-color: #2d5bff; }
.inline { display: flex; align-items: center; gap: 6px; font-size: 0.85rem; font-weight: 700; color: #868e96; }
.input.count { width: 72px; text-align: right; }

.segment { display: flex; background: #f1f3f5; border-radius: 11px; padding: 3px; }
.segment button {
  flex: 1; border: none; background: none; border-radius: 9px; padding: 8px 0; cursor: pointer;
  font: inherit; font-size: 0.8rem; font-weight: 800; color: #868e96;
}
.segment button.on { background: #fff; color: #2d5bff; box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06); }

.days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.days button {
  aspect-ratio: 1; border: 1px solid #e9ecef; background: #fff; border-radius: 10px; cursor: pointer;
  font: inherit; font-size: 0.8rem; font-weight: 800; color: #868e96;
}
.days button.on { background: #2d5bff; border-color: #2d5bff; color: #fff; }

.period { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-weight: 800; color: #adb5bd; }
.period .input { flex: 1; min-width: 130px; font-size: 0.8rem; }
.clear {
  border: none; background: #f1f3f5; color: #495057; border-radius: 8px; padding: 6px 9px; cursor: pointer;
  font: inherit; font-size: 0.7rem; font-weight: 800;
}
.help { margin: 0; font-size: 0.72rem; color: #adb5bd; }
.help.warn { color: #f08c00; font-weight: 700; }
.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }

.actions { display: flex; gap: 8px; margin-top: 4px; }
.btn {
  flex: 1; border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer;
  font-weight: 800; font-size: 0.85rem; padding: 11px; border-radius: 12px; font-family: inherit;
}
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn.danger { flex: none; color: #e03131; border-color: #ffc9c9; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
