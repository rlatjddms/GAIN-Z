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
const attachment = { name: '합주.png', type: 'image/png', url: '/api/v1/bands/10/posts/2/attachment' }
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
  const context = await browser.newContext({ viewport: { width: 1280, height: 960 } })
  t.after(() => context.close())
  await context.addInitScript(() => localStorage.setItem('accessToken', 'ui-test-token'))
  const page = await context.newPage()
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  const state = {
    posts: Array.from({ length: 15 }, (_, i) => makePost(i + 1)), comments: [
      { id: 100, parentId: null, content: '박자부터 맞춰 봐요', author, createdAt: '2026-10-16T12:10:00', mine: true, deleted: false },
      { id: 101, parentId: 100, content: '좋아요', author, createdAt: '2026-10-16T12:11:00', mine: true, deleted: false },
    ], requests: [], failUpload: false, failList: false, leader: true, ...options,
  }
  await page.route('http://localhost:8080/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()
    const headers = { 'access-control-allow-origin': '*', 'access-control-allow-headers': '*', 'access-control-allow-methods': '*' }
    const respond = (data, status = 200) => route.fulfill({ status, headers, contentType: 'application/json', body: JSON.stringify(status === 200 ? { success: true, data } : { success: false, message: data }) })
    if (method === 'OPTIONS') return route.fulfill({ status: 204, headers })
    state.requests.push({ method, path, body: method === 'POST' || method === 'PATCH' ? request.postData() : null })
    if (path === '/api/v1/characters/me') return respond({ nickname: '리듬냥', instrumentType: 'GUITAR', level: 1, exp: 0, gender: 'MALE', hairStyle: 'SHORT', hairColor: '#333333', outfitColor: '#ffffff', instrumentColor: '#ff6600', currentOutfitSpriteKey: 'DEFAULT' })
    if (path === '/api/v1/users/me') return respond({ id: 1, name: '테스트', profileImageUrl: author.profileImageUrl })
    if (path.endsWith('/profile-image')) return route.fulfill({ headers, contentType: 'image/png', body: photo })
    const band = { id: 10, name: '리듬 메이커스', myPosition: state.leader ? 'LEADER' : 'MEMBER', stage: 'STUDIO', members: [], description: '함께 합주하는 밴드' }
    if (path === '/api/v1/bands/10') return respond(band)
    if (path === '/api/v1/bands/me') return respond([band])
    if (path === '/api/v1/bands/10/posts') {
      if (method === 'GET') {
        if (state.failList) return respond('게시글 조회 실패', 503)
        const filter = url.searchParams.get('notice')
        const rows = state.posts.filter(p => filter === null || p.notice === (filter === 'true')).sort((a, b) => Number(b.notice) - Number(a.notice) || b.id - a.id)
        const number = Number(url.searchParams.get('page') || 0)
        const size = Number(url.searchParams.get('size') || 12)
        return respond({ content: rows.slice(number * size, (number + 1) * size), number, totalPages: Math.ceil(rows.length / size), totalElements: rows.length })
      }
      const next = makePost(50, { ...request.postDataJSON(), notice: false, attachment: null })
      state.posts.push(next)
      return respond(next)
    }
    const match = path.match(/^\/api\/v1\/bands\/10\/posts\/(\d+)(.*)$/)
    if (!match) return respond('테스트에서 처리하지 않는 경로', 404)
    const id = Number(match[1])
    const suffix = match[2]
    const post = state.posts.find(p => p.id === id)
    if (!post) return respond('글이 없습니다', 404)
    if (!suffix) {
      if (method === 'PATCH') Object.assign(post, request.postDataJSON())
      if (method === 'DELETE') state.posts = state.posts.filter(p => p.id !== id)
      return respond(post)
    }
    if (suffix === '/notice') { post.notice = request.postDataJSON().notice; return respond(post) }
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

test('공지 우선 목록, 페이지 이동, 사진 미리보기와 모바일 넘침', async t => {
  const { page, errors } = await fixture(t)
  await page.goto(`${origin}/bands/10/community`)
  await page.locator('.list-row').first().waitFor()
  assert.match(await page.locator('.list-row').first().innerText(), /공지/)
  assert.equal(await page.locator('.list-row').count(), 12)
  await page.getByRole('button', { name: '다음', exact: true }).click()
  await page.waitForURL(/page=1/)
  await page.locator('.list-row img').waitFor()
  assert.equal(await page.locator('.list-row').count(), 3)
  const size = await page.locator('.list-row img').boundingBox()
  assert.equal(size.width, 112)
  assert.equal(size.height, 84)
  await page.screenshot({ path: fileURLToPath(new URL('../test-results/community-desktop.png', import.meta.url)), fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true)
  await page.screenshot({ path: fileURLToPath(new URL('../test-results/community-mobile.png', import.meta.url)), fullPage: true })
  assert.deepEqual(errors, [])
})

test('첨부 실패 후 저장 재시도는 새 글을 중복 생성하지 않는다', async t => {
  const { page, state, errors } = await fixture(t, { failUpload: true })
  await page.goto(`${origin}/bands/10/community?new=1`)
  await page.getByLabel('제목', { exact: true }).fill('새 합주 계획')
  await page.getByLabel('내용', { exact: true }).fill('일요일에 만나요')
  await page.locator('input[type=file]').setInputFiles({ name: '합주.png', mimeType: 'image/png', buffer: photo })
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await page.getByRole('alert').filter({ hasText: '글은 저장됐지만' }).waitFor()
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await page.waitForURL(/post=50/)
  await page.locator('.detail img').last().waitFor()
  assert.equal(state.requests.filter(r => r.method === 'POST' && r.path === '/api/v1/bands/10/posts').length, 1)
  assert.equal(state.requests.filter(r => r.method === 'PUT' && r.path.endsWith('/attachment')).length, 2)
  await page.getByRole('button', { name: '공지로 지정' }).click()
  await page.getByRole('button', { name: '공지 해제' }).waitFor()
  await page.locator('.detail').getByRole('link', { name: '수정', exact: true }).click()
  await page.getByLabel('제목', { exact: true }).fill('수정된 합주 계획')
  await page.getByLabel('기존 첨부 삭제:', { exact: false }).check()
  await page.getByRole('button', { name: '저장', exact: true }).click()
  await page.locator('.detail h2').filter({ hasText: '수정된 합주 계획' }).waitFor()
  assert.equal(state.posts.find(p => p.id === 50).attachment, null)
  assert.deepEqual(errors, [])
})

test('댓글·대댓글 작성과 수정, 부모 삭제 후 답글 보존', async t => {
  const { page, state, errors } = await fixture(t)
  page.on('dialog', dialog => dialog.accept())
  await page.goto(`${origin}/bands/10/community?post=2`)
  await page.locator('.comment').first().waitFor()
  await page.locator('.comment').first().getByRole('button', { name: '답글', exact: true }).click()
  await page.getByLabel('답글', { exact: true }).fill('템포 100부터 시작해요')
  await page.getByRole('button', { name: '등록', exact: true }).click()
  await page.getByText('템포 100부터 시작해요', { exact: true }).waitFor()
  assert.equal(state.comments.at(-1).parentId, 100)
  const last = page.locator('.comment').last()
  await last.getByRole('button', { name: '수정', exact: true }).click()
  await page.getByRole('textbox', { name: '댓글 수정' }).fill('템포 110부터 시작해요')
  await page.getByRole('button', { name: '수정 저장' }).click()
  await page.getByText('템포 110부터 시작해요', { exact: true }).waitFor()
  await page.locator('.comment').first().getByRole('button', { name: '삭제', exact: true }).click()
  await page.locator('.comment .muted').filter({ hasText: '삭제된 댓글' }).waitFor()
  assert.equal(await page.getByText('템포 110부터 시작해요', { exact: true }).count(), 1)
  await page.locator('.comment').last().getByRole('button', { name: '삭제', exact: true }).click()
  await page.waitForFunction(() => !document.body.textContent.includes('템포 110부터 시작해요'))
  assert.deepEqual(errors, [])
})

test('일반 멤버는 공지 지정과 타인 글 수정 버튼이 없다', async t => {
  const { page, errors } = await fixture(t, { leader: false, posts: [makePost(2, { mine: false })] })
  await page.goto(`${origin}/bands/10/community?post=2`)
  await page.locator('.detail h2').waitFor()
  assert.equal(await page.getByRole('button', { name: '공지로 지정' }).count(), 0)
  assert.equal(await page.locator('.detail').getByRole('link', { name: '수정', exact: true }).count(), 0)
  assert.equal(await page.getByRole('button', { name: '글 삭제' }).count(), 0)
  assert.deepEqual(errors, [])
})

test('목록 오류 재시도와 빈 목록', async t => {
  const { page, state, errors } = await fixture(t, { failList: true, posts: [] })
  await page.goto(`${origin}/bands/10/community`)
  await page.getByRole('alert').waitFor()
  state.failList = false
  await page.getByRole('button', { name: '다시 불러오기', exact: true }).click()
  await page.getByText('아직 게시글이 없어요. 첫 글을 남겨 보세요.').waitFor()
  assert.deepEqual(errors, [])
})

test('홈 BAND는 실제 공지와 최근 글을 표시하고 글쓰기로 연결한다', async t => {
  const { page, errors } = await fixture(t)
  await page.goto(`${origin}/home`)
  await page.getByRole('button', { name: 'BAND', exact: true }).click()
  await page.locator('.community .post').first().waitFor()
  assert.equal(await page.locator('.community .post').count(), 6)
  assert.equal(await page.locator('.community .notice').count(), 1)
  assert.equal(await page.locator('.community .notice').evaluate(e => getComputedStyle(e).borderLeftWidth), '1px')
  await page.locator('.community').getByRole('button', { name: /글쓰기/ }).click()
  await page.waitForURL(/new=1/)
  await page.getByLabel('제목', { exact: true }).waitFor()
  assert.deepEqual(errors, [])
})
