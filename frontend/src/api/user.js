import axiosInstance from './axios'

// 내 계정 (마이페이지)

// { id, email, name, provider: LOCAL|GOOGLE|KAKAO|NAVER, createdAt, character: { nickname, level } | null }
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
