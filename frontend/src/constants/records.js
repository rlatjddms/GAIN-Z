// 업적 보상 레코드. 희귀도마다 음반 형태가 정해져 있다
// 백엔드 희귀도 enum(COMMON / RARE / LEGENDARY)과 순서를 맞춘다
export const RECORD_RARITIES = [
  { key: 'COMMON', type: 'CASSETTE', label: '카세트', rarity: '일반', border: '#ced4da' },
  { key: 'RARE', type: 'CD', label: 'CD', rarity: '레어', border: '#8ea8ff' },
  { key: 'LEGENDARY', type: 'LP', label: 'LP', rarity: '전설', border: '#f5b942' },
]

export const rarityOf = (key) => RECORD_RARITIES.find((r) => r.key === key) || RECORD_RARITIES[0]

// 진행도 글자: '3 / 7일', '12 / 50시간', '0 / 1'
export function progressText(progress) {
  if (!progress) return ''
  const unit = { DAY: '일', HOUR: '시간', COUNT: '' }[progress.unit] ?? ''
  return `${Math.min(progress.current, progress.goal)} / ${progress.goal}${unit}`
}

export const progressRatio = (progress) => (progress?.goal ? Math.min(1, progress.current / progress.goal) : 0)
