<template>
  <!-- 무대 장면: 배경(208×72)과 캐릭터를 같은 도트 격자에 놓는다. 화면 크기와 상관없이 비율이 같다.
       viewWidth를 주면 가운데(캐릭터 쪽)만 카메라처럼 잘라 보여 준다 (배율은 그대로) -->
  <div class="scene" :style="{ aspectRatio: `${viewWidth} / ${SCENE_HEIGHT}` }">
    <div class="world" :style="worldStyle">
      <div class="actor" :style="actorStyle" @click="emit('actor-click')">
        <slot name="behind" />
        <PixelCharacter
          :character="character"
          :outfit="character?.currentOutfitSpriteKey || 'DEFAULT'"
          :scale="1"
          :animate="animate"
        />
        <slot name="above" />
      </div>
      <!-- 무대 꾸미기 효과 (캐릭터 뒤) -->
      <StageEffect :effect="effectKey" />
      <!-- 무대 좌표 위에 얹는 것들 -->
      <slot name="world" />
    </div>
    <!-- 장면 위에 얹는 UI -->
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PixelCharacter from './PixelCharacter.vue'
import StageEffect from './StageEffect.vue'
import { LEVEL_TIERS } from '../constants/levelTier'
import { SPRITE_WIDTH, SPRITE_HEIGHT, STAGE_WIDTH as SCENE_WIDTH, STAGE_HEIGHT as SCENE_HEIGHT } from '../pixel/palette'

const props = defineProps({
  character: { type: Object, default: null },
  // 무대 배경 키 (ROOKIE / RISING / PRO / LEGEND)
  stageKey: { type: String, default: 'ROOKIE' },
  animate: { type: Boolean, default: true },
  // 보여 줄 가로 폭 (무대 도트 기준, 최대 208). 작게 주면 가운데만 잘라서 보여 준다
  viewWidth: { type: Number, default: SCENE_WIDTH },
  // 무대 효과. undefined면 캐릭터가 고른 효과(character.stageEffect), null이면 없음
  effect: { type: String, default: undefined },
})
const emit = defineEmits(['actor-click'])

const stage = computed(() => LEVEL_TIERS.find((t) => t.key === props.stageKey) || LEVEL_TIERS[0])
const effectKey = computed(() => (props.effect === undefined ? props.character?.stageEffect || null : props.effect))
const pct = (v, total) => `${(v / total) * 100}%`

// 무대 전체(world)는 보이는 폭보다 넓을 수 있고, 가운데를 맞춰 놓는다
const worldStyle = computed(() => {
  const view = Math.min(props.viewWidth, SCENE_WIDTH)
  return {
    width: pct(SCENE_WIDTH, view),
    left: pct(-(SCENE_WIDTH - view) / 2, view),
    backgroundImage: `url(${stage.value.background})`,
  }
})

// 캐릭터는 가운데, 발이 그 무대의 바닥 줄에 닿게 (캔버스는 들썩임 1줄을 포함해 SPRITE_HEIGHT + 1)
const actorStyle = computed(() => ({
  left: pct(SCENE_WIDTH / 2 - SPRITE_WIDTH / 2, SCENE_WIDTH),
  top: pct(stage.value.floorY - SPRITE_HEIGHT, SCENE_HEIGHT),
  width: pct(SPRITE_WIDTH, SCENE_WIDTH),
}))
</script>

<style scoped>
.scene {
  position: relative;
  width: 100%;
  overflow: hidden;
}
.world {
  position: absolute;
  top: 0;
  height: 100%;
  background-size: 100% 100%;
  background-repeat: no-repeat;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
.actor { position: absolute; line-height: 0; z-index: 2; }
/* 캐릭터 캔버스(24×33)를 배경과 같은 배율로 늘린다 */
.actor :deep(.pixel-character) {
  position: relative;
  z-index: 2;
  width: 100% !important;
  height: auto !important;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
</style>
