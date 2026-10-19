<template>
  <!-- 무대 꾸미기 효과 (레벨 보상). 무대 좌표(208×72) 위에 얹고, 위치는 모두 % -->
  <div v-if="effect" class="effect" :class="`fx-${effect.toLowerCase()}`" aria-hidden="true">
    <template v-if="effect === 'SPARKLE'">
      <i v-for="(s, i) in SPARKLES" :key="i" class="sparkle" :style="{ left: `${s[0]}%`, top: `${s[1]}%`, animationDelay: `${s[2]}s` }"></i>
    </template>
    <template v-else-if="effect === 'SPOTLIGHT'">
      <i class="beam left"></i>
      <i class="beam right"></i>
      <i class="pool"></i>
    </template>
    <template v-else-if="effect === 'SMOKE'">
      <i v-for="(s, i) in SMOKES" :key="i" class="puff" :style="{ left: `${s[0]}%`, animationDelay: `${s[1]}s` }"></i>
    </template>
    <template v-else-if="effect === 'FIREWORKS'">
      <span v-for="(f, i) in FIREWORKS" :key="i" class="burst" :style="{ left: `${f[0]}%`, top: `${f[1]}%`, animationDelay: `${f[2]}s`, '--c': f[3] }">
        <i v-for="n in 8" :key="n" :style="{ '--a': `${n * 45}deg` }"></i>
      </span>
    </template>
  </div>
</template>

<script setup>
// effect: SPARKLE / SPOTLIGHT / SMOKE / FIREWORKS (constants/rewards.js STAGE_EFFECTS, 백엔드 StageEffect)
defineProps({ effect: { type: String, default: null } })

// [left %, top %, 지연 s]
const SPARKLES = [[12, 18, 0], [26, 40, 0.6], [38, 12, 1.2], [62, 16, 0.3], [74, 42, 0.9], [88, 20, 1.5], [50, 8, 1.8], [20, 62, 1.1], [82, 64, 0.4]]
const SMOKES = [[-6, 0], [14, 1.5], [34, 3], [54, 0.8], [74, 2.2], [92, 3.6]]
// [left %, top %, 지연 s, 색]
const FIREWORKS = [[18, 22, 0, '#ffd43b'], [80, 18, 0.9, '#ff6b6b'], [34, 12, 1.8, '#74c0fc'], [66, 30, 2.6, '#b197fc']]
</script>

<style scoped>
.effect { position: absolute; inset: 0; pointer-events: none; overflow: hidden; z-index: 1; container-type: size; }
.effect i, .effect span { position: absolute; display: block; }

/* 반짝이: 십자 모양 별이 깜빡인다 */
.sparkle { width: 2.4%; aspect-ratio: 1; animation: twinkle 2.2s ease-in-out infinite; }
.sparkle::before, .sparkle::after { content: ''; position: absolute; background: #fff9db; box-shadow: 0 0 4px #ffd43b; }
.sparkle::before { left: 40%; top: 0; width: 20%; height: 100%; }
.sparkle::after { top: 40%; left: 0; height: 20%; width: 100%; }
@keyframes twinkle { 0%, 100% { opacity: 0; transform: scale(0.4); } 50% { opacity: 1; transform: scale(1); } }

/* 스포트라이트: 위에서 내려오는 빛 두 줄기가 흔들린다 */
.beam {
  top: -10%; width: 22%; height: 120%; transform-origin: 50% 0;
  background: linear-gradient(rgba(255, 249, 219, 0.55), rgba(255, 249, 219, 0.05));
  clip-path: polygon(42% 0, 58% 0, 100% 100%, 0 100%);
  mix-blend-mode: screen;
}
.beam.left { left: 20%; animation: sweep-left 4s ease-in-out infinite alternate; }
.beam.right { right: 20%; animation: sweep-right 4.6s ease-in-out infinite alternate; }
.pool { left: 38%; bottom: 8%; width: 24%; height: 10%; border-radius: 50%; background: radial-gradient(rgba(255, 249, 219, 0.45), transparent 70%); }
@keyframes sweep-left { from { transform: rotate(-18deg); } to { transform: rotate(14deg); } }
@keyframes sweep-right { from { transform: rotate(18deg); } to { transform: rotate(-14deg); } }

/* 스모크: 바닥에 낮게 깔린 연기가 옆으로 흐른다 */
.puff {
  bottom: -6%; width: 24%; height: 26%; border-radius: 50%;
  background: radial-gradient(rgba(233, 236, 239, 0.75), rgba(233, 236, 239, 0) 70%);
  animation: drift 7s ease-in-out infinite;
}
@keyframes drift {
  0% { transform: translateX(-8%) scale(0.9); opacity: 0.35; }
  50% { transform: translateX(12%) scale(1.1); opacity: 0.8; }
  100% { transform: translateX(-8%) scale(0.9); opacity: 0.35; }
}

/* 불꽃놀이: 점 여덟 개가 퍼졌다가 사라진다 */
.burst { width: 0; height: 0; animation: burst-life 3.4s ease-out infinite; opacity: 0; }
.burst i {
  left: 0; top: 0; width: 1.2cqw; height: 1.2cqw; margin: -0.6cqw; background: var(--c);
  box-shadow: 0 0 4px var(--c);
  transform: rotate(var(--a)) translateY(0);
  animation: burst-spread 3.4s ease-out infinite; animation-delay: inherit;
}
@keyframes burst-life { 0% { opacity: 0; } 5% { opacity: 1; } 45% { opacity: 1; } 60%, 100% { opacity: 0; } }
@keyframes burst-spread { 0% { transform: rotate(var(--a)) translateY(0); } 50%, 100% { transform: rotate(var(--a)) translateY(-8cqw); } }

@media (prefers-reduced-motion: reduce) {
  .effect i, .effect span { animation: none !important; }
  .sparkle, .burst { opacity: 1; }
  .burst i { transform: rotate(var(--a)) translateY(-6cqw); }
}
</style>
