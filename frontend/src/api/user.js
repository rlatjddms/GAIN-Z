import axiosInstance from './axios'

// 내 계정 (마이페이지)

// { id, email, name, provider: LOCAL|GOOGLE|KAKAO|NAVER, createdAt, profileImageUrl: 사진 주소 | null }
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
