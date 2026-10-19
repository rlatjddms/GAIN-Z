<template>
  <section
    class="character-stage"
    :class="[`tier-${bgTier.key.toLowerCase()}`, { bare }]"
    :style="stageStyle"
  >
    <div v-if="!bare" class="stage-head">
      <div class="level-tag">Lv.{{ level }} {{ nickname }}</div>
      <div class="tier-badge">{{ tier.name }}</div>
    </div>

    <!-- 배경과 캐릭터는 같은 도트 격자 (홈과 옷장 미리보기가 똑같이 보인다) -->
    <StageScene :character="character" :stage-key="bgTier.key" :view-width="viewWidth" @actor-click="!bare && toggleSpeech()">
      <template #behind>
        <div v-if="!bare && tier.aura !== 'none'" class="aura" :class="`aura-${tier.aura}`"></div>
      </template>
      <template #above>
        <transition name="pop">
          <div v-if="showSpeech" class="speech-bubble">{{ tier.speech }}</div>
        </transition>
      </template>
    </StageScene>

    <div v-if="!bare" class="exp-area">
      <div class="exp-bar">
        <div class="exp-fill" :style="{ width: `${progress}%` }"></div>
      </div>
      <div class="exp-text">{{ expLabel }}</div>
    </div>

    <slot />
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  LEVEL_TIERS,
  MAX_LEVEL,
  clampLevel,
  getLevelTier,
  getNextTier,
  getRequiredExp,
  getExpProgress,
} from '../constants/levelTier'
import StageScene from './StageScene.vue'

const props = defineProps({
  character: { type: Object, default: null },
  // 캐릭터와 배경만 (옷장 미리보기용: 레벨·칭호·경험치 숨김)
  bare: { type: Boolean, default: false },
  // 무대 중 보여 줄 가로 폭 (무대 도트 기준). 작게 주면 캐릭터 쪽만 카메라처럼 잘라서 보여 준다
  viewWidth: { type: Number, default: 208 },
})

const showSpeech = ref(false)
const toggleSpeech = () => { showSpeech.value = !showSpeech.value }

const level = computed(() => clampLevel(props.character?.level ?? 1))
// 기록을 줄여 경험치를 되돌리면 음수(부족분)일 수 있다. 막대는 0부터, 문구로 부족분을 알려 준다
const rawExp = computed(() => Number(props.character?.exp) || 0)
const exp = computed(() => Math.max(0, rawExp.value))
const nickname = computed(() => props.character?.nickname || '방구석 락스타')

const tier = computed(() => getLevelTier(level.value))
const nextTier = computed(() => getNextTier(level.value))
const progress = computed(() => getExpProgress(level.value, exp.value))

const expLabel = computed(() => {
  if (level.value >= MAX_LEVEL) return 'MAX LEVEL 🎉 모든 무대를 정복했어요'
  const required = getRequiredExp(level.value)
  if (rawExp.value < 0) return `EXP 0 / ${required} · 줄인 기록만큼 ${-rawExp.value} EXP를 더 채워야 해요`
  const base = `EXP ${exp.value} / ${required}`
  if (!nextTier.value) return base
  return `${base} · 다음 무대 「${nextTier.value.name}」까지 Lv.${nextTier.value.minLevel}`
})

const character = computed(() => props.character)

// 옷장에서 고른 무대 배경 (아직 모르면 레벨에 맞는 무대). 글자색도 배경에 맞춘다
const bgTier = computed(() => LEVEL_TIERS.find((t) => t.key === props.character?.stageBackground) || tier.value)

const stageStyle = computed(() => ({
  '--accent': bgTier.value.accent,
  '--accent-soft': bgTier.value.accentSoft,
  '--stage-text': bgTier.value.textColor,
}))
</script>

<style scoped>
.character-stage {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  border: 1px solid #e9ecef;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.02);
  color: var(--stage-text, #1a1a1a);
  text-align: center;
  overflow: hidden;
}

/* 레벨·칭호(위)와 경험치(아래)는 장면 위에 얹는다 */
.stage-head,
.exp-area {
  position: absolute;
  z-index: 3;
  left: 50%;
  transform: translateX(-50%);
}
.stage-head {
  top: 6%;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  width: 100%;
  flex-wrap: wrap;
}
.level-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--accent);
  background: var(--accent-soft);
}
.tier-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
  background: var(--accent);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.character-stage :deep(.actor) { cursor: pointer; }
.bare :deep(.actor) { cursor: default; }
.character-stage :deep(.pixel-character) { filter: drop-shadow(0 3px 3px rgba(0, 0, 0, 0.25)); }

/* 오라는 캐릭터 너비 기준 (장면 크기를 따라 같이 커진다) */
.aura {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 300%;
  aspect-ratio: 1;
  border-radius: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  z-index: 1;
}
.aura-soft {
  background: radial-gradient(circle, rgba(255, 171, 64, 0.45) 0%, rgba(255, 171, 64, 0) 70%);
  animation: aura-pulse 3s ease-in-out infinite;
}
.aura-glow {
  width: 380%;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.6) 0%, rgba(96, 165, 250, 0.25) 45%, rgba(96, 165, 250, 0) 72%);
  animation: aura-pulse 2.4s ease-in-out infinite;
}
.aura-legend {
  width: 460%;
  background: radial-gradient(circle, rgba(253, 230, 138, 0.8) 0%, rgba(251, 191, 36, 0.35) 45%, rgba(251, 191, 36, 0) 72%);
  animation: aura-pulse 1.8s ease-in-out infinite, aura-spin 10s linear infinite;
}

.speech-bubble {
  position: absolute;
  left: 50%;
  bottom: 100%;
  z-index: 4;
  margin-bottom: 6px;
  transform: translateX(-50%);
  padding: 8px 16px;
  border-radius: 14px;
  background: #1a1a1a;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
}

.exp-area {
  bottom: 5%;
  width: min(420px, 70%);
}
.exp-bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.25);
  overflow: hidden;
}
.tier-rookie .exp-bar { background: rgba(0, 0, 0, 0.12); }
.exp-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), #ffffff80);
  background-color: var(--accent);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
.exp-text {
  margin-top: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}
.tier-rookie .exp-text { text-shadow: none; }

.pop-enter-active { animation: pop-in 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
@keyframes pop-in {
  from { opacity: 0; transform: translateX(-50%) scale(0.6); }
  to { opacity: 1; transform: translateX(-50%) scale(1); }
}
@keyframes aura-pulse {
  0%, 100% { opacity: 0.7; scale: 0.92; }
  50% { opacity: 1; scale: 1.08; }
}
@keyframes aura-spin {
  from { rotate: 0deg; }
  to { rotate: 360deg; }
}

@media (prefers-reduced-motion: reduce) {
  .aura { animation: none; }
}

/* 좁은 화면: 장면이 작아서 글씨는 장면 위·아래로 뺀다 */
@media (max-width: 640px) {
  .character-stage { color: #1a1a1a; }
  .stage-head, .exp-area { position: static; transform: none; margin: 0 auto; }
  .stage-head { padding: 12px 12px 10px; }
  .exp-area { width: auto; padding: 10px 16px 14px; }
  .exp-text { text-shadow: none; }
  .speech-bubble { white-space: normal; width: max-content; max-width: 180px; }
}

/* 옷장 미리보기: 캐릭터와 배경만 */
.character-stage.bare { margin-bottom: 0; }
</style>
