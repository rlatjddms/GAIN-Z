<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="exp-guide-title">
          <ModalClose @close="emit('close')" />
          <h2 id="exp-guide-title">경험치는 이렇게 쌓여요</h2>
          <p class="sub">경험치는 연습 시간으로만 올라요.</p>

          <p class="now">
            <template v-if="level >= MAX_LEVEL">Lv.{{ MAX_LEVEL }} · 최고 레벨이에요</template>
            <template v-else>Lv.{{ level }} · 다음 레벨까지 <strong>{{ remaining }} EXP</strong> (연습 {{ remainingText }})</template>
          </p>

          <ul class="rules">
            <li>
              <Timer class="icon" :size="18" :stroke-width="2.2" aria-hidden="true" />
              <div><strong>연습 1분 = {{ EXP_PER_MINUTE }} EXP</strong><span>연습 타이머로 잰 시간이에요. 하루 합계로 계산해서 여러 번 나눠 기록해도 같아요.</span></div>
            </li>
            <li>
              <CalendarCheck class="icon" :size="18" :stroke-width="2.2" aria-hidden="true" />
              <div><strong>하루 최대 {{ MAX_EXP_MINUTES_PER_DAY * EXP_PER_MINUTE }} EXP</strong><span>하루 {{ MAX_EXP_MINUTES_PER_DAY / 60 }}시간까지 경험치로 쳐요.</span></div>
            </li>
            <li>
              <Pencil class="icon" :size="18" :stroke-width="2.2" aria-hidden="true" />
              <div><strong>직접 입력은 하루 {{ MANUAL_EXP_MINUTES_PER_DAY }}분까지</strong><span>타이머를 깜빡한 날을 위한 여유예요. 기록을 줄이면 줄인 만큼 경험치도 빠져요.</span></div>
            </li>
            <li>
              <TrendingUp class="icon" :size="18" :stroke-width="2.2" aria-hidden="true" />
              <div><strong>다음 레벨까지 50 × 레벨 + 20 EXP</strong><span>Lv.1 → {{ getRequiredExp(1) }} EXP, Lv.10 → {{ getRequiredExp(10) }} EXP. 최고 레벨은 Lv.{{ MAX_LEVEL }}이에요.</span></div>
            </li>
            <li>
              <Gift class="icon" :size="18" :stroke-width="2.2" aria-hidden="true" />
              <div><strong>레벨마다 보상</strong><span>옷·소품·헤어를 받고, 10레벨마다 새 무대가 열려요.</span></div>
            </li>
          </ul>

          <div class="dialog-actions">
            <button type="button" class="btn" @click="emit('close')">닫기</button>
            <button type="button" class="btn primary" @click="goPractice">연습하러 가기</button>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 경험치 규칙 안내 (#237). 홈 무대의 경험치 줄을 누르면 연다. 숫자는 constants/levelTier.js (백엔드 ExpPolicy·LevelPolicy와 같음)
import { computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { CalendarCheck, Gift, Pencil, Timer, TrendingUp } from 'lucide-vue-next'
import {
  MAX_LEVEL, EXP_PER_MINUTE, MAX_EXP_MINUTES_PER_DAY, MANUAL_EXP_MINUTES_PER_DAY, clampLevel, getRequiredExp,
} from '../../constants/levelTier'
import ModalClose from '../ui/ModalClose.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  character: { type: Object, default: null },
})
const emit = defineEmits(['close'])
const router = useRouter()

const level = computed(() => clampLevel(props.character?.level ?? 1))
// 기록을 줄여 경험치가 음수면 그만큼 더 채워야 한다
const remaining = computed(() => getRequiredExp(level.value) - (Number(props.character?.exp) || 0))
const remainingText = computed(() => {
  const minutes = Math.ceil(remaining.value / EXP_PER_MINUTE)
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h ? `${h}시간${m ? ` ${m}분` : ''}` : `${m}분`
})

const goPractice = () => {
  emit('close')
  router.push('/practice')
}

const onKey = (e) => { if (e.key === 'Escape') emit('close') }
watch(() => props.open, (open) => {
  if (open) window.addEventListener('keydown', onKey)
  else window.removeEventListener('keydown', onKey)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 420px; max-height: calc(100dvh - 32px); overflow-y: auto; box-sizing: border-box;
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 24px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text); text-align: left;
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; }
.sub { margin: 0 0 14px; font-size: 0.8rem; color: var(--c-muted); }
.now {
  margin: 0 0 14px; padding: 10px 12px; border-radius: 12px;
  background: var(--c-primary-soft); color: var(--c-primary-text); font-size: 0.82rem; font-weight: 700;
}
.now strong { font-weight: 900; }
.rules { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
.rules li { display: flex; gap: 10px; align-items: flex-start; }
.icon { flex: none; margin-top: 1px; color: var(--c-primary); }
.rules div { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.rules strong { font-size: 0.86rem; font-weight: 800; }
.rules span { font-size: 0.76rem; line-height: 1.5; color: var(--c-muted); }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
/* 휴대폰: 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .modal {
    width: 100%; max-width: none; max-height: 92dvh;
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    padding-bottom: var(--sheet-pad-bottom);
  }
}
/* 닫기 버튼 자리 (#292) */
.modal { position: relative; }
h2 { padding-right: 40px; }

</style>
