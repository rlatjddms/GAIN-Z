// 개발용 마디 악보 예시 (/dev/tabs). 포맷은 docs/note-data-format.md
// 백엔드 TabView처럼 noteData를 JSON 문자열로 만든다

const tab = (measureNumber, data, recommendedFingering = null) => ({
  measureNumber,
  noteData: JSON.stringify(data),
  recommendedFingering,
})

// 기타 아르페지오: 줄·프렛 목록을 8분음표로 늘어놓는다
const arpeggio = (pairs) => pairs.map(([string, fret], i) => ({ beat: i * 0.5, duration: 0.5, string, fret }))

// 기타 코드 한 번 치기 (같은 박에 여러 줄)
const strum = (beat, shape, duration = 1) => shape
  .map((fret, i) => (fret === null ? null : { beat, duration, string: 6 - i, fret }))
  .filter(Boolean)

const C = [null, 3, 2, 0, 1, 0]
const G = [3, 2, 0, 0, 0, 3]
const AM = [null, 0, 2, 2, 1, 0]
const F = [1, 3, 3, 2, 1, 1]

export const SAMPLE_KEY = 'C'
export const SAMPLE_BPM = 92

export const GUITAR_SAMPLE = [
  tab(1, {
    timeSignature: '4/4',
    section: 'Intro',
    chords: [{ beat: 0, name: 'C' }],
    notes: arpeggio([[5, 3], [3, 0], [2, 1], [1, 0], [2, 1], [3, 0], [2, 1], [3, 0]]),
  }, '1번 포지션'),
  tab(2, {
    chords: [{ beat: 0, name: 'G/B' }],
    notes: arpeggio([[5, 2], [3, 0], [2, 0], [1, 3], [2, 0], [3, 0], [2, 0], [3, 0]]),
  }),
  tab(3, {
    chords: [{ beat: 0, name: 'Am' }],
    notes: [
      ...arpeggio([[5, 0], [3, 2], [2, 1], [1, 0]]),
      { beat: 2, duration: 0.5, string: 3, fret: 2 },
      { beat: 2.5, duration: 0.5, string: 3, fret: 4, technique: 'hammer' },
      { beat: 3, duration: 0.5, string: 3, fret: 2, technique: 'pull' },
      { beat: 3.5, duration: 0.5, string: 2, fret: 1 },
    ],
  }),
  tab(4, {
    chords: [{ beat: 0, name: 'F' }, { beat: 2, name: 'G' }],
    notes: [...strum(0, F, 2), ...strum(2, G, 2)],
  }, '1프렛 검지 바레'),
  tab(5, {
    section: 'Verse 1',
    chords: [{ beat: 0, name: 'C' }],
    notes: [...strum(0, C), ...strum(1.5, C, 0.5), ...strum(2, C), { beat: 3, string: 5, technique: 'mute' }, { beat: 3, string: 4, technique: 'mute' }, ...strum(3.5, C, 0.5)],
  }),
  tab(6, {
    chords: [{ beat: 0, name: 'Am' }],
    notes: [...strum(0, AM), ...strum(1.5, AM, 0.5), ...strum(2, AM), ...strum(3, AM)],
  }),
  tab(7, {
    chords: [{ beat: 0, name: 'Dm7' }, { beat: 2, name: 'G7' }],
    notes: [
      { beat: 0, duration: 1, string: 3, fret: 5 },
      { beat: 1, duration: 1, string: 3, fret: 7, technique: 'slide' },
      { beat: 2, duration: 1, string: 2, fret: 8, technique: 'bend' },
      { beat: 3, duration: 1, string: 1, fret: 7, technique: 'vibrato' },
    ],
  }, '5프렛 포지션, 약지로 벤딩'),
  tab(8, {
    timeSignature: '3/4',
    chords: [{ beat: 0, name: 'C' }],
    notes: [
      { beat: 0, duration: 1, string: 3, fret: 12, technique: 'harmonic' },
      { beat: 0, duration: 1, string: 2, fret: 12, technique: 'harmonic' },
      { beat: 0, duration: 1, string: 1, fret: 12, technique: 'harmonic' },
      { beat: 1, duration: 1, string: 5, fret: 3, technique: 'slide' },
    ],
  }),
  tab(9, {
    timeSignature: '4/4',
    section: 'Chorus',
    chords: [{ beat: 0, name: 'F' }, { beat: 2, name: 'G' }],
    notes: [...strum(0, F), ...strum(1, F), ...strum(2, G), ...strum(3, G)],
  }),
  tab(10, {
    chords: [{ beat: 0, name: 'Em' }, { beat: 2, name: 'Am' }],
    notes: [...strum(0, [0, 2, 2, 0, 0, 0]), ...strum(1, [0, 2, 2, 0, 0, 0]), ...strum(2, AM), ...strum(3, AM)],
  }),
  tab(11, {
    chords: [{ beat: 0, name: 'Dm' }, { beat: 2, name: 'G' }],
    notes: [...strum(0, [null, null, 0, 2, 3, 1], 2), ...strum(2, G, 2)],
  }),
  tab(12, {
    chords: [{ beat: 0, name: 'C' }],
    notes: strum(0, C, 4),
  }),
]

// 베이스: 코드 근음을 8분음표로
const roots = (string, fret, count = 8) => Array.from({ length: count }, (_, i) => ({ beat: i * 0.5, duration: 0.5, string, fret }))

export const BASS_SAMPLE = [
  tab(1, { section: 'Intro', chords: [{ beat: 0, name: 'C' }], notes: roots(3, 3) }),
  tab(2, { chords: [{ beat: 0, name: 'G/B' }], notes: roots(3, 2) }),
  tab(3, { chords: [{ beat: 0, name: 'Am' }], notes: [...roots(4, 5, 6), { beat: 3, string: 4, fret: 7, technique: 'slide' }, { beat: 3.5, string: 3, fret: 0 }] }),
  tab(4, { chords: [{ beat: 0, name: 'F' }, { beat: 2, name: 'G' }], notes: [...roots(4, 1, 4), ...roots(4, 3, 4).map((n) => ({ ...n, beat: n.beat + 2 }))] }),
]

// 잘못된 데이터 처리 확인용
export const BROKEN_SAMPLE = [
  ...GUITAR_SAMPLE.slice(0, 2),
  { measureNumber: 3, noteData: '{ 잘못된 JSON', recommendedFingering: null },
  tab(4, { chords: [{ beat: 9, name: 'C' }, { beat: 0, name: 'Am' }], notes: [{ beat: 0, string: 9, fret: 3 }, { beat: 1, string: 2, fret: 99 }, { beat: 2, string: 2, fret: 3 }] }),
]
