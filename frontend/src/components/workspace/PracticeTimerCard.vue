<template>
  <WsCard :title="isToday ? '오늘 연습' : `${dateLabel(date)} 연습`">
    <template #actions>
      <span v-if="isToday && timer.running" class="badge live"><i></i>연습 중</span>
      <button v-else-if="isToday && editable" class="text-btn" :disabled="saving" @click="editing = date">시간 고치기</button>
    </template>

    <div class="timer-display" :class="{ running: isToday && timer.running }">{{ clock(shownSeconds) }}</div>

    <!-- 오늘: 시작/정지 -->
    <template v-if="isToday">
      <button
        class="toggle"
        :class="{ running: timer.running }"
        :disabled="saving"
        :aria-label="timer.running ? '연습 정지' : '연습 시작'"
        @click="toggle"
      >
        <span class="icon">{{ timer.running ? '■' : '▶' }}</span>
        {{ saving ? '저장 중…' : timer.running ? '정지' : '시작' }}
      </button>

      <p v-if="timer.running" class="status">이번 구간 {{ clock(sessionSeconds) }}</p>
      <p v-else-if="error" class="status error">
        {{ error }}
        <button class="retry" :disabled="saving" @click="saveParts(timer.pending)">다시 저장</button>
      </p>
      <p v-else-if="savedMessage" class="status saved">{{ savedMessage }}</p>
      <p v-else class="status">정지할 때마다 오늘 기록에 쌓이고, 연습한 만큼 경험치를 받아요.</p>
    </template>

    <!-- 지난 날: 그날 총 시간 + 고치기 -->
    <template v-else>
      <button v-if="editable" class="toggle secondary" @click="editing = date">시간 고치기</button>
      <p class="status">
        {{ editable ? '기록을 놓쳤거나 잘못 잰 날은 직접 고칠 수 있어요.' : '7일이 지난 기록은 볼 수만 있어요.' }}
      </p>
      <p v-if="timer.running" class="status live-note">
        오늘 타이머가 켜져 있어요 · <button class="link" @click="emit('go-today')">오늘로 가기</button>
      </p>
    </template>

    <EditDayModal :date="editing" @close="editing = null" @saved="onEdited" />
    <LevelUpModal :result="levelUp" @close="levelUp = null" />
  </WsCard>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import WsCard from './WsCard.vue'
import LevelUpModal from './LevelUpModal.vue'
import EditDayModal from './EditDayModal.vue'
import { usePracticeTimerStore } from '../../stores/practiceTimer'
import { usePracticeStore } from '../../stores/practice'
import { apiErrorMessage } from '../../api/practice'
import { clock, dateKey, dateLabel, durationText, isEditable, startOfDay } from '../../constants/practice'

// date: 보고 있는 날짜 ('YYYY-MM-DD'). 오늘이면 타이머, 지난 날이면 그날 총 시간과 고치기
const props = defineProps({ date: { type: String, required: true } })
const emit = defineEmits(['go-today'])
const timer = usePracticeTimerStore()
const practice = usePracticeStore()

// 화면 갱신용 현재 시각
const now = ref(Date.now())
let tick = null
onMounted(() => {
  timer.load()
  tick = setInterval(() => { now.value = Date.now() }, 250)
  // 지난번에 저장하지 못한 구간이 있으면 다시 시도
  if (timer.pending.length) saveParts(timer.pending)
})
onBeforeUnmount(() => clearInterval(tick))

// 오늘 시간 = 저장된 오늘 합계 + 켜져 있는 구간 중 오늘 자정 이후 부분
const todayKey = computed(() => dateKey(now.value))
const sessionSeconds = computed(() => (timer.running ? Math.floor((now.value - timer.startedAt) / 1000) : 0))
const todaySeconds = computed(() => {
  let live = 0
  if (timer.running) live = Math.floor((now.value - Math.max(timer.startedAt, startOfDay(now.value).getTime())) / 1000)
  return practice.totalOf(todayKey.value) + Math.max(0, live)
})
const isToday = computed(() => props.date === todayKey.value)
const shownSeconds = computed(() => (isToday.value ? todaySeconds.value : practice.totalOf(props.date)))
// 오늘은 타이머를 멈춘 뒤에만 고칠 수 있다 (켜져 있는 구간과 섞이지 않게)
const editable = computed(() => isEditable(props.date) && !(isToday.value && timer.running))

// 하루 시간 직접 고치기
const editing = ref(null)
const onEdited = (result) => {
  const gained = result?.expGained || 0
  savedMessage.value = `고쳤어요 · ${durationText(result?.dayTotalSeconds || 0)}${gained > 0 ? ` · +${gained} EXP` : gained < 0 ? ` · 경험치 ${gained}` : ''}`
  clearTimeout(savedTimer)
  savedTimer = setTimeout(() => { savedMessage.value = '' }, 6000)
  const exp = result?.experience
  if (exp && ((exp.isLeveledUp ?? exp.leveledUp) || exp.unlockedOutfits?.length)) {
    levelUp.value = { ...exp, expGained: result.expGained }
  }
}

const saving = ref(false)
const error = ref('')
const savedMessage = ref('')
const levelUp = ref(null)
let savedTimer = null

const LONG_SESSION_SECONDS = 8 * 3600

const toggle = () => {
  if (!timer.running) {
    error.value = ''
    savedMessage.value = ''
    timer.start()
    now.value = Date.now()
    return
  }
  const total = sessionSeconds.value
  // 깜빡 켜 두었을 수 있으니 아주 긴 구간은 한 번 확인
  if (total >= LONG_SESSION_SECONDS
    && !window.confirm(`타이머가 ${durationText(total)} 동안 켜져 있었어요. 그대로 기록할까요?\n(나중에 날짜를 눌러 시간을 고칠 수도 있어요)`)) {
    if (window.confirm('이번 구간을 기록하지 않고 버릴까요?')) timer.discard()
    return
  }
  saveParts(timer.stop())
}

// 날짜별 구간을 차례로 저장. 실패하면 남은 구간을 보관해 두고 다시 저장할 수 있게 한다
const saveParts = async (parts) => {
  if (!parts.length) return
  saving.value = true
  error.value = ''
  let expGained = 0
  const levels = []
  const unlocked = []
  let remaining = [...parts]
  try {
    for (const part of parts) {
      const result = await practice.recordSession(part.date, part.seconds)
      remaining = remaining.slice(1)
      timer.setPending(remaining)
      expGained += result.expGained || 0
      const exp = result.experience
      if (exp) {
        levels.push(exp)
        unlocked.push(...(exp.unlockedOutfits || []))
      }
    }
    showSaved(expGained)
    // 서버 직렬화에 따라 isLeveledUp 또는 leveledUp으로 올 수 있다
    const leveled = levels.filter((e) => e.isLeveledUp ?? e.leveledUp)
    if (leveled.length || unlocked.length) {
      levelUp.value = {
        previousLevel: (leveled[0] || levels[0]).previousLevel,
        currentLevel: levels[levels.length - 1].currentLevel,
        expGained,
        unlockedOutfits: unlocked,
      }
    }
  } catch (e) {
    timer.setPending(remaining)
    error.value = `${durationText(remaining.reduce((s, p) => s + p.seconds, 0))}을 저장하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

const showSaved = (exp) => {
  // 경험치는 구간이 아니라 오늘 총 시간 기준이라, 문구도 오늘 총 시간으로 보여 준다. 경험치는 받았을 때만 표시
  const total = `기록했어요 · 오늘 총 ${durationText(practice.totalOf(dateKey()), true)}`
  savedMessage.value = exp > 0 ? `${total} · +${exp} EXP` : total
  clearTimeout(savedTimer)
  savedTimer = setTimeout(() => { savedMessage.value = '' }, 6000)
}
</script>

<style scoped>
.timer-display {
  font-size: 3rem; font-weight: 900; letter-spacing: 2px; text-align: center;
  font-variant-numeric: tabular-nums; color: #868e96; padding: 8px 0 16px; transition: color 0.2s;
}
.timer-display.running { color: #1a1a1a; }

.toggle {
  width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px;
  border: none; background: var(--c-primary); color: #fff; cursor: pointer; font-family: inherit;
  font-weight: 900; font-size: 1rem; padding: 14px; border-radius: 14px;
  transition: background 0.15s, transform 0.1s;
}
.toggle:hover:not(:disabled) { background: #1f47e0; }
.toggle:active:not(:disabled) { transform: scale(0.98); }
.toggle.running { background: #1a1a1a; }
.toggle.running:hover:not(:disabled) { background: #343a40; }
.toggle:disabled { opacity: 0.6; cursor: wait; }
.toggle.secondary { background: #fff; color: var(--c-primary-text); box-shadow: inset 0 0 0 1px var(--c-primary-line); }
.toggle.secondary:hover { background: var(--c-primary-soft); }
.text-btn {
  border: none; background: none; padding: 0; font: inherit; font-size: 0.75rem; font-weight: 800;
  color: var(--c-primary-text); cursor: pointer;
}
.text-btn:disabled { color: #ced4da; cursor: default; }
.live-note { color: #e03131; font-weight: 700; }
.link { border: none; background: none; padding: 0; font: inherit; font-weight: 800; color: var(--c-primary-text); cursor: pointer; text-decoration: underline; }
.icon { font-size: 0.85rem; }

.badge { display: flex; align-items: center; gap: 6px; font-size: 0.7rem; font-weight: 800; color: #e03131; }
.badge i { width: 7px; height: 7px; border-radius: 50%; background: #e03131; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.2; } }

.status { margin: 12px 0 0; font-size: 0.75rem; color: #adb5bd; line-height: 1.5; text-align: center; font-variant-numeric: tabular-nums; }
.status.saved { color: var(--c-primary-text); font-weight: 800; }
.status.error { color: #e03131; font-weight: 700; }
.retry {
  margin-left: 6px; border: 1px solid #ffc9c9; background: #fff5f5; color: #e03131; cursor: pointer;
  font: inherit; font-weight: 800; padding: 2px 8px; border-radius: 6px;
}

@media (max-width: 760px) {
  .timer-display { font-size: 2.4rem; }
}
</style>
