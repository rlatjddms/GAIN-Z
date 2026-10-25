import api from './axios'

const base = (bandId) => `/api/v1/bands/${bandId}/posts`
const data = async (request) => (await request).data.data

// 게시글 목록. q: 제목·내용·작성자 검색어, from·to: 작성일 'YYYY-MM-DD' (둘 다 포함) (#188)
export const listBandPosts = (bandId, { page = 0, size = 12, notice = null, mine = false, q = '', from = null, to = null } = {}) =>
  data(api.get(base(bandId), { params: { page, size, notice, mine, q: q || null, from, to } }))
export const getBandPost = (bandId, id) => data(api.get(`${base(bandId)}/${id}`))
export const deleteBandPost = (bandId, id) => api.delete(`${base(bandId)}/${id}`)
export const setBandNotice = (bandId, id, notice, noticeExpiresAt = null) => data(api.patch(`${base(bandId)}/${id}/notice`, { notice, noticeExpiresAt }))
// 댓글 한 쪽 (#188): 최근 댓글부터 size개(답글은 원댓글과 함께). 쪽 안은 오래된 순
// → { content, number, totalPages, totalElements: 원댓글 수, commentCount: 지워지지 않은 댓글 수 }
export const listBandComments = (bandId, id, page = 0, size = 20) => data(api.get(`${base(bandId)}/${id}/comments`, { params: { page, size } }))
export const addBandComment = (bandId, id, comment) => data(api.post(`${base(bandId)}/${id}/comments`, comment))
export const updateBandComment = (bandId, id, commentId, content) => data(api.patch(`${base(bandId)}/${id}/comments/${commentId}`, { content }))
export const deleteBandComment = (bandId, id, commentId) => api.delete(`${base(bandId)}/${id}/comments/${commentId}`)

export async function loadBandAttachment(url) {
  const response = await api.get(url, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

export const BAND_ATTACHMENT_ACCEPT = 'image/png,image/jpeg,image/webp,video/mp4,video/webm,audio/mpeg,audio/wav,application/pdf'
export const BAND_ATTACHMENT_MAX_BYTES = 10 * 1024 * 1024
export const BAND_ATTACHMENT_MAX_COUNT = 8

export async function saveBandPost(bandId, id, post, files, onProgress) {
  const form = new FormData()
  form.append('post', new Blob([JSON.stringify(post)], { type: 'application/json' }))
  files.forEach(file => form.append('files', file))
  return data(api.request({
    method: id ? 'put' : 'post', url: id ? `${base(bandId)}/${id}` : base(bandId), data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: event => onProgress?.(event.total ? Math.round(event.loaded / event.total * 100) : 0),
  }))
}

export const downloadBandArchive = (bandId, id) => loadBandAttachment(`${base(bandId)}/${id}/attachments.zip`)
