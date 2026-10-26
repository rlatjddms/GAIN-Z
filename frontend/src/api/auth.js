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

// 소셜 로그인을 마치고 받은 한 번용 코드(1분)를 토큰으로 바꾼다 (#281). 토큰을 주소에 싣지 않으려고
// → { accessToken, refreshToken }
export async function exchangeOAuthCode(code) {
  const response = await axiosInstance.post('/api/v1/auth/oauth2/token', { code })
  return response.data.data
}

// 백엔드 ErrorCode와 같은 값
export const DUPLICATE_EMAIL = 'U001'
// 인증 표가 없거나 30분이 지남 → 다시 인증
export const EMAIL_NOT_VERIFIED = 'U012'
// 1분 안에 다시 요청함 → 메일은 새로 보내지 않았고, data.retryAfterSeconds 뒤에 다시 보낼 수 있다
export const MAIL_SEND_TOO_SOON = 'U017'

// 1분 안에 다시 요청한 오류면 남은 초, 아니면 null (화면을 나갔다 오면 기다릴 시간을 잊어서 서버가 알려 준다)
export function retryAfterOf(error) {
  const body = error.response?.data
  return body?.code === MAIL_SEND_TOO_SOON ? (body.data?.retryAfterSeconds ?? MAIL_RESEND_SECONDS) : null
}
// 메일을 다시 보낼 수 있을 때까지 (백엔드와 같게)
export const MAIL_RESEND_SECONDS = 60
