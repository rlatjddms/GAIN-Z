import api from './axios'
import { bandFileLink, uploadBandFiles, UPLOAD_KIND } from './upload'

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

// 첨부 파일 주소 → 저장소에서 바로 받는 몇 분짜리 주소 (#262). <img>·<video>·<audio> src에 그대로 쓴다.
// 예전처럼 blob:을 만들지 않아서 큰 영상도 받으면서 바로 재생된다 (URL.revokeObjectURL을 불러도 아무 일 없음)
export const loadBandAttachment = (url, download = false) => bandFileLink(url, download)

export const BAND_ATTACHMENT_ACCEPT = 'image/png,image/jpeg,image/webp,video/mp4,video/webm,audio/mpeg,audio/wav,application/pdf'
// 게시판 첨부 200MB, 셋리스트 곡 첨부 50MB (#262, 서버 BandAttachmentStorage와 같게)
export const BAND_ATTACHMENT_MAX_BYTES = 200 * 1024 * 1024
export const SETLIST_ATTACHMENT_MAX_BYTES = 50 * 1024 * 1024
export const BAND_ATTACHMENT_MAX_COUNT = 8

// 새 첨부는 먼저 저장소에 바로 올리고(onProgress 0~100) 글을 저장할 때 uploadIds로 보낸다 (#262)
export async function saveBandPost(bandId, id, post, files, onProgress) {
  const uploadIds = files.length ? await uploadBandFiles(bandId, UPLOAD_KIND.ATTACHMENT, files, onProgress) : []
  const form = new FormData()
  form.append('post', new Blob([JSON.stringify({ ...post, uploadIds })], { type: 'application/json' }))
  onProgress?.(100)
  return data(api.request({
    method: id ? 'put' : 'post', url: id ? `${base(bandId)}/${id}` : base(bandId), data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
  }))
}

// 글의 첨부 전체 ZIP은 서버가 묶어서 보내 준다 (blob: 주소, 다 쓰면 revoke)
export async function downloadBandArchive(bandId, id) {
  const response = await api.get(`${base(bandId)}/${id}/attachments.zip`, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}
