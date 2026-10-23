import axiosInstance from './axios'

// 밴드 (만들기·초대 코드 가입·멤버 관리)

// 백엔드 BandService와 같은 제한
export const BAND_NAME_MAX = 30
export const BAND_DESCRIPTION_MAX = 200
export const MAX_BAND_MEMBERS = 8
export const MAX_JOINED_BANDS = 5
export const INVITE_CODE_LENGTH = 8

// 밴드 상세
// → { id, name, description, inviteCode, stage: STUDIO|LIVE|GARAGE|ROOFTOP, myPosition: LEADER|MEMBER, createdAt,
//     members: [{ memberId, userId, part, position, me, joinedAt, character: CharacterResponse | null }] }
// 멤버는 합주실 무대에 선 순서 (왼쪽부터)

// 내 밴드 목록 (들어간 순서) → [밴드 상세]
export async function getMyBands() {
  const response = await axiosInstance.get('/api/v1/bands/me')
  return response.data.data
}

export async function getBand(bandId) {
  const response = await axiosInstance.get(`/api/v1/bands/${bandId}`)
  return response.data.data
}

// 밴드 만들기 (만든 사람이 리더) part: 내 파트 (InstrumentType) → 밴드 상세
export async function createBand({ name, description, part }) {
  const response = await axiosInstance.post('/api/v1/bands', { name, description, part })
  return response.data.data
}

// 이름·소개 고치기 (리더만) → 밴드 상세
export async function updateBand(bandId, { name, description }) {
  const response = await axiosInstance.patch(`/api/v1/bands/${bandId}`, { name, description })
  return response.data.data
}

// 합주실 배경 바꾸기 (리더만) → 밴드 상세
export async function updateBandStage(bandId, stage) {
  const response = await axiosInstance.patch(`/api/v1/bands/${bandId}/stage`, { stage })
  return response.data.data
}

// 무대 자리 순서 바꾸기 (리더만) memberIds: 지금 멤버 전부를 왼쪽부터 → 밴드 상세
export async function reorderMembers(bandId, memberIds) {
  const response = await axiosInstance.put(`/api/v1/bands/${bandId}/members/order`, { memberIds })
  return response.data.data
}

// 밴드 없애기 (리더만)
export async function deleteBand(bandId) {
  await axiosInstance.delete(`/api/v1/bands/${bandId}`)
}

// 초대 코드 새로 받기 (리더만, 예전 코드는 못 쓰게 된다) → 밴드 상세
export async function regenerateInviteCode(bandId) {
  const response = await axiosInstance.post(`/api/v1/bands/${bandId}/invite-code`)
  return response.data.data
}

// 초대 코드로 가입 → 들어간 밴드 상세
export async function joinBand({ inviteCode, part }) {
  const response = await axiosInstance.post('/api/v1/bands/join', { inviteCode, part })
  return response.data.data
}

// 파트 바꾸기 (본인 또는 리더) → 밴드 상세
export async function changeMemberPart(bandId, memberId, part) {
  const response = await axiosInstance.patch(`/api/v1/bands/${bandId}/members/${memberId}`, { part })
  return response.data.data
}

// 멤버 내보내기 (리더만) → 밴드 상세
export async function removeMember(bandId, memberId) {
  const response = await axiosInstance.delete(`/api/v1/bands/${bandId}/members/${memberId}`)
  return response.data.data
}

// 밴드 나가기 (리더는 리더를 넘긴 뒤에만)
export async function leaveBand(bandId) {
  await axiosInstance.delete(`/api/v1/bands/${bandId}/members/me`)
}

// 리더 넘기기 (리더만) → 밴드 상세
export async function transferLeader(bandId, memberId) {
  const response = await axiosInstance.patch(`/api/v1/bands/${bandId}/leader`, { memberId })
  return response.data.data
}
