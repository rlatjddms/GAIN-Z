// 픽셀 캐릭터 팔레트 규격
// 스프라이트 PNG 안에서 아래 "기준색"으로 칠한 픽셀은 렌더링 시 사용자가 고른 색으로 치환된다.
// 그 외의 색(피부, 눈, 바지 등)은 PNG에 있는 그대로 그려진다.

export const SPRITE_WIDTH = 24
export const SPRITE_HEIGHT = 32

// 무대 배경 원본 크기 (assets/stages/<tier>.png). 캐릭터와 같은 도트 격자에 놓는다
export const STAGE_WIDTH = 208
export const STAGE_HEIGHT = 72

// 레이어 그리는 순서 (아래 → 위). shoes·neck·glasses·hat은 레벨 보상 소품 (constants/rewards.js)
export const LAYER_ORDER = ['body', 'shoes', 'outfit', 'neck', 'hair', 'glasses', 'hat', 'instrument']

// 기준색: { 키: { color: PNG에 칠하는 색, from: 사용자 색 종류, shade: 밝기 보정 } }
export const KEY_COLORS = {
  // 피부는 몸 스프라이트의 실제 색을 그대로 기준색으로 쓴다 (기본 피부색일 때 원본과 거의 같게 보임)
  SKIN: { color: '#F8D7B5', from: 'skinColor', shade: 0 },
  SKIN_LIGHT: { color: '#FDE7CF', from: 'skinColor', shade: 12 },
  SKIN_SHADOW: { color: '#E3B48D', from: 'skinColor', shade: -32 },
  SKIN_LINE: { color: '#9C5B36', from: 'skinColor', shade: -105 },

  HAIR: { color: '#FF00FF', from: 'hairColor', shade: 0 },
  HAIR_LIGHT: { color: '#FF80FF', from: 'hairColor', shade: 48 },
  HAIR_DARK: { color: '#B000B0', from: 'hairColor', shade: -42 },
  HAIR_LINE: { color: '#600060', from: 'hairColor', shade: -90 },

  INSTRUMENT: { color: '#00FFFF', from: 'instrumentColor', shade: 0 },
  INSTRUMENT_LIGHT: { color: '#80FFFF', from: 'instrumentColor', shade: 40 },
  INSTRUMENT_DARK: { color: '#00B0B0', from: 'instrumentColor', shade: -48 },
  INSTRUMENT_LINE: { color: '#006060', from: 'instrumentColor', shade: -95 },

  OUTFIT: { color: '#FFFF00', from: 'outfitColor', shade: 0 },
  OUTFIT_DARK: { color: '#B0B000', from: 'outfitColor', shade: -40 },
  OUTFIT_LINE: { color: '#606000', from: 'outfitColor', shade: -85 },
}

export const DEFAULT_COLORS = {
  skinColor: '#F8D7B5',
  hairColor: '#111827',
  instrumentColor: '#B91C1C',
  outfitColor: '#FFFFFF',
}

// 온보딩에서 보여줄 피부색 프리셋 (밝은 톤 → 어두운 톤)
export const SKIN_PRESETS = ['#FDE4D0', '#F8D7B5', '#E8B894', '#C68642', '#8D5524', '#5C3A21']

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
