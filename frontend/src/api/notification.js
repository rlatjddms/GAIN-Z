import axiosInstance from './axios'

// 알림 (친구·밴드·커뮤니티·합주)

// 알림 → { id, type, title: 어디서(밴드 이름·'친구'), content: 무슨 일, redirectUrl: 누르면 갈 화면 | null, read, createdAt }
// type: FRIEND_REQUEST | FRIEND_ACCEPT
//     | BAND_JOIN | BAND_KICKED | BAND_LEADER | BAND_DISBAND | BAND_LEAVE
//     | NOTICE | COMMENT | REPLY | MENTION
//     | SETLIST_ASSIGN | JAM_NEW | JAM_TRACK
//     | BAND_INVITE | BAND_ACCEPT (밴드 매칭에서 사용)
// 읽은 지 30일 지난 알림은 서버가 매일 새벽 지운다

// 내 알림 (최근 순) → { items: [알림], hasNext }
// category: FRIEND | BAND | COMMUNITY | JAM (없으면 전체), unread: true면 안 읽은 것만
export async function getNotifications({ page = 0, size = 20, category = null, unread = false } = {}) {
  const params = { page, size }
  if (category) params.category = category
  if (unread) params.unread = true
  const response = await axiosInstance.get('/api/v1/notifications', { params })
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

// 고른 알림 읽음
export async function markNotificationsRead(ids) {
  await axiosInstance.patch('/api/v1/notifications/read', { ids })
}

export async function markAllNotificationsRead() {
  await axiosInstance.patch('/api/v1/notifications/read-all')
}

export async function deleteNotification(id) {
  await axiosInstance.delete(`/api/v1/notifications/${id}`)
}

// 고른 알림 지우기
export async function deleteNotifications(ids) {
  await axiosInstance.post('/api/v1/notifications/delete', { ids })
}

// 읽은 알림 모두 지우기
export async function deleteReadNotifications() {
  await axiosInstance.delete('/api/v1/notifications/read')
}

// 내 알림 모두 지우기
export async function deleteAllNotifications() {
  await axiosInstance.delete('/api/v1/notifications')
}
