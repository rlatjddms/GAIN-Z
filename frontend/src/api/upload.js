import axios from 'axios'
import api, { API_BASE_URL } from './axios'

// 밴드 파일을 서버를 거치지 않고 저장소(R2)에 바로 올리고 받는다 (#262)
// 1) 서버에 크기·이름을 알리고 몇 분짜리 올리기 주소를 받고 2) 그 주소에 PUT 하고 3) 글·곡·트랙을 저장할 때 uploadId를 보낸다

// 서버가 주는 주소가 "/api/…"면 서버 주소(로컬 개발), 아니면 R2 서명 주소
export const fileUrl = (url) => (url.startsWith('/api/') ? API_BASE_URL + url : url)

// 종류마다 서버 한도가 다르다 (BandAttachmentStorage)
export const UPLOAD_KIND = { ATTACHMENT: 'ATTACHMENT', SETLIST_FILE: 'SETLIST_FILE', JAM_TRACK: 'JAM_TRACK' }

// 파일 하나 → uploadId. onBytes(올린 바이트)
export async function uploadBandFile(bandId, kind, file, onBytes) {
  const response = await api.post(`/api/v1/bands/${bandId}/uploads`, {
    kind, name: file.name || 'file', size: file.size, contentType: file.type || 'application/octet-stream',
  })
  const { uploadId, url, contentType } = response.data.data
  // 로그인 토큰을 싣지 않는다 (R2는 서명 주소로 확인하고, 다른 출처라 헤더를 붙이면 CORS에 막힌다)
  await axios.put(fileUrl(url), file, {
    headers: { 'Content-Type': contentType },
    onUploadProgress: (event) => onBytes?.(event.loaded),
  })
  return uploadId
}

// 여러 파일을 차례로 올린다 → uploadId 목록. onProgress(0~100, 전체 바이트 기준)
export async function uploadBandFiles(bandId, kind, files, onProgress) {
  const total = files.reduce((sum, file) => sum + file.size, 0) || 1
  let done = 0
  const ids = []
  for (const file of files) {
    ids.push(await uploadBandFile(bandId, kind, file, (loaded) => onProgress?.(Math.min(100, Math.round((done + loaded) / total * 100)))))
    done += file.size
  }
  onProgress?.(100)
  return ids
}

// 밴드 파일 API 주소(…/attachments/{id}, …/file) → 몇 분 동안 쓸 수 있는 바로 받기 주소
// download: 저장(Content-Disposition: attachment)으로 받기
export async function bandFileLink(url, download = false) {
  const [path, query] = url.split('?')
  const params = new URLSearchParams(query || '')
  if (download) params.set('download', 'true')
  const response = await api.get(`${path}/link`, { params })
  return fileUrl(response.data.data.url)
}
