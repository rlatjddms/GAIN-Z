import { test } from 'node:test'
import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { mkdir, readFile } from 'node:fs/promises'
import { chromium } from 'playwright'
import { encodeWav } from '../src/audio/jamAudio.js'

const ME = '신대방불꽃기타리스트'

// seconds초짜리 사인파 WAV (모의 서버가 돌려줄 트랙 파일)
function sineWav(seconds, frequency, sampleRate = 22050) {
  const samples = Float32Array.from({ length: seconds * sampleRate }, (_, i) => Math.sin(2 * Math.PI * frequency * i / sampleRate) * 0.3)
  return Buffer.from(encodeWav({ numberOfChannels: 1, length: samples.length, sampleRate, getChannelData: () => samples }))
}

// 레이어 잼 API를 메모리에서 흉내 낸다. 응답 모양은 백엔드 JamResponse와 같다.
function createJamApi() {
  const state = { nextId: 60, files: new Map(), patches: [], uploads: [], jams: [] }
  const id = () => state.nextId++
  const track = (props, body) => {
    const t = { id: id(), volume: 100, offsetMs: 0, createdAt: '2026-10-18T10:00:00', ...props }
    state.files.set(t.id, body)
    return t
  }
  state.jams.push({
    id: 50, title: 'Hysteria 합주', bpm: 120, song: { setlistItemId: 31, title: 'Hysteria' }, creatorName: ME, canManage: true,
    tracks: [
      track({ userId: 2, nickname: '랍스타', part: 'BASS', name: 'bass.wav', type: 'audio/wav', durationMs: 2000, canDelete: false }, sineWav(2, 110)),
      track({ userId: 1, nickname: ME, part: 'GUITAR', name: 'guitar.wav', type: 'audio/wav', durationMs: 2000, canDelete: true }, sineWav(2, 440)),
    ],
  })
  const url = (jam, t) => `/api/v1/bands/10/jams/${jam.id}/tracks/${t.id}/file`
  const trackView = (jam, t) => ({ ...t, url: url(jam, t) })
  const view = jam => ({ ...jam, tracks: jam.tracks.map(t => trackView(jam, t)) })
  const summary = jam => ({ id: jam.id, title: jam.title, bpm: jam.bpm, song: jam.song, trackCount: jam.tracks.length, creatorName: jam.creatorName })
  const find = jamId => state.jams.find(jam => jam.id === Number(jamId))
  const song = setlistItemId => setlistItemId === 31 ? { setlistItemId: 31, title: 'Hysteria' } : null
  // multipart 본문에서 track JSON과 파일 바이트를 꺼낸다.
  function multipart(buffer) {
    const text = buffer.toString('latin1')
    const json = JSON.parse(Buffer.from(text.match(/name="track"[^\r]*\r\n(?:[^\r]+\r\n)*\r\n([\s\S]*?)\r\n--/)[1], 'latin1').toString('utf8'))
    const file = text.match(/name="file"; filename="([^"]+)"\r\n(?:[^\r]+\r\n)*\r\n([\s\S]*?)\r\n--/)
    return { json, name: Buffer.from(file[1], 'latin1').toString('utf8'), body: Buffer.from(file[2], 'latin1') }
  }
  function handle(method, path, request) {
    let m
    if (path === '/api/v1/bands/10/setlists') {
      const item = (id, title, artist) => ({ id, title, artist, memberIds: [], attachments: [] })
      return [
        // 연결 곡이 첫 셋리스트에 있지 않아야 셋리스트 전환을 확인할 수 있다
        { id: 23, title: '연습용', favorite: false, items: Array.from({ length: 10 }, (_, i) => item(40 + i, `연습곡 ${i + 1}`, null)) },
        { id: 20, title: '정기공연', favorite: false, items: [item(31, 'Hysteria', 'Muse'), item(32, 'Creep', 'Radiohead')] },
        { id: 21, title: '여름 축제', favorite: false, items: [item(33, 'Time Is Running Out', 'Muse')] },
        { id: 22, title: '빈 셋리스트', favorite: false, items: [] },
      ]
    }
    if (path === '/api/v1/bands/10/jams') {
      if (method === 'GET') return state.jams.map(summary)
      const body = request.postDataJSON()
      const jam = { id: id(), title: body.title, bpm: body.bpm, song: song(body.setlistItemId), creatorName: ME, canManage: true, tracks: [] }
      state.jams.unshift(jam)
      return view(jam)
    }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/jams\/(\d+)\/tracks\/(\d+)\/file$/))) return { raw: state.files.get(Number(m[2])) }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/jams\/(\d+)\/tracks\/(\d+)$/))) {
      const jam = find(m[1])
      const t = jam.tracks.find(item => item.id === Number(m[2]))
      if (method === 'DELETE') { jam.tracks = jam.tracks.filter(item => item !== t); return null }
      const body = request.postDataJSON()
      state.patches.push(body)
      Object.assign(t, body)
      return trackView(jam, t)
    }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/jams\/(\d+)\/tracks$/))) {
      const jam = find(m[1])
      const upload = multipart(request.postDataBuffer())
      state.uploads.push(upload)
      const t = track({ userId: 1, nickname: ME, part: 'GUITAR', name: upload.name, type: 'audio/webm', canDelete: true, ...upload.json }, upload.body)
      jam.tracks.push(t)
      return trackView(jam, t)
    }
    if ((m = path.match(/^\/api\/v1\/bands\/10\/jams\/(\d+)$/))) {
      const jam = find(m[1])
      if (method === 'DELETE') { state.jams = state.jams.filter(item => item !== jam); return null }
      if (method === 'PUT') { const body = request.postDataJSON(); Object.assign(jam, { title: body.title, bpm: body.bpm, song: song(body.setlistItemId) }) }
      return view(jam)
    }
    return undefined
  }
  return { state, handle }
}

test('레이어 잼 재생·믹서 저장·병합 내려받기·녹음 올리기·잼 만들기와 삭제', async () => {
  const server = spawn(process.execPath, ['node_modules/vite/bin/vite.js', '--host', '127.0.0.1', '--port', '4183', '--strictPort'], { cwd: fileURLToPath(new URL('../', import.meta.url)), stdio: 'pipe', windowsHide: true })
  let browser
  try {
    for (let i = 0; i < 100; i++) {
      try { if ((await fetch('http://127.0.0.1:4183')).ok) break } catch {}
      if (i === 99) throw new Error('서버 시작 실패')
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    // 가짜 마이크(삐 소리)로 실제 MediaRecorder 녹음을 돌린다.
    browser = await chromium.launch({ headless: true, args: ['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream', '--autoplay-policy=no-user-gesture-required'] })
    const context = await browser.newContext({ viewport: { width: 1280, height: 960 }, acceptDownloads: true, permissions: ['microphone'] })
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', e => errors.push(e.message))
    page.on('dialog', dialog => dialog.accept())
    await page.addInitScript(() => localStorage.setItem('accessToken', 'test'))
    const api = createJamApi()
    await page.route('http://localhost:8080/**', async route => {
      const headers = { 'access-control-allow-origin': '*', 'access-control-allow-headers': '*', 'access-control-allow-methods': '*' }
      const request = route.request()
      if (request.method() === 'OPTIONS') return route.fulfill({ status: 204, headers })
      const character = { nickname: ME, instrumentType: 'GUITAR', level: 1 }
      const path = new URL(request.url()).pathname
      let data = api.handle(request.method(), path, request)
      if (data?.raw !== undefined) return route.fulfill({ headers, contentType: 'audio/wav', body: data.raw })
      if (data === undefined) {
        data = path.includes('/bands/') ? { id: 10, name: '합주 밴드', members: [{ memberId: 1, part: 'GUITAR', character }, { memberId: 2, part: 'BASS', character: { nickname: '랍스타' } }] }
          : path.includes('/characters/') ? character : { id: 1, name: '테스트' }
      }
      await route.fulfill({ headers, contentType: 'application/json', body: JSON.stringify({ success: true, data }) })
    })
    await page.goto('http://127.0.0.1:4183/bands/10/jams')

    // 첫 잼을 고르고 두 트랙을 받아 디코딩한다.
    const tracks = page.locator('.tracks > .track')
    await tracks.first().waitFor()
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 2 && !document.querySelector('.tracks')?.textContent.includes('불러오는 중'))
    assert.match(page.url(), /jam=50/)
    assert.equal(await page.locator('.track-error').count(), 0)
    assert.match(await page.locator('.jam-meta').textContent(), /120 BPM · 셋리스트 Hysteria/)
    // 내가 올리지 않은 트랙은 삭제 버튼이 없다.
    assert.equal(await page.getByRole('button', { name: '랍스타 베이스 트랙 삭제' }).count(), 0)

    // 잼의 연결 곡을 누르면 그 곡이 펼쳐진 셋리스트로 가고, 곡 카드에서 다시 잼으로 돌아온다.
    await page.locator('.jam-meta').getByRole('link', { name: 'Hysteria' }).click()
    await page.waitForURL(/\/bands\/10\/setlists$/)
    const songCard = page.locator('#song-card-31')
    await songCard.waitFor()
    assert.equal(await page.getByRole('button', { name: '정기공연 2곡', exact: true }).getAttribute('aria-pressed'), 'true')
    assert.equal(await songCard.locator('.song-toggle').getAttribute('aria-expanded'), 'true')
    assert.match(await songCard.getAttribute('class'), /focused/)
    assert.equal(await page.locator('#song-card-32 .song-toggle').getAttribute('aria-expanded'), 'false')
    // 연결된 잼이 없는 곡에는 잼 버튼이 없다.
    assert.equal(await page.locator('#song-card-32 .jam-go').count(), 0)
    const jamButton = songCard.getByRole('link', { name: /Hysteria 합주/ })
    await mkdir('test-results', { recursive: true })
    await page.screenshot({ path: 'test-results/jams-setlist-link.png' })
    // 앞의 마이크는 선 아이콘(svg, #138)이라 글자에는 잼 이름과 트랙 수만 있다
    assert.match(await jamButton.textContent(), /Hysteria 합주\s*트랙 2/)
    assert.equal(await jamButton.locator('svg').count(), 1)
    await jamButton.click()
    await page.waitForURL(/\/bands\/10\/jams\?jam=50$/)
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 2 && !document.querySelector('.tracks')?.textContent.includes('불러오는 중'))
    // 지워진 곡으로 넘어오면 알려 준다.
    await page.goto('http://127.0.0.1:4183/bands/10/setlists?item=999')
    await page.getByRole('alert').filter({ hasText: '연결한 곡이 셋리스트에서 지워졌어요' }).waitFor()
    await page.goto('http://127.0.0.1:4183/bands/10/jams?jam=50')
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 2 && !document.querySelector('.tracks')?.textContent.includes('불러오는 중'))

    // 재생하면 위치가 움직이고, 정지하면 멈춘다.
    await page.getByRole('button', { name: '재생', exact: true }).click()
    await page.waitForFunction(() => parseFloat(document.querySelector('[data-testid=position]').textContent.split(':')[1]) >= 0.5)
    await page.getByRole('button', { name: '정지', exact: true }).click()
    const stopped = await page.getByTestId('position').textContent()
    await page.waitForTimeout(300)
    assert.equal(await page.getByTestId('position').textContent(), stopped)

    // 볼륨·시작 위치는 서버에 저장하고, 뮤트·솔로는 내 화면에서만 바꾼다.
    await page.getByLabel(`${ME} 일렉기타 볼륨`).evaluate(el => { el.value = '80'; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })) })
    await page.getByRole('button', { name: '랍스타 베이스 음소거' }).click()
    await page.getByLabel(`${ME} 일렉기타 시작 위치`).fill('120')
    await page.getByLabel(`${ME} 일렉기타 시작 위치`).press('Tab')
    await page.waitForFunction(() => document.querySelector('.tracks .track.silent'))
    await new Promise(resolve => setTimeout(resolve, 200))
    assert.deepEqual(api.state.patches, [{ volume: 80 }, { offsetMs: 120 }])
    assert.equal(await page.getByRole('button', { name: '랍스타 베이스 음소거' }).getAttribute('aria-pressed'), 'true')
    await page.getByRole('button', { name: '랍스타 베이스 음소거' }).click()
    await page.getByRole('button', { name: `${ME} 일렉기타 솔로` }).click()
    assert.equal(api.state.patches.length, 2)
    assert.equal(await tracks.first().getAttribute('class'), 'track silent')

    // 들리는 트랙(솔로한 기타 2초 + 시작 위치 120ms)만 WAV로 합친다.
    const [download] = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: '합친 음원 내려받기' }).click()])
    assert.equal(download.suggestedFilename(), 'Hysteria 합주.wav')
    const wav = await readFile(await download.path())
    assert.equal(wav.toString('latin1', 0, 4), 'RIFF')
    assert.equal(wav.toString('latin1', 8, 12), 'WAVE')
    const seconds = wav.readUInt32LE(40) / (44100 * 4)
    assert.ok(Math.abs(seconds - 2.12) < 0.01, `병합 길이 ${seconds}`)
    await page.getByRole('button', { name: `${ME} 일렉기타 솔로` }).click()

    // 마이크를 열고 카운트인 뒤 녹음한다. 시작 위치는 반주보다 먼저 시작한 만큼 음수로 잡힌다.
    await page.getByRole('button', { name: '마이크 연결', exact: true }).click()
    await page.getByLabel('녹음 지연 보정').evaluate(el => { el.value = '50'; el.dispatchEvent(new Event('input', { bubbles: true })) })
    assert.equal(await page.evaluate(() => localStorage.getItem('gainz.jam.latencyMs')), '50')
    await page.getByRole('button', { name: '● 녹음 시작' }).click()
    // 카운트인 중에는 녹음 시작까지 남은 박이 4→3→2→1로 보이고, 끝나면 '녹음 중'으로 바뀐다 (#234, 120 BPM이라 박마다 0.5초)
    const beats = await page.evaluate(async () => {
      const seen = []
      const until = performance.now() + 4000
      while (performance.now() < until) {
        const text = document.querySelector('.count-in')?.textContent.trim()
        if (text && seen.at(-1) !== text) seen.push(text)
        if (!text && seen.length && document.querySelector('.rec-time')) break
        await new Promise(resolve => setTimeout(resolve, 30))
      }
      return seen
    })
    assert.deepEqual(beats, ['4', '3', '2', '1'])
    await page.getByRole('status').filter({ hasText: '녹음 중' }).waitFor()
    await page.waitForTimeout(3200)
    // 마우스를 올려도 녹음 버튼은 회색이 아니라 더 진한 빨간색이 된다.
    const stopButton = page.getByRole('button', { name: '■ 녹음 끝내기' })
    await stopButton.hover()
    await page.waitForTimeout(100)
    assert.equal(await stopButton.evaluate(el => getComputedStyle(el).backgroundColor), 'rgb(201, 42, 42)')
    await page.getByRole('button', { name: '■ 녹음 끝내기' }).click()
    const take = page.locator('.track.take')
    await take.waitFor()
    const offset = Number(await page.getByLabel('새 녹음 시작 위치', { exact: true }).inputValue())
    // 카운트인 4박(2초) + 예약 여유 0.12초 + 지연 50ms 정도 앞당겨진다.
    assert.ok(offset < -2100 && offset > -2600, `녹음 시작 위치 ${offset}`)
    await take.getByRole('button', { name: '+10', exact: true }).click()
    await take.getByRole('button', { name: '+50', exact: true }).click()
    assert.equal(Number(await page.getByLabel('새 녹음 시작 위치', { exact: true }).inputValue()), offset + 60)
    await page.getByRole('button', { name: '트랙 올리기' }).click()
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 3)
    await take.waitFor({ state: 'detached' })
    const upload = api.state.uploads[0]
    assert.equal(upload.json.offsetMs, offset + 60)
    assert.equal(upload.json.volume, 100)
    assert.ok(upload.json.durationMs > 900, `녹음 길이 ${upload.json.durationMs}`)
    assert.match(upload.name, /^layer-jam-\d+\.webm$/)
    assert.equal(upload.body.subarray(0, 4).toString('hex'), '1a45dfa3')
    assert.equal(await tracks.count(), 3)
    assert.equal(await take.count(), 0)
    assert.match(await page.locator('.jam-link.active').textContent(), /트랙 3/)
    assert.match(await page.locator('.jam-link.active small').textContent(), /^셋리스트 · Hysteria$/)

    // 내가 올린 트랙을 지운다.
    const uploadedId = api.state.jams[0].tracks[2].id
    await page.getByRole('button', { name: `${ME} 일렉기타 트랙 삭제` }).last().click()
    await page.getByRole('dialog').getByRole('button', { name: '삭제', exact: true }).click()
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 2)
    assert.equal(await tracks.count(), 2)
    assert.ok(!api.state.jams[0].tracks.some(t => t.id === uploadedId))

    // 따로 녹음한 음원 파일도 들어 보고 시작 위치를 맞춰 올린다. 재생할 수 없는 파일은 거부한다.
    const picker = page.getByLabel('음원 파일 고르기')
    await picker.setInputFiles({ name: 'notes.wav', mimeType: 'audio/wav', buffer: Buffer.from('이건 음원이 아니에요') })
    await page.getByRole('alert').filter({ hasText: '재생할 수 없는 파일' }).waitFor()
    assert.equal(await take.count(), 0)
    await picker.setInputFiles({ name: '베이스 녹음.wav', mimeType: 'audio/wav', buffer: sineWav(1, 330) })
    await take.waitFor()
    assert.match(await take.textContent(), /불러온 파일.*베이스 녹음\.wav/)
    assert.equal(await page.getByLabel('불러온 파일 시작 위치', { exact: true }).inputValue(), '0')
    await take.getByRole('button', { name: '+1초', exact: true }).click()
    await take.getByRole('button', { name: '−10', exact: true }).click()
    await page.getByRole('button', { name: '트랙 올리기' }).click()
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 3)
    await take.waitFor({ state: 'detached' })
    const fileUpload = api.state.uploads[1]
    assert.equal(fileUpload.name, '베이스 녹음.wav')
    assert.equal(fileUpload.body.toString('latin1', 0, 4), 'RIFF')
    assert.deepEqual([fileUpload.json.offsetMs, fileUpload.json.durationMs], [990, 1000])
    assert.equal(await tracks.count(), 3)
    await page.getByRole('button', { name: `${ME} 일렉기타 트랙 삭제` }).last().click()
    await page.getByRole('dialog').getByRole('button', { name: '삭제', exact: true }).click()
    await page.waitForFunction(() => document.querySelectorAll('.tracks .track').length === 2)

    // M·S 뜻을 쉬운 말로 적어 둔다.
    assert.match(await page.locator('.legend').textContent(), /M뮤트: 끄기S솔로: 이 트랙만 듣기둘 다 내 화면에만 적용돼요/)
    // 두 설명이 한 줄에 놓인다.
    const legendTops = await page.locator('.legend li:not(.legend-note)').evaluateAll(items => items.map(item => Math.round(item.getBoundingClientRect().top)))
    assert.equal(new Set(legendTops).size, 1)
    assert.match(await page.getByRole('button', { name: '랍스타 베이스 솔로' }).getAttribute('title'), /^솔로: /)

    // 셋리스트 곡은 입력해야 찾은 곡만 몇 개 보여 준다. 고르면 이름이 채워진다. BPM 범위를 확인한다.
    await page.getByRole('button', { name: '+ 잼 만들기' }).click()
    const dialog = page.getByRole('dialog')
    const search = dialog.getByLabel('셋리스트 곡 검색')
    const options = dialog.locator('.song-option')
    // 입력칸은 다른 화면처럼 굵은 테두리 없이 테두리 색만 테마 색으로 바뀐다 (#142).
    await search.focus()
    assert.deepEqual(await search.evaluate(el => {
      const c = getComputedStyle(el)
      const probe = document.createElement('span')
      probe.style.color = 'var(--c-primary)'
      document.body.append(probe)
      const primary = getComputedStyle(probe).color
      probe.remove()
      return [c.outlineStyle, c.borderTopWidth, c.borderTopColor === primary]
    }), ['none', '1px', true])
    assert.equal(await search.getAttribute('placeholder'), '곡 제목, 아티스트 또는 셋리스트 이름을 입력해 주세요')
    assert.equal(await dialog.getByLabel('잼 이름').getAttribute('placeholder'), '잼 이름을 입력해 주세요')
    await search.fill('MUSE')
    await options.first().waitFor()
    assert.deepEqual(await options.locator('b').allTextContents(), ['Hysteria', 'Time Is Running Out'])
    await search.fill('')
    assert.equal(await options.count(), 0)
    await search.fill('여름축제')
    assert.deepEqual(await options.locator('b').allTextContents(), ['Time Is Running Out'])
    // 곡이 많아도 6개까지만 보여 주고 나머지 수를 알려 준다.
    await search.fill('연습곡')
    assert.equal(await options.count(), 6)
    await dialog.getByText('4곡이 더 있어요. 조금 더 자세히 입력해 주세요.').waitFor()
    await mkdir('test-results', { recursive: true })
    await page.screenshot({ path: 'test-results/jams-song-picker.png' })
    await search.fill('연습곡 10')
    assert.deepEqual(await options.locator('b').allTextContents(), ['연습곡 10'])
    await search.fill('없는 곡')
    await dialog.getByText('찾는 곡이 없어요.').waitFor()
    await search.fill('hyst')
    await options.filter({ hasText: 'Hysteria' }).click()
    assert.equal((await dialog.locator('.picked > span').textContent()).trim(), 'Hysteria · 정기공연')
    assert.equal(await search.count(), 0)
    assert.equal(await dialog.getByLabel('잼 이름').inputValue(), 'Hysteria')
    // 연결을 풀었다가 다시 고를 수 있다.
    await dialog.getByRole('button', { name: '연결 해제' }).click()
    assert.equal(await dialog.locator('.picked').count(), 0)
    await search.fill('hysteria')
    await options.filter({ hasText: 'Hysteria' }).click()
    await dialog.getByLabel('BPM').fill('300')
    await dialog.getByRole('button', { name: '만들기' }).click()
    await dialog.getByRole('alert').filter({ hasText: 'BPM은 40에서 240' }).waitFor()
    await dialog.getByLabel('BPM').fill('90')
    await dialog.getByRole('button', { name: '만들기' }).click()
    await page.getByRole('status').filter({ hasText: '잼을 만들었어요' }).waitFor()
    const created = api.state.jams[0]
    assert.deepEqual([created.title, created.bpm, created.song?.setlistItemId], ['Hysteria', 90, 31])
    assert.match(page.url(), new RegExp(`jam=${created.id}`))
    await page.getByText('아직 트랙이 없어요').waitFor()
    assert.equal(await page.locator('.jam-link').count(), 2)
    await mkdir('test-results', { recursive: true })
    await page.screenshot({ path: 'test-results/jams-desktop.png', fullPage: true })

    // 새 잼을 지우면 남은 잼으로 돌아간다.
    await page.getByRole('button', { name: '삭제', exact: true }).first().click()
    await page.getByRole('dialog').getByRole('button', { name: '삭제', exact: true }).click()
    await page.waitForFunction(() => document.querySelectorAll('.jam-link').length === 1)
    assert.equal(await page.locator('.jam-link').count(), 1)
    assert.match(page.url(), /jam=50/)

    // 모바일에서도 가로로 넘치지 않는다.
    await page.setViewportSize({ width: 390, height: 844 })
    await page.waitForTimeout(200)
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), '모바일 가로 넘침')
    const mobileTops = await page.locator('.legend li:not(.legend-note)').evaluateAll(items => items.map(item => Math.round(item.getBoundingClientRect().top)))
    assert.equal(new Set(mobileTops).size, 1, '모바일에서 M·S 설명이 두 줄로 나뉨')
    await page.screenshot({ path: 'test-results/jams-mobile.png', fullPage: true })
    assert.deepEqual(errors, [])
  } finally {
    await browser?.close()
    server.kill()
  }
})
