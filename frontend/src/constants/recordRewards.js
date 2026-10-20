// 레코드 수집 보상 (#129). 백엔드 RoomDecor·DecorItem·StageBackground.LISTENING_ROOM과 같은 값
//   종류(희귀도)별로 레코드를 다 모으면 소품 두 개(플레이어 + 보관함), 12개 전부 모으면 리스닝 룸 무대
//   소품은 하나씩 무대에 올리고 옷장 미리보기에서 끌어서 자리를 정한다 (decorLayout: [{ key, x }])
// 그림: assets/decor/<KEY>.png (24×32, 바닥에 붙여 그림), assets/stages/listening.png
const decorFiles = import.meta.glob('../assets/decor/*.png', { eager: true, query: '?url', import: 'default' })
export const decorUrl = (key) => decorFiles[`../assets/decor/${key}.png`] || null

export const ROOM_DECOR_SETS = [
  { key: 'CASSETTE', rarity: 'COMMON', name: '카세트 세트', player: { key: 'CASSETTE_DECK', name: '카세트 데크' }, rack: { key: 'TAPE_RACK', name: '테이프 보관함' } },
  { key: 'CD', rarity: 'RARE', name: 'CD 세트', player: { key: 'CD_PLAYER', name: 'CD 플레이어' }, rack: { key: 'CD_RACK', name: 'CD 랙' } },
  { key: 'LP', rarity: 'LEGENDARY', name: 'LP 세트', player: { key: 'TURNTABLE', name: '턴테이블' }, rack: { key: 'LP_SHELF', name: 'LP 선반' } },
]

// 소품 하나하나. set = 어느 종류를 다 모으면 얻는지, defaultX = 처음 올릴 때 자리 (무대 도트 기준 왼쪽 끝, 캐릭터는 x92~115)
export const ROOM_DECOR_ITEMS = ROOM_DECOR_SETS.flatMap((s) => [
  { ...s.player, set: s.key, defaultX: 46 },
  { ...s.rack, set: s.key, defaultX: 138 },
])
export const decorItemOf = (key) => ROOM_DECOR_ITEMS.find((i) => i.key === key) || null
export const DECOR_MAX_X = 208 - 24

export const LISTENING_ROOM = { key: 'LISTENING_ROOM', name: '리스닝 룸' }
