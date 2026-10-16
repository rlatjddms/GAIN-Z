import { test, before, after } from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { readFile, mkdir } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

// 실제 서버 대신 API 응답을 고정해 UI 동작과 실패 후 재시도를 검증한다
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
  const context = await browser.newContext({ viewport: { width: 1280, height: 960 }, timezoneId: options.timezoneId || 'Asia/Seoul' })
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
    ], requests: [], media: new Map(), failSave: false, failList: false, leader: true, ...options,
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
        state.media.set(id, { bytes: Buffer.from(await file.arrayBuffer()), type: file.type })
        kept.push({ id, name: file.name, type: file.type, url: `/api/v1/bands/10/posts/${next.id}/attachments/${id}` })
      }
      Object.assign(next, { title: info.title, content: info.content, notice: info.notice ?? next.notice, noticeExpiresAt: info.notice ? info.noticeExpiresAt : null, attachments: kept, attachment: kept[0] || null })
      if (!existing) state.posts.push(next)
      return respond(next)
    }
    if (path === '/api/v1/characters/me') return respond({ nickname: '리듬냥', instrumentType: 'GUITAR', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' })
    if (path === '/api/v1/users/me') return respond({ id: 1, name: '테스트', profileImageUrl: author.profileImageUrl })
    if (path.endsWith('/profile-image')) return route.fulfill({ headers, contentType: 'image/png', body: photo })
    const band = { id: 10, name: '리듬 메이커스', myPosition: state.leader ? 'LEADER' : 'MEMBER', stage: 'STUDIO', members: [{ memberId: 1, userId: 1, me: true, part: 'GUITAR', position: 'LEADER', character: { nickname: '리듬냥', instrumentType: 'GUITAR', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' } }, { memberId: 2, userId: 2, part: 'BASS', position: 'MEMBER', character: { nickname: '베이스곰', instrumentType: 'BASS', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' } }], description: '함께 합주하는 밴드' }
    if (path === '/api/v1/bands/10') return respond(band)
    if (path === '/api/v1/bands/me') return respond([band])
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
      if (method === 'GET') return respond(state.comments)
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
  const header = await page.locator('.main-header').innerHTML()
  assert.equal(await page.getByText('전체 보기', { exact: true }).count(), 0)
  await page.getByRole('button', { name: /이야기 더 보기/ }).click()
  await page.waitForFunction(() => document.querySelectorAll('.community .post').length === 14)
  await page.getByRole('button', { name: /이야기 더 보기/ }).click()
  await page.waitForFunction(() => document.querySelectorAll('.community .post').length === 21)
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
  await page.waitForFunction(() => !document.querySelector('dialog'))
  assert.equal(await page.locator('.community .post').count(), 21)
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
  assert.equal(await page.locator('dialog').evaluate(e => e.scrollWidth <= e.clientWidth), true)
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
  await page.waitForFunction(() => document.querySelector('.media-gallery video')?.readyState >= 1)
  await page.locator('.media-gallery video').evaluate(async video => { await video.play(); video.pause() })
  await screenshot(page, 'community-detail-media')
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
  page.on('dialog', dialog => dialog.accept())
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
  page.once('dialog', dialog => dialog.dismiss())
  await page.keyboard.press('Escape')
  assert.equal(await page.locator('dialog[open]').count(), 1)
  page.once('dialog', dialog => dialog.accept())
  await page.keyboard.press('Escape')
  await page.waitForFunction(() => !document.querySelector('dialog'))
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
  page.on('dialog', dialog => dialog.accept())
  await page.goto(`${origin}/home?band=10`)
  await page.locator('.community .post').first().waitFor()
  assert.equal(await page.locator('.card-menu').count(), 0)
  await page.getByRole('button', { name: '내가 쓴 글', exact: true }).click()
  await page.waitForFunction(() => document.querySelector('.post-total')?.textContent.trim() === '9')
  assert.equal(await page.locator('.community .post').count(), 7)
  await page.getByRole('button', { name: /이야기 더 보기/ }).click()
  await page.waitForFunction(() => document.querySelectorAll('.community .post').length === 9)
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
  await page.waitForFunction(() => document.querySelector('.post-total')?.textContent.trim() === '8')
  assert.equal(state.posts.some(p => p.id === 9), false)
  await screenshot(page, 'community-my-posts')
  assert.deepEqual(errors, [])
})

test('사진 확대·키보드 넘기기·개별/ZIP 저장·닫기와 모바일 표시', async t => {
  const second = { ...attachment, id: 'second', name: '두번째.png' }
  const { page, errors } = await fixture(t, { posts: [makePost(2, { attachments: [attachment, second] })] })
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
