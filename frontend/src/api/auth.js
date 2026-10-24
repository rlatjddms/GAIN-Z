import axiosInstance from './axios'

// 가입 전 이메일 인증·비밀번호 찾기 (#193). 모두 로그인 없이 부른다

// 회원가입 "인증하기": 6자리 코드를 메일로. 이미 가입된 이메일이면 409(DUPLICATE_EMAIL)
export async function sendSignupCode(email) {
  await axiosInstance.post('/api/v1/auth/signup/email-code', { email })
}

// 코드가 맞으면 가입 요청에 넣을 인증 표(emailVerificationToken)를 돌려준다
export async function verifySignupCode(email, code) {
  const response = await axiosInstance.post('/api/v1/auth/signup/email-code/verify', { email, code })
  return response.data.data.emailVerificationToken
}

// 비밀번호 재설정 메일 보내기
// 결과 { status: 'SENT' | 'NOT_FOUND' | 'SOCIAL_ONLY', provider }
export async function requestPasswordReset(email) {
  const response = await axiosInstance.post('/api/v1/auth/password-reset/request', { email })
  return response.data.data ?? { status: 'SENT', provider: null }
}

// 메일 링크의 토큰으로 새 비밀번호 정하기
export async function resetPassword(token, newPassword) {
  await axiosInstance.post('/api/v1/auth/password-reset/confirm', { token, newPassword })
}

// 백엔드 ErrorCode와 같은 값
export const DUPLICATE_EMAIL = 'U001'
// 인증 표가 없거나 30분이 지남 → 다시 인증
export const EMAIL_NOT_VERIFIED = 'U012'
// 메일을 다시 보낼 수 있을 때까지 (백엔드와 같게)
export const MAIL_RESEND_SECONDS = 60
