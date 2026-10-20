<template>
  <!-- 무대 장면: 배경(208×72)과 캐릭터를 같은 도트 격자에 놓는다. 화면 크기와 상관없이 비율이 같다.
       viewWidth를 주면 가운데(캐릭터 쪽)만 카메라처럼 잘라 보여 준다 (배율은 그대로) -->
  <div class="scene" :class="{ editing: decorEditable }" :style="{ aspectRatio: `${viewWidth} / ${SCENE_HEIGHT}` }">
    <div ref="worldRef" class="world" :style="worldStyle">
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
      <!-- 방 꾸미기 소품 (레코드 수집 보상): 바닥 줄에 놓고, 편집 중이면 끌어서 좌우로 옮긴다 -->
      <img
        v-for="d in decorItems" :key="d.key" class="decor" :src="d.url" :alt="decorEditable ? d.name : ''" :style="d.style"
        :title="decorEditable ? `${d.name} · 끌어서 옮기기` : null" draggable="false"
        @pointerdown="startDrag($event, d)" @pointermove="onDrag" @pointerup="endDrag" @pointercancel="endDrag"
      >
      <!-- 무대 꾸미기 효과 (여러 개를 겹쳐 켤 수 있다) -->
      <StageEffect v-for="e in effectKeys" :key="e" :effect="e" />
      <!-- 무대 좌표 위에 얹는 것들 -->
      <slot name="world" />
    </div>
    <!-- 장면 위에 얹는 UI -->
    <slot />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import PixelCharacter from './PixelCharacter.vue'
import StageEffect from './StageEffect.vue'
import { LEVEL_TIERS, stageOf } from '../constants/levelTier'
import { decorItemOf, decorUrl, DECOR_MAX_X, DECOR_MIN_Y, DECOR_MAX_Y } from '../constants/recordRewards'
import { SPRITE_WIDTH, SPRITE_HEIGHT, STAGE_WIDTH as SCENE_WIDTH, STAGE_HEIGHT as SCENE_HEIGHT } from '../pixel/palette'

const props = defineProps({
  character: { type: Object, default: null },
  // 무대 배경 키 (ROOKIE / RISING / PRO / LEGEND)
  stageKey: { type: String, default: 'ROOKIE' },
  animate: { type: Boolean, default: true },
  // 보여 줄 가로 폭 (무대 도트 기준, 최대 208). 작게 주면 가운데만 잘라서 보여 준다
  viewWidth: { type: Number, default: SCENE_WIDTH },
  // 무대 효과 목록. undefined면 캐릭터가 고른 효과(character.stageEffects), []이면 없음
  effects: { type: Array, default: undefined },
  // 방 꾸미기 소품 배치 [{ key, x }]. undefined면 캐릭터의 배치(character.decorLayout), []이면 없음
  decor: { type: Array, default: undefined },
  // true면 소품을 끌어서 옮길 수 있다 (옷장 미리보기) → decor-move(key, x, y)
  decorEditable: { type: Boolean, default: false },
})
const emit = defineEmits(['actor-click', 'decor-move'])

const stage = computed(() => stageOf(props.stageKey) || LEVEL_TIERS[0])
const effectKeys = computed(() => {
  const list = props.effects === undefined ? props.character?.stageEffects : props.effects
  return Array.isArray(list) ? list : []
})
const pct = (v, total) => `${(v / total) * 100}%`

// 소품 그림은 캐릭터와 같은 24×32 칸이고 바닥 줄에 붙인다
const decorLayout = computed(() => {
  const list = props.decor === undefined ? props.character?.decorLayout : props.decor
  return Array.isArray(list) ? list : []
})
const decorItems = computed(() => decorLayout.value
  .map((p) => ({ ...p, item: decorItemOf(p.key), url: decorUrl(p.key) }))
  .filter((d) => d.item && d.url)
  .map((d) => {
    const y = d.y ?? stage.value.floorY
    return {
      key: d.key,
      name: d.item.name,
      x: d.x,
      y,
      url: d.url,
      style: {
        left: pct(d.x, SCENE_WIDTH),
        top: pct(y - SPRITE_HEIGHT, SCENE_HEIGHT),
        width: pct(SPRITE_WIDTH, SCENE_WIDTH),
        // 캐릭터 발보다 앞(아래)에 놓은 소품은 캐릭터 앞에
        zIndex: y > stage.value.floorY ? 3 : 1,
      },
    }
  }))

// 끌기: 누른 곳과 그림의 거리를 유지한 채 무대 도트 단위로 상하좌우 옮긴다
const worldRef = ref(null)
let drag = null
const toStage = (e, rect) => ({
  x: ((e.clientX - rect.left) / rect.width) * SCENE_WIDTH,
  y: ((e.clientY - rect.top) / rect.height) * SCENE_HEIGHT,
})
const clamp = (v, min, max) => Math.round(Math.min(max, Math.max(min, v)))
const startDrag = (e, d) => {
  if (!props.decorEditable || !worldRef.value) return
  e.preventDefault()
  const rect = worldRef.value.getBoundingClientRect()
  const p = toStage(e, rect)
  drag = { key: d.key, dx: p.x - d.x, dy: p.y - d.y, rect }
  e.currentTarget.setPointerCapture?.(e.pointerId)
}
const onDrag = (e) => {
  if (!drag) return
  const p = toStage(e, drag.rect)
  emit('decor-move', drag.key, clamp(p.x - drag.dx, 0, DECOR_MAX_X), clamp(p.y - drag.dy, DECOR_MIN_Y, DECOR_MAX_Y))
}
const endDrag = () => { drag = null }

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
.decor { position: absolute; z-index: 1; height: auto; image-rendering: pixelated; image-rendering: crisp-edges; pointer-events: none; user-select: none; }
/* 소품 옮기기: 앞뒤 순서는 그대로 두고(캐릭터 뒤 소품은 뒤에), 캐릭터가 클릭을 가로채지 않게 해서 뒤에 있는 소품도 잡힌다 */
.editing .decor { pointer-events: auto; cursor: grab; touch-action: none; outline: 1px dashed rgba(45, 91, 255, 0.7); outline-offset: -1px; }
.editing .decor:active { cursor: grabbing; }
.editing .actor { pointer-events: none; }
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
