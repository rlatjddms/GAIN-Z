import rookieStage from '../assets/stages/rookie.png'
import clubStage from '../assets/stages/club.png'
import concertStage from '../assets/stages/concert.png'
import legendStage from '../assets/stages/legend.png'

export const MAX_LEVEL = 50

// 백엔드 LevelPolicy와 동일한 구간별 필요 경험치
const EXP_TABLE = [
  { minLevel: 1, maxLevel: 9, requiredExp: 100 },
  { minLevel: 10, maxLevel: 29, requiredExp: 300 },
  { minLevel: 30, maxLevel: 49, requiredExp: 1000 },
  { minLevel: 50, maxLevel: 50, requiredExp: Infinity },
]

// 레벨 구간별 무대(배경) 및 캐릭터 연출 에셋
export const LEVEL_TIERS = [
  {
    key: 'ROOKIE',
    name: '방구석 락스타',
    minLevel: 1,
    maxLevel: 9,
    background: rookieStage,
    accent: '#2d5bff',
    accentSoft: '#f0f4ff',
    textColor: '#1a1a1a',
    scale: 1,
    aura: 'none',
    speech: '오늘도 달려볼까?',
  },
  {
    key: 'RISING',
    name: '동네 클럽 신예',
    minLevel: 10,
    maxLevel: 29,
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
    name: '투어 뮤지션',
    minLevel: 30,
    maxLevel: 49,
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
    name: '전설의 락스타',
    minLevel: 50,
    maxLevel: 50,
    background: legendStage,
    accent: '#f59e0b',
    accentSoft: '#fff8e1',
    textColor: '#ffffff',
    scale: 1.3,
    aura: 'legend',
    speech: '이제 전설은 내 이름으로 쓰인다.',
  },
]

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
  const row = EXP_TABLE.find((r) => safeLevel >= r.minLevel && safeLevel <= r.maxLevel)
  return row ? row.requiredExp : 100
}

export function getExpProgress(level, exp) {
  const required = getRequiredExp(level)
  if (!Number.isFinite(required)) return 100
  const safeExp = Math.max(0, Number(exp) || 0)
  return Math.min(100, Math.round((safeExp / required) * 100))
}
