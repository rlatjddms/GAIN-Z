import api from './axios'

const base = (bandId) => `/api/v1/bands/${bandId}/posts`
const data = async (request) => (await request).data.data

export const listBandPosts = (bandId, page = 0, size = 12, notice = null) => data(api.get(base(bandId), { params: { page, size, notice } }))
export const getBandPost = (bandId, id) => data(api.get(`${base(bandId)}/${id}`))
export const createBandPost = (bandId, post) => data(api.post(base(bandId), post))
export const updateBandPost = (bandId, id, post) => data(api.patch(`${base(bandId)}/${id}`, post))
export const deleteBandPost = (bandId, id) => api.delete(`${base(bandId)}/${id}`)
export const setBandNotice = (bandId, id, notice) => data(api.patch(`${base(bandId)}/${id}/notice`, { notice }))
export const removeBandAttachment = (bandId, id) => data(api.delete(`${base(bandId)}/${id}/attachment`))
export const listBandComments = (bandId, id) => data(api.get(`${base(bandId)}/${id}/comments`))
export const addBandComment = (bandId, id, comment) => data(api.post(`${base(bandId)}/${id}/comments`, comment))
export const updateBandComment = (bandId, id, commentId, content) => data(api.patch(`${base(bandId)}/${id}/comments/${commentId}`, { content }))
export const deleteBandComment = (bandId, id, commentId) => api.delete(`${base(bandId)}/${id}/comments/${commentId}`)

export async function uploadBandAttachment(bandId, id, file) {
  const form = new FormData()
  form.append('file', file)
  return data(api.put(`${base(bandId)}/${id}/attachment`, form, { headers: { 'Content-Type': 'multipart/form-data' } }))
}

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
