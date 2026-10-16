export const attachmentsOf = post => post?.attachments || (post?.attachment ? [{ ...post.attachment, id: post.attachment.id || 'legacy' }] : [])

export const isActiveNotice = (post, now = Date.now()) => post.notice && (!post.noticeExpiresAt || Date.parse(post.noticeExpiresAt) > now)

export function localDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function postDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} · ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function postTimeAgo(value) {
  const minutes = Math.max(0, (Date.now() - new Date(value).getTime()) / 60000)
  if (!Number.isFinite(minutes)) return ''
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${Math.floor(minutes)}분 전`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}시간 전`
  return `${Math.floor(minutes / 1440)}일 전`
}

export const mentionMembers = band => (band.members || []).filter(m => m.character?.nickname).map(m => ({ userId: m.userId, nickname: m.character.nickname }))
