<template>
  <section
    class="character-stage"
    :class="`tier-${tier.key.toLowerCase()}`"
    :style="stageStyle"
  >
    <div class="stage-overlay"></div>

    <div class="stage-head">
      <div class="level-tag">Lv.{{ level }} {{ nickname }}</div>
      <div class="tier-badge">{{ tier.name }}</div>
    </div>

    <div class="stage-floor">
      <div class="character-box" @click="toggleSpeech">
        <div v-if="tier.aura !== 'none'" class="aura" :class="`aura-${tier.aura}`"></div>
        <div class="char-icon">
          <PixelCharacter :character="character" :scale="pixelScale" />
        </div>
        <transition name="pop">
          <div v-if="showSpeech" class="speech-bubble">{{ tier.speech }}</div>
        </transition>
      </div>
    </div>

    <div class="exp-area">
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
  MAX_LEVEL,
  clampLevel,
  getLevelTier,
  getNextTier,
  getRequiredExp,
  getExpProgress,
} from '../constants/levelTier'
import PixelCharacter from './PixelCharacter.vue'

const props = defineProps({
  character: { type: Object, default: null },
})

const showSpeech = ref(false)
const toggleSpeech = () => { showSpeech.value = !showSpeech.value }

const level = computed(() => clampLevel(props.character?.level ?? 1))
const exp = computed(() => Math.max(0, Number(props.character?.exp) || 0))
const nickname = computed(() => props.character?.nickname || '방구석 락스타')

const tier = computed(() => getLevelTier(level.value))
const nextTier = computed(() => getNextTier(level.value))
const progress = computed(() => getExpProgress(level.value, exp.value))

const expLabel = computed(() => {
  if (level.value >= MAX_LEVEL) return 'MAX LEVEL 🎉 모든 무대를 정복했어요'
  const required = getRequiredExp(level.value)
  const base = `EXP ${exp.value} / ${required}`
  if (!nextTier.value) return base
  return `${base} · 다음 무대 「${nextTier.value.name}」까지 Lv.${nextTier.value.minLevel}`
})

// 무대가 커질수록 캐릭터도 커진다 (정수 배율만 사용해야 도트가 깨지지 않음)
const pixelScale = computed(() => Math.round(5 * tier.value.scale))
const character = computed(() => props.character)

const stageStyle = computed(() => ({
  backgroundImage: `url(${tier.value.background})`,
  '--accent': tier.value.accent,
  '--accent-soft': tier.value.accentSoft,
  '--stage-text': tier.value.textColor,
}))
</script>

<style scoped>
.character-stage {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  padding: 48px 40px 36px;
  border: 1px solid #e9ecef;
  border-radius: 28px;
  background-color: #fff;
  background-size: cover;
  background-position: center bottom;
  background-repeat: no-repeat;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.02);
  color: var(--stage-text, #1a1a1a);
  text-align: center;
  overflow: hidden;
  transition: background-image 0.4s ease;
}

.stage-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0) 55%, rgba(0, 0, 0, 0.18) 100%);
}
.tier-rookie .stage-overlay {
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0) 60%, rgba(255, 255, 255, 0.25) 100%);
}

.stage-head,
.stage-floor,
.exp-area {
  position: relative;
  z-index: 1;
}

.stage-head {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
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

.stage-floor {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 48px;
  min-height: 150px;
}

.character-box {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  min-width: 160px;
  min-height: 150px;
  cursor: pointer;
}
.char-icon {
  position: relative;
  z-index: 2;
  line-height: 0;
  filter: drop-shadow(0 10px 10px rgba(0, 0, 0, 0.25));
}

.aura {
  position: absolute;
  left: 50%;
  bottom: 8px;
  width: 150px;
  height: 150px;
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
  width: 190px;
  height: 190px;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.6) 0%, rgba(96, 165, 250, 0.25) 45%, rgba(96, 165, 250, 0) 72%);
  animation: aura-pulse 2.4s ease-in-out infinite;
}
.aura-legend {
  width: 230px;
  height: 230px;
  background: radial-gradient(circle, rgba(253, 230, 138, 0.8) 0%, rgba(251, 191, 36, 0.35) 45%, rgba(251, 191, 36, 0) 72%);
  animation: aura-pulse 1.8s ease-in-out infinite, aura-spin 10s linear infinite;
}

.speech-bubble {
  position: absolute;
  left: 50%;
  bottom: -14px;
  z-index: 4;
  transform: translateX(-50%);
  padding: 10px 20px;
  border-radius: 14px;
  background: #1a1a1a;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 500;
  white-space: nowrap;
}

.exp-area {
  max-width: 420px;
  margin: 28px auto 0;
}
.exp-bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.12);
  overflow: hidden;
}
.tier-rookie .exp-bar { background: rgba(0, 0, 0, 0.08); }
.exp-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), #ffffff80);
  background-color: var(--accent);
  transition: width 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
.exp-text {
  margin-top: 8px;
  font-size: 0.78rem;
  font-weight: 600;
  opacity: 0.85;
}

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

@media (max-width: 640px) {
  .character-stage { padding: 48px 20px 28px; }
  .stage-floor { gap: 20px; }
  .speech-bubble { white-space: normal; max-width: 220px; }
}
</style>
