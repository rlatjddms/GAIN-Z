// 테마 색 7가지 (#122). styles/theme.css의 [data-theme]와 같은 값. 첫 번째가 기본
export const THEMES = [
  { key: 'mint', name: '민트', color: '#12b886' },
  { key: 'teal', name: '틸', color: '#0c8599' },
  { key: 'cherry', name: '체리 핑크', color: '#e64980' },
  { key: 'cobalt', name: '코발트', color: '#2d5bff' },
  { key: 'lavender', name: '라벤더', color: '#7950f2' },
  { key: 'amber', name: '앰버', color: '#f08c00' },
  { key: 'graphite', name: '그래파이트', color: '#343a40' },
]

export const DEFAULT_THEME = THEMES[0].key
export const themeOf = (key) => THEMES.find((t) => t.key === key) || THEMES[0]
