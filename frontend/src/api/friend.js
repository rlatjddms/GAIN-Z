import axiosInstance from './axios'

// 친구 (친구 코드로 신청 → 상대가 수락하면 친구)

// 백엔드 FriendService와 같은 제한
export const FRIEND_CODE_LENGTH = 8
export const MAX_FRIENDS = 100

// 친구·신청에 나오는 사람
// → { userId, name, profileImageUrl: 사진 주소 | null, character: CharacterResponse | null }

/* ---------- 친구 코드 ---------- */

// 내 친구 코드 → { code }
export async function getMyFriendCode() {
  const response = await axiosInstance.get('/api/v1/friends/code')
  return response.data.data
}

// 친구 코드 새로 받기 (예전 코드는 못 쓰게 된다) → { code }
export async function regenerateFriendCode() {
  const response = await axiosInstance.post('/api/v1/friends/code')
  return response.data.data
}

/* ---------- 친구 신청 ---------- */

// 신청 → { requestId, user, createdAt }  (받은 신청이면 user는 보낸 사람, 보낸 신청이면 받는 사람)

// 친구 코드로 신청 → 보낸 신청
export async function sendFriendRequest(code) {
  const response = await axiosInstance.post('/api/v1/friends/requests', { code })
  return response.data.data
}

// 같은 밴드 멤버에게 바로 신청 (코드 없이) → 보낸 신청
export async function sendFriendRequestToBandMate(userId) {
  const response = await axiosInstance.post(`/api/v1/friends/requests/users/${userId}`)
  return response.data.data
}

// 받은 신청 (최근 순) → [신청]
export async function getReceivedRequests() {
  const response = await axiosInstance.get('/api/v1/friends/requests/received')
  return response.data.data
}

// 보낸 신청 (최근 순) → [신청]
export async function getSentRequests() {
  const response = await axiosInstance.get('/api/v1/friends/requests/sent')
  return response.data.data
}

// 받은 신청 수락 → 새 친구
export async function acceptFriendRequest(requestId) {
  const response = await axiosInstance.post(`/api/v1/friends/requests/${requestId}/accept`)
  return response.data.data
}

// 받은 신청 거절 / 보낸 신청 취소
export async function deleteFriendRequest(requestId) {
  await axiosInstance.delete(`/api/v1/friends/requests/${requestId}`)
}

/* ---------- 친구 목록 ---------- */

// 친구 → { userId, name, profileImageUrl, character, since: 친구가 된 때, profileMusic: { title, artist, url } | null }

// 내 친구 목록 (이름 순) → [친구]
export async function getFriends() {
  const response = await axiosInstance.get('/api/v1/friends')
  return response.data.data
}

// 같은 밴드 멤버 (나 빼고, 이름 순) → [{ userId, name, profileImageUrl, character, bandNames: [같이 있는 밴드 이름] }]
// 친구인지·신청했는지는 친구 목록·신청 목록과 맞춰 본다
export async function getBandMates() {
  const response = await axiosInstance.get('/api/v1/friends/band-mates')
  return response.data.data
}

// 친구 끊기
export async function removeFriend(userId) {
  await axiosInstance.delete(`/api/v1/friends/${userId}`)
}
