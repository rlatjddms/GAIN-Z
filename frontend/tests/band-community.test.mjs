import { test, before, after } from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { readFile, mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'
import { confirmAppDialog } from './appDialog.mjs'

// 실제 서버 대신 API 응답을 고정해 UI 동작과 실패 후 재시도를 검증한다
// 앱 확인창(components/ui/AppDialog.vue)도 <dialog>라 늘 붙어 있다. 게시글 창은 dialog.post-dialog로 찾는다
const origin = 'http://127.0.0.1:4179'
let vite
let browser
const photo = await readFile(new URL('../src/assets/stages/concert.png', import.meta.url))
const author = { userId: 1, nickname: '리듬냥', profileImageUrl: '/api/v1/users/1/profile-image?v=test' }
const attachment = { id: 'legacy', name: '합주.png', type: 'image/png', url: '/api/v1/bands/10/posts/2/attachment' }
const makePost = (id, extra = {}) => ({ id, title: `합주 이야기 ${id}`, content: '이번 주 합주에서 맞춰 볼 곡과 연습 내용을 공유해요.', notice: id === 1,
  author, createdAt: '2026-10-16T12:00:00', mine: true, commentCount: 0, attachment: id === 2 ? attachment : null, ...extra })

before(async () => {
  const cwd = fileURLToPath(new URL('../', import.meta.url))
  vite = spawn(process.execPath, [fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url)), '--host', '127.0.0.1', '--port', '4179', '--strictPort'], { cwd, stdio: 'pipe', windowsHide: true })
  let output = ''
  vite.stdout.on('data', chunk => { output += chunk })
  vite.stderr.on('data', chunk => { output += chunk })
  for (let i = 0; i < 100; i++) {
    if (vite.exitCode !== null) throw new Error(output)
    try { if ((await fetch(origin)).ok) break } catch {}
    if (i === 99) throw new Error(`개발 서버 실행 실패: ${output}`)
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  browser = await chromium.launch({ headless: true })
  await mkdir(new URL('../test-results/', import.meta.url), { recursive: true })
})
after(async () => { await browser?.close(); vite?.kill() })

async function fixture(t, options = {}) {
  const context = await browser.newContext({ viewport: { width: 1280, height: 960 }, timezoneId: options.timezoneId || 'Asia/Seoul', deviceScaleFactor: options.scale || 1 })
  t.after(() => context.close())
  await context.addInitScript(() => localStorage.setItem('accessToken', 'ui-test-token'))
  const page = await context.newPage()
  page.setDefaultTimeout(10000)
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const state = {
    posts: Array.from({ length: 15 }, (_, i) => makePost(i + 1)), comments: [
      { id: 100, parentId: null, content: '박자부터 맞춰 봐요', author, createdAt: '2026-10-16T12:10:00', mine: true, deleted: false },
      { id: 101, parentId: 100, content: '좋아요', author, createdAt: '2026-10-16T12:11:00', mine: true, deleted: false },
    ], requests: [], media: new Map(), uploads: new Map(), failSave: false, failList: false, leader: true, ...options,
  }
  await page.route('http://localhost:8080/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()
    const headers = { 'access-control-allow-origin': '*', 'access-control-allow-headers': '*', 'access-control-allow-methods': '*' }
    const respond = (data, status = 200) => route.fulfill({ status, headers, contentType: 'application/json', body: JSON.stringify(status === 200 ? { success: true, data } : { success: false, message: data }) })
    if (method === 'OPTIONS') return route.fulfill({ status: 204, headers })
    state.requests.push({ method, path })
    async function saveMultipart(existing) {
      if (state.failSave) { state.failSave = false; return respond('첨부 저장 실패', 500) }
      const form = await new Response(request.postDataBuffer(), { headers: { 'content-type': request.headers()['content-type'] } }).formData()
      const info = JSON.parse(await form.get('post').text())
      const next = existing || makePost(50, { attachment: null, attachments: [] })
      const kept = (next.attachments || (next.attachment ? [next.attachment] : [])).filter(file => info.keepAttachmentIds.includes(file.id))
      for (const file of form.getAll('files')) {
        const id = 'file-' + state.media.size
        state.media.set(id, { bytes: Buffer.from(await file.arrayBuffer()), type: file.type, name: file.name })
        kept.push({ id, name: file.name, type: file.type, url: `/api/v1/bands/10/posts/${next.id}/attachments/${id}` })
      }
      // 브라우저가 바로 올린 첨부 (#262)
      state.uploadIds = info.uploadIds || []
      for (const uploadId of info.uploadIds || []) {
        const upload = state.uploads.get(uploadId)
        if (!upload?.bytes) return respond('올린 파일을 찾지 못했어요. 파일을 다시 올려 주세요.', 400)
        const id = 'file-' + state.media.size
        state.media.set(id, upload)
        kept.push({ id, name: upload.name, type: upload.type, url: `/api/v1/bands/10/posts/${next.id}/attachments/${id}` })
      }
      Object.assign(next, { title: info.title, content: info.content, notice: info.notice ?? next.notice, noticeExpiresAt: info.notice ? info.noticeExpiresAt : null, attachments: kept, attachment: kept[0] || null })
      if (!existing) state.posts.push(next)
      return respond(next)
    }
    // 바로 올리기 (#262): 올릴 주소를 주고, 그 주소(서버 주소 모양)로 PUT 한 바이트를 기억한다
    if (path === '/api/v1/bands/10/uploads') {
      if (state.storageFull) return respond('저장 공간이 가득 차서 지금은 새 파일을 올릴 수 없어요.', 507)
      const body = request.postDataJSON()
      const uploadId = 'up-' + state.uploads.size
      state.uploads.set(uploadId, { name: body.name, type: body.contentType, kind: body.kind })
      return respond({ uploadId, url: `/api/v1/files/put-${uploadId}`, contentType: body.contentType })
    }
    if (path.startsWith('/api/v1/files/put-') && method === 'PUT') {
      state.uploads.get(path.slice('/api/v1/files/put-'.length)).bytes = request.postDataBuffer()
      return route.fulfill({ status: 200, headers })
    }
    // 받기 주소: get-은 화면에 띄우기, dl-은 저장 (Content-Disposition: attachment)
    const served = path.match(/^\/api\/v1\/files\/(get|dl)-(.+)$/)
    if (served) {
      const media = served[2] === 'legacy' ? { bytes: photo, type: 'image/png', name: attachment.name } : state.media.get(served[2])
      const disposition = served[1] === 'dl' ? { 'content-disposition': `attachment; filename*=UTF-8''${encodeURIComponent(media.name)}` } : {}
      return route.fulfill({ headers: { ...headers, ...disposition }, contentType: media.type, body: media.bytes })
    }
    if (path === '/api/v1/characters/me') return respond({ nickname: '리듬냥', instrumentType: 'GUITAR', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' })
    // 요즘 듣는 음악 (#292): state.music이 있으면 내 정보에 싣고, 지우면 비운다
    if (path === '/api/v1/users/me/profile-music' && method === 'DELETE') {
      state.music = null
      return respond({ id: 1, name: '테스트', profileImageUrl: author.profileImageUrl, profileMusic: null })
    }
    if (path === '/api/v1/users/me') return respond({ id: 1, name: '테스트', profileImageUrl: author.profileImageUrl, profileMusic: state.music || null })
    if (path.endsWith('/profile-image')) return route.fulfill({ headers, contentType: 'image/png', body: photo })
    const band = { id: 10, name: '리듬 메이커스', myPosition: state.leader ? 'LEADER' : 'MEMBER', stage: 'STUDIO', members: [{ memberId: 1, userId: 1, me: true, part: 'GUITAR', position: 'LEADER', character: { nickname: '리듬냥', instrumentType: 'GUITAR', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' } }, { memberId: 2, userId: 2, part: 'BASS', position: 'MEMBER', character: { nickname: '베이스곰', instrumentType: 'BASS', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' } }], description: '함께 합주하는 밴드' }
    if (path === '/api/v1/bands/10') return respond(band)
    // 내 밴드 탭 순서 (#266): bandOrder에 든 밴드를 그 순서로. 10번 말고는 이름만 다른 밴드
    const myBand = id => id === 10 ? band : { ...band, id, name: `밴드 ${id}`, myPosition: 'MEMBER' }
    if (path === '/api/v1/bands/me/order') {
      if (state.failOrder) return respond('순서를 저장하지 못했어요', 500)
      state.bandOrder = request.postDataJSON().bandIds
      return respond(state.bandOrder.map(myBand))
    }
    if (path === '/api/v1/bands/me') return respond((state.bandOrder || [10]).map(myBand))
    // 소셜 로그인 한 번용 코드 (#281): 'good'만 토큰으로 바꿔 준다
    if (path === '/api/v1/auth/oauth2/token') {
      state.oauthCodes = [...(state.oauthCodes || []), request.postDataJSON().code]
      if (request.postDataJSON().code !== 'good') return respond('로그인 시간이 지났어요. 다시 로그인해 주세요.', 400)
      return respond({ accessToken: 'social-access', refreshToken: 'social-refresh' })
    }
    // 실시간 알림 (#224): 보낼 이벤트가 있으면 한 번 보내고 끊는다. 없으면 실패해서 화면은 다시 붙으려고 기다린다
    if (path === '/api/v1/notifications/stream') {
      if (!state.streamEvents) return respond('연결 안 됨', 503)
      const body = state.streamEvents.map(([event, data]) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`).join('')
      state.streamEvents = null
      return route.fulfill({ status: 200, headers, contentType: 'text/event-stream', body })
    }
    if (path === '/api/v1/bands/10/posts') {
      if (method === 'GET') {
        if (state.failList) return respond('게시글 조회 실패', 503)
        const filter = url.searchParams.get('notice')
        const rows = state.posts.map(p => ({ ...p, notice: p.notice && (!p.noticeExpiresAt || Date.parse(p.noticeExpiresAt) > Date.now()) })).filter(p => (url.searchParams.get('mine') !== 'true' || p.mine) && (filter === null || p.notice === (filter === 'true'))).sort((a, b) => Number(b.notice) - Number(a.notice) || b.id - a.id)
        const number = Number(url.searchParams.get('page') || 0)
        const size = Number(url.searchParams.get('size') || 12)
        return respond({ content: rows.slice(number * size, (number + 1) * size), number, totalPages: Math.ceil(rows.length / size), totalElements: rows.length })
      }
      return saveMultipart(null)
    }
    const match = path.match(/^\/api\/v1\/bands\/10\/posts\/(\d+)(.*)$/)
    if (!match) return respond('테스트에서 처리하지 않는 경로', 404)
    const id = Number(match[1])
    const suffix = match[2]
    const post = state.posts.find(p => p.id === id)
    if (!post) return respond('글이 없습니다', 404)
    if (!suffix) {
      if (method === 'PUT') return saveMultipart(post)
      if (method === 'PATCH') Object.assign(post, request.postDataJSON())
      if (method === 'DELETE') state.posts = state.posts.filter(p => p.id !== id)
      return respond(post)
    }
    if (suffix === '/notice') { Object.assign(post, request.postDataJSON()); return respond(post) }
    if (suffix === '/attachments.zip') return route.fulfill({ headers, contentType: 'application/zip', body: Buffer.from('PK-test-archive') })
    const link = suffix.match(/^\/attachments?(?:\/(.+))?\/link$/)
    if (link) return respond({ url: `/api/v1/files/${url.searchParams.get('download') === 'true' ? 'dl' : 'get'}-${link[1] || 'legacy'}` })
    if (suffix.startsWith('/attachments/')) {
      const media = state.media.get(suffix.split('/').at(-1))
      return route.fulfill({ headers, contentType: media.type, body: media.bytes })
    }
    if (suffix === '/attachment') {
      if (method === 'PUT') {
        if (state.failUpload) { state.failUpload = false; return respond('첨부 저장 실패', 500) }
        post.attachment = { ...attachment, url: path }
        return respond(post)
      }
      if (method === 'DELETE') { post.attachment = null; return respond(post) }
      return route.fulfill({ headers, contentType: 'image/png', body: photo })
    }
    if (suffix === '/comments') {
      if (method === 'GET') {
        // 댓글 한 쪽 (#188, 백엔드 BandBoardService.commentPage와 같은 모양): 최근 원댓글부터 size개, 쪽 안은 오래된 순, 답글은 원댓글 바로 뒤
        const number = Number(url.searchParams.get('page') || 0)
        const size = Number(url.searchParams.get('size') || 20)
        const roots = state.comments.filter(c => c.parentId == null).sort((a, b) => b.id - a.id)
        const content = roots.slice(number * size, (number + 1) * size).reverse()
          .flatMap(root => [root, ...state.comments.filter(c => c.parentId === root.id).sort((a, b) => a.id - b.id)])
        return respond({ content, number, totalPages: Math.ceil(roots.length / size), totalElements: roots.length, commentCount: state.comments.filter(c => !c.deleted).length })
      }
      if (state.failComment) return respond('댓글을 저장하지 못했어요', 500)
      const comment = { id: 102 + state.comments.length, ...request.postDataJSON(), author, mine: true, deleted: false, createdAt: '2026-10-16T12:20:00' }
      state.comments.push(comment)
      return respond(comment)
    }
    const commentId = Number(suffix.split('/').at(-1))
    const comment = state.comments.find(c => c.id === commentId)
    if (method === 'PATCH') comment.content = request.postDataJSON().content
    if (method === 'DELETE') {
      if (state.comments.some(c => c.parentId === commentId)) Object.assign(comment, { content: '삭제된 댓글', deleted: true, mine: false, author: null })
      else state.comments = state.comments.filter(c => c.id !== commentId)
    }
    return respond(comment)
  })
  return { page, state, errors }
}

const screenshot = (page, name) => page.screenshot({ path: fileURLToPath(new URL(`../test-results/${name}.png`, import.meta.url)), animations: 'disabled' })

async function sampleVideo(page) {
  const bytes = await page.evaluate(async () => {
    const canvas = document.createElement('canvas')
    canvas.width = 480; canvas.height = 270
    const ctx = canvas.getContext('2d')
    const draw = () => {
      ctx.fillStyle = '#243865'; ctx.fillRect(0, 0, 480, 270)
      ctx.fillStyle = '#bed0ff'; ctx.font = 'bold 30px sans-serif'; ctx.fillText('SUNDAY REHEARSAL', 44, 120)
      ctx.fillStyle = '#7f9bd4'; ctx.font = '18px sans-serif'; ctx.fillText('Take 03 · 00:18', 44, 158)
    }
    draw()
    const stream = canvas.captureStream(12)
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' })
    const chunks = []
    recorder.ondataavailable = event => chunks.push(event.data)
    const done = new Promise(resolve => { recorder.onstop = resolve })
    recorder.start()
    const timer = setInterval(draw, 70)
    await new Promise(resolve => setTimeout(resolve, 350))
    recorder.stop(); await done; clearInterval(timer)
    stream.getTracks().forEach(track => track.stop())
    return Array.from(new Uint8Array(await new Blob(chunks, { type: 'video/webm' }).arrayBuffer()))
  })
  return Buffer.from(bytes)
}

test('홈에서 더 보기와 상세 열기, 상단바 유지, 작성자 우선, 닫기 복귀', async t => {
  const { page, errors } = await fixture(t, { posts: Array.from({ length: 22 }, (_, i) => makePost(i + 1)) })
  await page.goto(`${origin}/home?band=10`)
  await page.locator('.community .post').first().waitFor()
  // 프로필 사진이 다 뜬 뒤 상단바를 찍는다. 사진이 늦게 오면 기본 아이콘 → 사진으로 바뀌어 비교가 틀어진다 (CI처럼 느린 곳에서 #248)
  await page.locator('.main-header .profile-avatar img').waitFor()
  const header = await page.locator('.main-header').innerHTML()
  assert.equal(await page.getByText('전체 보기', { exact: true }).count(), 0)
  // 더 보기 대신 페이지 번호 (#188): 공지 1개를 뺀 21개를 12개씩
  await page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 12)
  await page.getByRole('navigation', { name: '페이지' }).getByRole('button', { name: '2', exact: true }).click()
  await page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 9)
  await page.locator('.community .post').filter({ has: page.getByRole('heading', { name: '합주 이야기 2', exact: true }) }).click()
  await page.locator('.post-detail .post-title').waitFor()
  assert.equal(await page.locator('.main-header').innerHTML(), header)
  assert.equal(await page.locator('.main-header').count(), 1)
  const authorBox = await page.locator('.post-detail .author-row').boundingBox()
  const titleBox = await page.locator('.post-detail .post-title').boundingBox()
  assert.ok(authorBox.y + authorBox.height < titleBox.y)
  assert.equal(await page.getByRole('button', { name: '공지로 지정', exact: true }).count(), 0)
  await page.locator('.post-detail .media-frame img').waitFor()
  await screenshot(page, 'community-detail-desktop')
  await page.getByRole('button', { name: '게시글 창 닫기' }).click()
  await page.waitForFunction(() => !document.querySelector('dialog.post-dialog'))
  // 닫으면 보던 쪽(2쪽) 그대로
  assert.equal(await page.locator('.community .post').count(), 9)
  assert.equal(await page.evaluate(() => document.body.style.overflow), '')
  assert.deepEqual(errors, [])
})

test('작성 중 사진·영상 미리보기, 공지와 여러 첨부 저장 및 수정', async t => {
  const { page, state, errors } = await fixture(t)
  await page.goto(`${origin}/home?band=10&compose=1`)
  await page.getByLabel('제목', { exact: true }).fill('이번 주 합주, 이렇게 맞춰 봐요')
  await page.getByLabel('내용', { exact: true }).fill('지난 합주 사진과 영상을 함께 올려요.\n후렴으로 넘어가는 부분을 들어 보고 의견 남겨 주세요!')
  const video = await sampleVideo(page)
  await page.locator('input[type=file]').setInputFiles([
    { name: '합주실.png', mimeType: 'image/png', buffer: photo },
    { name: '합주영상.webm', mimeType: 'video/webm', buffer: video },
  ])
  await page.waitForFunction(() => document.querySelector('.local-preview video')?.readyState >= 1)
  await page.locator('.local-preview video').evaluate(async video => { await video.play(); video.pause() })
  assert.equal(await page.locator('.local-preview img').count(), 1)
  assert.equal(await page.locator('.local-preview video').count(), 1)
  await page.getByRole('switch', { name: '공지로 등록' }).check()
  await page.locator('.dialog-scroll').evaluate(e => { e.scrollTop = 0 })
  await screenshot(page, 'community-editor-desktop')
  await page.setViewportSize({ width: 390, height: 844 })
  assert.equal(await page.locator('dialog.post-dialog').evaluate(e => e.scrollWidth <= e.clientWidth), true)
  await screenshot(page, 'community-editor-mobile')
  await page.locator('.dialog-scroll').evaluate(e => { e.scrollTop = e.scrollHeight })
  await screenshot(page, 'community-editor-mobile-files')
  await page.setViewportSize({ width: 1280, height: 960 })
  await page.getByRole('button', { name: '등록하기', exact: true }).click()
  await page.waitForURL(/post=50/)
  await page.locator('.post-title').waitFor()
  const created = state.posts.find(p => p.id === 50)
  assert.equal(created.notice, true)
  assert.equal(created.attachments.length, 2)
  // 첨부는 저장소에 바로 올리고(서버로 multipart 파일을 보내지 않음) uploadId로 글을 저장한다 (#262)
  assert.deepEqual([...state.uploads.values()].map(u => [u.kind, u.name, u.bytes.length > 0]), [['ATTACHMENT', '합주실.png', true], ['ATTACHMENT', '합주영상.webm', true]])
  assert.deepEqual(state.uploadIds, ['up-0', 'up-1'])
  // 화면의 사진·영상은 받기 주소(…/link)로 저장소에서 바로 불러온다
  assert.match(await page.locator('.media-gallery video').getAttribute('src'), /\/api\/v1\/files\/get-file-/)
  await page.waitForFunction(() => document.querySelector('.media-gallery video')?.readyState >= 1)
  await page.locator('.media-gallery video').evaluate(async video => { await video.play(); video.pause() })
  await screenshot(page, 'community-detail-media')
  // #319: 재생 중에 크게 보기를 누르면 글 안의 영상은 멈추고, 큰 화면이 이어서 재생한다 (둘이 같이 재생되지 않음)
  await page.locator('.media-gallery video').evaluate(video => { video.muted = true; return video.play() })
  await page.getByRole('button', { name: '크게 보기', exact: true }).click()
  await page.waitForFunction(() => { const video = document.querySelector('.media-viewer video'); return video && !video.paused })
  assert.equal(await page.locator('.media-gallery video').evaluate(video => video.paused), true)
  // #321: 글에서 켠 음소거가 큰 화면에도 그대로, 큰 화면에서 바꾼 소리 설정은 닫을 때 글 안의 영상에 돌려준다
  assert.equal(await page.locator('.media-viewer video').evaluate(video => video.muted), true)
  await page.locator('.media-viewer video').evaluate(video => { video.muted = false; video.volume = 0.4 })
  await page.getByRole('button', { name: '미디어 닫기' }).click()
  await page.locator('.media-viewer').waitFor({ state: 'detached' })
  assert.deepEqual(await page.locator('.media-gallery video').evaluate(video => [video.paused, video.muted, video.volume]), [true, false, 0.4])
  await page.locator('.author-row summary').click()
  await page.getByRole('button', { name: '글 수정', exact: true }).click()
  await page.getByRole('button', { name: '합주실.png 첨부 삭제' }).click()
  await page.getByRole('switch', { name: '공지로 등록' }).uncheck()
  await page.getByLabel('제목', { exact: true }).fill('합주 영상만 남겨 둡니다')
  await page.getByRole('button', { name: '저장하기', exact: true }).click()
  await page.locator('.post-title').filter({ hasText: '합주 영상만 남겨 둡니다' }).waitFor()
  assert.equal(created.attachments.length, 1)
  assert.equal(created.attachments[0].type, 'video/webm')
  assert.equal(created.notice, false)
  await page.setViewportSize({ width: 390, height: 844 })
  await screenshot(page, 'community-detail-mobile')
  assert.deepEqual(errors, [])
})

test('저장 실패 시 입력과 첨부 유지, 재시도는 완성된 글 하나만 생성', async t => {
  const { page, state, errors } = await fixture(t, { failSave: true })
  await page.goto(`${origin}/home?band=10&compose=1`)
  await page.getByLabel('제목', { exact: true }).fill('새 합주 계획')
  await page.getByLabel('내용', { exact: true }).fill('일요일에 만나요')
  await page.locator('input[type=file]').setInputFiles({ name: '합주.png', mimeType: 'image/png', buffer: photo })
  await page.getByRole('button', { name: '등록하기', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: '첨부 저장 실패' }).waitFor()
  assert.equal(state.posts.filter(p => p.id === 50).length, 0)
  assert.equal(await page.getByLabel('제목', { exact: true }).inputValue(), '새 합주 계획')
  assert.equal(await page.locator('.local-preview img').count(), 1)
  await page.getByRole('button', { name: '등록하기', exact: true }).click()
  await page.waitForURL(/post=50/)
  assert.equal(state.posts.filter(p => p.id === 50).length, 1)
  assert.equal(state.posts.find(p => p.id === 50).attachments.length, 1)
  assert.deepEqual(errors, [])
})

test('댓글·대댓글 작성과 수정, 삭제된 부모 아래 대댓글 보존', async t => {
  const { page, state, errors } = await fixture(t)
  await page.goto(`${origin}/home?band=10&post=2`)
  await page.locator('.comment').first().waitFor()
  await page.locator('.comment').first().getByRole('button', { name: '답글 달기' }).click()
  await page.getByRole('textbox', { name: '답글', exact: true }).fill('템포 100부터 시작해요')
  await page.getByRole('button', { name: '등록', exact: true }).click()
  await page.locator('.comment-content').filter({ hasText: '템포 100부터 시작해요' }).waitFor()
  assert.equal(state.comments.at(-1).parentId, 100)
  await page.locator('.comment').last().locator('summary').click()
  await page.locator('.comment').last().getByRole('button', { name: '수정', exact: true }).click()
  await page.getByRole('textbox', { name: '댓글 수정' }).fill('템포 110부터 시작해요')
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await page.getByText('템포 110부터 시작해요', { exact: true }).waitFor()
  await page.locator('.comment').first().locator('summary').click()
  await page.locator('.comment').first().getByRole('button', { name: '삭제', exact: true }).click()
  await confirmAppDialog(page, '삭제')
  await page.getByText('삭제된 댓글이에요.', { exact: true }).waitFor()
  assert.equal(await page.getByText('템포 110부터 시작해요', { exact: true }).count(), 1)
  assert.deepEqual(errors, [])
})

test('권한에 따라 공지 설정과 편집 범위 제한', async t => {
  const { page, errors } = await fixture(t, { leader: false, posts: [makePost(2, { mine: false })] })
  await page.goto(`${origin}/home?band=10&post=2`)
  await page.locator('.post-title').waitFor()
  assert.equal(await page.locator('.author-row summary').count(), 0)
  await page.getByRole('button', { name: '게시글 창 닫기' }).click()
  await page.getByRole('button', { name: '글쓰기', exact: true }).click()
  await page.getByLabel('제목', { exact: true }).waitFor()
  assert.equal(await page.getByRole('switch', { name: '공지로 등록' }).count(), 0)
  assert.deepEqual(errors, [])
})

test('리더는 타인 글의 작성 화면에서 공지 설정만 변경', async t => {
  const { page, state, errors } = await fixture(t, { posts: [makePost(2, { mine: false })] })
  await page.goto(`${origin}/home?band=10&post=2`)
  await page.locator('.author-row summary').click()
  await page.getByRole('button', { name: '공지 설정', exact: true }).click()
  await page.getByLabel('제목', { exact: true }).waitFor()
  assert.equal(await page.getByLabel('제목', { exact: true }).getAttribute('readonly'), '')
  assert.equal(await page.locator('input[type=file]').count(), 0)
  await page.getByRole('switch', { name: '공지로 등록' }).check()
  await page.getByRole('button', { name: '저장하기' }).click()
  await page.locator('.post-title').waitFor()
  assert.equal(state.posts[0].notice, true)
  assert.equal(state.requests.some(r => r.method === 'PUT'), false)
  assert.deepEqual(errors, [])
})

test('첨부 개수 제한과 작성 중 닫기 확인, Escape와 스크롤 복구', async t => {
  const { page, errors } = await fixture(t)
  await page.goto(`${origin}/home?band=10&compose=1`)
  await page.getByLabel('제목', { exact: true }).fill('작성 중인 글')
  await page.locator('input[type=file]').setInputFiles(Array.from({ length: 9 }, (_, i) => ({ name: `${i}.png`, mimeType: 'image/png', buffer: photo })))
  await page.getByRole('alert').filter({ hasText: '최대 8개' }).waitFor()
  assert.equal(await page.locator('.file-tile').count(), 0)
  await page.keyboard.press('Escape')
  await confirmAppDialog(page, '취소')
  assert.equal(await page.locator('dialog.post-dialog[open]').count(), 1)
  await page.keyboard.press('Escape')
  await confirmAppDialog(page, '닫기')
  await page.waitForFunction(() => !document.querySelector('dialog.post-dialog'))
  assert.equal(await page.evaluate(() => document.body.style.overflow), '')
  assert.deepEqual(errors, [])
})

test('기존 게시판 주소는 홈으로 연결하고 목록 오류를 재시도', async t => {
  const { page, state, errors } = await fixture(t, { failList: true, posts: [] })
  await page.goto(`${origin}/bands/10/community`)
  await page.waitForURL(/home\?band=10/)
  await page.getByRole('alert').waitFor()
  state.failList = false
  await page.getByRole('button', { name: '다시 불러오기', exact: true }).click()
  await page.locator('.community .empty').waitFor()
  assert.deepEqual(errors, [])
})

test('내 글 필터는 전체 페이지에 적용하고 카드에서 바로 수정·삭제', async t => {
  const { page, state, errors } = await fixture(t, { posts: Array.from({ length: 20 }, (_, i) => makePost(i + 1, { mine: i < 9, notice: false })) })
  await page.goto(`${origin}/home?band=10`)
  await page.locator('.community .post').first().waitFor()
  // 글 관리 메뉴는 내 글에만 붙는다. 1쪽(20~9번)에서 내 글은 9번 하나
  assert.deepEqual(await page.locator('.card-menu').evaluateAll(menus => menus.map(m => m.closest('.post')?.querySelector('h3, h4, .post-title')?.textContent.trim())), ['합주 이야기 9'])
  await page.getByRole('button', { name: '내가 쓴 글', exact: true }).click()
  await page.waitForFunction(() => document.querySelector('.post-total')?.textContent.trim() === '9')
  // 한 쪽 12개라 내 글 9개가 한 번에 보인다 (#188)
  await page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 9)
  await page.getByLabel('합주 이야기 9 글 관리').click()
  const menuStyle = await page.getByLabel('합주 이야기 9 글 관리').evaluate(el => {
    const style = getComputedStyle(el)
    return { border: style.borderWidth, background: style.backgroundColor, outline: style.outlineWidth, shadow: style.boxShadow }
  })
  assert.deepEqual(menuStyle, { border: '0px', background: 'rgba(0, 0, 0, 0)', outline: '0px', shadow: 'none' })
  await page.getByRole('button', { name: '글 수정', exact: true }).click()
  await page.getByLabel('제목', { exact: true }).fill('내 글 수정 완료')
  await page.getByRole('button', { name: '저장하기' }).click()
  await page.locator('.post-title').filter({ hasText: '내 글 수정 완료' }).waitFor()
  await page.getByRole('button', { name: '게시글 창 닫기' }).click()
  assert.match(page.url(), /mine=1/)
  await page.getByLabel('내 글 수정 완료 글 관리').click()
  await page.getByRole('button', { name: '글 삭제', exact: true }).click()
  await confirmAppDialog(page, '삭제')
  await page.waitForFunction(() => document.querySelector('.post-total')?.textContent.trim() === '8')
  assert.equal(state.posts.some(p => p.id === 9), false)
  await screenshot(page, 'community-my-posts')
  assert.deepEqual(errors, [])
})

test('사진 확대·키보드 넘기기·개별/ZIP 저장·닫기와 모바일 표시', async t => {
  const second = { ...attachment, id: 'second', name: '두번째.png', url: '/api/v1/bands/10/posts/2/attachments/second' }
  const media = new Map([['second', { bytes: photo, type: 'image/png', name: '두번째.png' }]])
  const { page, errors } = await fixture(t, { posts: [makePost(2, { attachments: [attachment, second] })], media })
  await page.goto(`${origin}/home?band=10&post=2`)
  await page.getByRole('button', { name: '합주.png 크게 보기' }).click()
  await page.locator('.media-viewer img').waitFor()
  assert.equal(await page.locator('.media-gallery .file-row').count(), 0)
  await page.getByRole('button', { name: '이미지 확대' }).click()
  assert.equal(await page.locator('.media-viewer .stage').evaluate(e => e.classList.contains('zoomed')), true)
  await page.getByRole('button', { name: '이미지 축소' }).click()
  await page.keyboard.press('ArrowRight')
  await page.locator('.media-viewer header strong').filter({ hasText: '두번째.png' }).waitFor()
  await page.locator('.media-viewer img').waitFor()
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: '저장', exact: true }).click()
  assert.equal((await download).suggestedFilename(), '두번째.png')
  const archive = page.waitForEvent('download')
  await page.getByRole('button', { name: '전체 ZIP 저장' }).click()
  assert.equal((await archive).suggestedFilename(), 'community-2.zip')
  await screenshot(page, 'community-media-viewer')
  await page.setViewportSize({ width: 390, height: 844 })
  assert.equal(await page.locator('.media-viewer').evaluate(e => e.scrollWidth <= e.clientWidth), true)
  await screenshot(page, 'community-media-viewer-mobile')
  await page.keyboard.press('Escape')
  assert.equal(await page.locator('.media-viewer').count(), 0)
  assert.equal(await page.locator('.post-dialog[open]').count(), 1)
  assert.deepEqual(errors, [])
})

test('한글 조합 첫 글자 등록, 답글 접기·태그 및 멤버 자동완성', async t => {
  const { page, state, errors } = await fixture(t)
  await page.goto(`${origin}/home?band=10&post=2`)
  await page.locator('.comment').first().waitFor()
  assert.equal(await page.locator('.comment.reply').count(), 0)
  await page.getByRole('button', { name: '답글 1개 보기' }).click()
  assert.equal(await page.locator('.comment.reply').count(), 1)
  await page.getByRole('button', { name: '답글 접기' }).click()
  const input = page.getByRole('textbox', { name: '댓글', exact: true })
  await input.evaluate(el => {
    el.dispatchEvent(new CompositionEvent('compositionstart', { bubbles: true }))
    el.value = '가'
    el.dispatchEvent(new InputEvent('input', { bubbles: true, data: '가', isComposing: true }))
  })
  assert.equal(await page.getByRole('button', { name: '등록', exact: true }).isEnabled(), true)
  await input.dispatchEvent('compositionend', { data: '가' })
  await page.getByRole('button', { name: '등록', exact: true }).click()
  await page.getByText('가', { exact: true }).waitFor()
  assert.equal(state.comments.at(-1).content, '가')
  await input.fill('@베')
  await page.getByRole('option', { name: /베이스곰/ }).click()
  assert.equal(await input.inputValue(), '@베이스곰 ')
  await input.press('End')
  await input.pressSequentially('함께 해요')
  await page.getByRole('button', { name: '등록', exact: true }).click()
  await page.locator('.comment-content .mention').filter({ hasText: '@베이스곰' }).waitFor()
  await page.locator('.comment').first().getByRole('button', { name: '답글 달기' }).click()
  await page.locator('.reply-target').filter({ hasText: '@리듬냥' }).waitFor()
  await page.getByRole('textbox', { name: '답글', exact: true }).fill('좋아요')
  await page.getByRole('button', { name: '등록', exact: true }).click()
  await page.locator('.comment.reply .mention').filter({ hasText: '@리듬냥' }).waitFor()
  assert.equal(state.comments.at(-1).parentId, 100)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.locator('.comments-section').scrollIntoViewIfNeeded()
  await screenshot(page, 'community-comments-mobile')
  assert.deepEqual(errors, [])
})

test('여러 공지를 모두 상단에 표시하고 일반 카드와 분리', async t => {
  const posts = Array.from({ length: 24 }, (_, i) => makePost(i + 1, { notice: i < 22 }))
  const { page, errors } = await fixture(t, { posts })
  await page.goto(`${origin}/home?band=10`)
  await page.waitForFunction(() => document.querySelectorAll('.community .notice').length === 22)
  assert.equal(await page.locator('.community .post').count(), 2)
  assert.equal(await page.locator('.small-notice').count(), 0)
  assert.equal(await page.getByRole('button', { name: /이야기 더 보기/ }).count(), 0)
  assert.equal(await page.locator('.post-total').textContent(), '24')
  assert.deepEqual(errors, [])
})

test('공지 종료일 저장·편집·해제, 시간대 변환과 모바일 기간 입력', async t => {
  const { page, state, errors } = await fixture(t)
  await page.goto(`${origin}/home?band=10&compose=1`)
  await page.getByLabel('제목', { exact: true }).fill('이번 주 합주 일정 안내')
  await page.getByLabel('내용', { exact: true }).fill('합주 시작 전까지 확인해 주세요.')
  await page.getByRole('switch', { name: '공지로 등록' }).check()
  await page.getByRole('radio', { name: '종료일 지정', exact: true }).check()
  assert.equal(await page.getByRole('button', { name: '등록하기', exact: true }).isDisabled(), true)
  await page.getByLabel('공지 종료 일시').fill('2000-01-01T09:30')
  assert.equal(await page.getByRole('button', { name: '등록하기', exact: true }).isDisabled(), true)
  await page.getByLabel('공지 종료 일시').fill('2030-01-02T19:30')
  await page.setViewportSize({ width: 390, height: 844 })
  await page.locator('.notice-schedule').scrollIntoViewIfNeeded()
  assert.equal(await page.locator('.post-dialog').evaluate(e => e.scrollWidth <= e.clientWidth), true)
  await screenshot(page, 'community-notice-period-mobile')
  await page.getByRole('button', { name: '등록하기', exact: true }).click()
  await page.waitForURL(/post=50/)
  await page.locator('.post-title').waitFor()
  const post = state.posts.find(p => p.id === 50)
  assert.equal(post.noticeExpiresAt, '2030-01-02T10:30:00.000Z')
  await page.locator('.notice-tag').filter({ hasText: '2030.01.02 · 19:30까지' }).waitFor()
  await page.locator('.author-row summary').click()
  await page.getByRole('button', { name: '글 수정', exact: true }).click()
  assert.equal(await page.getByLabel('공지 종료 일시').inputValue(), '2030-01-02T19:30')
  await page.getByRole('radio', { name: '무기한', exact: true }).check()
  await page.getByRole('button', { name: '저장하기', exact: true }).click()
  await page.locator('.post-title').waitFor()
  assert.equal(post.notice, true)
  assert.equal(post.noticeExpiresAt, null)
  await page.locator('.author-row summary').click()
  await page.getByRole('button', { name: '글 수정', exact: true }).click()
  await page.getByRole('switch', { name: '공지로 등록' }).uncheck()
  await page.getByRole('button', { name: '저장하기', exact: true }).click()
  await page.locator('.post-title').waitFor()
  assert.equal(post.notice, false)
  assert.equal(post.noticeExpiresAt, null)
  assert.deepEqual(errors, [])
})

test('공지 두 개를 세로로 고정하고 종료 시각에 자동으로 일반 글로 이동', async t => {
  const { page, state, errors } = await fixture(t, { posts: [
    makePost(1, { notice: true, title: '합주실 이용 안내' }),
    makePost(2, { notice: true, title: '이번 주 일정', noticeExpiresAt: new Date(Date.now() + 4000).toISOString() }),
    makePost(3, { notice: false }),
  ] })
  await page.goto(`${origin}/home?band=10`)
  await page.waitForFunction(() => document.querySelectorAll('.community .notice').length === 2)
  const boxes = await page.locator('.notice').evaluateAll(elements => elements.map(e => ({ top: e.getBoundingClientRect().top, bottom: e.getBoundingClientRect().bottom })))
  assert.ok(boxes[1].top >= boxes[0].bottom)
  await page.locator('.community').scrollIntoViewIfNeeded()
  await screenshot(page, 'community-pinned-notices')
  await page.waitForFunction(() => document.querySelectorAll('.community .notice').length === 1 && document.querySelectorAll('.community .post').length === 2)
  assert.equal(await page.locator('.community .post').filter({ hasText: '이번 주 일정' }).count(), 1)
  assert.equal(await page.locator('.post-total').textContent(), '3')
  assert.equal(state.posts.find(p => p.id === 2).notice, true)
  assert.deepEqual(errors, [])
})

test('앱으로 돌아오거나 연결이 다시 붙고 새 공지 알림이 오면 글 목록을 새로고침 없이 다시 불러온다', async t => {
  const { page, state, errors } = await fixture(t, { posts: [makePost(2), makePost(3)] })
  await page.goto(`${origin}/home?band=10`)
  await page.locator('.community .post').filter({ hasText: '합주 이야기 3' }).waitFor()

  // 다른 멤버가 글을 썼다 → 잠깐(15초 안) 다녀오면 그대로, 오래 있다 돌아오면 다시 불러온다
  state.posts.push(makePost(4, { title: '다른 멤버의 새 글', mine: false }))
  const away = ms => page.evaluate(async ms => {
    const setState = s => { Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => s }); document.dispatchEvent(new Event('visibilitychange')) }
    const realNow = Date.now
    setState('hidden')
    Date.now = () => realNow() + ms
    setState('visible')
  }, ms)
  await away(3000)
  await page.waitForTimeout(500)
  assert.equal(await page.getByText('다른 멤버의 새 글').count(), 0)
  await away(20000)
  await page.getByText('다른 멤버의 새 글').waitFor()

  // 실시간 연결이 다시 붙고(그동안 놓친 것) 새 공지 알림이 오면 바로 다시 불러온다
  state.posts.push(makePost(5, { title: '이번 주 합주 공지', notice: true, mine: false }))
  state.streamEvents = [['unread', { count: 1 }], ['notification', { id: 900, type: 'NOTICE', title: '새 공지', content: '이번 주 합주 공지', read: false, createdAt: '2026-10-16T12:30:00', redirectUrl: '/home?band=10' }]]
  await page.getByText('이번 주 합주 공지', { exact: true }).first().waitFor({ timeout: 15000 })
  assert.deepEqual(errors, [])
})

test('켜 둔 화면에서 다른 밴드 신호는 넘기고, 같은 밴드의 글이 바뀌었다는 신호면 바로 다시 불러온다', async t => {
  const { page, state, errors } = await fixture(t, { posts: [makePost(2), makePost(3)] })
  await page.goto(`${origin}/home?band=10`)
  await page.locator('.community .post').filter({ hasText: '합주 이야기 3' }).waitFor()
  const listCalls = () => state.requests.filter(r => r.method === 'GET' && r.path === '/api/v1/bands/10/posts').length

  // 알림이 없는 일반 글 (#259): 다른 밴드 신호에는 다시 부르지 않는다
  // (unread 없이 change만 보내서, 다시 붙을 때 놓친 것을 불러오는 #224 동작과 섞이지 않게 한다)
  state.posts.push(makePost(4, { title: '알림 없는 일반 글', mine: false }))
  const before = listCalls()
  state.streamEvents = [['change', { topic: 'BAND_BOARD', bandId: 99 }]]
  // 화면이 신호를 받아 갈 때까지 (연결 실패 뒤 다시 붙는 데 몇 초 걸린다)
  for (let i = 0; state.streamEvents && i < 300; i++) await new Promise(resolve => setTimeout(resolve, 100))
  assert.equal(state.streamEvents, null)
  await page.waitForTimeout(1000)
  assert.equal(listCalls(), before)
  assert.equal(await page.getByText('알림 없는 일반 글').count(), 0)

  // 같은 밴드 신호 → 바로. 연달아 온 신호는 한 번만 불러온다 (공지 목록 + 일반 목록 = 요청 2개)
  state.streamEvents = [['change', { topic: 'BAND_BOARD', bandId: 10, postId: 4 }], ['change', { topic: 'BAND_BOARD', bandId: 10, postId: 4 }]]
  await page.getByText('알림 없는 일반 글').waitFor({ timeout: 15000 })
  await page.waitForTimeout(800)
  assert.equal(listCalls() - before, 2)
  assert.deepEqual(errors, [])
})

test('홈 밴드 탭을 끌어서 순서를 바꾸고, 휴대폰은 꾹 눌러서 끌고, 저장 실패면 되돌린다', async t => {
  const { page, state, errors } = await fixture(t, { posts: [makePost(2)], bandOrder: [10, 11, 12] })
  await page.goto(`${origin}/home?band=10`)
  const tabs = page.locator('.band-tabs-left [data-tab-id]')
  await page.waitForFunction(() => document.querySelectorAll('.band-tabs-left [data-tab-id]').length === 3)
  const names = () => tabs.allTextContents().then(list => list.map(name => name.trim()))
  assert.deepEqual(await names(), ['리듬 메이커스', '밴드 11', '밴드 12'])

  // 마우스: 맨 오른쪽 탭을 맨 왼쪽으로 끌기. 고른 밴드는 그대로
  const last = await tabs.nth(2).boundingBox()
  const first = await tabs.nth(0).boundingBox()
  // 고른(흰) 탭을 잡았을 때 모양: 아래 무대에 회색 그림자가 번지지 않게 (눈으로 보려고 저장). 조금만 움직였다 놓으면 순서는 그대로
  await page.mouse.move(first.x + first.width / 2, first.y + first.height / 2)
  await page.mouse.down()
  await page.mouse.move(first.x + first.width / 2 + 24, first.y + first.height / 2)
  await page.locator('.band-tabs-left.dragging').waitFor()
  assert.equal(await page.locator('.band-tabs-left button.held').evaluate(el => getComputedStyle(el).borderBottomColor), 'rgb(255, 255, 255)')
  await page.screenshot({ path: fileURLToPath(new URL('../test-results/band-tab-held.png', import.meta.url)),
    clip: { x: first.x - 10, y: first.y - 10, width: first.width + 60, height: first.height + 30 }, scale: "device" })
  await page.mouse.up()
  assert.deepEqual(state.bandOrder, [10, 11, 12])
  await page.mouse.move(last.x + last.width / 2, last.y + last.height / 2)
  await page.mouse.down()
  for (let i = 1; i <= 10; i++) await page.mouse.move(last.x + last.width / 2 - (last.x - first.x + 10) * i / 10, last.y + last.height / 2)
  await page.mouse.up()
  await page.waitForFunction(() => document.querySelector('.band-tabs-left [data-tab-id]').textContent.includes('밴드 12'))
  assert.deepEqual(state.bandOrder, [12, 10, 11])
  assert.deepEqual(await names(), ['밴드 12', '리듬 메이커스', '밴드 11'])
  assert.equal((await page.locator('.band-tabs-left button.active').textContent()).trim(), '리듬 메이커스')

  // 그냥 누르면 지금처럼 탭 고르기 (끌기로 치지 않음)
  const requestsBefore = state.requests.filter(r => r.path === '/api/v1/bands/me/order').length
  await tabs.nth(1).click()
  assert.equal(state.requests.filter(r => r.path === '/api/v1/bands/me/order').length, requestsBefore)

  // 저장에 실패하면 원래 순서로 되돌리고 알려 준다
  state.failOrder = true
  const a = await tabs.nth(0).boundingBox()
  const b = await tabs.nth(2).boundingBox()
  await page.mouse.move(a.x + a.width / 2, a.y + a.height / 2)
  await page.mouse.down()
  for (let i = 1; i <= 10; i++) await page.mouse.move(a.x + a.width / 2 + (b.x + b.width - a.x) * i / 10, a.y + a.height / 2)
  await page.mouse.up()
  await page.getByText('순서를 저장하지 못했어요').waitFor()
  assert.deepEqual(await names(), ['밴드 12', '리듬 메이커스', '밴드 11'])
  state.failOrder = false

  // 휴대폰: 꾹 누르고(0.35초) 끌기. 바로 밀면 끌기가 아니다
  const cdp = await page.context().newCDPSession(page)
  const touch = (type, x, y) => cdp.send('Input.dispatchTouchEvent', { type, touchPoints: type === 'touchEnd' ? [] : [{ x, y }] })
  const m = await tabs.nth(1).boundingBox()
  const z = await tabs.nth(0).boundingBox()
  const y = m.y + m.height / 2
  await touch('touchStart', m.x + m.width / 2, y)
  await touch('touchMove', z.x + 4, y)
  await touch('touchEnd')
  await page.waitForTimeout(300)
  assert.deepEqual(await names(), ['밴드 12', '리듬 메이커스', '밴드 11'])
  await touch('touchStart', m.x + m.width / 2, y)
  await page.waitForTimeout(500)
  for (let i = 1; i <= 8; i++) await touch('touchMove', m.x + m.width / 2 - (m.x + m.width / 2 - z.x + 6) * i / 8, y)
  await touch('touchEnd')
  await page.waitForFunction(() => document.querySelector('.band-tabs-left [data-tab-id]').textContent.includes('리듬'))
  assert.deepEqual(state.bandOrder, [10, 12, 11])
  assert.deepEqual(errors, [])
})

test('오류 알림은 열린 게시글 창 위에 떠서 누를 수 있고, 쓰던 댓글은 그대로, 바로 보이는 동작은 성공 알림 없음', async t => {
  const { page, state, errors } = await fixture(t, { failComment: true })
  await page.goto(`${origin}/home?band=10&post=2`)
  await page.locator('.comment').first().waitFor()
  const box = page.getByRole('textbox', { name: '댓글', exact: true })
  await box.fill('다음 주에 봐요')
  await page.getByRole('button', { name: '등록', exact: true }).click()
  // 공통 알림 (#267): 빨간 알림이 열린 게시글 창(<dialog>)보다 위에 보인다
  const toast = page.locator('.toast.error')
  await toast.filter({ hasText: '댓글을 저장하지 못했어요' }).waitFor()
  const top = await toast.evaluate(el => {
    const r = el.getBoundingClientRect()
    return el.contains(document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2))
  })
  assert.equal(top, true)
  assert.equal(await box.inputValue(), '다음 주에 봐요')
  await screenshot(page, 'toast-error-desktop')
  await page.setViewportSize({ width: 390, height: 844 })
  await screenshot(page, 'toast-error-mobile')
  await page.setViewportSize({ width: 1280, height: 960 })
  // × 로 닫을 수 있고, 다시 실패하면 다시 뜬다
  await toast.getByRole('button', { name: '알림 닫기' }).click()
  await toast.waitFor({ state: 'detached' })
  state.failComment = false
  await page.getByRole('button', { name: '등록', exact: true }).click()
  await page.locator('.comment-content').filter({ hasText: '다음 주에 봐요' }).waitFor()
  // 결과가 화면에 바로 보이는 동작(댓글 달기)은 성공 알림을 띄우지 않는다
  assert.equal(await page.locator('.toast').count(), 0)
  assert.deepEqual(errors, [])
})

test('소셜 로그인은 주소의 한 번용 코드를 토큰으로 바꾸고, 토큰을 주소에 남기지 않는다', async t => {
  const { page, state, errors } = await fixture(t)
  await page.goto(`${origin}/oauth2/redirect?code=good`)
  await page.waitForURL(/\/home$/)
  assert.deepEqual(state.oauthCodes, ['good'])
  assert.equal(await page.evaluate(() => localStorage.getItem('accessToken')), 'social-access')
  assert.equal(await page.evaluate(() => localStorage.getItem('refreshToken')), 'social-refresh')
  // 돌아온 주소는 방문 기록에서 바뀌어(replace) 뒤로 가기로 다시 열리지 않는다
  assert.ok(!(await page.evaluate(() => location.href)).includes('code='))

  // 시간이 지난 코드면 로그인 화면으로 보내고 알려 준다
  await page.goto(`${origin}/oauth2/redirect?code=old`)
  await page.locator('.toast.error').filter({ hasText: '로그인 시간이 지났어요' }).waitFor()
  assert.deepEqual(errors, [])
})

test('공지만 있거나 글이 없는 밴드는 다시 불러올 때 빈 카드가 깜빡이지 않는다', async t => {
  const { page, state, errors } = await fixture(t, { posts: [makePost(1)] })
  await page.goto(`${origin}/home?band=10`)
  await page.locator('.community .notice').first().waitFor()
  await page.waitForFunction(() => !document.querySelector('.community .skeleton'))
  const listCalls = () => state.requests.filter(r => r.method === 'GET' && r.path === '/api/v1/bands/10/posts').length
  // 처음 다 보여 준 뒤 빈 카드가 한 번이라도 생기는지 지켜본다 (#295)
  await page.evaluate(() => {
    window.skeletonSeen = 0
    new MutationObserver(() => { if (document.querySelector('.community .skeleton')) window.skeletonSeen++ })
      .observe(document.body, { subtree: true, childList: true })
  })
  const before = listCalls()
  // 같은 밴드에서 글이 바뀌었다는 신호 → 다시 불러온다
  state.streamEvents = [['change', { topic: 'BAND_BOARD', bandId: 10 }]]
  for (let i = 0; i < 300 && listCalls() < before + 2; i++) await page.waitForTimeout(100)
  await page.waitForTimeout(500)
  assert.ok(listCalls() >= before + 2, '다시 불러왔어야 한다')
  assert.equal(await page.evaluate(() => window.skeletonSeen), 0)
  assert.equal(await page.locator('.community .notice').count(), 1)
  assert.deepEqual(errors, [])
})

test('팝업 오른쪽 위 X로 닫고, 요즘 듣는 음악은 곡 옆 X로 빼고 저장하면 지워진다', async t => {
  const { page, state, errors } = await fixture(t, { music: { title: 'Hysteria', artist: 'Muse', url: null, artworkUrl: null, previewUrl: null } })
  await page.goto(`${origin}/home`)
  const openMusic = page.getByRole('button', { name: /Hysteria/ })
  await openMusic.click()
  const dialog = page.getByRole('dialog', { name: '요즘 듣는 음악' })
  await dialog.waitFor()
  // 공통 닫기 버튼 (#292): 바탕 칸 없이 X만
  const closeX = dialog.getByRole('button', { name: '닫기', exact: true })
  assert.equal(await closeX.evaluate(el => getComputedStyle(el).backgroundColor), 'rgba(0, 0, 0, 0)')
  await closeX.click()
  await dialog.waitFor({ state: 'detached' })
  // 바깥(어두운 부분)을 눌러도 닫힌다
  await openMusic.click()
  await dialog.waitFor()
  await page.mouse.click(5, 5)
  await dialog.waitFor({ state: 'detached' })

  await openMusic.click()
  await dialog.waitFor()
  // 따로 있던 지우기 버튼은 없고, 직접 적은 곡(커버 없음)도 곡 옆 X가 보인다
  await page.waitForTimeout(300)
  await dialog.screenshot({ path: fileURLToPath(new URL('../test-results/modal-close-music.png', import.meta.url)) })
  assert.equal(await dialog.getByRole('button', { name: '지우기', exact: true }).count(), 0)
  await dialog.getByRole('button', { name: '고른 곡 빼기', exact: true }).click()
  await dialog.getByRole('button', { name: '저장', exact: true }).click()
  await page.locator('.toast').filter({ hasText: '요즘 듣는 음악을 지웠어요' }).waitFor()
  assert.ok(state.requests.some(r => r.method === 'DELETE' && r.path === '/api/v1/users/me/profile-music'))
  assert.deepEqual(errors, [])
})

// #315: 페이지를 새로 열 때(아이폰 홈 화면 앱은 오래 두면 새로 열린다) 빈 카드가 뜨던 문제
async function watchSkeleton(page) {
  await page.addInitScript(() => {
    window.skeletonSeen = 0
    document.addEventListener('DOMContentLoaded', () => new MutationObserver(() => {
      if (document.querySelector('.community .skeleton')) window.skeletonSeen++
    }).observe(document.body, { subtree: true, childList: true }))
  })
}
const slowList = (page, ms) => page.route('http://localhost:8080/api/v1/bands/10/posts?**', async route => {
  await new Promise(resolve => setTimeout(resolve, ms))
  return route.fallback()
})

test('앱을 새로 열면 기기에 기억한 목록을 바로 보여 주고 빈 카드를 띄우지 않는다', async t => {
  const { page, state, errors } = await fixture(t)
  await page.goto(`${origin}/home?band=10`)
  await page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 12)
  await watchSkeleton(page)
  await slowList(page, 800)
  state.posts.at(-1).title = '뒤에서 새로 불러온 제목'
  await page.reload()
  // 응답(0.8초)보다 먼저 기억한 목록이 보인다
  await page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 12, null, { timeout: 600 })
  await page.locator('.community').getByText('뒤에서 새로 불러온 제목').waitFor()
  assert.equal(await page.evaluate(() => window.skeletonSeen), 0)
  // 로그인 토큰과 같은 곳(로그인 유지 → localStorage)에 기억한다
  assert.ok(await page.evaluate(() => localStorage.getItem('gainz.communityCache')?.includes('뒤에서 새로 불러온 제목')))
  assert.deepEqual(errors, [])
})

test('처음 여는 목록은 금방 오면 빈 카드 없이, 늦으면 빈 카드를 보여 준다', async t => {
  const fast = await fixture(t)
  await watchSkeleton(fast.page)
  await fast.page.goto(`${origin}/home?band=10`)
  await fast.page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 12)
  assert.equal(await fast.page.evaluate(() => window.skeletonSeen), 0)

  const slow = await fixture(t)
  await watchSkeleton(slow.page)
  await slowList(slow.page, 900)
  await slow.page.goto(`${origin}/home?band=10`)
  await slow.page.locator('.community .skeleton').first().waitFor()
  await slow.page.waitForFunction(() => document.querySelectorAll('.community .post:not(.skeleton)').length === 12)
  assert.deepEqual([...fast.errors, ...slow.errors], [])
})
