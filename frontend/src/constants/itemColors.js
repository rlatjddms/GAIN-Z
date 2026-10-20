// 아이템 색 바꾸기 (#120). 아이템마다 색을 바꿀 부분의 원래 색과 추천 색
//   colors: 그림에서 색이 바뀌는 원래 색들. 첫 번째가 바탕색이고, 나머지(그림자·선·밝은 부분)는 바탕색과의 밝기 차이를 그대로 유지한다
//   key: true → 그림을 의상 기준색(노랑 계열)으로 칠한 아이템. 고른 색이 의상색(outfitColor)이 된다
//   presets: 추천 색. 첫 번째가 기본 색 (colors[0]과 같으면 원래 그림 그대로)
// 표에 없는 아이템(왕관 등)은 색이 고정. 그림 색을 바꾸면 colors도 같이 바꾼다
// 키: 상의는 스프라이트 키(HOODIE), 나머지는 백엔드 Accessory 값
export const ITEM_COLORS = {
  // 상의
  DEFAULT: { key: true, presets: ['#FFFFFF', '#343A40', '#A5D8FF'] },
  CROP: { key: true, presets: ['#FFFFFF', '#FFC9C9', '#212529'] },
  HOODIE: { colors: ['#adb5bd', '#868e96', '#5c636a', '#9aa1a8'], presets: ['#ADB5BD', '#FFEC99', '#B2F2BB'] },
  BAND_TEE: { colors: ['#212529', '#141518', '#0b0c0e'], presets: ['#212529', '#C92A2A', '#1C2A5A'] },
  DENIM_JACKET: { colors: ['#228be6', '#1971c2', '#0b4f8a', '#74c0fc', '#145a9c'], presets: ['#228BE6', '#4263EB', '#495057'] },
  LEATHER_JACKET: { colors: ['#25262b', '#141518', '#0b0c0e'], presets: ['#25262B', '#5C3A1E', '#7D1A1A'] },
  // 무대 정장은 그림(보라)과 달리 아이보리가 기본 색
  STAGE_SUIT: { colors: ['#6741d9', '#4c2a9e', '#2b1866'], presets: ['#F3EAD3', '#1C2A5A', '#212529'] },
  GOLD_TRIM_JACKET: { colors: ['#25262b', '#141518', '#0b0c0e'], presets: ['#25262B', '#1C2A5A', '#7D1A1A'] },
  // 하의
  JEANS: { colors: ['#3f6db5', '#2f5490', '#203f6e'], presets: ['#3F6DB5', '#212529', '#ADB5BD'] },
  JOGGERS: { colors: ['#adb5bd', '#868e96', '#5c636a'], presets: ['#ADB5BD', '#212529', '#1C2A5A'] },
  SKIRT: { colors: ['#495057', '#343a40'], presets: ['#495057', '#C92A2A', '#F783AC'] },
  RIPPED_JEANS: { colors: ['#343a40', '#25262b', '#141518'], presets: ['#343A40', '#3F6DB5', '#ADB5BD'] },
  CARGO_PANTS: { colors: ['#8f8a5c', '#77724a', '#4d4a2e'], presets: ['#8F8A5C', '#212529', '#5C3A1E'] },
  LEATHER_PANTS: { colors: ['#25262b', '#141518', '#0b0c0e', '#5c636a'], presets: ['#25262B', '#5C3A1E', '#7D1A1A'] },
  SUIT_PANTS: { colors: ['#6741d9', '#4c2a9e', '#2b1866'], presets: ['#F3EAD3', '#1C2A5A', '#212529'] },
  GOLD_STRIPE_PANTS: { colors: ['#25262b', '#0b0c0e'], presets: ['#25262B', '#1C2A5A', '#7D1A1A'] },
  // 모자
  BEANIE: { colors: ['#bdc2c4', '#adb5bd', '#9b9ba3', '#caccca'], presets: ['#BDC2C4', '#E03131', '#FCC419'] },
  CAP: { colors: ['#1971c2', '#145a9c', '#0b3d6e'], presets: ['#1971C2', '#E03131', '#212529'] },
  // 안경
  ROUND_GLASSES: { colors: ['#343a40'], presets: ['#343A40', '#C08A52', '#E03131'] },
  SUNGLASSES: { colors: ['#212529', '#141518', '#5c636a'], presets: ['#212529', '#C92A2A', '#1971C2'] },
  STAR_GLASSES: { colors: ['#fcc419', '#e67700'], presets: ['#FCC419', '#F06595', '#4DABF7'] },
  HEART_GLASSES: { colors: ['#f06595', '#a61e4d', '#fcc2d7'], presets: ['#F06595', '#E03131', '#845EF7'] },
  // 귀걸이
  STUD_EARRINGS: { colors: ['#f8f9fa', '#adb5bd'], presets: ['#F8F9FA', '#FCC419', '#E03131'] },
  HOOP_EARRINGS: { colors: ['#fcc419', '#e67700'], presets: ['#FCC419', '#CED4DA', '#F06595'] },
  STAR_EARRINGS: { colors: ['#fcc419', '#fff3bf'], presets: ['#FCC419', '#CED4DA', '#4DABF7'] },
  // 목·허리
  HEADPHONES: { colors: ['#e03131'], presets: ['#E03131', '#1971C2', '#F8F9FA'] },
  CHAIN: { colors: ['#fcc419', '#e67700'], presets: ['#FCC419', '#CED4DA'] },
  BELT: { colors: ['#2b2d31'], presets: ['#2B2D31', '#5C3A1E', '#C92A2A'] },
  // 신발
  BASIC_SHOES: { colors: ['#2b2d31', '#141518'], presets: ['#2B2D31', '#5C3A1E', '#F8F9FA'] },
  SNEAKERS: { colors: ['#e03131'], presets: ['#E03131', '#1971C2', '#2F9E44'] },
  HIGH_TOPS: { colors: ['#e03131', '#7d1a1a'], presets: ['#E03131', '#212529', '#1971C2'] },
  COMBAT_BOOTS: { colors: ['#343a40', '#141518'], presets: ['#343A40', '#5C3A1E', '#7D1A1A'] },
}

// 상의 스프라이트 이름(HOODIE_MALE)이나 소품 키 → 색표 키
export const itemKeyOf = (name) => (name ? String(name).replace(/_(MALE|FEMALE)$/, '') : null)

export const isColorable = (key) => !!ITEM_COLORS[key]

// 고른 색 (고른 적 없으면 기본 색)
export const itemColorOf = (key, itemColors) => (ITEM_COLORS[key] ? itemColors?.[key] || ITEM_COLORS[key].presets[0] : null)
