import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  audibleTracks, countInBeatAt, detectOnsets, encodeWav, estimateLatencyMs, formatTime, pickRecordingType,
  recordedOffsetMs, recordingExtension, timelineLength, trackSchedule,
} from '../src/audio/jamAudio.js'

// 지정한 시각(초)에 짧은 클릭이 들어 있는 녹음을 흉내 낸다
function clicks(times, { sampleRate = 8000, length = 4, noise = 0.005 } = {}) {
  const samples = new Float32Array(sampleRate * length)
  for (let i = 0; i < samples.length; i++) samples[i] = (Math.sin(i * 12.9898) * noise)
  for (const time of times) {
    const at = Math.round(time * sampleRate)
    for (let i = 0; i < 80; i++) samples[at + i] = Math.sin(i / 2) * (1 - i / 80)
  }
  return samples
}

test('클릭 시작 지점을 찾고 같은 소리는 한 번만 센다', () => {
  const onsets = detectOnsets(clicks([0.5, 1, 1.5]), 8000)
  assert.equal(onsets.length, 3)
  onsets.forEach((onset, i) => assert.ok(Math.abs(onset - (0.5 + i * 0.5)) < 0.002))
})

test('예정 시각과 들린 시각 차이의 중앙값을 지연으로 잰다', () => {
  const expected = [0.4, 0.9, 1.4, 1.9, 2.4, 2.9]
  const heard = expected.map((t, i) => t + (i === 2 ? 0.2 : 0.085))
  const onsets = detectOnsets(clicks(heard), 8000)
  assert.equal(estimateLatencyMs(onsets, expected), 85)
})

test('클릭을 3개 미만으로 찾으면 측정 실패로 본다', () => {
  assert.equal(estimateLatencyMs([0.5, 1.0], [0.4, 0.9, 1.4, 1.9]), null)
  assert.equal(estimateLatencyMs([], [0.4, 0.9, 1.4]), null)
})

test('녹음 시작 위치는 반주보다 먼저 시작한 만큼과 지연만큼 앞으로 당긴다', () => {
  // 녹음이 반주 0초보다 2.12초 먼저 시작했고 지연이 80ms면 파일 0초는 타임라인 -2200ms
  assert.equal(recordedOffsetMs(10, 12.12, 80), -2200)
  assert.equal(recordedOffsetMs(5, 5, 0), 0)
})

test('재생 위치에 따라 트랙을 언제·어디부터 틀지 계산한다', () => {
  assert.deepEqual(trackSchedule(1000, 3, 0), { delay: 1, start: 0 })
  assert.deepEqual(trackSchedule(1000, 3, 2), { delay: 0, start: 1 })
  assert.deepEqual(trackSchedule(-500, 3, 0), { delay: 0, start: 0.5 })
  assert.equal(trackSchedule(0, 3, 3), null)
  assert.equal(timelineLength([{ offsetMs: 500, duration: 2 }, { offsetMs: -200, duration: 1 }]), 2.5)
})

test('솔로가 있으면 솔로 트랙만, 없으면 음소거하지 않은 트랙만 들린다', () => {
  const tracks = [{ id: 1, muted: false }, { id: 2, muted: true }, { id: 3, muted: false }]
  assert.deepEqual(audibleTracks(tracks, []).map(t => t.id), [1, 3])
  assert.deepEqual(audibleTracks(tracks, [2]).map(t => t.id), [2])
})

test('16bit WAV 머리와 샘플을 쓴다', () => {
  const left = Float32Array.from([0, 1, -1, 2])
  const right = Float32Array.from([0.5, 0, 0, -2])
  const wav = new DataView(encodeWav({ numberOfChannels: 2, length: 4, sampleRate: 44100, getChannelData: c => (c ? right : left) }))
  const text = (at, n) => String.fromCharCode(...Array.from({ length: n }, (_, i) => wav.getUint8(at + i)))
  assert.equal(text(0, 4), 'RIFF')
  assert.equal(text(8, 4), 'WAVE')
  assert.equal(wav.getUint16(22, true), 2)
  assert.equal(wav.getUint32(24, true), 44100)
  assert.equal(wav.getUint32(40, true), 16)
  assert.equal(wav.byteLength, 44 + 16)
  assert.equal(wav.getInt16(48, true), 32767) // 1 → 최대
  assert.equal(wav.getInt16(52, true), -32768) // -1 → 최소
  assert.equal(wav.getInt16(56, true), 32767) // 범위를 넘으면 자른다
})

test('녹음 형식은 서버가 받는 것 중 브라우저가 되는 것을 고른다', () => {
  assert.equal(pickRecordingType(type => type.startsWith('audio/webm')), 'audio/webm;codecs=opus')
  assert.equal(pickRecordingType(type => type === 'audio/mp4'), 'audio/mp4')
  assert.equal(pickRecordingType(() => false), '')
  assert.equal(recordingExtension('audio/ogg;codecs=opus'), 'ogg')
  assert.equal(recordingExtension('audio/mp4'), 'm4a')
  assert.equal(recordingExtension('audio/webm;codecs=opus'), 'webm')
  assert.equal(formatTime(65.25), '1:05.3')
})

test('카운트인 중 지금 몇 번째 박인지 알려 준다', () => {
  // 120 BPM 4박: 박마다 0.5초, 첫 클릭은 반주 2초 전. left = 반주까지 남은 초
  assert.equal(countInBeatAt(2.1, 120, 4), 1)  // 첫 클릭 전 (예약 틈)도 첫 박으로 (#294)
  assert.equal(countInBeatAt(2.0, 120, 4), 1)
  assert.equal(countInBeatAt(1.6, 120, 4), 1)
  assert.equal(countInBeatAt(1.5, 120, 4), 2)
  assert.equal(countInBeatAt(0.8, 120, 4), 3)
  assert.equal(countInBeatAt(0.01, 120, 4), 4)
  assert.equal(countInBeatAt(0, 120, 4), 0)    // 반주 시작
  assert.equal(countInBeatAt(1, 120, 0), 0)    // 카운트인 끔
  assert.equal(countInBeatAt(1, 0, 4), 0)
})
