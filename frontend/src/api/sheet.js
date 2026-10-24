import axiosInstance from './axios'

// 내 보관함 목록 → [{ id, sheetId, title, artist, instrumentType, sheetType, originalKey, customKey, bpm,
//                     pageCount, coverPageId, favorite, hasMemo, mine, addedAt }]
// favorite: 즐겨찾기만, sort: 'recent'(담은 순) / 'title'(제목 순)
export async function getWorkspace({ favorite = false, sort = 'recent' } = {}) {
  const response = await axiosInstance.get('/api/v1/workspace/sheets', { params: { favorite, sort } })
  return response.data.data
}

// 보관함 악보 보기 → { id, sheet: { id, title, artist, instrumentType, sheetTypes, originalKey, bpm, content,
//                       pages: [{ id, pageNumber, contentType, fileName, size }], tabs, mine },
//                     customKey, personalMemo, favorite, addedAt }
export async function getWorkspaceItem(id) {
  const response = await axiosInstance.get(`/api/v1/workspace/sheets/${id}`)
  return response.data.data
}

// 이미 있는 악보(공용 악보, 내 밴드 셋리스트에 연결된 악보)를 보관함에 담기 → 보관함 항목
export async function addToWorkspace(sheetId) {
  const response = await axiosInstance.post('/api/v1/workspace/sheets', { sheetId })
  return response.data.data
}

// 메모·연습 키·즐겨찾기 중 보낸 것만 수정 (원래 키를 보내면 원래 키로, 빈 메모는 지움)
// customChords: 키 바꿔 치기 표에 내가 추가한 코드 목록 (#192, 보낸 것으로 통째로 바뀜)
export async function updateWorkspaceItem(id, { customKey, personalMemo, isFavorite, customChords }) {
  const response = await axiosInstance.patch(`/api/v1/workspace/sheets/${id}`, { customKey, personalMemo, isFavorite, customChords })
  return response.data.data
}

// 보관함에서 빼기 (내가 올린 악보는 파일도 지워짐)
export async function removeWorkspaceItem(id) {
  await axiosInstance.delete(`/api/v1/workspace/sheets/${id}`)
}

// content: 코드 악보 글 ([C]가사 형식, #157). 코드 악보를 만들거나 고칠 때만 보냄
const infoBody = ({ title, artist, originalKey, bpm, instrumentType, sheetType, content }) => ({
  title, artist, originalKey: originalKey || null, bpm: bpm || null, instrumentType, sheetType,
  ...(content !== undefined ? { content } : {}),
})

// 기본 헤더가 JSON이라 파일 보낼 때는 바꿔 준다 (경계값은 브라우저가 붙임)
const MULTIPART = { 'Content-Type': 'multipart/form-data' }

const filesForm = (files, info) => {
  const form = new FormData()
  if (info) form.append('info', new Blob([JSON.stringify(infoBody(info))], { type: 'application/json' }))
  files.forEach((f) => form.append('files', f))
  return form
}

// 악보 파일 올리기 (곡 정보 + 파일 여러 장, 고른 순서가 장 순서) → 보관함 항목 (getWorkspaceItem과 같은 모양)
// onProgress(0~1)
export async function uploadSheet(info, files, onProgress) {
  const response = await axiosInstance.post('/api/v1/sheets', filesForm(files, info), {
    headers: MULTIPART,
    onUploadProgress: (e) => onProgress?.(e.total ? e.loaded / e.total : 0),
  })
  return response.data.data
}

// 글 악보 만들기 (파일 없이 글만) → 보관함 항목 (uploadSheet와 같은 모양)
// info.sheetType: CHORD = 코드 붙여넣기(#157, [C]가사 형식), TAB = 타브 붙여넣기·직접 쓰기(#166, 글자 타브)
export async function createTextSheet(info, content) {
  const response = await axiosInstance.post('/api/v1/sheets/text', infoBody({ ...info, content }))
  return response.data.data
}

// 내가 올린 악보의 곡 정보 고치기 → 악보
export async function updateSheet(sheetId, info) {
  const response = await axiosInstance.put(`/api/v1/sheets/${sheetId}`, infoBody(info))
  return response.data.data
}

// 장 더 올리기 (뒤에 붙음) → 악보
export async function addSheetPages(sheetId, files, onProgress) {
  const response = await axiosInstance.post(`/api/v1/sheets/${sheetId}/pages`, filesForm(files), {
    headers: MULTIPART,
    onUploadProgress: (e) => onProgress?.(e.total ? e.loaded / e.total : 0),
  })
  return response.data.data
}

// 장 지우기 (마지막 한 장은 못 지움) → 악보
export async function deleteSheetPage(sheetId, pageId) {
  const response = await axiosInstance.delete(`/api/v1/sheets/${sheetId}/pages/${pageId}`)
  return response.data.data
}

// 장 순서 바꾸기 (#191): 지금 장 ID를 원하는 순서대로 모두 보낸다 → 악보
export async function reorderSheetPages(sheetId, pageIds) {
  const response = await axiosInstance.put(`/api/v1/sheets/${sheetId}/pages/order`, { pageIds })
  return response.data.data
}

// 내 악보로 복사 (#191): 남이 올린 악보를 사본으로 만들어 이 보관함 항목을 사본으로 바꾼다
// (내 메모·연습 키·필기도 사본으로 옮김) → 보관함 항목 상세 (항목 ID는 그대로)
export async function copyWorkspaceItem(itemId) {
  const response = await axiosInstance.post(`/api/v1/workspace/sheets/${itemId}/copy`)
  return response.data.data
}

// 장 파일은 로그인 토큰이 필요해서 <img src>로 바로 못 쓴다. 받아서 화면용 주소(blob:)로 만든다 (다 쓰면 revoke)
export async function loadSheetPageUrl(sheetId, pageId) {
  const response = await axiosInstance.get(`/api/v1/sheets/${sheetId}/pages/${pageId}`, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

// 악보 위 내 필기 (#167): [{ pageId, pdfPage, data, updatedAt }] (data = 필기 JSON 글, stores/annotation.js)
export async function getAnnotations(sheetId) {
  const response = await axiosInstance.get(`/api/v1/sheets/${sheetId}/annotations`)
  return response.data.data
}

// 장(PDF는 쪽) 하나의 필기 저장. 획이 없으면 서버에서 지운다
export async function saveAnnotation(sheetId, pageId, pdfPage, data) {
  const response = await axiosInstance.put(`/api/v1/sheets/${sheetId}/pages/${pageId}/annotations/${pdfPage}`, { data })
  return response.data.data
}
