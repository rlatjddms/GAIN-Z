import rookieStage from '../assets/stages/rookie.png'
import clubStage from '../assets/stages/club.png'
import concertStage from '../assets/stages/concert.png'
import legendStage from '../assets/stages/legend.png'
import listeningStage from '../assets/stages/listening.png'

// 백엔드 LevelPolicy와 같은 규칙: 최대 Lv.30, 다음 레벨까지 필요 경험치 = 50 × 지금 레벨 + 20
// 한쪽을 바꾸면 같이 바꾼다
export const MAX_LEVEL = 30

// 레벨 구간별 무대(배경) 및 캐릭터 연출 에셋
export const LEVEL_TIERS = [
  {
    key: 'ROOKIE',
    floorY: 58,   // 무대 배경(208×72)에서 캐릭터 발이 닿는 줄
    minLevel: 1,
    maxLevel: 9,
    background: rookieStage,
    accent: 'var(--c-primary)',
    accentSoft: 'var(--c-primary-soft)',
    textColor: '#1a1a1a',
    scale: 1,
    aura: 'none',
    speech: '오늘도 달려볼까?',
  },
  {
    key: 'RISING',
    floorY: 54,   // 무대 배경(208×72)에서 캐릭터 발이 닿는 줄
    minLevel: 10,
    maxLevel: 19,
    background: clubStage,
    accent: '#e8590c',
    accentSoft: '#fff4e6',
    textColor: '#ffffff',
    scale: 1.1,
    aura: 'soft',
    speech: '오늘 공연, 관객이 꽤 모였어!',
  },
  {
    key: 'PRO',
    floorY: 54,   // 무대 배경(208×72)에서 캐릭터 발이 닿는 줄
    minLevel: 20,
    maxLevel: 29,
    background: concertStage,
    accent: '#7c3aed',
    accentSoft: '#f3f0ff',
    textColor: '#ffffff',
    scale: 1.2,
    aura: 'glow',
    speech: '다음 도시로 떠날 준비 됐어?',
  },
  {
    key: 'LEGEND',
    floorY: 54,   // 무대 배경(208×72)에서 캐릭터 발이 닿는 줄
    minLevel: 30,
    maxLevel: 30,
    background: legendStage,
    accent: '#f59e0b',
    accentSoft: '#fff8e1',
    textColor: '#ffffff',
    scale: 1.3,
    aura: 'legend',
    speech: '이제 전설은 내 이름으로 쓰인다.',
  },
]

// 고를 수 있는 무대 전체: 레벨 무대 + 레코드를 모두 모으면 얻는 리스닝 룸 (#129)
export const STAGE_BACKGROUNDS = [
  ...LEVEL_TIERS,
  {
    key: 'LISTENING_ROOM',
    floorY: 58,
    background: listeningStage,
    accent: '#ae3ec9',
    accentSoft: '#f8f0fc',
    textColor: '#ffffff',
    scale: 1,
    aura: 'none',
    speech: '오늘은 어떤 판을 걸어 볼까?',
  },
]

export const stageOf = (key) => STAGE_BACKGROUNDS.find((t) => t.key === key) || null

export function clampLevel(level) {
  const parsed = Number(level)
  if (!Number.isFinite(parsed) || parsed < 1) return 1
  return Math.min(Math.floor(parsed), MAX_LEVEL)
}

export function getLevelTier(level) {
  const safeLevel = clampLevel(level)
  return LEVEL_TIERS.find((tier) => safeLevel >= tier.minLevel && safeLevel <= tier.maxLevel) || LEVEL_TIERS[0]
}

export function getNextTier(level) {
  const current = getLevelTier(level)
  const index = LEVEL_TIERS.indexOf(current)
  return LEVEL_TIERS[index + 1] || null
}

export function getRequiredExp(level) {
  const safeLevel = clampLevel(level)
  return safeLevel >= MAX_LEVEL ? Infinity : 50 * safeLevel + 20
}

export function getExpProgress(level, exp) {
  const required = getRequiredExp(level)
  if (!Number.isFinite(required)) return 100
  const safeExp = Math.max(0, Number(exp) || 0)
  return Math.min(100, Math.round((safeExp / required) * 100))
}
