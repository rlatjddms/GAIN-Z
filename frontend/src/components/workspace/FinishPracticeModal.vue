<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="onBackdrop">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="finish-title">
          <!-- 1. 달성률 입력 -->
          <template v-if="!result">
            <h2 id="finish-title">연습 기록하기</h2>
            <p class="duration">{{ durationLabel }} 연습했어요</p>

            <div class="field">
              <div class="field-head">
                <strong>오늘 목표를 얼마나 해냈나요?</strong>
                <span class="rate">{{ ratePercent }}%</span>
              </div>
              <div class="chips">
                <button
                  v-for="p in PRESETS" :key="p" class="chip" :class="{ on: ratePercent === p }"
                  :disabled="saving" @click="ratePercent = p"
                >{{ p }}%</button>
              </div>
              <input v-model.number="ratePercent" type="range" min="0" max="100" step="5" :disabled="saving" aria-label="달성률">
            </div>

            <div class="exp-preview">
              <span>예상 경험치</span>
              <strong>+{{ expectedExp }} EXP</strong>
              <small>연습 {{ practiceMinutes }}분 +{{ practiceMinutes }} · 달성 보너스 +{{ expectedExp - practiceMinutes }}</small>
            </div>
            <p v-if="durationSeconds < 60" class="note">1분 미만은 연습 시간 경험치가 없고, 달성 보너스만 받아요.</p>
            <p v-else-if="durationSeconds / 60 > MAX_MINUTES_PER_LOG" class="note">한 번에 {{ MAX_MINUTES_PER_LOG }}분까지만 경험치로 인정돼요.</p>

            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button class="btn" :disabled="saving" @click="emit('cancel')">계속 연습하기</button>
              <button class="btn primary" :disabled="saving" @click="submit">{{ saving ? '기록 중…' : '기록하기' }}</button>
            </div>
            <button class="discard" :disabled="saving" @click="discard">기록하지 않고 버리기</button>
          </template>

          <!-- 2. 결과 -->
          <template v-else>
            <div class="result" :class="{ levelup: leveledUp }">
              <div v-if="leveledUp" class="levelup-badge">LEVEL UP!</div>
              <div class="gained">+{{ result.expGained }} EXP</div>
              <p v-if="leveledUp" class="level-change">
                Lv.{{ exp.previousLevel }} → <strong>Lv.{{ exp.currentLevel }}</strong>
                <span v-if="newTier">&nbsp;· {{ newTier.name }} 무대로!</span>
              </p>
              <p v-else class="level-change">Lv.{{ exp.currentLevel }} · 다음 레벨까지 조금 더!</p>

              <div v-if="exp.unlockedOutfits?.length" class="unlocked">
                <strong>새로 해금된 의상</strong>
                <ul><li v-for="o in exp.unlockedOutfits" :key="o.id">{{ o.name }}</li></ul>
              </div>
            </div>
            <div class="actions">
              <button class="btn primary" @click="close">확인</button>
            </div>
          </template>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { createPracticeLog, apiErrorMessage } from '../../api/practice'
import { useAuthStore } from '../../stores/auth'
import { calculateExp, MAX_MINUTES_PER_LOG } from '../../constants/expPolicy'
import { getLevelTier } from '../../constants/levelTier'

const props = defineProps({
  open: { type: Boolean, default: false },
  durationSeconds: { type: Number, default: 0 },
})
// cancel: 창만 닫기(타이머 유지) · discard: 기록 없이 버리기 · recorded: 기록 완료(결과 전달)
const emit = defineEmits(['cancel', 'discard', 'recorded'])
const authStore = useAuthStore()

const PRESETS = [0, 25, 50, 75, 100]
const ratePercent = ref(100)
const saving = ref(false)
const error = ref('')
const result = ref(null)

// 열 때마다 입력 상태 초기화
watch(() => props.open, (v) => {
  if (v) { ratePercent.value = 100; saving.value = false; error.value = ''; result.value = null }
})

const pad = (n) => String(n).padStart(2, '0')
const durationLabel = computed(() => {
  const s = props.durationSeconds
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60
  return h ? `${h}시간 ${pad(m)}분` : m ? `${m}분 ${pad(sec)}초` : `${sec}초`
})
const practiceMinutes = computed(() => Math.min(Math.floor(props.durationSeconds / 60), MAX_MINUTES_PER_LOG))
const expectedExp = computed(() => calculateExp(props.durationSeconds, ratePercent.value / 100))

const submit = async () => {
  saving.value = true
  error.value = ''
  try {
    result.value = await createPracticeLog({
      durationSeconds: props.durationSeconds,
      achievementRate: ratePercent.value / 100,
    })
    // 홈 무대의 레벨·경험치를 바로 갱신
    await authStore.fetchCharacter()
  } catch (e) {
    error.value = `기록하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

const exp = computed(() => result.value?.experience || {})
// 서버 직렬화에 따라 isLeveledUp 또는 leveledUp으로 올 수 있다
const leveledUp = computed(() => !!(exp.value.isLeveledUp ?? exp.value.leveledUp))
const newTier = computed(() => {
  if (!leveledUp.value) return null
  const before = getLevelTier(exp.value.previousLevel), after = getLevelTier(exp.value.currentLevel)
  return before.key !== after.key ? after : null
})

const discard = () => {
  if (window.confirm('이번 연습 시간을 기록하지 않고 버릴까요?')) emit('discard')
}
const close = () => emit('recorded', result.value)
const onBackdrop = () => {
  if (result.value) close()
  else if (!saving.value) emit('cancel')
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 420px; max-height: calc(100vh - 32px); overflow-y: auto; box-sizing: border-box;
  background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a;
}
h2 { margin: 0 0 4px; font-size: 1.1rem; font-weight: 900; }
.duration { margin: 0 0 20px; font-size: 0.9rem; font-weight: 700; color: #2d5bff; }

.field { margin-bottom: 16px; }
.field-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 10px; }
.field-head strong { font-size: 0.85rem; font-weight: 800; }
.rate { font-size: 1.1rem; font-weight: 900; color: #2d5bff; font-variant-numeric: tabular-nums; }
.chips { display: flex; gap: 6px; margin-bottom: 10px; }
.chip {
  flex: 1; border: 1px solid #e9ecef; background: #fff; color: #868e96; cursor: pointer; font-family: inherit;
  font-size: 0.75rem; font-weight: 800; padding: 7px 0; border-radius: 9px;
}
.chip.on { background: #f0f4ff; border-color: #c9d6ff; color: #2d5bff; }
input[type='range'] { width: 100%; accent-color: #2d5bff; }

.exp-preview {
  background: #f8f9fa; border-radius: 14px; padding: 12px 14px;
  display: grid; grid-template-columns: 1fr auto; row-gap: 2px; align-items: baseline;
}
.exp-preview span { font-size: 0.8rem; font-weight: 700; color: #868e96; }
.exp-preview strong { font-size: 1.1rem; font-weight: 900; color: #2d5bff; }
.exp-preview small { grid-column: 1 / -1; font-size: 0.7rem; color: #adb5bd; }
.note { margin: 8px 0 0; font-size: 0.75rem; color: #ff922b; font-weight: 700; }
.error { margin: 10px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }

.actions { display: flex; gap: 8px; margin-top: 18px; }
.btn {
  flex: 1; border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer;
  font-weight: 800; font-size: 0.9rem; padding: 12px; border-radius: 12px; font-family: inherit;
}
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.discard {
  display: block; margin: 12px auto 0; border: none; background: none; cursor: pointer; font-family: inherit;
  font-size: 0.75rem; font-weight: 700; color: #adb5bd; text-decoration: underline;
}
.discard:hover:not(:disabled) { color: #e03131; }

.result { text-align: center; padding: 8px 0 4px; }
.levelup-badge {
  display: inline-block; font-size: 0.75rem; font-weight: 900; letter-spacing: 1px; color: #fff;
  background: linear-gradient(90deg, #ff922b, #f59f00); padding: 5px 12px; border-radius: 8px; margin-bottom: 10px;
  animation: pop 0.5s ease;
}
.gained { font-size: 2.2rem; font-weight: 900; color: #2d5bff; animation: pop 0.4s ease; }
.result.levelup .gained { color: #f08c00; }
.level-change { margin: 6px 0 0; font-size: 0.9rem; font-weight: 700; color: #495057; }
.level-change strong { color: #1a1a1a; }
.unlocked { margin-top: 16px; background: #fff9db; border-radius: 12px; padding: 12px; font-size: 0.8rem; }
.unlocked strong { display: block; margin-bottom: 4px; color: #e67700; }
.unlocked ul { margin: 0; padding: 0; list-style: none; color: #495057; font-weight: 700; }
@keyframes pop { 0% { transform: scale(0.6); opacity: 0; } 70% { transform: scale(1.08); } 100% { transform: scale(1); opacity: 1; } }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
