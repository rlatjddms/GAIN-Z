import { audibleTracks, detectOnsets, encodeWav, estimateLatencyMs, pickRecordingType, timelineLength, trackSchedule } from './jamAudio'
import { allowSoundInSilentMode, pauseSilentAudio, resetSilentMode, setAudioSession } from './silentMode'

const LEAD = 0.12 // 예약한 소리가 늦지 않게 지금보다 조금 뒤에 시작한다
const LOOKAHEAD = 0.15
const CLICK_TIMES = [0, 0.5, 1, 1.5, 2, 2.5]

// 레이어 잼 오디오: 트랙 동시 재생, 메트로놈, 녹음, 지연 측정, 병합
// tracks: [{ id, buffer, volume(0~150), offsetMs, muted }]
export class JamEngine {
  constructor() {
    this.ctx = null
    this.stream = null
    this.nodes = new Map()
    this.metronome = null
    this.startedAt = 0
    this.from = 0
    this.playing = false
    this.onEnded = null
    this.endTimer = null
  }

  context() {
    if (!this.ctx) this.ctx = new AudioContext({ latencyHint: 'interactive' })
    return this.ctx
  }

  async decode(arrayBuffer) {
    return this.context().decodeAudioData(arrayBuffer)
  }

  get position() {
    return this.playing ? this.from + Math.max(0, this.ctx.currentTime - this.startedAt) : this.from
  }

  // 출력·입력 지연 합(ms). 기기가 알려 주는 값이라 오차가 있다
  estimatedLatencyMs() {
    const ctx = this.context()
    const input = this.stream?.getAudioTracks()[0]?.getSettings?.().latency || 0
    return Math.round(((ctx.baseLatency || 0) + (ctx.outputLatency || 0) + input) * 1000)
  }

  // 같은 시각(at)에 들리는 트랙을 모두 예약한다
  schedule(ctx, tracks, soloIds, from, at, destination = ctx.destination) {
    const nodes = new Map()
    for (const track of audibleTracks(tracks, soloIds)) {
      if (!track.buffer) continue
      const timing = trackSchedule(track.offsetMs, track.buffer.duration, from)
      if (!timing) continue
      const source = ctx.createBufferSource()
      const gain = ctx.createGain()
      source.buffer = track.buffer
      gain.gain.value = track.volume / 100
      source.connect(gain).connect(destination)
      source.start(at + timing.delay, timing.start)
      nodes.set(track.id, { source, gain })
    }
    return nodes
  }

  // bpm·metronome: 메트로놈을 함께 울림, countIn: 시작 전 몇 박 세기, endless: 트랙이 끝나도 멈추지 않음(녹음 중)
  async play(tracks, soloIds, from, { bpm = 0, metronome = false, countIn = 0, endless = false } = {}) {
    this.stop()
    // 아이폰 무음 스위치가 켜져 있어도 들리게 (#225). 누른 순간과 가깝게 await보다 먼저
    allowSoundInSilentMode(!!this.stream)
    const ctx = this.context()
    await ctx.resume()
    const lead = bpm ? countIn * (60 / bpm) : 0
    const at = ctx.currentTime + LEAD + lead
    this.nodes = this.schedule(ctx, tracks, soloIds, from, at)
    if (bpm && (metronome || countIn)) this.startMetronome(bpm, from, at, countIn, metronome)
    this.startedAt = at
    this.from = from
    this.playing = true
    const remaining = timelineLength(tracks.filter(track => track.buffer).map(track => ({ offsetMs: track.offsetMs, duration: track.buffer.duration }))) - from
    // 녹음 중이거나 트랙 없이 메트로놈만 켠 경우는 직접 멈출 때까지 계속 재생한다
    if (remaining > 0 && !endless) this.endTimer = setTimeout(() => { this.stop(true); this.onEnded?.() }, (at - ctx.currentTime + remaining) * 1000 + 50)
    return at
  }

  stop(toStart = false) {
    if (this.playing) this.from = toStart ? 0 : this.position
    this.playing = false
    clearTimeout(this.endTimer)
    for (const { source } of this.nodes.values()) { try { source.stop() } catch { /* 이미 끝남 */ } }
    this.nodes.clear()
    this.stopMetronome()
    pauseSilentAudio()
  }

  seek(position) {
    this.from = Math.max(0, position)
  }

  setVolume(trackId, volume) {
    const node = this.nodes.get(trackId)
    if (node) node.gain.gain.setTargetAtTime(volume / 100, this.ctx.currentTime, 0.01)
  }

  // 박마다 짧은 소리를 낸다. 첫 박은 높게. 미리 조금씩 예약해서 트랙과 같은 시계로 맞춘다
  // continuous가 false면 카운트인만 울리고 멈춘다
  startMetronome(bpm, from, at, countIn = 0, continuous = true) {
    this.stopMetronome()
    const ctx = this.context()
    const beat = 60 / bpm
    const first = Math.ceil(from / beat - 1e-6)
    let index = first - countIn
    const timeOf = i => at + (i * beat - from)
    const tick = () => {
      while (timeOf(index) < ctx.currentTime + LOOKAHEAD) {
        if (!continuous && index >= first) return this.stopMetronome()
        if (timeOf(index) >= ctx.currentTime - 0.01) this.click(timeOf(index), ((index % 4) + 4) % 4 === 0)
        index++
      }
    }
    this.metronome = setInterval(tick, 25)
    tick()
  }

  stopMetronome() {
    clearInterval(this.metronome)
    this.metronome = null
  }

  click(time, accent, ctx = this.context(), volume = 0.35) {
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.frequency.value = accent ? 1600 : 1000
    gain.gain.setValueAtTime(volume, time)
    gain.gain.exponentialRampToValueAtTime(0.001, time + 0.04)
    osc.connect(gain).connect(ctx.destination)
    osc.start(time)
    osc.stop(time + 0.05)
  }

  // 악기 소리가 바뀌지 않게 통화용 처리를 끄고 마이크를 연다
  async openMic() {
    if (this.stream) return this.stream
    setAudioSession(true)
    this.stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: false, noiseSuppression: false, autoGainControl: false } })
    return this.stream
  }

  closeMic() {
    this.stream?.getTracks().forEach(track => track.stop())
    this.stream = null
    setAudioSession(false)
  }

  // 녹음을 시작하고 recordStart(녹음 시작 시각)를 알려 준다. stop()이 녹음 파일을 돌려준다
  async startRecorder() {
    const ctx = this.context()
    await ctx.resume()
    const mimeType = pickRecordingType(type => MediaRecorder.isTypeSupported(type))
    const recorder = new MediaRecorder(this.stream, mimeType ? { mimeType } : {})
    const chunks = []
    recorder.ondataavailable = event => { if (event.data.size) chunks.push(event.data) }
    await new Promise((resolve, reject) => {
      recorder.onstart = resolve
      recorder.onerror = event => reject(event.error)
      recorder.start()
    })
    const recordStart = ctx.currentTime
    const stop = () => new Promise(resolve => {
      recorder.onstop = () => resolve(new Blob(chunks, { type: recorder.mimeType || mimeType || 'audio/webm' }))
      if (recorder.state === 'inactive') recorder.onstop()
      else recorder.stop()
    })
    return { recordStart, stop }
  }

  // 기존 트랙(과 메트로놈)을 들으며 녹음한다. 반환값 timelineStart는 반주 0초가 시작된 시각
  async record(tracks, soloIds, options = {}) {
    this.stop(true)
    const { recordStart, stop } = await this.startRecorder()
    const timelineStart = await this.play(tracks, soloIds, 0, { ...options, endless: true })
    return { recordStart, timelineStart, stop: async () => { const blob = await stop(); this.stop(true); return blob } }
  }

  // 스피커로 클릭 6번을 내며 녹음해서 실제 지연(ms)을 잰다. 소리를 못 찾으면 null
  async measureLatency() {
    this.stop(true)
    const ctx = this.context()
    const { recordStart, stop } = await this.startRecorder()
    const start = ctx.currentTime + 0.4
    CLICK_TIMES.forEach(t => this.click(start + t, true, ctx, 0.9))
    await new Promise(resolve => setTimeout(resolve, (start - ctx.currentTime + CLICK_TIMES.at(-1) + 1.2) * 1000))
    const blob = await stop()
    const buffer = await this.decode(await blob.arrayBuffer())
    const onsets = detectOnsets(buffer.getChannelData(0), buffer.sampleRate)
    return estimateLatencyMs(onsets, CLICK_TIMES.map(t => start + t - recordStart))
  }

  // 지금 들리는 볼륨·음소거·솔로·시작 위치 그대로 스테레오 WAV로 합친다
  async mixdown(tracks, soloIds, sampleRate = 44100) {
    const playable = audibleTracks(tracks, soloIds).filter(track => track.buffer)
    const length = timelineLength(playable.map(track => ({ offsetMs: track.offsetMs, duration: track.buffer.duration })))
    if (!playable.length || length <= 0) return null
    const offline = new OfflineAudioContext(2, Math.ceil(length * sampleRate), sampleRate)
    this.schedule(offline, tracks, soloIds, 0, 0)
    return new Blob([encodeWav(await offline.startRendering())], { type: 'audio/wav' })
  }

  close() {
    this.stop()
    this.closeMic()
    resetSilentMode()
    this.ctx?.close()
    this.ctx = null
  }
}
