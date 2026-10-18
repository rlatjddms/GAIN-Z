# 레이어 잼: 비동기 합주 멀티트랙 녹음·싱크·병합 조사 (#91)

레이어 잼은 멤버가 각자 시간 날 때 앞사람 트랙을 들으며 자기 파트를 녹음해 쌓는 비대면 합주입니다.
이 문서는 브라우저 녹음, 녹음 지연 보정, 여러 트랙 믹싱·병합, 메트로놈 방식을 비교하고 GAIN-Z가 채택할 구조를 정리합니다.

- 화면: #93 (FE) / API: #92 (BE)
- 음원 관련 선행 조사: #52

## 1. 브라우저 녹음

### 방법 비교

| 방법 | 장점 | 단점 |
| --- | --- | --- |
| `MediaRecorder` | 모든 최신 브라우저 지원, 압축 코덱(Opus·AAC)이라 용량이 작음, 코드가 짧음 | 녹음 시작 시각을 샘플 단위로 알 수 없음(수십 ms 오차), 형식이 브라우저마다 다름 |
| `AudioWorklet`로 PCM 받기 → WAV | 샘플 단위로 정확, 형식이 하나 | 용량이 큼(모노 16bit 44.1kHz 1분 ≈ 5MB), 인코딩·워커 코드 직접 작성 |

### 브라우저별 MediaRecorder 형식

`MediaRecorder.isTypeSupported()`로 아래 순서대로 고릅니다.

| 브라우저 | 기본 형식 | 비고 |
| --- | --- | --- |
| Chrome·Edge | `audio/webm;codecs=opus` | 파일 머리는 EBML(`1A 45 DF A3`) |
| Firefox | `audio/ogg;codecs=opus` (webm도 가능) | 파일 머리는 `OggS` |
| Safari | `audio/mp4` (AAC) | 파일 머리는 `ftyp`. 구버전은 WebM/Opus 디코딩이 안 될 수 있음 |

서버는 확장자나 `Content-Type`을 믿지 않고 파일 머리로 형식을 판별합니다(`BandAttachmentStorage`와 같은 방식).
WebM과 MP4는 영상과 머리가 같으므로 레이어 잼 트랙에서는 `audio/webm`, `audio/mp4`로 저장합니다.

### 녹음 품질 설정

악기 소리는 통화용 처리를 끄는 것이 좋습니다.

```js
navigator.mediaDevices.getUserMedia({
  audio: { echoCancellation: false, noiseSuppression: false, autoGainControl: false },
})
```

반주가 스피커로 나가면 마이크에 다시 들어가므로 **이어폰 사용을 안내**합니다.

### 용량

Opus·AAC는 기본 비트레이트 약 128kbps, 1분에 약 1MB입니다. 한 곡(3~5분) 트랙이 3~5MB이므로
트랙 하나 **30MB**(약 25분)로 제한하면 충분합니다. 기존 밴드 첨부 제한(10MB)과 따로 둡니다.

## 2. 녹음 지연(latency) 측정과 보정

녹음 트랙은 실제 연주보다 늦게 기록됩니다.

```
녹음된 위치 = 실제로 들은 반주 위치 + (출력 지연 + 입력 지연 + 녹음 시작 오차)
```

- 출력 지연: `AudioContext.baseLatency + outputLatency` (블루투스 이어폰은 150ms 이상도 나옴)
- 입력 지연: `MediaStreamTrack.getSettings().latency` (Chrome 지원, 없으면 0)
- 녹음 시작 오차: `MediaRecorder`의 `start` 이벤트 시각과 실제 첫 샘플 사이 차이

### 보정 방법

1. **자동 추정**: 위 값을 더해 기본 지연으로 씁니다. 기기에 따라 오차가 큽니다.
2. **지연 측정(권장)**: 실제 녹음과 같은 경로로 메트로놈 클릭 6번을 스피커로 내보내며 녹음하고,
   녹음 파일에서 클릭이 시작된 지점(진폭이 갑자기 커지는 곳)을 찾아 예정 시각과의 차이의 **중앙값**을 지연으로 씁니다.
   녹음 시작 오차까지 함께 재므로 가장 정확합니다. 이 측정은 스피커를 써야 합니다.
3. **수동 보정**: ms 슬라이더로 직접 맞춥니다. 측정·수동 값은 기기마다 다르므로 `localStorage`에 기기별로 기억합니다.

### 타임라인 계산

녹음 버튼을 누르면 `recorder.start()` 뒤 `start` 이벤트 때의 `AudioContext.currentTime`(R)을 기록하고,
조금 뒤 시각 T0에 기존 트랙 재생을 예약합니다. T0가 타임라인 0초입니다.

```
새 트랙 시작 위치(offsetMs) = (R − T0) × 1000 − 지연(ms)
```

R < T0이므로 보통 음수입니다. 음수 시작 위치는 "파일 앞부분을 그만큼 잘라 0초에 맞춘다"는 뜻입니다.
녹음 후에는 다시 들으며 ±ms 단위로 시작 위치를 미세 조정할 수 있게 합니다.

## 3. 브라우저 믹싱 (Web Audio API)

- 트랙 파일을 `decodeAudioData`로 `AudioBuffer`로 바꿔 둡니다.
- 재생할 때 트랙마다 `AudioBufferSourceNode → GainNode → destination`을 만들고, 같은 시각 기준으로 예약합니다.

```js
const at = ctx.currentTime + 0.1           // 모든 트랙이 함께 시작할 시각
source.start(at + Math.max(0, offset - from), Math.max(0, from - offset))
```

- 볼륨: `GainNode.gain` (0~150%), 음소거·솔로: 들리는 트랙만 연결 (솔로가 하나라도 있으면 솔로 트랙만).
- `AudioBufferSourceNode`는 한 번만 재생되므로 멈추거나 위치를 옮기면 새로 만듭니다.
- `HTMLAudioElement` 여러 개로 재생하면 트랙끼리 수십 ms씩 어긋나므로 쓰지 않습니다.

## 4. 병합 (믹스다운)

### 서버 ffmpeg

```
ffmpeg -i a.webm -i b.ogg -filter_complex \
  "[0]adelay=0|0,volume=0.8[a0];[1]atrim=start=0.12,volume=1.0[a1];[a0][a1]amix=inputs=2:normalize=0" out.mp3
```

- 처리 시간: 4분짜리 4트랙 기준 수 초 (재생 시간의 수십 분의 1)
- 저장 용량: 결과 MP3 192kbps 4분 ≈ 6MB가 잼마다 추가로 쌓임
- 단점: 서버(배포·개발 PC 모두)에 ffmpeg 설치가 필요하고, 외부 프로세스 실행·시간 제한·임시 파일 관리가 필요함

### 브라우저 OfflineAudioContext

- 화면의 믹서와 같은 그래프를 `OfflineAudioContext`에 만들고 `startRendering()`으로 실제 시간보다 빠르게 합칩니다.
- 결과 `AudioBuffer`를 16bit WAV로 만들어 바로 내려받습니다 (스테레오 4분 ≈ 40MB, 저장하지 않으므로 서버 용량 영향 없음).
- 지금 들리는 볼륨·음소거·솔로·시작 위치가 그대로 반영됩니다.
- 이미 모든 트랙을 디코딩해 둔 상태라 추가 다운로드가 없습니다.

## 5. 메트로놈·기준 트랙

| 방식 | 비고 |
| --- | --- |
| 클릭 음원 파일을 반복 재생 | 파일 관리 필요, 템포 바꾸기 어려움 |
| Web Audio로 직접 생성 (권장) | `OscillatorNode`를 30ms 정도 짧게 울림. 첫 박은 높은 음. `AudioContext` 시각으로 미리 예약하므로 트랙과 정확히 맞음 |

- 잼에 BPM을 저장하고, 녹음 전 한 마디(4박) 카운트인을 줍니다.
- 메트로놈은 듣기용이며 병합 결과에는 넣지 않습니다.
- 첫 녹음자는 메트로놈만 듣고 녹음하므로 메트로놈이 사실상 기준 트랙입니다.

## 6. 결론

| 항목 | 채택 |
| --- | --- |
| 녹음 | `MediaRecorder` (webm/ogg/mp4, Opus·AAC), 통화용 처리 끔, 이어폰 안내 |
| 지연 보정 | 자동 추정 + 클릭 녹음 지연 측정 + 수동 ms 슬라이더 (기기별 `localStorage`) |
| 동시 재생 | Web Audio `AudioBufferSourceNode` + `GainNode`, 볼륨·음소거·솔로 |
| 시작 위치 | 트랙마다 `offsetMs` 저장 (음수 = 앞을 잘라냄), 녹음 후 미세 조정 |
| 병합 | **브라우저 `OfflineAudioContext` → WAV 내려받기**. 서버 ffmpeg는 설치 의존성 때문에 채택하지 않음 |
| 메트로놈 | Web Audio 클릭 생성, 잼 BPM, 한 마디 카운트인 |

### #92 (BE) 범위

- `Jam`(밴드, 만든 멤버, 제목, BPM, 연결한 셋리스트 곡 id·이름) / `JamTrack`(올린 멤버, 파트, 파일, 볼륨, 시작 위치, 음소거, 길이)
- 잼 만들기·목록·조회·수정·삭제, 트랙 올리기·내려받기·볼륨/시작 위치/음소거 수정·삭제 (모두 밴드 멤버만)
- 트랙 파일은 기존 밴드 파일 저장소를 쓰되 오디오 전용 판별(webm·ogg·mp4·mp3·wav)과 30MB 제한
- 병합은 브라우저에서 하므로 서버 병합 API는 만들지 않음 (ffmpeg를 도입하면 별도 이슈로)

### #93 (FE) 범위

- 홈 BAND 탭 `LAYER JAM` 카드 → 잼 목록·만들기 → 잼 화면(믹서)
- 트랙 동시 재생, 볼륨·음소거·솔로, 메트로놈
- 기존 트랙·메트로놈을 들으며 녹음, 지연 측정·수동 보정, 다시 듣기 후 올리기, 시작 위치 미세 조정
- 합친 음원(WAV) 내려받기
