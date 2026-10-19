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
