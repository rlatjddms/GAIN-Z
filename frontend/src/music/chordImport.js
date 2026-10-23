// 코드 붙여넣기 (#157): 코드 사이트에서 복사한 "가사 위에 코드" 글을 코드 악보 형식(chordSheet.js)으로 바꾼다
//   [Verse 1] / Intro: / 후렴        → # 구간 이름
//   C        G         Am            (코드만 있는 줄)
//   이 밤 그날의 반딧불을          → [C]이 밤 그[G]날의 반딧불[Am]을   (코드 위치 = 아래 가사의 같은 칸)
//   코드만 있고 아래 가사가 없는 줄 → [C] [G] [Am]
//   이미 [C]가사 형식인 줄, 가사만 있는 줄은 그대로
// 칸 위치는 고정폭 글꼴 기준이라 한글·한자는 두 칸으로 센다

// 코드 한 개: 으뜸음 + 꾸밈(m, maj7, sus4, add9, dim, aug, 7, b5 …) + 분수 코드(/B)
const CHORD = /^[A-G][#b]?(?:maj|min|dim|aug|sus|add|m|M|\+|-|°|ø|[0-9]|#|b|\(|\))*(?:\/[A-G][#b]?)?$/
// 코드 줄에 같이 있어도 되는 것: 마디선, 반복 표시, 코드 없음
const FILLER = /^(?:\|+|:?\|\|?:?|x\d+|\(?\d+x\)?|N\.?C\.?|-+|\.+|\/|%)$/i
const SECTION_NAMES = /^(?:intro|verse|pre-?chorus|chorus|bridge|interlude|solo|outro|hook|refrain|instrumental|ending|coda|전주|간주|후주|후렴|브릿지|도입|\d+\s*절|[0-9]+\s*verse)\b/i

export const isChord = (token) => CHORD.test(token)

// 한 글자가 고정폭에서 차지하는 칸 (한글·한자·전각 = 2)
const cellWidth = (ch) => (/[ᄀ-ᇿ⺀-꓏가-힯豈-﫿︰-﹏＀-｠￠-￦]/.test(ch) ? 2 : 1)

// 코드만 있는 줄이면 [{ col, chord }], 아니면 null
export function chordLine(line) {
  const tokens = [...line.matchAll(/\S+/g)]
  if (!tokens.length) return null
  const chords = []
  for (const t of tokens) {
    if (isChord(t[0])) chords.push({ col: displayCol(line, t.index), chord: t[0] })
    else if (!FILLER.test(t[0])) return null
  }
  return chords.length ? chords : null
}

const displayCol = (line, index) => [...line.slice(0, index)].reduce((sum, ch) => sum + cellWidth(ch), 0)

// 구간 이름 줄이면 이름, 아니면 null
export function sectionName(line) {
  const t = line.trim()
  const bracket = /^\[([^\]]{1,30})\]$/.exec(t)
  if (bracket && !isChord(bracket[1].trim())) return bracket[1].trim()
  const named = /^(.{1,30}?):?$/.exec(t)
  if (named && SECTION_NAMES.test(t) && !/\[/.test(t) && t.length <= 30) return named[1].trim()
  return null
}

// 코드들을 가사의 같은 칸에 끼워 넣기 (뒤에서부터 넣어야 앞 위치가 안 밀림)
function merge(chords, lyric) {
  const chars = [...lyric]
  const starts = []
  let col = 0
  for (const ch of chars) { starts.push(col); col += cellWidth(ch) }
  const total = col
  let out = chars.slice()
  for (const { col: c, chord } of [...chords].reverse()) {
    let i = starts.findIndex((s) => s >= c)
    if (i === -1) {
      // 가사보다 오른쪽에 있는 코드: 가사 끝을 공백으로 늘려서 붙임
      const pad = Math.max(0, c - total)
      out = out.concat(Array(pad).fill(' '))
      i = out.length
      starts.push(c)
    }
    out.splice(i, 0, `[${chord}]`)
  }
  return out.join('').replace(/\s+$/, '')
}

// 붙여 넣은 글 → 코드 악보 글. 빈 줄은 연달아 하나만 남김
export function importChordText(text) {
  const lines = (text || '').replace(/\r\n?/g, '\n').replace(/\t/g, '    ').split('\n').map((l) => l.replace(/\s+$/, ''))
  const out = []
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (!line.trim()) {
      if (out.length && out[out.length - 1] !== '') out.push('')
      continue
    }
    const section = sectionName(line)
    if (section) { out.push(`# ${section}`); continue }
    const chords = chordLine(line)
    if (chords) {
      const next = lines[i + 1]
      if (next && next.trim() && !chordLine(next) && !sectionName(next) && !/\[[^\]]+\]/.test(next)) {
        out.push(merge(chords, next))
        i++
      } else {
        out.push(chords.map((c) => `[${c.chord}]`).join(' '))
      }
      continue
    }
    out.push(line)
  }
  while (out.length && out[out.length - 1] === '') out.pop()
  return out.join('\n')
}
