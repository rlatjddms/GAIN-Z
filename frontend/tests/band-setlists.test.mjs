import { test } from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright'

test('셋리스트 생성·드래그·멤버 선택·악보 연결·첨부·삭제 및 반응형 배치', async () => {
  const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', '4181', '--strictPort'], { cwd: fileURLToPath(new URL('../', import.meta.url)), stdio: 'pipe', windowsHide: true })
  let browser
  try {
    for (let i = 0; i < 100; i++) {
      try { if ((await fetch('http://127.0.0.1:4181')).ok) break } catch {}
      if (i === 99) throw new Error('서버 시작 실패')
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    browser = await chromium.launch({ headless: true })
    const page = await browser.newPage({ viewport: { width: 1280, height: 960 } })
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    await page.addInitScript(() => localStorage.setItem('accessToken', 'test'))
    await page.route('http://localhost:8080/**', async route => {
      const headers = { 'access-control-allow-origin': '*', 'access-control-allow-headers': '*', 'access-control-allow-methods': '*' }
      if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
      const character = { nickname: '신대방불꽃기타리스트', instrumentType: 'GUITAR', level: 1 }
      const path = new URL(route.request().url()).pathname
      const data = path.includes('/workspace/sheets') ? [{ id: 7, title: 'Hysteria', artist: 'Muse' }] : path.includes('/bands/') ? { id: 10, name: '합주 밴드', members: [{ memberId: 1, part: 'BASS', character }, { memberId: 2, part: 'GUITAR', character: { nickname: '랍스타' } }] } : path.includes('/characters/') ? character : { id: 1, name: '테스트' }
      await route.fulfill({ headers, contentType: 'application/json', body: JSON.stringify({ success: true, data }) })
    })
    await page.goto('http://127.0.0.1:4181/bands/10/setlists')
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
    await songs.first().locator('input[type=file]').setInputFiles([{ name: '악보.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF') }, { name: '영상.mp4', mimeType: 'video/mp4', buffer: Buffer.from('preview') }])
    // 첫 곡을 저장해도 다른 곡은 작성 상태로 남아 있어야 한다.
    await addSong('둘째 곡')
    await songs.nth(1).getByPlaceholder('합주 메모를 적어주세요').fill('작성 중인 다른 곡')
    await page.evaluate(() => {
      const original = IDBObjectStore.prototype.put
      IDBObjectStore.prototype.put = function() { IDBObjectStore.prototype.put = original; throw new DOMException('공간 부족', 'QuotaExceededError') }
    })
    await songs.first().getByRole('button', { name: '저장', exact: true }).click()
    await page.getByRole('alert').filter({ hasText: '저장하지 못했어요' }).waitFor()
    assert.equal(await songs.first().getByPlaceholder('합주 메모를 적어주세요').inputValue(), '후렴 반복')
    await songs.first().getByRole('button', { name: '저장', exact: true }).click()
    await songs.first().getByRole('button', { name: '첫 곡 수정', exact: true }).waitFor()
    assert.equal(await songs.first().locator('input,textarea').count(), 0)
    assert.equal(await songs.first().locator('.read-memo').textContent(), '후렴 반복')
    assert.equal(await songs.first().getByRole('link', { name: 'Hysteria 보기 →' }).getAttribute('href'), '/sheets/7')
    assert.equal(await songs.nth(1).getByPlaceholder('합주 메모를 적어주세요').inputValue(), '작성 중인 다른 곡')
    const stored = await page.evaluate(() => new Promise((resolve, reject) => {
      const open = indexedDB.open('gainz-setlists', 1)
      open.onsuccess = () => {
        const db = open.result
        const get = db.transaction('bands').objectStore('bands').get('1:10')
        get.onsuccess = () => { resolve(get.result[0].songs.map(song => song.title)); db.close() }
        get.onerror = reject
      }
    }))
    assert.deepEqual(stored, ['첫 곡'])
    await songs.nth(1).getByRole('button', { name: '저장', exact: true }).click()
    await songs.nth(1).getByRole('button', { name: '둘째 곡 수정', exact: true }).waitFor()
    await songs.first().getByRole('button', { name: '첫 곡 수정', exact: true }).click()
    await songs.first().getByPlaceholder('합주 메모를 적어주세요').fill('취소할 메모')
    page.once('dialog', dialog => dialog.accept())
    await songs.first().getByRole('button', { name: '취소', exact: true }).click()
    assert.equal(await songs.first().locator('.read-memo').textContent(), '후렴 반복')
    await page.getByRole('button', { name: '모두 접기', exact: true }).click()
    async function dragItem(items, handleName) {
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
    await page.reload()
    await page.getByRole('button', { name: '첫 곡 수정', exact: true }).waitFor()
    assert.equal(await songs.first().locator('.song-name strong').textContent(), '둘째 곡')
    await songs.nth(1).locator('.song-toggle').click()
    assert.match(await songs.nth(1).locator('.member-tag').textContent(), /신대방불꽃/)
    const bytes = await songs.nth(1).locator('.file-list a').first().evaluate(async el => (await fetch(el.href)).text())
    assert.equal(bytes, '%PDF')
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
    await page.getByRole('button', { name: '정기 합주 수정 즐겨찾기', exact: true }).click()
    await page.waitForFunction(() => !document.querySelector('.workspace').inert)
    await page.getByRole('button', { name: '☆ 즐겨찾기', exact: true }).click()
    assert.equal(await entries.count(), 1)
    await entries.first().locator('.list-link').click()
    await page.getByRole('button', { name: '★ 즐겨찾기', exact: true }).click()
    await page.getByRole('button', { name: '모두 펼치기', exact: true }).click()
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
    assert.deepEqual(errors, [])

  } finally { await browser?.close(); server.kill() }
})
