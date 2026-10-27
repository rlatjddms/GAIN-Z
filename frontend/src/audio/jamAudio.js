// 레이어 잼 계산 도우미 (브라우저 API 없이 동작해서 node 테스트에서도 쓴다). 근거: docs/layer-jam-research.md

export const JAM_TRACK_MAX_BYTES = 50 * 1024 * 1024
export const JAM_MAX_TRACKS = 8
export const JAM_VOLUME_MAX = 150
export const JAM_OFFSET_MIN_MS = -10000
export const JAM_OFFSET_MAX_MS = 600000
export const LATENCY_MAX_MS = 1000
// 따로 녹음한 음원 파일 (서버가 파일 머리로 다시 확인한다)
export const JAM_FILE_ACCEPT = 'audio/mpeg,audio/wav,audio/x-wav,audio/mp4,audio/x-m4a,audio/ogg,audio/webm,.mp3,.wav,.m4a,.ogg,.oga,.opus,.webm'

// 브라우저가 녹음할 수 있는 형식 중 서버가 받는 순서대로 고른다
const RECORDING_TYPES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4']
export function pickRecordingType(isSupported) {
  return RECORDING_TYPES.find(type => { try { return isSupported(type) } catch { return false } }) || ''
}
export function recordingExtension(type) {
  if (type.includes('ogg')) return 'ogg'
  if (type.includes('mp4')) return 'm4a'
  return 'webm'
}

export const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

// 녹음 파일 0초가 타임라인 어디에 놓이는지. recordStart: 녹음이 시작된 AudioContext 시각, timelineStart: 반주 0초 시각
export function recordedOffsetMs(recordStart, timelineStart, latencyMs) {
  return Math.round((recordStart - timelineStart) * 1000 - latencyMs)
}

// 타임라인 from초부터 재생할 때 트랙을 언제(지금부터 몇 초 뒤), 파일 몇 초부터 틀지. 이미 끝난 트랙은 null
export function trackSchedule(offsetMs, duration, from) {
  const offset = offsetMs / 1000
  if (from >= offset + duration) return null
  return { delay: Math.max(0, offset - from), start: Math.max(0, from - offset) }
}

// 트랙들이 모두 끝나는 시각(초)
export function timelineLength(tracks) {
  return tracks.reduce((max, track) => Math.max(max, track.offsetMs / 1000 + (track.duration || 0)), 0)
}

// 솔로가 하나라도 있으면 솔로 트랙만, 아니면 음소거하지 않은 트랙만 들린다
export function audibleTracks(tracks, soloIds) {
  const solo = tracks.filter(track => soloIds.includes(track.id))
  return solo.length ? solo : tracks.filter(track => !track.muted)
}

// 진폭이 기준을 처음 넘는 지점을 소리가 시작된 곳(초)으로 본다. 한 번 찾으면 gap초 동안은 같은 소리로 친다
export function detectOnsets(samples, sampleRate, { gap = 0.2, ratio = 0.35, floor = 0.02 } = {}) {
  let peak = 0
  for (let i = 0; i < samples.length; i++) peak = Math.max(peak, Math.abs(samples[i]))
  const threshold = Math.max(floor, peak * ratio)
  const onsets = []
  let last = -Infinity
  for (let i = 0; i < samples.length; i++) {
    if (Math.abs(samples[i]) >= threshold && i - last >= gap * sampleRate) {
      onsets.push(i / sampleRate)
      last = i
    }
  }
  return onsets
}

// 예정한 클릭 시각마다 그 뒤에 처음 들린 소리를 짝지어 차이의 중앙값(ms)을 지연으로 본다. 3개 미만이면 측정 실패(null)
export function estimateLatencyMs(onsets, expected, maxLatency = LATENCY_MAX_MS / 1000) {
  const diffs = []
  for (const time of expected) {
    const hit = onsets.find(onset => onset >= time - 0.05 && onset <= time + maxLatency)
    if (hit !== undefined) diffs.push(hit - time)
  }
  if (diffs.length < 3) return null
  diffs.sort((a, b) => a - b)
  const mid = diffs.length >> 1
  const median = diffs.length % 2 ? diffs[mid] : (diffs[mid - 1] + diffs[mid]) / 2
  return Math.max(0, Math.round(median * 1000))
}

// AudioBuffer(또는 같은 모양)를 16bit PCM WAV로 만든다
export function encodeWav(buffer) {
  const channels = buffer.numberOfChannels
  const length = buffer.length
  const data = Array.from({ length: channels }, (_, c) => buffer.getChannelData(c))
  const bytes = 44 + length * channels * 2
  const view = new DataView(new ArrayBuffer(bytes))
  const text = (at, value) => { for (let i = 0; i < value.length; i++) view.setUint8(at + i, value.charCodeAt(i)) }
  text(0, 'RIFF'); view.setUint32(4, bytes - 8, true); text(8, 'WAVE')
  text(12, 'fmt '); view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, channels, true)
  view.setUint32(24, buffer.sampleRate, true); view.setUint32(28, buffer.sampleRate * channels * 2, true)
  view.setUint16(32, channels * 2, true); view.setUint16(34, 16, true)
  text(36, 'data'); view.setUint32(40, length * channels * 2, true)
  let at = 44
  for (let i = 0; i < length; i++) {
    for (let c = 0; c < channels; c++) {
      const sample = clamp(data[c][i], -1, 1)
      view.setInt16(at, sample < 0 ? sample * 0x8000 : sample * 0x7fff, true)
      at += 2
    }
  }
  return view.buffer
}

export function formatTime(seconds) {
  const total = Math.max(0, seconds)
  const minutes = Math.floor(total / 60)
  return `${minutes}:${(total % 60).toFixed(1).padStart(4, '0')}`
}

// 카운트인 중 지금 몇 번째 박인지(1부터) (#234). left: 반주가 시작하기까지 남은 초. 카운트인이 아니면 0.
// 첫 클릭 전 잠깐(소리를 늦지 않게 조금 뒤로 예약한 틈)도 1로 본다. 0이면 화면이 그 사이 "녹음 중"을 보였다 숨겨서 (#294)
export function countInBeatAt(left, bpm, countIn) {
  if (!bpm || !countIn || left <= 0) return 0
  const beat = countIn - Math.ceil(left / (60 / bpm) - 1e-6) + 1
  return Math.min(Math.max(beat, 1), countIn)
}
