// 레벨 보상 (#120). 백엔드 Accessory·HairStyle·LevelOutfits·StageBackground와 같은 값
// 한쪽을 바꾸면 같이 바꾼다. 스프라이트: assets/sprites/<slot 폴더>/<key>.png
import { MAX_LEVEL } from './levelTier'

// 소품 착용 자리. folder = 스프라이트 폴더, field = 캐릭터 응답의 필드 이름
export const ACCESSORY_SLOTS = [
  { key: 'HAT', label: '모자', folder: 'hat', field: 'hat' },
  { key: 'GLASSES', label: '안경', folder: 'glasses', field: 'glasses' },
  { key: 'NECK', label: '목', folder: 'neck', field: 'neck' },
  { key: 'SHOES', label: '신발', folder: 'shoes', field: 'shoes' },
]

export const ACCESSORIES = [
  { key: 'SNEAKERS', slot: 'SHOES', name: '운동화', level: 2 },
  { key: 'BEANIE', slot: 'HAT', name: '비니', level: 3 },
  { key: 'ROUND_GLASSES', slot: 'GLASSES', name: '동그란 안경', level: 6 },
  { key: 'HEADPHONES', slot: 'NECK', name: '헤드폰', level: 7 },
  { key: 'HIGH_TOPS', slot: 'SHOES', name: '하이탑', level: 8 },
  { key: 'CAP', slot: 'HAT', name: '볼캡', level: 9 },
  { key: 'SUNGLASSES', slot: 'GLASSES', name: '선글라스', level: 12 },
  { key: 'PICK_NECKLACE', slot: 'NECK', name: '피크 목걸이', level: 13 },
  { key: 'CHELSEA_BOOTS', slot: 'SHOES', name: '첼시 부츠', level: 14 },
  { key: 'BUCKET_HAT', slot: 'HAT', name: '버킷햇', level: 16 },
  { key: 'SCARF', slot: 'NECK', name: '스카프', level: 18 },
  { key: 'STAR_GLASSES', slot: 'GLASSES', name: '별 선글라스', level: 19 },
  { key: 'COMBAT_BOOTS', slot: 'SHOES', name: '워커', level: 21 },
  { key: 'BERET', slot: 'HAT', name: '베레모', level: 22 },
  { key: 'CHAIN', slot: 'NECK', name: '체인 목걸이', level: 24 },
  { key: 'HEART_GLASSES', slot: 'GLASSES', name: '하트 선글라스', level: 26 },
  { key: 'GOLD_SNEAKERS', slot: 'SHOES', name: '골드 운동화', level: 27 },
  { key: 'CROWN', slot: 'HAT', name: '왕관', level: 28 },
]

// 레벨로 얻는 헤어스타일 (처음부터 고를 수 있는 머리는 characterOptions.js에 level 1)
export const REWARD_HAIR_STYLES = [
  { key: 'PONYTAIL', name: '포니테일', level: 4 },
  { key: 'SPIKY', name: '스파이크', level: 11 },
  { key: 'BUN', name: '번 헤어', level: 17 },
  { key: 'TWIN_TAILS', name: '트윈테일', level: 23 },
  { key: 'AFRO', name: '아프로', level: 29 },
]

// 5레벨마다 의상 (spriteKey = 의상 스프라이트 이름)
export const REWARD_OUTFITS = [
  { key: 'HOODIE', name: '후드티', level: 5 },
  { key: 'BAND_TEE', name: '밴드 티셔츠', level: 10 },
  { key: 'DENIM_JACKET', name: '데님 재킷', level: 15 },
  { key: 'LEATHER_JACKET', name: '가죽 재킷', level: 20 },
  { key: 'STAGE_SUIT', name: '무대 정장', level: 25 },
  { key: 'SEQUIN_JACKET', name: '반짝이 재킷', level: 30 },
]

export const STAGE_REWARDS = [
  { key: 'RISING', name: '동네 클럽', level: 10 },
  { key: 'PRO', name: '투어 공연장', level: 20 },
  { key: 'LEGEND', name: '전설의 무대', level: 30 },
]

export const slotOf = (key) => ACCESSORY_SLOTS.find((s) => s.key === key)
export const accessoryOf = (key) => ACCESSORIES.find((a) => a.key === key)

// 그 레벨에 받는 보상 목록: [{ type: 'OUTFIT'|'ACCESSORY'|'HAIR'|'STAGE', key, name, slot? }]
export function rewardsAt(level) {
  const out = []
  for (const o of REWARD_OUTFITS) if (o.level === level) out.push({ type: 'OUTFIT', key: o.key, name: o.name })
  for (const s of STAGE_REWARDS) if (s.level === level) out.push({ type: 'STAGE', key: s.key, name: s.name })
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

export const REWARD_TYPE_LABELS = { OUTFIT: '의상', STAGE: '무대', HAIR: '헤어', ACCESSORY: '소품' }
export const rewardLabel = (r) => (r.type === 'ACCESSORY' ? slotOf(r.slot)?.label : REWARD_TYPE_LABELS[r.type])
