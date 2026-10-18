import api from './axios'

const base = (bandId) => `/api/v1/bands/${bandId}/setlists`
const data = async (request) => (await request).data.data

// 셋리스트 → [{ id, title, favorite, items: [{ id, title, artist, memo, sheet: { sheetId, title, artist, workspaceItemId } | null,
//                                               memberIds, attachments: [{ id, name, type, url }] }] }]
export const listSetlists = (bandId) => data(api.get(base(bandId)))
export const createSetlist = (bandId, title) => data(api.post(base(bandId), { title }))
// 보낸 값(title, favorite)만 바꾼다
export const updateSetlist = (bandId, id, changes) => data(api.patch(`${base(bandId)}/${id}`, changes))
export const deleteSetlist = (bandId, id) => api.delete(`${base(bandId)}/${id}`)
export const reorderSetlists = (bandId, ids) => data(api.put(`${base(bandId)}/order`, { ids }))

// 곡 저장 (itemId가 없으면 추가). item: { title, artist, memo, sheetId, memberIds, keepAttachmentIds } → 셋리스트
export async function saveSetlistItem(bandId, setlistId, itemId, item, files) {
  const form = new FormData()
  form.append('item', new Blob([JSON.stringify(item)], { type: 'application/json' }))
  files.forEach(file => form.append('files', file))
  const url = `${base(bandId)}/${setlistId}/items${itemId ? `/${itemId}` : ''}`
  return data(api.request({ method: itemId ? 'put' : 'post', url, data: form, headers: { 'Content-Type': 'multipart/form-data' } }))
}
export const deleteSetlistItem = (bandId, setlistId, itemId) => data(api.delete(`${base(bandId)}/${setlistId}/items/${itemId}`))
export const reorderSetlistItems = (bandId, setlistId, ids) => data(api.put(`${base(bandId)}/${setlistId}/items/order`, { ids }))

// 멤버별 이번 주 연습 → { from, to, members: [{ memberId, userId, practiceSeconds, practiceDays, routineTotal, routineCompleted }] }
export const getSetlistPractice = (bandId) => data(api.get(`${base(bandId)}/practice`))
