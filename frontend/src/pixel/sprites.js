import { SPRITE_WIDTH, SPRITE_HEIGHT, buildSwapTable } from './palette'

// assets/sprites/<layer>/<NAME>.png 를 전부 등록한다. 파일명 = 백엔드 enum 값.
const files = import.meta.glob('../assets/sprites/*/*.png', { eager: true, query: '?url', import: 'default' })

const registry = {}
for (const [path, url] of Object.entries(files)) {
  const match = path.match(/sprites\/([^/]+)\/([^/]+)\.png$/)
  if (!match) continue
  const [, layer, name] = match
  registry[layer] = registry[layer] || {}
  registry[layer][name] = url
}

export function listSprites(layer) {
  return Object.keys(registry[layer] || {}).sort()
}

export function spriteUrl(layer, name) {
  return registry[layer]?.[name] || null
}

// 상의 스프라이트 파일 이름: 성별 그림(<키>_<성별>) → 공용(<키>) → 기본 의상
export function outfitSpriteName(key, gender) {
  const body = gender === 'MALE' ? 'MALE' : 'FEMALE'
  const k = key || 'DEFAULT'
  if (spriteUrl('outfit', `${k}_${body}`)) return `${k}_${body}`
  if (spriteUrl('outfit', k)) return k
  return `DEFAULT_${body}`
}

const imageDataCache = new Map()

function loadImage(url) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error(`스프라이트를 불러오지 못했습니다: ${url}`))
    img.src = url
  })
}

// PNG → ImageData (기준색 그대로). 한 번 읽은 파일은 캐시한다.
export async function loadSpriteData(layer, name) {
  const url = spriteUrl(layer, name)
  if (!url) return null
  if (imageDataCache.has(url)) return imageDataCache.get(url)

  const promise = loadImage(url).then((img) => {
    const canvas = document.createElement('canvas')
    canvas.width = SPRITE_WIDTH
    canvas.height = SPRITE_HEIGHT
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    return ctx.getImageData(0, 0, SPRITE_WIDTH, SPRITE_HEIGHT)
  })
  imageDataCache.set(url, promise)
  return promise
}

// 기준색을 사용자 색으로 바꾼 새 캔버스를 돌려준다 (1배 크기).
export function recolorSprite(imageData, colors) {
  const table = buildSwapTable(colors)
  const out = new ImageData(new Uint8ClampedArray(imageData.data), imageData.width, imageData.height)
  const d = out.data
  for (let i = 0; i < d.length; i += 4) {
    if (d[i + 3] === 0) continue
    const swapped = table.get(`${d[i]},${d[i + 1]},${d[i + 2]}`)
    if (swapped) {
      d[i] = swapped[0]
      d[i + 1] = swapped[1]
      d[i + 2] = swapped[2]
    }
  }
  const canvas = document.createElement('canvas')
  canvas.width = imageData.width
  canvas.height = imageData.height
  canvas.getContext('2d').putImageData(out, 0, 0)
  return canvas
}

// ---------- 아이템 색 바꾸기 (constants/itemColors.js) ----------
const hexToRgb = (hex) => {
  const n = parseInt(String(hex).replace('#', ''), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}
function rgbToHsl([r, g, b]) {
  r /= 255; g /= 255; b /= 255
  const max = Math.max(r, g, b), min = Math.min(r, g, b), l = (max + min) / 2
  if (max === min) return [0, 0, l]
  const d = max - min
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
  const h = max === r ? (g - b) / d + (g < b ? 6 : 0) : max === g ? (b - r) / d + 2 : (r - g) / d + 4
  return [h / 6, s, l]
}
function hslToRgb([h, s, l]) {
  if (!s) return [l, l, l].map((v) => Math.round(v * 255))
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q
  const f = (t) => {
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1 / 6) return p + (q - p) * 6 * t
    if (t < 1 / 2) return q
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6
    return p
  }
  return [f(h + 1 / 3), f(h), f(h - 1 / 3)].map((v) => Math.round(v * 255))
}
const clamp01 = (v) => Math.max(0, Math.min(1, v))

// 원래 색들 → 고른 색에 맞춘 색. 바탕색과의 밝기 차이(그림자·선)와 채도 비율은 유지하고 색조는 고른 색으로
// 회색 계열 아이템(바탕 채도 0.15 이하)은 모든 색을 고른 색의 채도로 (안 그러면 옅은 무늬만 회색으로 남는다)
export function shiftTable(colors, chosen, contrast = 1) {
  const [, sBase, lBase] = rgbToHsl(hexToRgb(colors[0]))
  const [hC, sC, lC] = rgbToHsl(hexToRgb(chosen))
  const table = new Map()
  for (const c of colors) {
    const [, s, l] = rgbToHsl(hexToRgb(c))
    const sat = sBase > 0.15 ? clamp01(sC * (s / sBase)) : sC
    table.set(hexToRgb(c).join(','), hslToRgb([hC, sat, clamp01(lC + (l - lBase) * contrast)]))
  }
  return table
}

// 아이템 한 장을 그린다: 기준색 치환 + 고른 아이템 색 (그림의 바탕색과 같으면 그림 그대로)
// spec: ITEM_COLORS의 항목, chosen: 고른 색
export function paintSprite(imageData, colors, spec, chosen) {
  const base = spec?.key && chosen ? { ...colors, outfitColor: chosen } : colors
  const canvas = recolorSprite(imageData, base)
  if (!spec?.colors || !chosen || chosen.toLowerCase() === spec.colors[0].toLowerCase()) return canvas
  const table = shiftTable(spec.colors, chosen, spec.contrast)
  const ctx = canvas.getContext('2d')
  const out = ctx.getImageData(0, 0, canvas.width, canvas.height)
  const d = out.data
  for (let i = 0; i < d.length; i += 4) {
    if (!d[i + 3]) continue
    const t = table.get(`${d[i]},${d[i + 1]},${d[i + 2]}`)
    if (t) { d[i] = t[0]; d[i + 1] = t[1]; d[i + 2] = t[2] }
  }
  ctx.putImageData(out, 0, 0)
  return canvas
}
