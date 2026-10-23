// GAIN-Z 픽셀 로고 (#122): 5×7 픽셀 글자, Z는 번개 모양
const GLYPHS = {
  G: ['01111', '10000', '10000', '10011', '10001', '10001', '01110'],
  A: ['01110', '10001', '10001', '11111', '10001', '10001', '10001'],
  I: ['111', '010', '010', '010', '010', '010', '111'],
  N: ['10001', '11001', '10101', '10011', '10001', '10001', '10001'],
  '-': ['000', '000', '000', '111', '000', '000', '000'],
  Z: ['00011', '00110', '01100', '11111', '00110', '01100', '11000'], // 번개
}

// 글자들을 픽셀 칸 목록으로: { width, height, cells: [[x, y], ...] } (글자 사이 1칸)
export function logoCells(text = 'GAIN-Z') {
  const cells = []
  let x = 0
  for (const ch of text) {
    const g = GLYPHS[ch]
    if (!g) continue
    g.forEach((row, y) => [...row].forEach((bit, k) => { if (bit === '1') cells.push([x + k, y]) }))
    x += g[0].length + 1
  }
  return { width: x - 1, height: 7, cells }
}

// 파비콘: 테마 색 바탕의 둥근 칸에 흰 번개 Z
export function faviconSvg(color) {
  const { cells } = logoCells('Z')
  const rects = cells.map(([x, y]) => `<rect x="${x + 2}" y="${y + 1}" width="1" height="1"/>`).join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 9 9" shape-rendering="crispEdges">`
    + `<rect width="9" height="9" rx="2" fill="${color}"/><g fill="#fff">${rects}</g></svg>`
}
