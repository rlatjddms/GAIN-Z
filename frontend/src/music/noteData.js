// 마디 악보(TabData.noteData) 읽기·키 바꾸기. 포맷은 docs/note-data-format.md
//   readMeasures(tabs)            → 그리기 좋은 마디 목록 (박자 이어받기, 잘못된 값 걸러내기)
//   transposeMeasures(ms, ctx)    → 코드 이름·프렛을 반음만큼 옮긴 마디 목록
//   linkTechniques(ms)            → 해머링·풀링·슬라이드 표시에 쓸 앞 음 정보 붙이기

import { transposeChord } from './transpose'

// 타브를 그리는 악기: 줄 이름 (1번 줄 = 가장 높은 음)
export const TAB_TUNINGS = {
  GUITAR: ['E', 'B', 'G', 'D', 'A', 'E'],
  BASS: ['G', 'D', 'A', 'E'],
}

export const TECHNIQUES = ['hammer', 'pull', 'slide', 'bend', 'vibrato', 'mute', 'harmonic']

const MAX_FRET = 24
const DEFAULT_TIME = { num: 4, den: 4 }
const TIME_PATTERN = /^(\d{1,2})\/(2|4|8|16)$/

const isNum = (v) => typeof v === 'number' && Number.isFinite(v)
// 박은 소수 셋째 자리까지 (셋잇단 0.333 …)
const roundBeat = (v) => Math.round(v * 1000) / 1000

function parseTime(value) {
  const m = typeof value === 'string' ? TIME_PATTERN.exec(value.trim()) : null
  if (!m || Number(m[1]) < 1) return null
  return { num: Number(m[1]), den: Number(m[2]) }
}

// 마디 길이 (4분음표 = 1박)
export const measureLength = (time) => (time.num * 4) / time.den

function readChords(list, length) {
  if (!Array.isArray(list)) return []
  return list
    .filter((c) => c && isNum(c.beat) && c.beat >= 0 && c.beat < length && typeof c.name === 'string' && c.name.trim())
    .map((c) => ({ beat: roundBeat(c.beat), name: c.name.trim().slice(0, 12) }))
    .sort((a, b) => a.beat - b.beat)
}

function readNotes(list, length) {
  if (!Array.isArray(list)) return []
  return list
    .filter((n) => n && isNum(n.beat) && n.beat >= 0 && n.beat < length && Number.isInteger(n.string) && n.string >= 1)
    .map((n) => {
      const technique = TECHNIQUES.includes(n.technique) ? n.technique : null
      const fret = Number.isInteger(n.fret) && n.fret >= 0 && n.fret <= MAX_FRET ? n.fret : null
      return { beat: roundBeat(n.beat), duration: isNum(n.duration) && n.duration > 0 ? n.duration : null, string: n.string, fret, technique }
    })
    // 프렛이 없으면 뮤트만 그릴 수 있다
    .filter((n) => n.fret !== null || n.technique === 'mute')
    .sort((a, b) => a.beat - b.beat || a.string - b.string)
}

function parseJson(text) {
  if (typeof text !== 'string') return null
  try {
    const value = JSON.parse(text)
    return value && typeof value === 'object' && !Array.isArray(value) ? value : null
  } catch {
    return null
  }
}

// tabs: 백엔드 TabView 목록 [{ measureNumber, noteData, recommendedFingering }]
export function readMeasures(tabs) {
  if (!Array.isArray(tabs)) return []
  let time = DEFAULT_TIME
  return [...tabs]
    .sort((a, b) => a.measureNumber - b.measureNumber)
    .map((tab) => {
      const data = parseJson(tab.noteData)
      const changed = data && parseTime(data.timeSignature)
      const timeChanged = Boolean(changed) && (changed.num !== time.num || changed.den !== time.den)
      if (changed) time = changed
      const length = measureLength(time)
      return {
        number: tab.measureNumber,
        time,
        timeChanged,
        length,
        section: data && typeof data.section === 'string' ? data.section.trim().slice(0, 40) : '',
        chords: data ? readChords(data.chords, length) : [],
        notes: data ? readNotes(data.notes, length) : [],
        fingering: (tab.recommendedFingering || '').trim(),
        invalid: !data,
      }
    })
}

// 프렛을 반음만큼 옮긴다. 0 아래면 한 옥타브 위로, 24 위면 한 옥타브 아래로
export function shiftFret(fret, semitones) {
  if (fret === null) return null
  let f = fret + semitones
  while (f < 0) f += 12
  while (f > MAX_FRET) f -= 12
  return f
}

// 프렛을 옮길 반음 수: 운지 모양이 깨지지 않게 악보 전체를 같은 만큼 옮긴다.
// semitones, semitones±12 중 모든 음이 0~24 프렛에 들어가는 가장 작은 이동. 없으면 semitones (음마다 옥타브 조정)
export function fretShiftFor(measures, semitones) {
  const frets = measures.flatMap((m) => m.notes.map((n) => n.fret)).filter((f) => f !== null)
  if (!frets.length) return semitones
  const min = Math.min(...frets)
  const max = Math.max(...frets)
  const fits = (s) => min + s >= 0 && max + s <= MAX_FRET
  return [semitones, semitones - 12, semitones + 12]
    .sort((a, b) => Math.abs(a) - Math.abs(b))
    .find(fits) ?? semitones
}

// ctx: transpose.js의 transposeContext 결과 { semitones, preferFlat }
// 운지 안내(fingering)는 원래 키 기준이라 키를 옮기면 숨긴다
export function transposeMeasures(measures, ctx) {
  if (!ctx?.semitones) return measures
  const { semitones, preferFlat } = ctx
  const fretShift = fretShiftFor(measures, semitones)
  return measures.map((m) => ({
    ...m,
    chords: m.chords.map((c) => ({ ...c, name: transposeChord(c.name, semitones, preferFlat) })),
    notes: m.notes.map((n) => ({ ...n, fret: shiftFret(n.fret, fretShift) })),
    fingering: '',
  }))
}

// 해머링·풀링·슬라이드: 같은 줄 바로 앞 음(앞 마디 포함)의 프렛을 fromFret으로 붙인다
export function linkTechniques(measures) {
  const lastFret = {}
  return measures.map((m) => ({
    ...m,
    notes: m.notes.map((n) => {
      const fromFret = lastFret[n.string] ?? null
      if (n.fret !== null) lastFret[n.string] = n.fret
      return ['hammer', 'pull', 'slide'].includes(n.technique) ? { ...n, fromFret } : n
    }),
  }))
}

// 주법 표시 글자. before: 숫자 앞 (앞 음과 사이), after: 숫자 뒤
export function techniqueMark(note) {
  switch (note.technique) {
    case 'hammer': return { before: 'h' }
    case 'pull': return { before: 'p' }
    case 'slide': return { before: note.fromFret !== null && note.fret < note.fromFret ? '\\' : '/' }
    case 'bend': return { after: 'b' }
    case 'vibrato': return { after: '~' }
    default: return {}
  }
}

// 타브 숫자 글자: 뮤트는 x, 하모닉스는 <12>
export function fretLabel(note) {
  if (note.technique === 'mute') return 'x'
  if (note.technique === 'harmonic') return `<${note.fret}>`
  return String(note.fret)
}

// 따라가기 커서: 경과한 박 수 → { index(마디), beat(마디 안 박) }. 끝나면 null
export function locateBeat(measures, elapsed) {
  let start = 0
  for (let i = 0; i < measures.length; i += 1) {
    const end = start + measures[i].length
    if (elapsed < end) return { index: i, beat: elapsed - start }
    start = end
  }
  return null
}
