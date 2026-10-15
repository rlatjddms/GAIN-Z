// 업적 보상 레코드 (구 피크). 희귀도마다 음반 형태가 정해져 있다
// 백엔드 희귀도 enum(COMMON / RARE / LEGENDARY)과 순서를 맞춘다
export const RECORD_RARITIES = [
  { key: 'COMMON', type: 'CASSETTE', label: '카세트', rarity: '일반', border: '#ced4da' },
  { key: 'RARE', type: 'CD', label: 'CD', rarity: '레어', border: '#8ea8ff' },
  { key: 'LEGENDARY', type: 'LP', label: 'LP', rarity: '전설', border: '#f5b942' },
]

// 실제 레코드 목록(#47)이 연결되기 전까지 보여 줄 빈 칸 (카세트 6 · CD 4 · LP 2)
export const PLACEHOLDER_RECORDS = [
  ...Array(6).fill(RECORD_RARITIES[0]),
  ...Array(4).fill(RECORD_RARITIES[1]),
  ...Array(2).fill(RECORD_RARITIES[2]),
]
