<template>
  <svg
    class="record-icon"
    :viewBox="`0 0 ${N} ${N}`"
    :width="size"
    :height="size"
    shape-rendering="crispEdges"
    role="img"
    :aria-label="label"
  >
    <rect v-for="(p, i) in pixels" :key="i" :x="p.x" :y="p.y" width="1" height="1" :fill="p.c" />
  </svg>
</template>

<script setup>
// 업적 보상 레코드 픽셀 아이콘 (16×16). type: CASSETTE(일반) · CD(레어) · LP(전설)
// color는 재킷·라벨 색. locked면 회색 실루엣으로 그린다
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'CASSETTE' },
  color: { type: String, default: '#5a7dff' },
  locked: { type: Boolean, default: false },
  size: { type: Number, default: 48 },
})

const N = 16
const LABEL = { CASSETTE: '카세트 테이프', CD: 'CD', LP: 'LP' }
const label = computed(() => (props.locked ? `잠긴 ${LABEL[props.type]}` : LABEL[props.type]))

const shade = (hex, amt) => {
  const n = parseInt(hex.slice(1), 16)
  const ch = (v) => Math.max(0, Math.min(255, v + amt)).toString(16).padStart(2, '0')
  return `#${ch(n >> 16)}${ch((n >> 8) & 255)}${ch(n & 255)}`
}

// 카세트는 도트 지도로, CD·LP는 원을 계산해서 찍는다
const CASSETTE = [
  '................',
  '................',
  '................',
  'oooooooooooooooo',
  'obbbbbbbbbbbbbbo',
  'obllllllllllllbo',
  'oblkklwwwwlkklbo',
  'oblkklwwwwlkklbo',
  'obllllllllllllbo',
  'obbbbbbbbbbbbbbo',
  'obbboooooooobbbo',
  'obbobbbbbbbbobbo',
  'oooooooooooooooo',
  '................',
  '................',
  '................',
]

const pixels = computed(() => {
  const base = props.locked ? '#dee2e6' : props.color
  const pal = props.locked
    ? { o: '#adb5bd', b: '#dee2e6', l: '#f1f3f5', k: '#adb5bd', w: '#ced4da', s: '#e9ecef', sh: '#f8f9fa', v: '#adb5bd', g: '#bcc3ca', j: '#e9ecef', jl: '#f8f9fa', jd: '#ced4da', art: '#dee2e6' }
    : { o: '#1f2430', b: base, l: '#f8f4e8', k: '#2b2d31', w: '#4a3a30', s: '#d5dbe5', sh: '#ffffff', v: '#1a1a1f', g: '#3a3a44', j: base, jl: shade(base, 45), jd: shade(base, -40), art: shade(base, 80) }
  const out = []
  const put = (x, y, c) => out.push({ x, y, c })
  // 픽셀 중심 기준 거리
  const dist = (x, y, cx, cy) => Math.hypot(x + 0.5 - cx, y + 0.5 - cy)
  // 원 안의 칸 중 상하좌우에 바깥 칸이 있으면 외곽선 (대각선에서 끊기지 않게)
  const isEdge = (x, y, inside) => !inside(x - 1, y) || !inside(x + 1, y) || !inside(x, y - 1) || !inside(x, y + 1)

  if (props.type === 'CASSETTE') {
    CASSETTE.forEach((row, y) => [...row].forEach((ch, x) => { if (ch !== '.') put(x, y, pal[ch]) }))
    return out
  }

  if (props.type === 'CD') {
    // 은색 원판 + 가운데 라벨 + 구멍, 대각선 반사광
    const disc = (x, y) => dist(x, y, 8, 8) <= 7.4
    for (let y = 0; y < N; y++) for (let x = 0; x < N; x++) {
      const d = dist(x, y, 8, 8)
      if (!disc(x, y)) continue
      if (isEdge(x, y, disc)) put(x, y, pal.o)
      else if (d <= 1.3) continue                     // 구멍
      else if (d <= 2.1) put(x, y, pal.o)
      else if (d <= 4.1) put(x, y, props.locked ? pal.s : pal.j)
      else put(x, y, x - y >= 2 && x - y <= 4 ? pal.sh : pal.s)
    }
    return out
  }

  // LP: 재킷 오른쪽으로 검은 판이 나온 모양
  const vinyl = (x, y) => dist(x, y, 10, 8) <= 6.4
  for (let y = 0; y < N; y++) for (let x = 0; x < N; x++) {
    const d = dist(x, y, 10, 8)
    if (!vinyl(x, y)) continue
    if (isEdge(x, y, vinyl)) put(x, y, pal.o)
    else if (d <= 1.0) put(x, y, pal.o)
    else if (d <= 2.2) put(x, y, props.locked ? pal.g : shade(base, 70))
    else put(x, y, Math.floor(d) % 2 ? pal.v : pal.g)   // 홈(그루브)
  }
  for (let y = 1; y <= 14; y++) for (let x = 0; x <= 9; x++) {
    const edge = y === 1 || y === 14 || x === 0 || x === 9
    const art = dist(x, y, 5, 8) <= 2.6                // 재킷 가운데 동그란 그림
    put(x, y, edge ? pal.o : x === 1 || y === 2 ? pal.jl : x === 8 || y === 13 ? pal.jd : art ? pal.art : pal.j)
  }
  return out
})
</script>

<style scoped>
.record-icon { display: block; image-rendering: pixelated; }
</style>
