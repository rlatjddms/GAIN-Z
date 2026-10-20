// 캐릭터 외형 선택지 (캐릭터 만들기·옷장에서 함께 쓴다). value는 백엔드 enum 값과 같다
import { SKIN_PRESETS } from '../pixel/palette'

export const GENDER_OPTIONS = [
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
]

export const HAIR_STYLE_OPTIONS = [
  { value: 'BALD', label: '대머리', emoji: '🧑‍🦲' },
  { value: 'SHORT_CUT', label: '숏컷', emoji: '👦' },
  { value: 'MID_LENGTH_BOB', label: '보브컷', emoji: '👩' },
  { value: 'LONG_STRAIGHT', label: '생머리', emoji: '👱‍♀️' },
  { value: 'NATURAL_WAVE', label: '웨이브', emoji: '👩‍🦱' },
  { value: 'WOLF_CUT', label: '울프컷', emoji: '🐺' },
  { value: 'SIDE_PART_QUIFF', label: '포마드', emoji: '🧔' },
  { value: 'MESSY_ROUGH', label: '헝클어짐', emoji: '🦁' },
  { value: 'DREADLOCKS', label: '드레드락', emoji: '🦱' },
  { value: 'MOHAWK_SHARP', label: '모히칸', emoji: '🐔' },
  // 레벨 보상 (constants/rewards.js의 REWARD_HAIR_STYLES와 같은 레벨)
  { value: 'PONYTAIL', label: '포니테일', emoji: '🎀', level: 4 },
  { value: 'SPIKY', label: '스파이크', emoji: '⚡', level: 11 },
  { value: 'BUN', label: '번 헤어', emoji: '🍡', level: 17 },
  { value: 'TWIN_TAILS', label: '트윈테일', emoji: '🎎', level: 23 },
  { value: 'AFRO', label: '아프로', emoji: '🌳', level: 29 },
]

// 처음부터 고를 수 있는 머리 (캐릭터 만들기)
export const STARTER_HAIR_STYLE_OPTIONS = HAIR_STYLE_OPTIONS.filter((h) => !h.level)

export const SKIN_COLOR_PRESETS = SKIN_PRESETS

export const HAIR_COLOR_PRESETS = [
  '#111827', // 제트 블랙
  '#4B5563', // 애쉬 그레이
  '#D4B275', // 허니 블론드
  '#B85C5C', // 오번 레드
  '#7C3AED', // 네온 퍼플
  '#EC4899', // 코튼 핑크
  '#3B82F6', // 코발트 블루
]

// thumb: 고르기 썸네일 크기 비율 (세로로 긴 마이크는 꽉 채우면 커 보여서 작게)
export const INSTRUMENT_OPTIONS = [
  { value: 'GUITAR', label: '일렉기타', emoji: '🎸' },
  { value: 'BASS', label: '베이스', emoji: '🎸' },
  { value: 'DRUM', label: '드럼', emoji: '🥁' },
  { value: 'VOCAL', label: '보컬', emoji: '🎤', thumb: 0.72 },
  { value: 'KEYBOARD', label: '건반', emoji: '🎹' },
]

export const INSTRUMENT_COLOR_PRESETS = [
  '#B91C1C', // 체리 레드
  '#1D4ED8', // 메탈릭 블루
  '#D97706', // 선버스트 오렌지
  '#111827', // 매트 블랙
  '#F3F4F6', // 빈티지 화이트
  '#059669', // 에메랄드 그린
  '#EC4899', // 캔디 핑크
]

// 닉네임 규칙 (백엔드 CharacterUpdateRequest와 같게 2~10자, 공백·특수문자 없이). 문제가 없으면 ''
export function nicknameProblem(value) {
  const v = (value || '').trim()
  if (!v) return '닉네임은 필수 입력 항목입니다.'
  if (v.length < 2 || v.length > 10) return '닉네임은 2자 이상 10자 이하로 입력해주세요.'
  if (!/^[a-zA-Z0-9가-힣]+$/.test(v)) return '특수문자 및 공백은 포함될 수 없습니다.'
  return ''
}
