import axiosInstance from './axios'

// 이메일 인증·비밀번호 찾기 (#193). 모두 로그인 없이 부른다.
// 가입 여부가 드러나지 않게, 메일 요청은 이메일이 없어도 똑같이 성공으로 돌아온다

// 인증 메일 다시 보내기
export async function resendVerificationMail(email) {
  await axiosInstance.post('/api/v1/auth/email-verification/resend', { email })
}

// 메일 링크의 토큰으로 이메일 인증 마치기
export async function confirmEmail(token) {
  await axiosInstance.post('/api/v1/auth/email-verification/confirm', { token })
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
export const EMAIL_NOT_VERIFIED = 'U012'
// 메일을 다시 보낼 수 있을 때까지 (백엔드와 같게)
export const MAIL_RESEND_SECONDS = 60
