// 키 변조(transpose): 코드 이름과 키 이름을 반음 단위로 옮긴다.
// 키 형식은 백엔드 SheetRequest.KEY_PATTERN과 같다 (으뜸음 A~G + #/b + 단조 m)

const SHARP_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
const FLAT_NAMES = ['C', 'Db', 'D', 'Eb', 'E', 'F', 'Gb', 'G', 'Ab', 'A', 'Bb', 'B']
const NOTE_INDEX = {
  C: 0, 'C#': 1, Db: 1, D: 2, 'D#': 3, Eb: 3, E: 4, Fb: 4, 'E#': 5, F: 5, 'F#': 6, Gb: 6,
  G: 7, 'G#': 8, Ab: 8, A: 9, 'A#': 10, Bb: 10, B: 11, Cb: 11, 'B#': 0,
}

// 키 고르기 목록 (자주 쓰는 표기로)
export const MAJOR_KEYS = ['C', 'Db', 'D', 'Eb', 'E', 'F', 'F#', 'G', 'Ab', 'A', 'Bb', 'B']
export const MINOR_KEYS = ['Cm', 'C#m', 'Dm', 'Ebm', 'Em', 'Fm', 'F#m', 'Gm', 'G#m', 'Am', 'Bbm', 'Bm']

// 이 키들로 옮기면 코드도 b(플랫)으로 적는다
const FLAT_KEYS = new Set(['F', 'Bb', 'Eb', 'Ab', 'Db', 'Gb', 'Dm', 'Gm', 'Cm', 'Fm', 'Bbm', 'Ebm'])

export const KEY_PATTERN = /^[A-G][#b]?m?$/

const mod12 = (n) => ((n % 12) + 12) % 12

export function parseKey(key) {
  if (!key || !KEY_PATTERN.test(key)) return null
  const minor = key.endsWith('m')
  const root = minor ? key.slice(0, -1) : key
  return { root: NOTE_INDEX[root], minor }
}

// from 키에서 to 키까지 반음 수 (-5 ~ +6, 가까운 쪽)
export function semitonesBetween(from, to) {
  const a = parseKey(from)
  const b = parseKey(to)
  if (!a || !b) return 0
  const d = mod12(b.root - a.root)
  return d > 6 ? d - 12 : d
}

// 키를 반음만큼 옮긴 키 이름 (장조·단조는 그대로)
export function transposeKey(key, semitones) {
  const k = parseKey(key)
  if (!k) return key
  const list = k.minor ? MINOR_KEYS : MAJOR_KEYS
  return list[mod12(k.root + semitones)]
}

const noteName = (index, preferFlat) => (preferFlat ? FLAT_NAMES : SHARP_NAMES)[mod12(index)]

// 코드 한 개 (C, F#m7, Bbmaj7, D/F#, Asus4 …). 코드로 읽히지 않으면 그대로 돌려준다 (N.C. 등)
const CHORD_PATTERN = /^([A-G])([#b]?)([^/]*)(?:\/([A-G])([#b]?))?$/

export function transposeChord(chord, semitones, preferFlat = false) {
  if (!semitones) return chord
  const m = CHORD_PATTERN.exec(chord)
  if (!m) return chord
  const [, root, acc, quality, bass, bassAcc] = m
  const newRoot = noteName(NOTE_INDEX[root + acc] + semitones, preferFlat)
  const newBass = bass ? `/${noteName(NOTE_INDEX[bass + bassAcc] + semitones, preferFlat)}` : ''
  return `${newRoot}${quality}${newBass}`
}

// 원래 키 → 보는 키로 옮길 때 쓸 반음 수와 표기
export function transposeContext(originalKey, viewKey) {
  return {
    semitones: semitonesBetween(originalKey, viewKey),
    preferFlat: FLAT_KEYS.has(viewKey),
  }
}

// 기타 카포 위치 안내: 보는 키를 원래 키 운지로 치려면 몇 번 프렛에 끼우면 되는지 (1~7만)
export function capoFor(originalKey, viewKey) {
  const d = mod12(semitonesBetween(originalKey, viewKey))
  return d >= 1 && d <= 7 ? d : null
}
