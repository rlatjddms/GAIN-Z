<template>
  <!-- 무대 장면: 배경(208×72)과 캐릭터를 같은 도트 격자에 놓는다. 화면 크기와 상관없이 비율이 같다 -->
  <div class="scene" :style="{ backgroundImage: `url(${stage.background})` }">
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
    <!-- 장면 위에 올리는 것들 (말풍선 밖 UI, 나중에 꾸미기 효과) -->
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PixelCharacter from './PixelCharacter.vue'
import { LEVEL_TIERS } from '../constants/levelTier'
import { SPRITE_WIDTH, SPRITE_HEIGHT, STAGE_WIDTH as SCENE_WIDTH, STAGE_HEIGHT as SCENE_HEIGHT } from '../pixel/palette'

const props = defineProps({
  character: { type: Object, default: null },
  // 무대 배경 키 (ROOKIE / RISING / PRO / LEGEND)
  stageKey: { type: String, default: 'ROOKIE' },
  animate: { type: Boolean, default: true },
})
const emit = defineEmits(['actor-click'])

const stage = computed(() => LEVEL_TIERS.find((t) => t.key === props.stageKey) || LEVEL_TIERS[0])

// 캐릭터는 가운데, 발이 그 무대의 바닥 줄에 닿게 (캔버스는 들썩임 1줄을 포함해 SPRITE_HEIGHT + 1)
const pct = (v, total) => `${(v / total) * 100}%`
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
  aspect-ratio: 208 / 72;
  background-size: 100% 100%;
  background-repeat: no-repeat;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
.actor { position: absolute; line-height: 0; }
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
