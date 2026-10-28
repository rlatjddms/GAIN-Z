// 커뮤니티 글 목록을 기기에 기억해 둔다 (#315). 앱을 새로 열었을 때(아이폰 홈 화면 앱은 오래 두면 새로 열린다)
// 빈 카드(불러오는 중) 대신 마지막으로 본 목록을 바로 보여 주고 뒤에서 새로 불러온다.
// 로그인 토큰과 같은 곳에 둔다: "로그인 유지"면 localStorage, 아니면 sessionStorage(창을 닫으면 사라짐).
// 로그아웃·로그인 만료 때 지운다 (stores/auth.js, api/axios.js)
export const COMMUNITY_CACHE_KEY = 'gainz.communityCache'
// 밴드·쪽·검색 조건 몇 개까지만
const MAX_ENTRIES = 8

const storage = () => (localStorage.getItem('accessToken') ? localStorage : sessionStorage)

function readAll() {
  try { return JSON.parse(storage().getItem(COMMUNITY_CACHE_KEY) || '[]') } catch { return [] }
}

export function readCommunityCache(key) {
  return readAll().find(entry => entry.key === key)?.list || null
}

export function writeCommunityCache(key, list) {
  const entries = [{ key, list }, ...readAll().filter(entry => entry.key !== key)].slice(0, MAX_ENTRIES)
  try { storage().setItem(COMMUNITY_CACHE_KEY, JSON.stringify(entries)) } catch { /* 저장 공간이 없으면 기억하지 않는다 */ }
}
