// 타브 붙여넣기·직접 쓰기 (#166): 코드 사이트의 글자 타브를 마디 데이터(noteData, docs/note-data-format.md)로 바꾼다
//
//   [Verse]                      → 다음 마디부터 구간 이름
//   C             G              → (타브 바로 위) 코드 줄: 같은 칸의 박에 코드
//   e|-------0-----|-------3-----|
//   B|-----1---1---|-----0---0---|   → 한 묶음 = 6줄(기타) 또는 4줄(베이스), 맨 위 줄이 1번 줄(가장 높은 음)
//   G|---0-------0-|---0-------0-|     마디선 | 사이가 한 마디, 마디 안 칸 위치로 박을 계산 (4/4로 보고 1/4박 단위로 맞춤)
//   D|-2-----------|-------------|     두 자리 숫자는 한 프렛(10~24), h p / \ b ~ x 는 주법
//
// 글자 타브에는 음 길이가 없어서 박은 칸 간격으로 짐작한다. 처음 보는 기호는 무시한다

const STRING_LINE = /^\s*([A-Ga-g][#b]?)?\s*[|:]?(.*[-|].*)$/
const BODY_CHARS = /^[-0-9|:hpbrx~/\\()<>.*\s^sSvV=]+$/
const SECTION = /^\s*\[([^\]]{1,30})\]\s*$/
const BEATS_PER_MEASURE = 4
const STEP = 0.25

// 한 줄이 타브 줄인지: (줄 이름) + | 뒤로 -, 숫자, 마디선 … 이고 - 가 여럿
function tabBody(line) {
  const m = STRING_LINE.exec(line)
  if (!m) return null
  const pipe = line.indexOf('|')
  if (pipe === -1) return null
  const body = line.slice(pipe + 1).replace(/\s+$/, '')
  if (!body || !BODY_CHARS.test(body) || (body.match(/-/g) || []).length < 3) return null
  return { name: (m[1] || '').trim(), body, offset: pipe + 1 }
}

const isChordToken = (t) => /^[A-G][#b]?(?:maj|min|dim|aug|sus|add|m|M|\+|°|ø|[0-9]|#|b|\(|\))*(?:\/[A-G][#b]?)?$/.test(t)

// 타브 묶음 바로 위의 코드 줄: [{ col, name }] (칸은 줄 맨 앞 기준)
function chordRow(line) {
  if (!line || !line.trim()) return null
  const tokens = [...line.matchAll(/\S+/g)]
  if (!tokens.length || !tokens.every((t) => isChordToken(t[0]) || /^\|+$/.test(t[0]))) return null
  return tokens.filter((t) => isChordToken(t[0])).map((t) => ({ col: t.index, name: t[0] }))
}

// 마디선 칸 위치 (묶음 안 모든 줄에서 같은 칸에 | 가 있는 곳). 맨 끝 | 도 포함
function barColumns(bodies) {
  const width = Math.max(...bodies.map((b) => b.length))
  const bars = []
  for (let c = 0; c < width; c++) {
    if (bodies.some((b) => b[c] === '|')) bars.push(c)
  }
  return { bars, width }
}

// 한 줄의 마디 조각에서 음 읽기: [{ col, fret, technique }]
function readLine(seg) {
  const notes = []
  let i = 0
  let pending = null // 앞 음과 이어지는 주법 (h p / \)
  while (i < seg.length) {
    const ch = seg[i]
    if (/[0-9]/.test(ch)) {
      let text = ch
      if (/[0-9]/.test(seg[i + 1] || '') && Number(ch + seg[i + 1]) <= 24) text += seg[i + 1]
      const note = { col: i, fret: Number(text) }
      if (pending) note.technique = pending
      pending = null
      const after = seg[i + text.length]
      i += text.length
      if (after === 'b') {
        // 7b9 처럼 벤드 뒤 숫자는 올릴 음이라 따로 치지 않는다
        note.technique = 'bend'
        i++
        while (/[0-9]/.test(seg[i] || '')) i++
      } else if (after === '~') note.technique = 'vibrato'
      notes.push(note)
      continue
    }
    if (ch === 'x' || ch === 'X') { notes.push({ col: i, fret: null, technique: 'mute' }); pending = null }
    else if (ch === 'h') pending = 'hammer'
    else if (ch === 'p') pending = 'pull'
    else if (ch === '/' || ch === '\\') pending = 'slide'
    else if (ch === '-') { /* 빈 칸 */ } else if (ch !== ' ') pending = pending || null
    i++
  }
  return notes
}

const toBeat = (col, width) => {
  if (width <= 0) return 0
  const beat = Math.round(((col / width) * BEATS_PER_MEASURE) / STEP) * STEP
  return Math.min(beat, BEATS_PER_MEASURE - STEP)
}

// 묶음 하나 → 마디들
function blockMeasures(lines, chords) {
  const bodies = lines.map((l) => l.body)
  const offset = lines[0].offset
  const { bars, width } = barColumns(bodies)
  const edges = [-1, ...bars.filter((c) => c < width), width]
  const measures = []
  for (let k = 0; k < edges.length - 1; k++) {
    const start = edges[k] + 1
    const end = edges[k + 1]
    const size = end - start
    if (size < 2) continue // || 같은 두 줄 마디선
    // 마디선 바로 뒤가 모든 줄에서 - 이면 그 한 칸은 여백으로 보고, 다음 칸을 첫 박으로 친다 (|-2---…)
    const segs = bodies.map((body) => body.slice(start, end).padEnd(size, '-'))
    const lead = segs.every((seg) => seg[0] === '-') ? 1 : 0
    const beatOf = (col) => toBeat(Math.max(0, col - lead), size - lead)
    const notes = []
    segs.forEach((seg, s) => {
      for (const n of readLine(seg)) {
        const note = { beat: beatOf(n.col), string: s + 1 }
        if (n.fret !== null) note.fret = n.fret
        if (n.technique) note.technique = n.technique
        notes.push(note)
      }
    })
    // 코드 줄은 줄 이름(e|) 칸까지 포함해 맞춰 쓰는 경우가 많아서, 타브 앞에 걸친 코드는 첫 칸으로 본다
    const measureChords = (chords || [])
      .map((c) => ({ col: Math.max(0, c.col - offset), name: c.name }))
      .filter((c) => c.col >= start && c.col < end)
      .map((c) => ({ beat: beatOf(c.col - start), name: c.name }))
    const data = {}
    if (measureChords.length) data.chords = measureChords
    if (notes.length) data.notes = notes.sort((a, b) => a.beat - b.beat || a.string - b.string)
    measures.push(data)
  }
  return measures
}

// 글자 타브 → { measures: [noteData], strings: 줄 수(4 또는 6, 섞이면 많은 쪽), noteCount }
export function importTabText(text) {
  const lines = (text || '').replace(/\r\n?/g, '\n').replace(/\t/g, '    ').split('\n')
  const measures = []
  let section = ''
  let strings = 0
  let i = 0
  while (i < lines.length) {
    const sec = SECTION.exec(lines[i])
    if (sec) { section = sec[1].trim(); i++; continue }
    const first = tabBody(lines[i])
    if (!first) { i++; continue }
    // 연달아 오는 타브 줄을 한 묶음으로 (최대 7줄)
    const block = [first]
    let j = i + 1
    while (j < lines.length && block.length < 7) {
      const next = tabBody(lines[j])
      if (!next) break
      block.push(next)
      j++
    }
    if (block.length >= 4) {
      const chords = chordRow(lines[i - 1])
      const got = blockMeasures(block.slice(0, block.length >= 6 ? 6 : 4), chords)
      if (got.length && section) { got[0].section = section; section = '' }
      measures.push(...got)
      strings = Math.max(strings, block.length >= 6 ? 6 : 4)
    }
    i = j
  }
  const noteCount = measures.reduce((sum, m) => sum + (m.notes ? m.notes.length : 0), 0)
  return { measures, strings, noteCount }
}

// 마디 데이터 → 렌더러(MeasureSheet)가 받는 TabData 모양 [{ measureNumber, noteData }]
export const toTabRows = (measures) => measures.map((m, k) => ({ measureNumber: k + 1, noteData: JSON.stringify(m) }))

// 글자 타브 악보 글 → 렌더러용 TabData 모양 (악보 보기에서 바로 그릴 때)
export const tabRowsFromText = (text) => toTabRows(importTabText(text).measures)
