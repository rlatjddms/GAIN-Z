import axiosInstance from './axios'

// 내 보관함 목록 → [{ id, sheetId, title, artist, instrumentType, originalKey, customKey, bpm, favorite, hasMemo, mine, addedAt }]
// favorite: 즐겨찾기만, sort: 'recent'(담은 순) / 'title'(제목 순)
export async function getWorkspace({ favorite = false, sort = 'recent' } = {}) {
  const response = await axiosInstance.get('/api/v1/workspace/sheets', { params: { favorite, sort } })
  return response.data.data
}

// 보관함 악보 보기 → { id, sheet: { id, title, artist, instrumentType, sheetTypes, originalKey, bpm, content, tabs, mine },
//                     customKey, personalMemo, favorite, addedAt }
export async function getWorkspaceItem(id) {
  const response = await axiosInstance.get(`/api/v1/workspace/sheets/${id}`)
  return response.data.data
}

// 메모·바꾼 키·즐겨찾기 중 보낸 것만 수정 (원래 키를 보내면 원래 키로, 빈 메모는 지움)
export async function updateWorkspaceItem(id, { customKey, personalMemo, isFavorite }) {
  const response = await axiosInstance.patch(`/api/v1/workspace/sheets/${id}`, { customKey, personalMemo, isFavorite })
  return response.data.data
}

// 보관함에서 빼기 (내가 쓴 악보는 악보도 지워짐)
export async function removeWorkspaceItem(id) {
  await axiosInstance.delete(`/api/v1/workspace/sheets/${id}`)
}

// 코드 악보 직접 쓰기 → 보관함 항목 (getWorkspaceItem과 같은 모양)
export async function createSheet(sheet) {
  const response = await axiosInstance.post('/api/v1/sheets', sheetBody(sheet))
  return response.data.data
}

// 내가 쓴 악보 고치기
export async function updateSheet(sheetId, sheet) {
  const response = await axiosInstance.put(`/api/v1/sheets/${sheetId}`, sheetBody(sheet))
  return response.data.data
}

const sheetBody = ({ title, artist, originalKey, bpm, instrumentType, content }) => ({
  title, artist, originalKey, bpm: bpm || null, instrumentType, content,
})
