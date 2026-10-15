// 픽셀 캐릭터 팔레트 규격
// 스프라이트 PNG 안에서 아래 "기준색"으로 칠한 픽셀은 렌더링 시 사용자가 고른 색으로 치환된다.
// 그 외의 색(피부, 눈, 바지 등)은 PNG에 있는 그대로 그려진다.

export const SPRITE_WIDTH = 24
export const SPRITE_HEIGHT = 32

// 레이어 그리는 순서 (아래 → 위)
export const LAYER_ORDER = ['body', 'outfit', 'hair', 'instrument']

// 기준색: { 키: { color: PNG에 칠하는 색, from: 사용자 색 종류, shade: 밝기 보정 } }
export const KEY_COLORS = {
  HAIR: { color: '#FF00FF', from: 'hairColor', shade: 0 },
  HAIR_LIGHT: { color: '#FF80FF', from: 'hairColor', shade: 48 },
  HAIR_DARK: { color: '#B000B0', from: 'hairColor', shade: -42 },
  HAIR_LINE: { color: '#600060', from: 'hairColor', shade: -90 },

  INSTRUMENT: { color: '#00FFFF', from: 'instrumentColor', shade: 0 },
  INSTRUMENT_DARK: { color: '#00B0B0', from: 'instrumentColor', shade: -48 },
  INSTRUMENT_LINE: { color: '#006060', from: 'instrumentColor', shade: -95 },

  OUTFIT: { color: '#FFFF00', from: 'outfitColor', shade: 0 },
  OUTFIT_DARK: { color: '#B0B000', from: 'outfitColor', shade: -40 },
  OUTFIT_LINE: { color: '#606000', from: 'outfitColor', shade: -85 },
}

export const DEFAULT_COLORS = {
  hairColor: '#111827',
  instrumentColor: '#B91C1C',
  outfitColor: '#FFFFFF',
}

export function hexToRgb(hex) {
  const clean = String(hex || '').replace('#', '')
  const full = clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean
  const n = parseInt(full, 16)
  if (!Number.isFinite(n) || full.length !== 6) return null
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}

export function shadeRgb([r, g, b], amount) {
  const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)))
  return [clamp(r + amount), clamp(g + amount), clamp(b + amount)]
}

// 사용자 색 → 기준색 치환표. 반환: { 'r,g,b'(기준색): [r,g,b](실제 색) }
export function buildSwapTable(colors = {}) {
  const table = new Map()
  for (const def of Object.values(KEY_COLORS)) {
    const source = hexToRgb(colors[def.from] ?? DEFAULT_COLORS[def.from]) || hexToRgb(DEFAULT_COLORS[def.from])
    table.set(hexToRgb(def.color).join(','), shadeRgb(source, def.shade))
  }
  return table
}
