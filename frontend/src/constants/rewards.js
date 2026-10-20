// 레벨 보상 (#120). 백엔드 Accessory·HairStyle·LevelOutfits·StageBackground와 같은 값
// 한쪽을 바꾸면 같이 바꾼다. 스프라이트: assets/sprites/<slot 폴더>/<key>.png
import { MAX_LEVEL } from './levelTier'

// 소품 착용 자리. folder = 스프라이트 폴더, field = 캐릭터 응답의 필드 이름
// required: 늘 하나는 입는 자리 (안 고른 캐릭터는 defaultKey)
export const ACCESSORY_SLOTS = [
  { key: 'HAT', label: '모자', folder: 'hat', field: 'hat' },
  { key: 'GLASSES', label: '안경', folder: 'glasses', field: 'glasses' },
  { key: 'EARRINGS', label: '귀걸이', folder: 'earrings', field: 'earrings' },
  { key: 'NECK', label: '목', folder: 'neck', field: 'neck' },
  { key: 'WAIST', label: '허리', folder: 'waist', field: 'waist' },
  { key: 'SHOES', label: '신발', folder: 'shoes', field: 'shoes', required: true, defaultKey: 'BASIC_SHOES' },
]

// level 1 = 처음부터 가진 기본 아이템
export const ACCESSORIES = [
  { key: 'BASIC_SHOES', slot: 'SHOES', name: '기본 신발', level: 1 },
  { key: 'SNEAKERS', slot: 'SHOES', name: '운동화', level: 2 },
  { key: 'BEANIE', slot: 'HAT', name: '비니', level: 3 },
  { key: 'ROUND_GLASSES', slot: 'GLASSES', name: '동그란 안경', level: 6 },
  { key: 'CAP', slot: 'HAT', name: '볼캡', level: 9 },
  { key: 'SUNGLASSES', slot: 'GLASSES', name: '선글라스', level: 12 },
  // Lv.13 헤드폰은 목에 걸기·머리에 쓰기 두 가지로 받는다
  { key: 'HEADPHONES', slot: 'NECK', name: '헤드폰', level: 13 },
  { key: 'HEAD_HEADPHONES', slot: 'HAT', name: '헤드폰', level: 13 },
  { key: 'STUD_EARRINGS', slot: 'EARRINGS', name: '스터드 귀걸이', level: 14 },
  { key: 'BELT', slot: 'WAIST', name: '체인 벨트', level: 16 },
  { key: 'HOOP_EARRINGS', slot: 'EARRINGS', name: '링 귀걸이', level: 18 },
  { key: 'STAR_GLASSES', slot: 'GLASSES', name: '별 선글라스', level: 19 },
  { key: 'HIGH_TOPS', slot: 'SHOES', name: '하이탑', level: 21 },
  { key: 'STAR_EARRINGS', slot: 'EARRINGS', name: '별 귀걸이', level: 22 },
  { key: 'CHAIN', slot: 'NECK', name: '체인 목걸이', level: 24 },
  { key: 'HEART_GLASSES', slot: 'GLASSES', name: '하트 선글라스', level: 26 },
  { key: 'COMBAT_BOOTS', slot: 'SHOES', name: '워커', level: 27 },
  { key: 'CROWN', slot: 'HAT', name: '왕관', level: 28 },
]

// 하의: 늘 하나는 입는다 (기본은 청바지). 5레벨마다 상의와 한 벌, 치마는 Lv.8
export const BOTTOM_SLOT = { key: 'BOTTOM', label: '하의', folder: 'bottom', field: 'bottom', required: true, defaultKey: 'JEANS' }
export const BOTTOMS = [
  { key: 'JEANS', slot: 'BOTTOM', name: '청바지', level: 1 },
  { key: 'JOGGERS', slot: 'BOTTOM', name: '조거 팬츠', level: 5 },
  { key: 'SKIRT', slot: 'BOTTOM', name: '치마', level: 8 },
  { key: 'RIPPED_JEANS', slot: 'BOTTOM', name: '찢어진 청바지', level: 10 },
  { key: 'CARGO_PANTS', slot: 'BOTTOM', name: '카고 바지', level: 15 },
  { key: 'LEATHER_PANTS', slot: 'BOTTOM', name: '가죽 바지', level: 20 },
  { key: 'SUIT_PANTS', slot: 'BOTTOM', name: '정장 바지', level: 25 },
  { key: 'GOLD_STRIPE_PANTS', slot: 'BOTTOM', name: '금줄 바지', level: 30 },
]

// 레벨로 얻는 헤어스타일 (처음부터 고를 수 있는 머리는 characterOptions.js에 level 1)
export const REWARD_HAIR_STYLES = [
  { key: 'PONYTAIL', name: '포니테일', level: 4 },
  { key: 'SPIKY', name: '스파이크', level: 11 },
  { key: 'BUN', name: '번 헤어', level: 17 },
  { key: 'TWIN_TAILS', name: '트윈테일', level: 23 },
  { key: 'AFRO', name: '아프로', level: 29 },
]

// 레벨 보상 상의 (spriteKey = 의상 스프라이트 이름). 5레벨마다 + 크롭티는 Lv.7
export const REWARD_OUTFITS = [
  { key: 'HOODIE', name: '후드티', level: 5 },
  { key: 'CROP', name: '크롭티', level: 7 },
  { key: 'BAND_TEE', name: '밴드 티셔츠', level: 10 },
  { key: 'DENIM_JACKET', name: '데님 재킷', level: 15 },
  { key: 'LEATHER_JACKET', name: '가죽 재킷', level: 20 },
  { key: 'STAGE_SUIT', name: '무대 정장', level: 25 },
  { key: 'GOLD_TRIM_JACKET', name: '금장 재킷', level: 30 },
]

// 무대 꾸미기 효과: 5레벨 보상 중 배경이 없는 Lv.5·15·25와 마지막 Lv.30 (components/StageEffect.vue)
export const STAGE_EFFECTS = [
  { key: 'SPARKLE', name: '반짝이', level: 5 },
  { key: 'SMOKE', name: '스모크', level: 15 },
  { key: 'SPOTLIGHT', name: '스포트라이트', level: 25 },
  { key: 'FIREWORKS', name: '불꽃놀이', level: 30 },
]

export const STAGE_REWARDS = [
  { key: 'RISING', name: '동네 클럽', level: 10 },
  { key: 'PRO', name: '투어 공연장', level: 20 },
  { key: 'LEGEND', name: '전설의 무대', level: 30 },
]

// 입고 있는 아이템 (늘 입는 자리는 비어 있으면 기본 아이템)
export const wornIn = (slot, character) => character?.[slot.field] || (slot.required ? slot.defaultKey : null)

export const slotOf = (key) => (key === BOTTOM_SLOT.key ? BOTTOM_SLOT : ACCESSORY_SLOTS.find((s) => s.key === key))

// 그 레벨에 받는 보상 목록: [{ type: 'OUTFIT'|'BOTTOM'|'STAGE'|'EFFECT'|'ACCESSORY'|'HAIR', key, name, slot? }]
export function rewardsAt(level) {
  const out = []
  for (const o of REWARD_OUTFITS) if (o.level === level) out.push({ type: 'OUTFIT', key: o.key, name: o.name })
  for (const b of BOTTOMS) if (b.level === level) out.push({ type: 'BOTTOM', key: b.key, name: b.name, slot: 'BOTTOM' })
  for (const s of STAGE_REWARDS) if (s.level === level) out.push({ type: 'STAGE', key: s.key, name: s.name })
  for (const e of STAGE_EFFECTS) if (e.level === level) out.push({ type: 'EFFECT', key: e.key, name: e.name })
  for (const a of ACCESSORIES) if (a.level === level) out.push({ type: 'ACCESSORY', key: a.key, name: a.name, slot: a.slot })
  for (const h of REWARD_HAIR_STYLES) if (h.level === level) out.push({ type: 'HAIR', key: h.key, name: h.name })
  return out
}

// from보다 높고 to 이하인 레벨에서 받은 보상 (레벨업 연출용)
export function rewardsBetween(from, to) {
  const out = []
  for (let lv = from + 1; lv <= Math.min(to, MAX_LEVEL); lv++) out.push(...rewardsAt(lv).map((r) => ({ ...r, level: lv })))
  return out
}

// 다음에 받을 보상 (지금 레벨보다 높은 첫 레벨)
export function nextReward(level) {
  for (let lv = level + 1; lv <= MAX_LEVEL; lv++) {
    const r = rewardsAt(lv)
    if (r.length) return { level: lv, rewards: r }
  }
  return null
}

export const REWARD_TYPE_LABELS = { OUTFIT: '상의', BOTTOM: '하의', STAGE: '무대', EFFECT: '무대 효과', HAIR: '헤어', ACCESSORY: '소품' }
export const rewardLabel = (r) => (r.type === 'ACCESSORY' ? slotOf(r.slot)?.label : REWARD_TYPE_LABELS[r.type])
