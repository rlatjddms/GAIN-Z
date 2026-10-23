<template>
  <canvas
    ref="canvasRef"
    class="pixel-character"
    :width="SPRITE_WIDTH * scale"
    :height="(SPRITE_HEIGHT + 1) * scale"
    :style="{ width: `${SPRITE_WIDTH * scale}px`, height: `${(SPRITE_HEIGHT + 1) * scale}px` }"
    role="img"
    :aria-label="label"
  ></canvas>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { SPRITE_WIDTH, SPRITE_HEIGHT, LAYER_ORDER, DEFAULT_COLORS } from '../pixel/palette'
import { loadSpriteData, paintSprite, spriteUrl, outfitSpriteName } from '../pixel/sprites'
import { ITEM_COLORS, itemKeyOf, itemColorOf } from '../constants/itemColors'

const props = defineProps({
  // 백엔드 CharacterResponse 와 같은 모양. 없으면 기본 캐릭터.
  character: { type: Object, default: null },
  scale: { type: Number, default: 5 },
  animate: { type: Boolean, default: true },
  // 의상 스프라이트 키 (OutfitItem 연동 전까지 DEFAULT)
  outfit: { type: String, default: 'DEFAULT' },
})

const canvasRef = ref(null)
let frame = 0
let timer = null
let layers = []          // [{ layer, canvas }]
let loadToken = 0

// hairStyle / instrumentType 이 null 이면 그 레이어를 그리지 않는다 (온보딩에서 아직 고르기 전)
const spec = computed(() => {
  const c = props.character || {}
  const body = c.gender === 'MALE' ? 'MALE' : 'FEMALE'
  // 의상은 체형이 달라서 성별별 파일(<이름>_<성별>.png)을 먼저 찾고, 없으면 공용 파일,
  // 그림이 아직 없는 의상이면 기본 의상을 입힌다
  const outfit = outfitSpriteName(props.outfit, body)
  return {
    body,
    outfit,
    // 소품: 캐릭터 응답의 shoes·neck·glasses·hat (없으면 안 그림)
    // 하의·신발은 늘 입는다 (고른 적 없으면 기본 청바지·신발)
    bottom: c.bottom || 'JEANS',
    shoes: c.shoes || 'BASIC_SHOES',
    waist: c.waist || null,
    neck: c.neck || null,
    earrings: c.earrings || null,
    glasses: c.glasses || null,
    hat: c.hat || null,
    hair: c.hairStyle === null ? null : (c.hairStyle || 'SHORT_CUT'),
    // 악기를 내려놓은 캐릭터(instrumentHidden)는 악기를 그리지 않는다
    instrument: c.instrumentType === null || c.instrumentHidden ? null : (c.instrumentType || 'GUITAR'),
    colors: {
      skinColor: c.skinColor || DEFAULT_COLORS.skinColor,
      hairColor: c.hairColor || DEFAULT_COLORS.hairColor,
      instrumentColor: c.instrumentColor || DEFAULT_COLORS.instrumentColor,
      outfitColor: c.outfitColor || DEFAULT_COLORS.outfitColor,
    },
    // 아이템마다 고른 색 { HOODIE: '#ffd8a8', ... } (constants/itemColors.js)
    itemColors: c.itemColors || {},
  }
})

const label = computed(() => {
  const hair = spec.value.hair ? `${spec.value.hair} 머리` : '머리 없음'
  const inst = spec.value.instrument ? `${spec.value.instrument} 연주자` : '악기 없음'
  return `${hair}, ${inst} 픽셀 캐릭터`
})

// 악기는 몸과 반대 위상으로 1px 움직여서 "연주 중" 느낌을 준다
const INSTRUMENT_COUNTER_BOB = new Set(['GUITAR', 'BASS', 'VOCAL'])

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.imageSmoothingEnabled = false
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  const bob = props.animate ? frame % 2 : 0
  for (const { layer, canvas: sprite } of layers) {
    let dy = bob
    if (layer === 'instrument' && INSTRUMENT_COUNTER_BOB.has(spec.value.instrument)) dy = bob ? 0 : 1
    if (layer === 'instrument' && !INSTRUMENT_COUNTER_BOB.has(spec.value.instrument)) dy = 0
    ctx.drawImage(sprite, 0, dy * props.scale, SPRITE_WIDTH * props.scale, SPRITE_HEIGHT * props.scale)
  }
}

async function rebuild() {
  const token = ++loadToken
  const s = spec.value
  const names = { body: s.body, outfit: s.outfit, hair: s.hair, instrument: s.instrument, bottom: s.bottom, shoes: s.shoes, waist: s.waist, neck: s.neck, earrings: s.earrings, glasses: s.glasses, hat: s.hat }

  const loaded = await Promise.all(
    LAYER_ORDER.map(async (layer) => {
      const name = names[layer]
      if (!name || !spriteUrl(layer, name)) return null
      const data = await loadSpriteData(layer, name)
      if (!data) return null
      // 아이템이면 고른 색으로 (몸·머리·악기는 표에 없어서 기준색만 바뀐다)
      const key = itemKeyOf(name)
      return { layer, canvas: paintSprite(data, s.colors, ITEM_COLORS[key], itemColorOf(key, s.itemColors)) }
    })
  )
  if (token !== loadToken) return
  layers = loaded.filter(Boolean)
  draw()
}

function startAnimation() {
  stopAnimation()
  if (!props.animate) return
  const reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduce) return
  timer = setInterval(() => { frame++; draw() }, 500)
}

function stopAnimation() {
  if (timer) clearInterval(timer)
  timer = null
}

onMounted(() => { rebuild(); startAnimation() })
onBeforeUnmount(stopAnimation)
watch(spec, rebuild, { deep: true })
watch(() => props.scale, draw)
watch(() => props.animate, startAnimation)
</script>

<style scoped>
.pixel-character {
  display: block;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}
</style>
