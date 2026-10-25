// 아이폰 무음 스위치가 켜져 있어도 레이어 잼 소리가 나게 한다 (#225)
// iOS는 Web Audio(AudioContext)를 "주변 소리"로 보고 무음 스위치를 따른다. <audio>·<video>처럼 "미디어 재생"으로 바꾸면 소리가 난다
// - iOS 17+: navigator.audioSession.type을 'playback'으로 (마이크를 쓰는 동안은 'play-and-record')
// - 그보다 오래된 iOS: 소리 없는 <audio>를 함께 재생해서 미디어 재생으로 바꾼다 (재생하는 동안만)

const IS_IOS = typeof navigator !== 'undefined'
  && (/iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1))

let silentAudio = null

function session() {
  return typeof navigator !== 'undefined' ? navigator.audioSession : undefined
}

// 0.5초짜리 무음 WAV (8kHz, 8비트 모노. 8비트는 128이 무음)
function silentWavUrl() {
  const rate = 8000
  const samples = rate / 2
  const view = new DataView(new ArrayBuffer(44 + samples))
  const text = (offset, value) => [...value].forEach((c, i) => view.setUint8(offset + i, c.charCodeAt(0)))
  text(0, 'RIFF'); view.setUint32(4, 36 + samples, true); text(8, 'WAVE')
  text(12, 'fmt '); view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, 1, true)
  view.setUint32(24, rate, true); view.setUint32(28, rate, true); view.setUint16(32, 1, true); view.setUint16(34, 8, true)
  text(36, 'data'); view.setUint32(40, samples, true)
  for (let i = 0; i < samples; i++) view.setUint8(44 + i, 128)
  return URL.createObjectURL(new Blob([view.buffer], { type: 'audio/wav' }))
}

// iOS 17+만: 마이크를 쓰는지에 맞춰 오디오 방식을 정한다 (오래된 iOS는 아무것도 하지 않는다)
export function setAudioSession(micOpen) {
  const s = session()
  if (s) s.type = micOpen ? 'play-and-record' : 'playback'
}

// 소리를 내기 직전에 부른다 (누른 순간과 가까울수록 좋다: 오래된 iOS는 사용자 동작 안에서만 재생을 허락한다)
// micOpen: 마이크를 쓰는 중이면 녹음도 되는 방식으로 (이때는 iOS가 원래 무음 스위치를 따르지 않는다)
export function allowSoundInSilentMode(micOpen = false) {
  if (session()) return setAudioSession(micOpen)
  if (!IS_IOS || micOpen) return
  if (!silentAudio) {
    silentAudio = new Audio(silentWavUrl())
    silentAudio.loop = true
    silentAudio.setAttribute('x-webkit-airplay', 'deny')
  }
  silentAudio.play().catch(() => { /* 허락되지 않으면 무음 스위치를 따른다 */ })
}

// 재생을 멈출 때: 무음 <audio>도 멈춘다 (잠금 화면에 재생 중으로 남지 않게)
export function pauseSilentAudio() {
  silentAudio?.pause()
}

// 레이어 잼 화면을 나갈 때: 원래대로
export function resetSilentMode() {
  pauseSilentAudio()
  const s = session()
  if (s) s.type = 'auto'
}
