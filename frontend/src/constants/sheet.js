// 악보 보관함 공용 값. 제한 값은 백엔드 SheetService·SheetFileStorage와 같게 맞춘다

// 백엔드 SheetType과 같은 값
export const SHEET_TYPE_OPTIONS = [
  { value: 'SCORE', label: '오선보' },
  { value: 'TAB', label: '타브' },
  { value: 'CHORD', label: '코드' },
  { value: 'DRUM_TAB', label: '드럼 악보' },
  { value: 'LYRICS', label: '가사' },
]

export const sheetTypeLabel = (value) => SHEET_TYPE_OPTIONS.find((o) => o.value === value)?.label || ''

// 올릴 수 있는 파일
export const SHEET_FILE_ACCEPT = 'image/png,image/jpeg,image/webp,application/pdf'
export const SHEET_FILE_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'application/pdf']
export const MAX_SHEET_PAGES = 20
// 코드 붙여넣기로 만든 코드 악보 글 길이 (#157, 백엔드 SheetRequest와 같게)
export const MAX_CHORD_TEXT = 20000
export const MAX_SHEET_FILE_MB = 10

export const isPdf = (contentType) => contentType === 'application/pdf'

// 올리기 전에 파일을 확인한다. 문제가 없으면 ''
export function sheetFileProblem(file) {
  if (!SHEET_FILE_TYPES.includes(file.type)) return `「${file.name}」은(는) 올릴 수 없는 형식이에요. (PNG·JPG·WEBP 이미지, PDF)`
  if (file.size > MAX_SHEET_FILE_MB * 1024 * 1024) return `「${file.name}」이(가) ${MAX_SHEET_FILE_MB}MB보다 커요.`
  return ''
}

export const fileSizeText = (bytes) => (bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)}MB` : `${Math.max(1, Math.round(bytes / 1024))}KB`)

// 파일 이름에서 곡 제목 짐작 (확장자·번호 떼기)
export const titleFromFileName = (name) => name.replace(/\.[^.]+$/, '').replace(/[_-]+/g, ' ').replace(/\s*\(?\d{1,2}\)?\s*$/, '').trim()
