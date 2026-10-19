import axiosInstance from './axios'

// 업적 레코드 (카세트·CD·LP)

// 레코드 → { key, name, description, condition: 얻는 조건, rarity: COMMON|RARE|LEGENDARY, color: 재킷 색,
//            owned, acquiredAt | null, progress: { current, goal, unit: DAY|HOUR|COUNT } }
// → { ownedCount, total, records: [레코드] }  (도감 순서: 희귀도 → 정한 순서)
export async function getRecords() {
  const response = await axiosInstance.get('/api/v1/records')
  return response.data.data
}
