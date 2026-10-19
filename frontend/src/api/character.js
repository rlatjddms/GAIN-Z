import axiosInstance from './axios'

// 옷장 → { outfits: [{ id, name, category, requiredLevel, imageUrl, owned, equipped }],
//         backgrounds: [{ key, requiredLevel, unlocked }], stageBackground }
export async function getWardrobe() {
  const response = await axiosInstance.get('/api/v1/characters/me/wardrobe')
  return response.data.data
}

// 옷장 저장: 입을 의상(보유한 것만), 무대 배경(해금한 것만, null이면 레벨에 맞춰 자동),
// 소품(자리마다 하나, null이면 안 씀. 레벨이 닿은 것만) → 갱신된 캐릭터
export async function updateWardrobe({ outfitId, stageBackground, hat, glasses, neck, shoes }) {
  const response = await axiosInstance.put('/api/v1/characters/me/wardrobe', { outfitId, stageBackground, hat, glasses, neck, shoes })
  return response.data.data
}

// 외형 수정 (닉네임·성별·피부·머리·악기)
export async function updateAppearance({ nickname, gender, skinColor, hairStyle, hairColor, instrumentType, instrumentColor }) {
  await axiosInstance.put('/api/v1/characters/me', { nickname, gender, skinColor, hairStyle, hairColor, instrumentType, instrumentColor })
}
