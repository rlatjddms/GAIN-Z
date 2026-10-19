import axiosInstance from './axios'

// 알림 (친구·밴드·커뮤니티·합주)

// 알림 → { id, type, title: 어디서(밴드 이름·'친구'), content: 무슨 일, redirectUrl: 누르면 갈 화면 | null, read, createdAt }
// type: FRIEND_REQUEST | FRIEND_ACCEPT
//     | BAND_JOIN | BAND_KICKED | BAND_LEADER | BAND_DISBAND | BAND_LEAVE
//     | NOTICE | COMMENT | REPLY | MENTION
//     | SETLIST_ASSIGN | JAM_NEW | JAM_TRACK
//     | BAND_INVITE | BAND_ACCEPT (밴드 매칭에서 사용)

// 내 알림 (최근 순) → { items: [알림], hasNext }
export async function getNotifications({ page = 0, size = 20 } = {}) {
  const response = await axiosInstance.get('/api/v1/notifications', { params: { page, size } })
  return response.data.data
}

// 안 읽은 알림 개수 → { count }
export async function getUnreadCount() {
  const response = await axiosInstance.get('/api/v1/notifications/unread-count')
  return response.data.data
}

export async function markNotificationRead(id) {
  await axiosInstance.patch(`/api/v1/notifications/${id}/read`)
}

export async function markAllNotificationsRead() {
  await axiosInstance.patch('/api/v1/notifications/read-all')
}

export async function deleteNotification(id) {
  await axiosInstance.delete(`/api/v1/notifications/${id}`)
}

// 내 알림 모두 지우기
export async function deleteAllNotifications() {
  await axiosInstance.delete('/api/v1/notifications')
}
