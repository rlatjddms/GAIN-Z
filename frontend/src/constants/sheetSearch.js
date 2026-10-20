// 악보 찾기 링크 (#156): 곡 제목·가수로 악보를 파는 곳·무료 코드 사이트의 검색 결과를 새 탭으로 연다
// API·계약 없이 검색 주소만 쓴다. 산 악보(PDF·사진)는 보관함에 올리면 된다
export const SHEET_SEARCH_SITES = [
  { key: 'akbobada', name: '악보바다', note: '국내 밴드 악보 구매', url: (q) => `https://www.akbobada.com/searchAll.html?searchKeyword=${encodeURIComponent(q)}` },
  { key: 'ug', name: 'Ultimate Guitar', note: '무료 코드·타브', url: (q) => `https://www.ultimate-guitar.com/search.php?search_type=title&value=${encodeURIComponent(q)}` },
  // Songsterr는 한글 검색어를 받으면 검색하지 않고 첫 화면으로 돌아가서, 영어 검색어일 때만 쓴다
  { key: 'songsterr', name: 'Songsterr', note: '타브 · 영어 제목', latinOnly: true, url: (q) => `https://www.songsterr.com/?pattern=${encodeURIComponent(q)}` },
]

// 한글이 들어 있는지 (영어 제목 전용 사이트를 끌 때)
export const hasHangul = (q) => /[\u1100-\u11FF\u3130-\u318F\uAC00-\uD7AF]/.test(q)

// 검색어: "제목 가수" (앞뒤 공백 정리, 비면 '')
export const sheetSearchQuery = (title, artist) => [title, artist].map((v) => (v || '').trim()).filter(Boolean).join(' ')
