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
    const t = { id: id(), volume: 100, offsetMs: 0, muted: false, createdAt: '2026-10-18T10:00:00', ...props }
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
    return { json, name: file[1], body: Buffer.from(file[2], 'latin1') }
  }
  function handle(method, path, request) {
    let m
    if (path === '/api/v1/bands/10/setlists') return [{ id: 20, title: '정기공연', favorite: false, items: [{ id: 31, title: 'Hysteria', memberIds: [], attachments: [] }] }]
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

    // 재생하면 위치가 움직이고, 정지하면 멈춘다.
    await page.getByRole('button', { name: '재생', exact: true }).click()
    await page.waitForFunction(() => parseFloat(document.querySelector('[data-testid=position]').textContent.split(':')[1]) >= 0.5)
    await page.getByRole('button', { name: '정지', exact: true }).click()
    const stopped = await page.getByTestId('position').textContent()
    await page.waitForTimeout(300)
    assert.equal(await page.getByTestId('position').textContent(), stopped)

    // 볼륨·음소거·시작 위치는 서버에 저장하고, 솔로는 화면에서만 바꾼다.
    await page.getByLabel(`${ME} 일렉기타 볼륨`).evaluate(el => { el.value = '80'; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })) })
    await page.getByRole('button', { name: '랍스타 베이스 음소거' }).click()
    await page.getByLabel(`${ME} 일렉기타 시작 위치`).fill('120')
    await page.getByLabel(`${ME} 일렉기타 시작 위치`).press('Tab')
    await page.waitForFunction(() => document.querySelector('.tracks .track.silent'))
    await new Promise(resolve => setTimeout(resolve, 200))
    assert.deepEqual(api.state.patches, [{ volume: 80 }, { muted: true }, { offsetMs: 120 }])
    assert.equal(await page.getByRole('button', { name: '랍스타 베이스 음소거' }).getAttribute('aria-pressed'), 'true')
    await page.getByRole('button', { name: '랍스타 베이스 음소거' }).click()
    await page.getByRole('button', { name: `${ME} 일렉기타 솔로` }).click()
    assert.equal(api.state.patches.length, 4)
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
    await page.getByRole('button', { name: '🎙️ 마이크 연결' }).click()
    await page.getByLabel('녹음 지연 보정').evaluate(el => { el.value = '50'; el.dispatchEvent(new Event('input', { bubbles: true })) })
    assert.equal(await page.evaluate(() => localStorage.getItem('gainz.jam.latencyMs')), '50')
    await page.getByRole('button', { name: '● 녹음 시작' }).click()
    await page.getByRole('status').filter({ hasText: '녹음 중' }).waitFor()
    await page.waitForTimeout(3200)
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
    await page.getByRole('status').filter({ hasText: '트랙을 올렸어요' }).waitFor()
    const upload = api.state.uploads[0]
    assert.equal(upload.json.offsetMs, offset + 60)
    assert.equal(upload.json.volume, 100)
    assert.ok(upload.json.durationMs > 900, `녹음 길이 ${upload.json.durationMs}`)
    assert.match(upload.name, /^layer-jam-\d+\.webm$/)
    assert.equal(upload.body.subarray(0, 4).toString('hex'), '1a45dfa3')
    assert.equal(await tracks.count(), 3)
    assert.equal(await take.count(), 0)
    assert.match(await page.locator('.jam-link.active').textContent(), /트랙 3/)

    // 내가 올린 트랙을 지운다.
    const uploadedId = api.state.jams[0].tracks[2].id
    await page.getByRole('button', { name: `${ME} 일렉기타 트랙 삭제` }).last().click()
    await page.getByRole('dialog').getByRole('button', { name: '삭제', exact: true }).click()
    await page.getByRole('status').filter({ hasText: '트랙을 삭제했어요' }).waitFor()
    assert.equal(await tracks.count(), 2)
    assert.ok(!api.state.jams[0].tracks.some(t => t.id === uploadedId))

    // 셋리스트 곡을 고르면 이름이 채워지고, BPM 범위를 확인한다.
    await page.getByRole('button', { name: '+ 잼 만들기' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('셋리스트 곡 (선택)').selectOption({ label: '정기공연 · Hysteria' })
    assert.equal(await dialog.getByLabel('잼 이름').inputValue(), 'Hysteria')
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
    await page.getByRole('status').filter({ hasText: '잼을 삭제했어요' }).waitFor()
    assert.equal(await page.locator('.jam-link').count(), 1)
    assert.match(page.url(), /jam=50/)

    // 모바일에서도 가로로 넘치지 않는다.
    await page.setViewportSize({ width: 390, height: 844 })
    await page.waitForTimeout(200)
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), '모바일 가로 넘침')
    await page.screenshot({ path: 'test-results/jams-mobile.png', fullPage: true })
    assert.deepEqual(errors, [])
  } finally {
    await browser?.close()
    server.kill()
  }
})
