import axiosInstance from './axios'

// 옷장 → { outfits: [{ id, name, category, requiredLevel, imageUrl, owned, equipped }],
//         backgrounds: [{ key, requiredLevel, unlocked }], stageBackground }
export async function getWardrobe() {
  const response = await axiosInstance.get('/api/v1/characters/me/wardrobe')
  return response.data.data
}

// 옷장 저장: 입을 의상(보유한 것만), 무대 배경(해금한 것만, null이면 레벨에 맞춰 자동),
// 하의·소품(자리마다 하나)·무대 효과 (null이면 안 씀. 레벨이 닿은 것만),
// itemColors: 아이템마다 고른 색 { HOODIE: '#ffd8a8' } (기본 색인 아이템은 빼고 보냄) → 갱신된 캐릭터
export async function updateWardrobe({ outfitId, stageBackground, stageEffect, bottom, hat, glasses, earrings, neck, waist, shoes, itemColors }) {
  const response = await axiosInstance.put('/api/v1/characters/me/wardrobe', { outfitId, stageBackground, stageEffect, bottom, hat, glasses, earrings, neck, waist, shoes, itemColors })
  return response.data.data
}

// 외형 수정 (닉네임·성별·피부·머리·악기)
export async function updateAppearance({ nickname, gender, skinColor, hairStyle, hairColor, instrumentType, instrumentColor }) {
  await axiosInstance.put('/api/v1/characters/me', { nickname, gender, skinColor, hairStyle, hairColor, instrumentType, instrumentColor })
}
