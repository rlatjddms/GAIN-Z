<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="date" class="backdrop" @click.self="!saving && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="edit-day-title">
          <h2 id="edit-day-title">{{ dateLabel(date) }} 연습 시간</h2>
          <p class="sub">기록을 놓쳤거나 잘못 잰 날은 직접 고칠 수 있어요.</p>

          <div class="inputs">
            <label>
              <input v-model.number="hours" type="number" min="0" max="24" inputmode="numeric" :disabled="saving">
              <span>시간</span>
            </label>
            <label>
              <input v-model.number="minutes" type="number" min="0" max="59" step="5" inputmode="numeric" :disabled="saving">
              <span>분</span>
            </label>
          </div>
          <div class="quick">
            <button v-for="q in QUICK" :key="q" class="btn small" :disabled="saving" @click="addMinutes(q)">+{{ q }}분</button>
          </div>

          <p class="note">지금 기록: {{ durationText(currentSeconds) }}</p>
          <p class="note">직접 입력한 시간은 하루 30분까지만 경험치로 쳐요. 줄이면 직접 입력분부터 빠지고, 줄어든 만큼 경험치가 빠져요 (레벨은 그대로예요).</p>
          <p v-if="error" class="error">{{ error }}</p>

          <div class="actions">
            <button class="btn" :disabled="saving" @click="emit('close')">취소</button>
            <button class="btn primary" :disabled="saving || !valid" @click="submit">{{ saving ? '저장 중…' : '저장' }}</button>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { usePracticeStore } from '../../stores/practice'
import { apiErrorMessage } from '../../api/practice'
import { dateLabel, durationText, MAX_SECONDS_PER_DAY } from '../../constants/practice'

// date: 'YYYY-MM-DD' (null이면 닫힘)
const props = defineProps({ date: { type: String, default: null } })
// saved: 저장 결과 (레벨업 표시용)
const emit = defineEmits(['close', 'saved'])
const practice = usePracticeStore()

const QUICK = [10, 30, 60]
const hours = ref(0)
const minutes = ref(0)
const saving = ref(false)
const error = ref('')

const currentSeconds = computed(() => (props.date ? practice.totalOf(props.date) : 0))
watch(() => props.date, (d) => {
  if (!d) return
  hours.value = Math.floor(currentSeconds.value / 3600)
  minutes.value = Math.floor((currentSeconds.value % 3600) / 60)
  error.value = ''
  saving.value = false
})

const totalSeconds = computed(() => (Number(hours.value) || 0) * 3600 + (Number(minutes.value) || 0) * 60)
const valid = computed(() => hours.value >= 0 && minutes.value >= 0 && minutes.value < 60 && totalSeconds.value <= MAX_SECONDS_PER_DAY)

const addMinutes = (m) => {
  const t = Math.min(totalSeconds.value + m * 60, MAX_SECONDS_PER_DAY)
  hours.value = Math.floor(t / 3600)
  minutes.value = Math.floor((t % 3600) / 60)
}

const submit = async () => {
  saving.value = true
  error.value = ''
  try {
    const result = await practice.updateDay(props.date, totalSeconds.value)
    emit('saved', result)
    emit('close')
  } catch (e) {
    error.value = `저장하지 못했어요. ${apiErrorMessage(e)}`
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

.inputs { display: flex; gap: 10px; }
.inputs label { flex: 1; display: flex; align-items: center; gap: 8px; background: var(--c-bg); border-radius: 12px; padding: 10px 14px; }
.inputs input {
  width: 100%; min-width: 0; border: none; background: none; outline: none; font-family: inherit;
  font-size: 1.4rem; font-weight: 900; text-align: right; font-variant-numeric: tabular-nums;
}
.inputs span { flex: none; white-space: nowrap; font-size: 0.85rem; font-weight: 800; color: var(--c-muted); }
.quick { display: flex; gap: 6px; margin-top: 10px; }
.quick .btn { flex: 1; }

.note { margin: 14px 0 0; font-size: 0.75rem; color: var(--c-faint); line-height: 1.5; }
.error { margin: 8px 0 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }

.actions { display: flex; gap: 8px; margin-top: 18px; }
.btn { flex: 1; min-height: 44px; font-size: 0.88rem; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
