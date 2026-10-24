import axiosInstance from './axios'

// 내 계정 (마이페이지)

// { id, email, name, provider: LOCAL|GOOGLE|KAKAO|NAVER, createdAt, profileImageUrl: 사진 주소 | null,
//   profileMusic: { title, artist, url: 링크 | null } | null, profileVisibility: PUBLIC|FRIENDS|PRIVATE }
export async function getMyInfo() {
  const response = await axiosInstance.get('/api/v1/users/me')
  return response.data.data
}

export async function updateMyName(name) {
  const response = await axiosInstance.patch('/api/v1/users/me', { name })
  return response.data.data
}

// 이메일로 가입한 사람만
export async function changePassword({ currentPassword, newPassword }) {
  await axiosInstance.patch('/api/v1/users/me/password', { currentPassword, newPassword })
}

// password: 이메일 가입자는 현재 비밀번호 확인, 소셜 가입자는 null
export async function withdraw(password) {
  await axiosInstance.delete('/api/v1/users/me', { data: { password } })
}

/* ---------- 프로필 사진 ---------- */

// 백엔드 ProfileImageStorage와 같은 제한
export const PROFILE_IMAGE_ACCEPT = 'image/png,image/jpeg,image/webp'
export const PROFILE_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
export const MAX_PROFILE_IMAGE_MB = 5

// 사진 바꾸기 → 내 정보 (getMyInfo와 같은 모양)
export async function uploadProfileImage(file) {
  const form = new FormData()
  form.append('file', file)
  const response = await axiosInstance.put('/api/v1/users/me/profile-image', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data.data
}

// 사진 지우기 (기본 프로필로) → 내 정보
export async function deleteProfileImage() {
  const response = await axiosInstance.delete('/api/v1/users/me/profile-image')
  return response.data.data
}

// 사진은 로그인 토큰이 필요해서 받아서 화면용 주소(blob:)로 만든다 (다 쓰면 revoke)
// path: 내 정보의 profileImageUrl
export async function loadProfileImageUrl(path) {
  const response = await axiosInstance.get(path, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

/* ---------- 프로필 뮤직·공개 범위 ---------- */

// 백엔드 ProfileMusicRequest와 같은 제한
export const PROFILE_MUSIC_TITLE_MAX = 100
export const PROFILE_MUSIC_ARTIST_MAX = 100
export const PROFILE_MUSIC_URL_MAX = 500

export const PROFILE_VISIBILITY_OPTIONS = [
  { value: 'PUBLIC', label: '전체 공개', desc: '누구나 내 프로필을 볼 수 있어요' },
  { value: 'FRIENDS', label: '친구에게만', desc: '친구만 내 프로필을 볼 수 있어요' },
  { value: 'PRIVATE', label: '비공개', desc: '다른 사람에게는 이름과 사진만 보여요' },
]

// 요즘 듣는 음악 설정 (url은 http/https만, 없으면 null) → 내 정보
// artworkUrl·previewUrl은 곡 검색으로 골랐을 때만 (Apple 주소만 저장된다, #187)
export async function updateProfileMusic({ title, artist, url, artworkUrl = null, previewUrl = null }) {
  const response = await axiosInstance.put('/api/v1/users/me/profile-music', { title, artist, url, artworkUrl, previewUrl })
  return response.data.data
}

// 곡 검색 (#187): Apple iTunes 검색 API를 브라우저에서 바로 부른다 (키 없음, 로그인 토큰을 보내지 않는다).
// 한국 스토어는 음악 결과가 없어서 미국 스토어를 한국어로 찾는다. 일부 곡은 제목이 영어로 나온다
// → [{ id, title, artist, album, artworkUrl, previewUrl, url }]
export async function searchSongs(term, signal) {
  const params = new URLSearchParams({ term, country: 'US', lang: 'ko_kr', media: 'music', entity: 'song', limit: '12' })
  const response = await fetch(`https://itunes.apple.com/search?${params}`, { signal })
  if (!response.ok) throw new Error('곡을 찾지 못했어요.')
  const { results = [] } = await response.json()
  return results
    .filter(r => r.trackName && r.artistName)
    .map(r => ({
      id: r.trackId,
      title: r.trackName.slice(0, PROFILE_MUSIC_TITLE_MAX),
      artist: r.artistName.slice(0, PROFILE_MUSIC_ARTIST_MAX),
      album: r.collectionName || '',
      // 100px 커버를 화면용 200px로
      artworkUrl: r.artworkUrl100 ? r.artworkUrl100.replace('100x100bb', '200x200bb') : null,
      previewUrl: r.previewUrl || null,
      url: r.trackViewUrl && r.trackViewUrl.length <= PROFILE_MUSIC_URL_MAX ? r.trackViewUrl : null,
    }))
}

// 요즘 듣는 음악 지우기 → 내 정보
export async function deleteProfileMusic() {
  const response = await axiosInstance.delete('/api/v1/users/me/profile-music')
  return response.data.data
}

// 프로필 공개 범위 바꾸기 → 내 정보
export async function updateProfileVisibility(visibility) {
  const response = await axiosInstance.patch('/api/v1/users/me/visibility', { visibility })
  return response.data.data
}

// 다른 사람 프로필 (공개 범위에 따라 숨김)
// → { userId, name, profileImageUrl, visible: 볼 수 있는지,
//     relation: ME|FRIEND|SENT|RECEIVED|NONE (나와의 친구 관계),
//     profileMusic | null, character | null, dailyPractice: [{ date, totalSeconds }] (최근 30주),
//     bands: [{ bandId, name, part, position, together }] }
// visible이 false면 이름·사진·관계만 오고 나머지는 null·빈 목록
export async function getUserProfile(userId) {
  const response = await axiosInstance.get(`/api/v1/users/${userId}/profile`)
  return response.data.data
}
