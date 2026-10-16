// 직접 쓰는 코드 악보 읽기.
//   # 1절            → 구간 이름
//   [C]이 밤 [G]그날의  → 코드는 대괄호, 바로 뒤 가사 위에 표시
//   (빈 줄)           → 줄 띄움
// → [{ type: 'section', text } | { type: 'blank' } | { type: 'line', segments: [{ chord, lyric }] }]

const CHORD_TOKEN = /\[([^\]\n]{1,12})\]/g

export function parseChordSheet(text) {
  if (!text) return []
  return text.replace(/\r\n?/g, '\n').split('\n').map(parseLine)
}

function parseLine(raw) {
  const line = raw.replace(/\s+$/, '')
  if (!line.trim()) return { type: 'blank' }
  if (line.trimStart().startsWith('#')) return { type: 'section', text: line.trimStart().replace(/^#+\s*/, '') }

  const segments = []
  let last = 0
  let chord = null
  for (const m of line.matchAll(CHORD_TOKEN)) {
    const lyric = line.slice(last, m.index)
    if (chord !== null || lyric) segments.push({ chord, lyric })
    chord = m[1].trim()
    last = m.index + m[0].length
  }
  segments.push({ chord, lyric: line.slice(last) })
  return { type: 'line', segments: segments.filter((s) => s.chord !== null || s.lyric) }
}

// 악보에 쓰인 코드 개수 (목록 요약용)
export function countChords(text) {
  return text ? [...text.matchAll(CHORD_TOKEN)].length : 0
}
