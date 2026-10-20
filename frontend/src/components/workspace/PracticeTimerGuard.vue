<template>
  <!-- 타이머가 2시간 넘게 켜져 있으면 '아직 연습 중이에요?' 확인. 10분 동안 답이 없으면 마지막 확인 시점에 멈춘다 -->
  <Teleport to="body">
    <transition name="modal">
      <div v-if="asking || stoppedAt" class="backdrop">
        <section class="modal" role="alertdialog" aria-modal="true" aria-labelledby="timer-guard-title">
          <template v-if="asking">
            <div class="icon" aria-hidden="true"><PixelIcon name="clock" :size="36" /></div>
            <h2 id="timer-guard-title">아직 연습 중이에요?</h2>
            <p class="sub">타이머가 {{ durationText(runningSeconds) }} 동안 켜져 있어요.</p>
            <p class="countdown">{{ countdownText }} 뒤에 답이 없으면 {{ clockText(cutoff) }}에 멈춰요</p>
            <div class="actions">
              <button class="btn" :disabled="saving" @click="stopNow">여기서 멈출게요</button>
              <button class="btn primary" :disabled="saving" @click="keepGoing">계속 연습할게요</button>
            </div>
          </template>
          <template v-else>
            <div class="icon" aria-hidden="true"><Pause :size="32" :stroke-width="2.4" /></div>
            <h2 id="timer-guard-title">타이머를 멈췄어요</h2>
            <p class="sub">2시간 동안 확인이 없어 {{ clockText(stoppedAt) }}까지만 기록했어요. 더 연습했다면 연습 화면에서 그날 시간을 고칠 수 있어요.</p>
            <p v-if="error" class="error">{{ error }}</p>
            <div class="actions">
              <button class="btn primary" @click="stoppedAt = null">확인</button>
            </div>
          </template>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { Pause } from 'lucide-vue-next'
import PixelIcon from '../../components/PixelIcon.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { usePracticeStore } from '../../stores/practice'
import { apiErrorMessage } from '../../api/practice'
import { durationText } from '../../constants/practice'

// 확인 간격과 답을 기다리는 시간
const CHECK_MS = 2 * 60 * 60 * 1000
const GRACE_MS = 10 * 60 * 1000

const timer = usePracticeTimerStore()
const practice = usePracticeStore()

const now = ref(Date.now())
const saving = ref(false)
const error = ref('')
// 자동으로 멈춘 시각 (안내를 보여 주는 동안)
const stoppedAt = ref(null)

const confirmedAt = computed(() => timer.confirmedAt ?? timer.startedAt)
// 이 시각까지는 연습한 것으로 본다 (마지막 확인 + 2시간)
const cutoff = computed(() => confirmedAt.value + CHECK_MS)
const asking = computed(() => timer.running && now.value >= cutoff.value)
const runningSeconds = computed(() => (timer.running ? Math.floor((now.value - timer.startedAt) / 1000) : 0))
const countdownText = computed(() => {
  const left = Math.max(0, Math.ceil((cutoff.value + GRACE_MS - now.value) / 1000))
  return `${Math.floor(left / 60)}분 ${String(left % 60).padStart(2, '0')}초`
})

const clockText = (ms) => {
  const d = new Date(ms)
  return `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 날짜별 구간을 저장 (실패한 구간은 타이머 저장소에 남겨 연습 화면에서 다시 저장)
const saveParts = async (parts) => {
  saving.value = true
  error.value = ''
  let remaining = [...parts]
  try {
    for (const part of parts) {
      await practice.recordSession(part.date, part.seconds)
      remaining = remaining.slice(1)
    }
    timer.setPending([])
  } catch (e) {
    timer.setPending(remaining)
    error.value = `기록을 저장하지 못했어요. 연습 화면에서 다시 저장할 수 있어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

const keepGoing = () => timer.confirm()

const stopNow = () => saveParts(timer.stop())

const check = () => {
  timer.load()
  now.value = Date.now()
  if (!timer.running || saving.value) return
  // 10분 동안 답이 없으면 마지막 확인 + 2시간 시점에서 멈춘다 (창을 닫아 둔 사이에 지난 경우도)
  if (now.value >= cutoff.value + GRACE_MS) {
    const at = cutoff.value
    stoppedAt.value = at
    saveParts(timer.stop(at))
  }
}

let tick = null
onMounted(() => {
  check()
  tick = setInterval(check, 1000)
})
onBeforeUnmount(() => clearInterval(tick))
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2600; background: rgba(15, 23, 42, 0.5);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 360px; box-sizing: border-box; text-align: center;
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 26px 24px 22px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
.icon { color: var(--c-primary-text); display: flex; justify-content: center; }
h2 { margin: 8px 0 6px; font-size: 1.1rem; font-weight: 900; }
.sub { margin: 0; font-size: 0.85rem; font-weight: 700; color: var(--c-text-2); line-height: 1.5; }
.countdown { margin: 10px 0 0; font-size: 0.78rem; font-weight: 800; color: #e8590c; font-variant-numeric: tabular-nums; }
.error { margin: 10px 0 0; font-size: 0.78rem; font-weight: 700; color: #e03131; }
.actions { display: flex; gap: 8px; margin-top: 20px; }
.btn { flex: 1; min-height: 44px; font-size: 0.88rem; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
