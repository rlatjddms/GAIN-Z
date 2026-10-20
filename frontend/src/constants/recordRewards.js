// 레코드 수집 보상 (#129). 백엔드 RoomDecor·StageBackground.LISTENING_ROOM과 같은 값
//   종류(희귀도)별로 레코드를 다 모으면 방 꾸미기 세트(플레이어 + 보관함), 12개 전부 모으면 리스닝 룸 무대
// 그림: assets/decor/<KEY>.png (24×32, 바닥에 붙여 그림), assets/stages/listening.png
const decorFiles = import.meta.glob('../assets/decor/*.png', { eager: true, query: '?url', import: 'default' })
export const decorUrl = (key) => decorFiles[`../assets/decor/${key}.png`] || null

export const ROOM_DECOR_SETS = [
  { key: 'CASSETTE', rarity: 'COMMON', name: '카세트 세트', player: { key: 'CASSETTE_DECK', name: '카세트 데크' }, rack: { key: 'TAPE_RACK', name: '테이프 보관함' } },
  { key: 'CD', rarity: 'RARE', name: 'CD 세트', player: { key: 'CD_PLAYER', name: 'CD 플레이어' }, rack: { key: 'CD_RACK', name: 'CD 랙' } },
  { key: 'LP', rarity: 'LEGENDARY', name: 'LP 세트', player: { key: 'TURNTABLE', name: '턴테이블' }, rack: { key: 'LP_SHELF', name: 'LP 선반' } },
]

export const decorSetOf = (key) => ROOM_DECOR_SETS.find((s) => s.key === key) || null

// 무대 위 자리 (무대 도트 기준 x, 그림 왼쪽 끝). 캐릭터는 가운데 x92~115
export const DECOR_SLOTS = { player: 46, rack: 138 }

export const LISTENING_ROOM = { key: 'LISTENING_ROOM', name: '리스닝 룸' }
