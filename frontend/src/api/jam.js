import api from './axios'

const base = (bandId) => `/api/v1/bands/${bandId}/jams`
const data = async (request) => (await request).data.data

// 잼 목록 → [{ id, title, bpm, song: { setlistItemId, title } | null, trackCount, creatorName, updatedAt,
//            favorite(밴드 전체가 같이 봄), canManage(보는 사람이 지울 수 있는지) }]
export const listJams = (bandId) => data(api.get(base(bandId)))
// 잼 → { id, title, bpm, song, creatorName, canManage,
//        tracks: [{ id, userId, nickname, part, name, type, volume, offsetMs, durationMs, canDelete, url, createdAt }] }
export const getJam = (bandId, id) => data(api.get(`${base(bandId)}/${id}`))
// jam: { title, bpm, setlistItemId }
export const createJam = (bandId, jam) => data(api.post(base(bandId), jam))
export const updateJam = (bandId, id, jam) => data(api.put(`${base(bandId)}/${id}`, jam))
export const deleteJam = (bandId, id) => api.delete(`${base(bandId)}/${id}`)
// 즐겨찾기 켜기·끄기 (밴드 멤버 누구나) → 목록 한 줄
export const setJamFavorite = (bandId, id, favorite) => data(api.patch(`${base(bandId)}/${id}/favorite`, { favorite }))

// 녹음 올리기. track: { offsetMs, volume, durationMs } → 트랙
export async function uploadJamTrack(bandId, jamId, track, file, onProgress) {
  const form = new FormData()
  form.append('track', new Blob([JSON.stringify(track)], { type: 'application/json' }))
  form.append('file', file)
  return data(api.post(`${base(bandId)}/${jamId}/tracks`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: e => e.total && onProgress?.(Math.round(e.loaded / e.total * 100)),
  }))
}
// 보낸 값(volume, offsetMs)만 바꾼다 → 트랙. 뮤트·솔로는 화면에서만 바꾼다
export const updateJamTrack = (bandId, jamId, id, changes) => data(api.patch(`${base(bandId)}/${jamId}/tracks/${id}`, changes))
export const deleteJamTrack = (bandId, jamId, id) => api.delete(`${base(bandId)}/${jamId}/tracks/${id}`)
// 트랙 파일은 로그인 토큰이 필요해서 받아서 디코딩한다
export const loadJamTrackFile = async (url) => (await api.get(url, { responseType: 'arraybuffer' })).data
