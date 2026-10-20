// 악보 찾기 링크 (#156): 곡 제목·가수로 악보를 파는 곳·무료 코드 사이트의 검색 결과를 새 탭으로 연다
// API·계약 없이 검색 주소만 쓴다. 산 악보(PDF·사진)는 보관함에 올리면 된다

// 한글이 들어 있는지
export const hasHangul = (q) => /[ᄀ-ᇿ㄰-㆏가-힯]/.test(q)

export const SHEET_SEARCH_SITES = [
  { key: 'akbobada', name: '악보바다', note: '국내 밴드 악보 구매', url: (q) => `https://www.akbobada.com/searchAll.html?searchKeyword=${encodeURIComponent(q)}` },
  { key: 'ug', name: 'Ultimate Guitar', note: '무료 코드·타브', url: (q) => `https://www.ultimate-guitar.com/search.php?search_type=title&value=${encodeURIComponent(q)}` },
  // Songsterr는 바깥에서 한글 검색어 주소로 들어오면 검색하지 않고 첫 화면을 연다 (사이트 안 검색창에선 한글이 됨)
  // → 그래도 사이트로 바로 보내고, 한글이면 누를 때 검색어를 복사해 검색창에 붙여 넣게 한다 (#164)
  { key: 'songsterr', name: 'Songsterr', note: '타브', copyIfHangul: true, url: (q) => `https://www.songsterr.com/?pattern=${encodeURIComponent(q)}` },
]

// 검색어: "제목 가수" (앞뒤 공백 정리, 비면 '')
export const sheetSearchQuery = (title, artist) => [title, artist].map((v) => (v || '').trim()).filter(Boolean).join(' ')
