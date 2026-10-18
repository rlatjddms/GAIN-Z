import { test } from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

// 셋리스트 API를 메모리에서 흉내 낸다. 응답 모양은 백엔드 SetlistResponse와 같다.
function createSetlistApi() {
  const state = { lists: [], files: new Map(), nextId: 30, failNextSave: false, saves: 0 }
  const id = () => state.nextId++
  const view = list => ({ id: list.id, title: list.title, favorite: list.favorite, items: list.items.map(item => ({ ...item, attachments: item.attachments.map(a => ({ ...a, url: `/api/v1/bands/10/setlists/${list.id}/items/${item.id}/attachments/${a.id}` })) })) })
  const find = listId => state.lists.find(list => list.id === Number(listId))
  // multipart 본문에서 item JSON과 파일 이름·내용만 꺼낸다.
  function multipart(buffer) {
    const text = buffer.toString('utf8')
    const item = JSON.parse(text.match(/name="item"[^\r]*\r\n(?:[^\r]+\r\n)*\r\n([\s\S]*?)\r\n--/)[1])
    const files = [...text.matchAll(/name="files"; filename="([^"]+)"\r\n(?:[^\r]+\r\n)*\r\n([\s\S]*?)\r\n--/g)].map(m => ({ name: m[1], body: m[2] }))
    return { item, files }
  }
  function saveItem(list, itemId, body) {
    const { item, files } = multipart(body)
    let target = itemId ? list.items.find(i => i.id === Number(itemId)) : null
    if (!target) { target = { id: id(), attachments: [] }; list.items.push(target) }
    const kept = target.attachments.filter(a => item.keepAttachmentIds.includes(a.id))
    const added = files.map(file => { const key = `file-${id()}`; state.files.set(key, file.body); return { id: key, name: file.name, type: 'application/pdf' } })
    const sheet = item.sheetId ? { sheetId: item.sheetId, title: 'Hysteria', artist: 'Muse', workspaceItemId: item.sheetId === 7 ? 70 : null } : null
    Object.assign(target, { title: item.title, artist: item.artist || null, memo: item.memo || null, sheet, memberIds: item.memberIds, attachments: [...kept, ...added] })
    return view(list)
  }
  function handle(method, path, request) {
    let m
    if ((m = path.match(/^\/api\/v1\/bands\/10\/setlists\/(\d+)\/items\/(\d+)\/attachments\/(.+)$/))) return { raw: state.files.get(m[3]) }
    if (path === '/api/v1/bands/10/setlists/practice') return { from: '2026-10-12', to: '2026-10-18', members: [{ memberId: 1, userId: 1, practiceSeconds: 5400, practiceDays: 2, routineTotal: 4, routineCompleted: 3 }, { memberId: 2, userId: 2, practiceSeconds: 0, practiceDays: 0, routineTotal: 0, routineCompleted: 0 }] }
    if (path === '/api/v1/bands/10/setlists/order') { const ids = request.postDataJSON().ids; state.lists = ids.map(find); return state.lists.map(view) }
    if (path === '/api/v1/bands/10/setlists') {
      if (method === 'POST') { const list = { id: id(), title: request.postDataJSON().title, favorite: false, items: [] }; state.lists.push(list); return view(list) }
      return state.lists.map(view)
    }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/setlists\/(\d+)\/items\/order$/))) { const list = find(m[1]); list.items = request.postDataJSON().ids.map(i => list.items.find(item => item.id === i)); return view(list) }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/setlists\/(\d+)\/items(?:\/(\d+))?$/))) {
      const list = find(m[1])
      if (method === 'DELETE') { list.items = list.items.filter(item => item.id !== Number(m[2])); return view(list) }
      state.saves++
      if (state.failNextSave) { state.failNextSave = false; return { status: 500, error: '저장하지 못했어요. 잠시 후 다시 시도해 주세요.' } }
      return saveItem(list, m[2], request.postDataBuffer())
    }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/setlists\/(\d+)$/))) {
      if (method === 'DELETE') { state.lists = state.lists.filter(list => list.id !== Number(m[1])); return null }
      const list = find(m[1]); const body = request.postDataJSON()
      if (body.title != null) list.title = body.title
      if (body.favorite != null) list.favorite = body.favorite
      return view(list)
    }
    return undefined
  }
  return { state, handle }
}

test('셋리스트 생성·드래그·멤버 선택·악보 연결·첨부·연습 현황·삭제 및 반응형 배치', async () => {
  const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', '4181', '--strictPort'], { cwd: fileURLToPath(new URL('../', import.meta.url)), stdio: 'pipe', windowsHide: true })
  let browser
  try {
    for (let i = 0; i < 100; i++) {
      try { if ((await fetch('http://127.0.0.1:4181')).ok) break } catch {}
      if (i === 99) throw new Error('서버 시작 실패')
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    browser = await chromium.launch({ headless: true })
    const page = await browser.newPage({ viewport: { width: 1280, height: 960 }, acceptDownloads: true })
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    await page.addInitScript(() => localStorage.setItem('accessToken', 'test'))
    const api = createSetlistApi()
    await page.route('http://localhost:8080/**', async route => {
      const headers = { 'access-control-allow-origin': '*', 'access-control-allow-headers': '*', 'access-control-allow-methods': '*' }
      const request = route.request()
      if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
      const character = { nickname: '신대방불꽃기타리스트', instrumentType: 'GUITAR', level: 1 }
      const path = new URL(request.url()).pathname
      let data = api.handle(request.method(), path, request)
      if (data?.raw !== undefined) return route.fulfill({ headers, contentType: 'application/pdf', body: data.raw })
      if (data?.status) return route.fulfill({ status: data.status, headers, contentType: 'application/json', body: JSON.stringify({ success: false, message: data.error }) })
      if (data === undefined) {
        data = path === '/api/v1/workspace/sheets' && request.method() === 'GET' ? [{ id: 70, sheetId: 7, title: 'Hysteria', artist: 'Muse' }]
          : path.includes('/bands/') ? { id: 10, name: '합주 밴드', members: [{ memberId: 1, part: 'BASS', character }, { memberId: 2, part: 'GUITAR', character: { nickname: '랍스타' } }] }
          : path.includes('/characters/') ? character : { id: 1, name: '테스트' }
      }
      await route.fulfill({ headers, contentType: 'application/json', body: JSON.stringify({ success: true, data }) })
    })
    await page.goto('http://127.0.0.1:4181/bands/10/setlists')
    // 멤버별 이번 주 연습 현황
    const practice = page.getByRole('region', { name: '이번 주 연습 현황' })
    await practice.waitFor()
    assert.match(await practice.textContent(), /10\.12 ~ 10\.18/)
    assert.match(await practice.locator('li').first().textContent(), /신대방불꽃기타리스트.*1시간 30분.*2일 연습 · 루틴 3\/4/)
    assert.match(await practice.locator('li').nth(1).textContent(), /0분.*루틴 없음/)

    const create = page.getByRole('button', { name: '+ 셋리스트 만들기', exact: true })
    async function createList(title) {
      await create.click()
      await page.getByRole('dialog').getByPlaceholder('셋리스트 이름을 적어주세요').fill(title)
      await page.getByRole('dialog').getByRole('button', { name: '만들기', exact: true }).click()
      await page.getByRole('dialog').waitFor({ state: 'hidden' })
    }
    await createList('정기 합주')
    assert.equal(await page.getByRole('button', { name: '수정', exact: true }).count(), 0)
    const songs = page.locator('.songs > .reorder-item')
    async function addSong(title) {
      await page.getByRole('button', { name: '+ 곡 추가', exact: true }).click()
      const editor = songs.last()
      await editor.getByPlaceholder('곡 제목을 적어주세요').fill(title)
      return editor
    }
    await addSong('첫 곡')
    await songs.first().getByRole('button', { name: /신대방불꽃/ }).click()
    await songs.first().getByPlaceholder('합주 메모를 적어주세요').fill('후렴 반복')
    await songs.first().getByLabel('보관함 악보 연결').selectOption('7')
    // 제목은 이미 적었으니 유지하고, 비어 있던 아티스트만 악보에서 채운다.
    assert.equal(await songs.first().getByPlaceholder('곡 제목을 적어주세요').inputValue(), '첫 곡')
    assert.equal(await songs.first().getByPlaceholder('아티스트 이름을 적어주세요').inputValue(), 'Muse')
    await songs.first().locator('input[type=file]').setInputFiles([{ name: '악보.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF') }])
    // 첫 곡을 저장해도 다른 곡은 작성 상태로 남아 있어야 한다.
    await addSong('둘째 곡')
    await songs.nth(1).getByPlaceholder('합주 메모를 적어주세요').fill('작성 중인 다른 곡')
    // 서버 저장이 실패하면 작성 내용을 그대로 둔다.
    api.state.failNextSave = true
    await songs.first().getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('alert').filter({ hasText: '저장하지 못했어요' }).waitFor()
    assert.equal(await songs.first().getByPlaceholder('합주 메모를 적어주세요').inputValue(), '후렴 반복')
    await songs.first().getByRole('button', { name: '저장', exact: true }).click()
    await songs.first().getByRole('button', { name: '첫 곡 수정', exact: true }).waitFor()
    assert.equal(await songs.first().locator('input,textarea').count(), 0)
    assert.equal(await songs.first().locator('.read-memo').textContent(), '후렴 반복')
    assert.equal(await songs.nth(1).getByPlaceholder('합주 메모를 적어주세요').inputValue(), '작성 중인 다른 곡')
    const saved = api.state.lists[0].items[0]
    assert.deepEqual([saved.title, saved.artist, saved.memo, saved.sheet.sheetId, saved.memberIds, saved.attachments.map(a => a.name)], ['첫 곡', 'Muse', '후렴 반복', 7, [1], ['악보.pdf']])
    assert.equal(api.state.lists[0].items.length, 1)
    await songs.nth(1).getByRole('button', { name: '저장', exact: true }).click()
    await songs.nth(1).getByRole('button', { name: '둘째 곡 수정', exact: true }).waitFor()
    // 수정 취소는 확인 후 서버에 보내지 않는다.
    const savesBeforeCancel = api.state.saves
    await songs.first().getByRole('button', { name: '첫 곡 수정', exact: true }).click()
    await songs.first().getByPlaceholder('합주 메모를 적어주세요').fill('취소할 메모')
    page.once('dialog', dialog => dialog.accept())
    await songs.first().getByRole('button', { name: '취소', exact: true }).click()
    assert.equal(await songs.first().locator('.read-memo').textContent(), '후렴 반복')
    assert.equal(api.state.saves, savesBeforeCancel)
    // 첨부를 남긴 채 메모만 고쳐도 기존 첨부는 유지된다.
    await songs.first().getByRole('button', { name: '첫 곡 수정', exact: true }).click()
    await songs.first().getByPlaceholder('합주 메모를 적어주세요').fill('후렴 두 번')
    await songs.first().getByRole('button', { name: '저장', exact: true }).click()
    await songs.first().getByRole('button', { name: '첫 곡 수정', exact: true }).waitFor()
    assert.deepEqual(api.state.lists[0].items[0].attachments.map(a => a.name), ['악보.pdf'])
    await page.getByRole('button', { name: '모두 접기', exact: true }).click()
    const idle = () => page.waitForFunction(() => !document.querySelector('.workspace').inert)
    async function dragItem(items, handleName) {
      await idle()
      const h = await items.first().getByRole('button', { name: handleName, exact: true }).boundingBox()
      const t = await items.nth(1).boundingBox()
      await page.mouse.move(h.x + h.width / 2, h.y + h.height / 2)
      await page.mouse.down()
      await page.mouse.move(t.x + 40, t.y + t.height - 2, { steps: 10 })
      assert.notEqual(await items.first().evaluate(el => getComputedStyle(el).transform), 'none')
      await page.mouse.up()
      await page.locator('.reorder-list.moving').waitFor({ state: 'detached' })
      await page.waitForFunction(() => !document.querySelector('.workspace').inert)
    }
    await dragItem(songs, '첫 곡 순서 변경')
    assert.equal(await songs.first().locator('.song-name strong').textContent(), '둘째 곡')
    assert.deepEqual(api.state.lists[0].items.map(item => item.title), ['둘째 곡', '첫 곡'])
    await page.reload()
    await page.getByRole('button', { name: '첫 곡 수정', exact: true }).waitFor()
    assert.equal(await songs.first().locator('.song-name strong').textContent(), '둘째 곡')
    await songs.nth(1).locator('.song-toggle').click()
    assert.match(await songs.nth(1).locator('.member-tag').textContent(), /신대방불꽃/)
    // 서버 첨부는 로그인 요청으로 받아 저장한다.
    const [download] = await Promise.all([page.waitForEvent('download'), songs.nth(1).locator('.file-list a').first().click()])
    assert.equal(download.suggestedFilename(), '악보.pdf')
    // 연결한 악보는 내 보관함 항목의 악보 보기 화면으로 연다.
    assert.doesNotMatch(await songs.nth(1).locator('.read-section').first().textContent(), /연결한 악보가 없어요/)
    await songs.first().locator('.song-toggle').click()
    assert.match(await songs.first().locator('.read-section').first().textContent(), /연결한 악보가 없어요/)
    await songs.nth(1).getByRole('button', { name: 'Hysteria 보기 →' }).click()
    await page.waitForURL('**/sheets/70')
    await page.goBack()
    await page.getByRole('button', { name: '첫 곡 수정', exact: true }).waitFor()
    // 빈 곡은 저장되지 않으며 다른 곡의 보기 화면은 유지한다.
    await page.getByRole('button', { name: '+ 곡 추가', exact: true }).click()
    await songs.last().getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('alert').filter({ hasText: '곡 제목을 적어주세요' }).waitFor()
    await songs.last().getByRole('button', { name: '취소', exact: true }).click()
    assert.equal(await songs.count(), 2)
    await page.getByRole('button', { name: '이름 변경', exact: true }).click()
    await page.getByRole('dialog').getByPlaceholder('셋리스트 이름을 적어주세요').fill('정기 합주 수정')
    await page.getByRole('dialog').getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    await createList('공연 준비')
    const entries = page.locator('.list-nav > .reorder-item')
    await dragItem(entries, '정기 합주 수정 셋리스트 순서 변경')
    assert.equal(await entries.first().locator('.list-link strong').textContent(), '공연 준비')
    assert.deepEqual(api.state.lists.map(list => list.title), ['공연 준비', '정기 합주 수정'])
    await page.getByRole('button', { name: '정기 합주 수정 즐겨찾기', exact: true }).click()
    await page.waitForFunction(() => !document.querySelector('.workspace').inert)
    assert.equal(api.state.lists[1].favorite, true)
    await page.getByRole('button', { name: '☆ 즐겨찾기', exact: true }).click()
    assert.equal(await entries.count(), 1)
    await entries.first().locator('.list-link').click()
    await page.getByRole('button', { name: '★ 즐겨찾기', exact: true }).click()
    await page.getByRole('button', { name: '모두 펼치기', exact: true }).click()
    // 곡 삭제는 확인한 뒤 서버에서 지운다.
    page.once('dialog', dialog => dialog.accept())
    await page.getByRole('button', { name: '둘째 곡 삭제', exact: true }).click()
    await page.getByRole('button', { name: '둘째 곡 수정', exact: true }).waitFor({ state: 'detached' })
    assert.deepEqual(api.state.lists[1].items.map(item => item.title), ['첫 곡'])
    await mkdir(new URL('../test-results/', import.meta.url), { recursive: true })
    async function screenshot(name) {
      await page.evaluate(async () => { window.scrollTo(0, 0); await new Promise(requestAnimationFrame); await Promise.all(document.getAnimations().map(animation => animation.finished)) })
      await page.screenshot({ path: fileURLToPath(new URL(`../test-results/${name}.png`, import.meta.url)), fullPage: true })
    }
    // 저장된 곡 옆에 작성 중인 곡이 공존하는 화면도 확인한다.
    await addSong('새 합주곡')
    await screenshot('setlists-song-edit-desktop')
    for (const width of [375, 760, 1040]) {
      await page.setViewportSize({ width, height: 900 })
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${width}px 가로 넘침`)
    }
    await page.setViewportSize({ width: 375, height: 900 })
    await screenshot('setlists-song-edit-mobile')
    await songs.last().getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('button', { name: '새 합주곡 수정', exact: true }).waitFor()
    await screenshot('setlists-mobile')
    await page.getByRole('button', { name: '선택', exact: true }).click()
    await page.getByRole('checkbox', { name: '정기 합주 수정 선택', exact: true }).check()
    await page.getByRole('button', { name: '선택 삭제', exact: true }).click()
    await page.getByRole('dialog').getByRole('button', { name: '삭제', exact: true }).click()
    await page.getByRole('dialog').waitFor({ state: 'hidden' })
    assert.equal(await entries.count(), 1)
    assert.deepEqual(api.state.lists.map(list => list.title), ['공연 준비'])
    assert.deepEqual(errors, [])
  } finally { await browser?.close(); server.kill() }
})
