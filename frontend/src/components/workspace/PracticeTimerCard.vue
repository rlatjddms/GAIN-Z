<template>
  <WsCard title="연습 타이머">
    <template #actions>
      <span v-if="timer.status === 'running'" class="badge live"><i></i>연습 중</span>
      <span v-else-if="timer.status === 'paused'" class="badge">일시정지</span>
    </template>

    <div class="timer-display" :class="timer.status">{{ display }}</div>

    <div class="timer-btns">
      <button v-if="timer.status === 'idle'" class="btn primary" @click="timer.start()">시작</button>
      <template v-else>
        <button v-if="timer.status === 'running'" class="btn" @click="timer.pause()">일시정지</button>
        <button v-else class="btn" @click="timer.start()">계속</button>
        <button class="btn primary" :disabled="elapsedSeconds < MIN_DURATION_SECONDS" @click="finish">종료하고 기록</button>
      </template>
    </div>

    <p class="hint">끝나면 달성률을 입력해 기록하고, 연습한 만큼 경험치를 받아요. 화면을 나가도 타이머는 계속 돌아가요.</p>

    <FinishPracticeModal
      :open="isFinishing"
      :duration-seconds="finishedSeconds"
      @cancel="cancelFinish"
      @discard="discard"
      @recorded="onRecorded"
    />
  </WsCard>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import WsCard from './WsCard.vue'
import FinishPracticeModal from './FinishPracticeModal.vue'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { MIN_DURATION_SECONDS, MAX_DURATION_SECONDS } from '../../constants/expPolicy'

const emit = defineEmits(['recorded'])
const timer = usePracticeTimerStore()

// 화면 갱신용 현재 시각 (타이머 값 자체는 store가 시작 시각으로 계산)
const now = ref(Date.now())
let tick = null
onMounted(() => {
  timer.load()
  tick = setInterval(() => { now.value = Date.now() }, 250)
})
onBeforeUnmount(() => clearInterval(tick))

const elapsedSeconds = computed(() => Math.min(Math.floor(timer.elapsedMs(now.value) / 1000), MAX_DURATION_SECONDS))
const pad = (n) => String(n).padStart(2, '0')
const display = computed(() => {
  const s = elapsedSeconds.value
  return `${pad(Math.floor(s / 3600))}:${pad(Math.floor((s % 3600) / 60))}:${pad(s % 60)}`
})

// 종료: 타이머를 멈추고 기록 창을 연다. "계속 연습하기"면 멈추기 전 상태로 되돌린다
const isFinishing = ref(false)
const finishedSeconds = ref(0)
let wasRunning = false
const finish = () => {
  wasRunning = timer.status === 'running'
  timer.pause()
  now.value = Date.now()
  finishedSeconds.value = elapsedSeconds.value
  isFinishing.value = true
}
const cancelFinish = () => {
  isFinishing.value = false
  if (wasRunning) timer.start()
}
const discard = () => { isFinishing.value = false; timer.reset() }
const onRecorded = (result) => {
  isFinishing.value = false
  timer.reset()
  emit('recorded', result)
}
</script>

<style scoped>
.timer-display {
  font-size: 3rem; font-weight: 900; letter-spacing: 2px; text-align: center;
  font-variant-numeric: tabular-nums; color: #ced4da; padding: 12px 0 16px; transition: color 0.2s;
}
.timer-display.running { color: #1a1a1a; }
.timer-display.paused { color: #868e96; }

.timer-btns { display: flex; gap: 8px; }
.btn {
  flex: 1; border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer;
  font-weight: 800; font-size: 0.9rem; padding: 12px; border-radius: 12px; font-family: inherit;
  transition: background 0.15s, border-color 0.15s;
}
.btn:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #1f47e0; color: #fff; }
.btn:disabled { opacity: 0.4; cursor: not-allowed; }

.badge { display: flex; align-items: center; gap: 6px; font-size: 0.7rem; font-weight: 800; color: #868e96; }
.badge.live { color: #e03131; }
.badge.live i { width: 7px; height: 7px; border-radius: 50%; background: #e03131; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.2; } }

.hint { margin: 14px 0 0; font-size: 0.75rem; color: #adb5bd; line-height: 1.5; }

@media (max-width: 760px) {
  .timer-display { font-size: 2.4rem; }
}
</style>
