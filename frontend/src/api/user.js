import axiosInstance from './axios'

// 내 계정 (마이페이지)

// { id, email, name, provider: 처음 가입한 방법 LOCAL|GOOGLE|KAKAO|NAVER, createdAt, profileImageUrl: 사진 주소 | null,
//   profileMusic: { title, artist, url: 링크 | null } | null, profileVisibility: PUBLIC|FRIENDS|PRIVATE,
//   hasPassword: 이메일 비밀번호로 로그인할 수 있는지, socialLinks: 연결된 소셜 ['GOOGLE', …] (#189) }
export async function getMyInfo() {
  const response = await axiosInstance.get('/api/v1/users/me')
  return response.data.data
}

// 비밀번호가 있는 계정만
export async function changePassword({ currentPassword, newPassword }) {
  await axiosInstance.patch('/api/v1/users/me/password', { currentPassword, newPassword })
}

// 비밀번호가 없는 계정(소셜로만 가입)이 처음 만들 때 (#189). 만들면 이메일로도 로그인할 수 있다
export async function createPassword(newPassword) {
  await axiosInstance.post('/api/v1/users/me/password', { newPassword })
}

/* ---------- 소셜 연결 (#189) ---------- */

export const SOCIAL_PROVIDERS = [
  { value: 'GOOGLE', label: '구글', mark: 'G' },
  { value: 'KAKAO', label: '카카오', mark: 'K' },
  { value: 'NAVER', label: '네이버', mark: 'N' },
]

// 5분짜리 연결 표. /oauth2/authorization/{provider}?link=표 로 보내면 로그인한 계정에 연결된다
export async function startSocialLink(provider) {
  const response = await axiosInstance.post(`/api/v1/users/me/social-links/${provider}/start`)
  return response.data.data.linkToken
}

// 내 계정 정보를 돌려준다. 로그인 방법이 하나만 남으면 해제할 수 없다
export async function unlinkSocial(provider) {
  const response = await axiosInstance.delete(`/api/v1/users/me/social-links/${provider}`)
  return response.data.data
}

// 소셜 로그인·연결이 실패해 돌아왔을 때 (?socialError= / ?linkError=) 보여 줄 문구
export function socialErrorMessage(code, provider) {
  const name = SOCIAL_PROVIDERS.find(p => p.value === provider)?.label || '소셜'
  switch (code) {
    case 'EMAIL_EXISTS':
      return `${name} 계정의 이메일로 이미 가입한 계정이 있어요. 원래 방법으로 로그인한 뒤 마이페이지 > 로그인 방법에서 ${name}를 연결해 주세요.`
    case 'EMAIL_REQUIRED':
      return `${name}에서 이메일 제공에 동의해야 가입할 수 있어요.`
    case 'LINK_EXPIRED':
      return '연결 시간이 지났어요. 마이페이지에서 다시 연결해 주세요.'
    case 'LINK_TAKEN':
      return `이 ${name} 계정은 이미 다른 GAIN-Z 계정에 연결돼 있어요.`
    case 'LINK_PROVIDER_EXISTS':
      return `이미 다른 ${name} 계정이 연결돼 있어요. 해제한 뒤 다시 연결해 주세요.`
    default:
      return `${name} 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.`
  }
}

// password: 비밀번호가 있는 계정은 현재 비밀번호 확인, 없으면 null
export async function withdraw(password) {
  await axiosInstance.delete('/api/v1/users/me', { data: { password } })
}

/* ---------- 프로필 사진 ---------- */

// 백엔드 ProfileImageStorage와 같은 제한
export const PROFILE_IMAGE_ACCEPT = 'image/png,image/jpeg,image/webp'
export const PROFILE_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp']
export const MAX_PROFILE_IMAGE_MB = 10

// 프로필 사진은 작게 보이니 긴 변 1024px로 줄여서 올린다 (#262). 휴대폰 원본(몇 MB)도 수백 KB가 된다
const PROFILE_IMAGE_EDGE = 1024

// 줄일 필요가 없거나 브라우저가 못 줄이면 원본 그대로
export async function shrinkImage(file, edge = PROFILE_IMAGE_EDGE) {
  let image
  try { image = await createImageBitmap(file) } catch { return file }
  const scale = Math.min(1, edge / Math.max(image.width, image.height))
  if (scale === 1 && file.size <= 512 * 1024) { image.close?.(); return file }
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(image.width * scale)
  canvas.height = Math.round(image.height * scale)
  const context = canvas.getContext('2d')
  // JPEG는 투명을 못 담아서 흰 바탕을 깐다
  context.fillStyle = '#fff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(image, 0, 0, canvas.width, canvas.height)
  image.close?.()
  const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.85))
  if (!blob || blob.size >= file.size) return file
  return new File([blob], file.name.replace(/\.[^.]+$/, '') + '.jpg', { type: 'image/jpeg' })
}

// 사진 바꾸기 → 내 정보 (getMyInfo와 같은 모양)
export async function uploadProfileImage(file) {
  const form = new FormData()
  form.append('file', await shrinkImage(file))
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
// 아이폰은 iTunes가 음악 앱 주소(musics://)로 돌려보내서 브라우저로는 못 받는다 (#228)
// → 브라우저로 먼저 찾고, 안 되면 백엔드가 대신 찾는다 (iTunes는 같은 IP에서 분당 약 20번까지라 모두 서버로 보내지 않는다)
// → [{ id, title, artist, album, artworkUrl, previewUrl, url }]
export async function searchSongs(term, signal) {
  try {
    return await searchSongsInBrowser(term, signal)
  } catch (e) {
    if (e.name === 'AbortError') throw e
    const response = await axiosInstance.get('/api/v1/music/search', { params: { term }, signal })
    return response.data.data
  }
}

async function searchSongsInBrowser(term, signal) {
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
