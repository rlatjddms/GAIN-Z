<template>
  <canvas
    ref="canvasRef"
    class="pixel-sprite"
    :width="size.w"
    :height="size.h"
    :style="{ width: `${size.w}px`, height: `${size.h}px` }"
    role="img"
    :aria-label="label"
  ></canvas>
</template>

<script setup>
// 캐릭터 스프라이트 조각만 그린다 (옷장 소품·악기 썸네일, 헤어는 머리 부분만)
// parts: [['hat', 'BEANIE']] 처럼 [레이어, 이름]을 아래 → 위 순서로. 그림이 있는 칸만 잘라서 box 안에 들어가게 정수배로 키운다
import { ref, computed, watch, onMounted } from 'vue'
import { SPRITE_WIDTH, SPRITE_HEIGHT } from '../pixel/palette'
import { loadSpriteData, paintSprite } from '../pixel/sprites'
import { ITEM_COLORS, itemKeyOf, itemColorOf } from '../constants/itemColors'

const props = defineProps({
  parts: { type: Array, required: true },
  // 기준색을 바꿀 색 (skinColor·hairColor·instrumentColor·outfitColor)
  colors: { type: Object, default: () => ({}) },
  // 아이템마다 고른 색 (없으면 기본 색)
  itemColors: { type: Object, default: () => ({}) },
  // 자를 영역 { x, y, w, h }. 없으면 그림이 있는 칸만
  region: { type: Object, default: null },
  // 들어갈 상자 크기(px). 이 안에 맞는 가장 큰 정수배
  box: { type: Number, default: 48 },
  // true면 정수배가 아니어도 상자에 꽉 맞춘다 (크기가 제각각인 악기를 비슷한 크기로 보여 줄 때)
  exact: { type: Boolean, default: false },
  label: { type: String, default: '' },
})

const canvasRef = ref(null)
const crop = ref({ x: 0, y: 0, w: SPRITE_WIDTH, h: SPRITE_HEIGHT })
const scale = computed(() => {
  const fit = Math.min(props.box / crop.value.w, props.box / crop.value.h)
  return props.exact ? fit : Math.max(1, Math.floor(fit))
})
const size = computed(() => ({ w: Math.round(crop.value.w * scale.value), h: Math.round(crop.value.h * scale.value) }))

let token = 0
async function render() {
  const mine = ++token
  const layers = (await Promise.all(props.parts.map(async ([layer, name]) => {
    const data = name ? await loadSpriteData(layer, name) : null
    if (!data) return null
    const key = itemKeyOf(name)
    return paintSprite(data, props.colors, ITEM_COLORS[key], itemColorOf(key, props.itemColors))
  }))).filter(Boolean)
  if (mine !== token) return

  // 레이어를 1배로 합친다
  const merged = document.createElement('canvas')
  merged.width = SPRITE_WIDTH
  merged.height = SPRITE_HEIGHT
  const mctx = merged.getContext('2d')
  for (const l of layers) mctx.drawImage(l, 0, 0)

  crop.value = props.region || boundsOf(mctx.getImageData(0, 0, SPRITE_WIDTH, SPRITE_HEIGHT))
  await new Promise((r) => requestAnimationFrame(r))   // 캔버스 크기가 바뀐 뒤에 그린다
  if (mine !== token || !canvasRef.value) return
  const ctx = canvasRef.value.getContext('2d')
  ctx.imageSmoothingEnabled = false
  ctx.clearRect(0, 0, size.value.w, size.value.h)
  const { x, y, w, h } = crop.value
  ctx.drawImage(merged, x, y, w, h, 0, 0, size.value.w, size.value.h)
}

// 투명하지 않은 칸을 모두 담는 가장 작은 사각형
function boundsOf(image) {
  let minX = SPRITE_WIDTH, minY = SPRITE_HEIGHT, maxX = -1, maxY = -1
  for (let y = 0; y < image.height; y++) {
    for (let x = 0; x < image.width; x++) {
      if (!image.data[(y * image.width + x) * 4 + 3]) continue
      if (x < minX) minX = x
      if (x > maxX) maxX = x
      if (y < minY) minY = y
      if (y > maxY) maxY = y
    }
  }
  if (maxX < 0) return { x: 0, y: 0, w: SPRITE_WIDTH, h: SPRITE_HEIGHT }
  return { x: minX, y: minY, w: maxX - minX + 1, h: maxY - minY + 1 }
}

onMounted(render)
watch(() => [props.parts, props.colors, props.itemColors, props.region, props.box, props.exact], render, { deep: true })
</script>

<style scoped>
.pixel-sprite {
  display: block;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
</style>
