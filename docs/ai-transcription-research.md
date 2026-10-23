# AI 악보 만들기: 음원 → 타브·코드 기술 조사 (#52)

사용자가 음원(MP3·WAV·M4A)을 올리면 그 사람 악기(기타·베이스)에 맞는 타브와 코드 악보를 만들어 `SheetMusic`·`TabData`로 저장하는 기능(#53)을 위한 조사입니다.
조사 시점은 2026-10이고, 확인하지 못한 값은 **(미확인)**으로 표시했습니다.

- 구현: #53 / 화면: `TranscribeView.vue`
- 결과 형식: [`docs/note-data-format.md`](note-data-format.md) (마디 하나 = `TabData` 한 줄, `noteData` JSON)

## 0. 결론 요약

- **직접 돌리는 조합(MVP 추천)**: Demucs `htdemucs_6s`(음원 분리) → Beat This!(비트·다운비트) → BTC(코드) → Basic Pitch(음 추출) → 직접 짠 동적계획법(줄·프렛). 전부 MIT 또는 Apache-2.0이라 상업적으로 써도 됩니다.
- **악보를 바로 주는 API**는 Klangio 하나만 확인했습니다(GP5·MusicXML·MIDI, 코드·비트 포함, 월 $99에 500건, 곡 최대 300초). 직접 만든 결과가 부족할 때의 대안으로 둡니다.
- **기대치**: 기타 타브 연구의 대표 지표(GuitarSet, 줄까지 맞혀야 하는 Tab F1)가 0.72~0.75 수준입니다. 그래서 타브는 "AI 초안"으로 보여 주고 사용자가 고치게 하며, **코드 악보와 마디·박자를 핵심 가치**로 둡니다.
- **상업 사용 주의**: madmom 모델(비상업), Essentia(AGPL, 모델 비상업), Chordino(GPL, 미확인)는 쓰지 않습니다.

## 1. 처리 단계

```
음원 업로드 → 전처리(ffmpeg, 44.1kHz WAV, 길이 제한)
  → 음원 분리 (기타·베이스 stem)
  → 비트·다운비트 (원곡 믹스)  → 마디 경계·박자
  → 코드 인식 (원곡 믹스)      → 마디별 chords[{beat, name}]
  → 음 추출 (내 악기 stem)     → 음 시작·길이·높이
  → 줄·프렛 배정               → notes[{beat, duration, string, fret}]
  → 마디별 noteData JSON → TabData 저장
```

## 2. 음원 분리

| 이름 | 유형 | 라이선스 | 기타 stem | 메모 | 속도·비용 (4분 곡) |
| --- | --- | --- | --- | --- | --- |
| **Demucs `htdemucs_6s`** | 오픈소스 | MIT | 있음 | 기타는 쓸 만함. 2025-01 저장소 보관(archived), pip로는 계속 사용 가능 | CPU 약 6분, GPU 수십 초 (미확인). GPU 메모리 3~7GB |
| Spleeter | 오픈소스 | MIT | 없음 | 오래됐고 품질이 낮음 | 빠름 |
| Open-Unmix | 오픈소스 | MIT | 없음 | 기준선 용도 | 보통 |
| BS/MelBand-RoFormer | 오픈소스 구현 | 코드 MIT, **가중치마다 다름** | 커뮤니티 체크포인트 | 보컬 분리는 최상위권, 가중치 라이선스가 불명확 | GPU 권장 |
| Music.AI (Moises) | API | 상용 | 있음 | 상용 품질 | 분당 $0.07~0.10 → 약 $0.3~0.4 |
| LALAL.AI · AudioShake | API | 상용 | 있음 | 공식 API 단가 (미확인) | — |
| Replicate의 Demucs | 호스팅 | MIT | 있음 | Demucs와 같음 | 약 65초, 실행당 약 $0.06 |

## 3. 음 추출 (오디오 → 음표)

| 이름 | 유형 | 라이선스 | 메모 |
| --- | --- | --- | --- |
| **Spotify Basic Pitch** | 오픈소스 | Apache-2.0 | 여러 음 동시 인식, 악기 무관, 가벼워서 CPU로 가능. 악기 하나만 있을 때 잘 되므로 분리 뒤에 넣음. CPU 속도 수치는 직접 측정 필요 |
| MT3 | 오픈소스 | Apache-2.0 | 여러 악기 동시, 무거움(GPU) |
| YourMT3+ | 오픈소스 | GPL-3.0 | 서버 안에서만 쓰면 가능하나 주의 |
| TabCNN · FretNet | 연구용 | 저장소마다 다름 | 기타 줄·프렛을 직접 예측(Tab F1 약 0.75). 어쿠스틱 단독 녹음으로 학습해 밴드 음원·일렉에 약함 |
| TART (2025~2026) | 연구용 | 코드 공개 (미확인) | 음 → 주법 → 줄·프렛, 제로샷 Tab F1 약 0.72 |

베이스는 단음 위주라 "베이스 stem + Basic Pitch"로 기타보다 잘 나올 것으로 예상합니다(근거 수치는 없음).

## 4. 줄·프렛 배정

| 방법 | 메모 |
| --- | --- |
| **직접 구현: 동적계획법(Viterbi)** | 각 음(화음)마다 가능한 (줄, 프렛) 후보를 만들고, 손 위치 이동·프렛 범위(0~15)·개방현 보너스를 비용으로 둬서 비용이 가장 작은 경로를 고름. 학계 표준 방식. Python 200줄 안팎 예상 |
| FretPath (MuseScore 플러그인) | 같은 방식의 참고 구현 |
| MIDI-to-Tab (2024), 트랜스포머 방식 (2025) | 사람이 짠 듯한 운지. 라이선스 (미확인), 다음 단계 후보 |

조율은 `note-data-format.md`와 같이 표준만 씁니다 (기타 E A D G B E, 베이스 E A D G).

## 5. 코드 인식

| 이름 | 유형 | 라이선스 | 메모 |
| --- | --- | --- | --- |
| **BTC (ISMIR19)** | 오픈소스 | MIT | 장·단3화음 모드와 큰 어휘 모드(7th 등). 가중치 포함 여부 (미확인) |
| madmom | 오픈소스 | 코드 BSD, **모델 비상업** | 정확도 좋지만 상업 사용 불가 |
| Chordino (NNLS-Chroma) | 오픈소스 | GPL (미확인) | 고전적, 안정적 |
| autochord | 오픈소스 | (미확인) | 장·단3화음만, 정확도 67% |
| Essentia | 오픈소스 | AGPL, 모델 비상업 | 상업은 별도 계약 |
| Music.AI Chords | API | 상용 | 분당 $0.04 |
| Chordify | 제품 | — | 공개 개발자 API 없음 |

## 6. 비트·다운비트·박자

| 이름 | 유형 | 라이선스 | 메모 |
| --- | --- | --- | --- |
| **Beat This! (ISMIR 2024)** | 오픈소스 | 코드·가중치 MIT | 비트와 다운비트. CPU 가능. **박자 추정은 없음** → 다운비트 사이 비트 수로 직접 계산 |
| madmom DBN | 오픈소스 | 모델 비상업 | 박자 후보 추정 가능, 상업 불가 |
| BeatNet | 오픈소스 | (미확인) | 비트·다운비트·박자 |
| librosa `beat_track` | 오픈소스 | ISC | 비트·템포만, 정확도 낮음 |

## 7. 악보를 바로 주는 서비스

| 서비스 | 개발자 API | 출력 | 요금 |
| --- | --- | --- | --- |
| **Klangio** | 있음 (REST) | 기타·베이스 채보를 GP5·MusicXML·MIDI로, 비트·박자·코드 | Basic 무료(월 50건, 15초 제한) / Startup 월 $99(500건, 초과 건당 $0.20, **최대 300초**) / Business 월 $499 |
| Music.AI (Moises) | 있음 | 분리·코드·비트·범용 채보(분당 $0.08). 타브를 직접 주는지 (미확인) | 종량제 |
| Songsterr AI | 없음 | 앱 안에서만 | — |

Klangio를 쓰면 GP5(줄·프렛 포함)나 MusicXML을 읽어 `noteData`로 바꾸는 일만 하면 됩니다.

## 8. `TabData.noteData`로 바꾸기

1. **마디 나누기**: 다운비트에서 다음 다운비트까지를 한 마디로 잡고, `measureNumber`를 1부터 매깁니다.
2. **박자**: 마디 안 비트 수의 최빈값으로 분자를 정합니다(4 → `4/4`, 3 → `3/4`). 바뀌는 마디에만 `timeSignature`를 적습니다.
3. **박 위치**: 음·코드의 시작 시각(초)을 그 마디의 비트 시각들 사이로 보간해 "마디 첫 박에서 몇 박 뒤"(4분음표 = 1)로 바꾸고, **0.25(16분음표)** 단위로 맞춥니다. 셋잇단은 MVP에서 다루지 않습니다.
4. **길이**: 끝 시각도 같은 방법으로 바꿔 `duration = 끝 - 시작`, 최소 0.25.
5. **코드**: 코드가 바뀌는 시점만 `chords[{beat, name}]`로 적고, 이름은 `C`, `F#m7`, `D/F#` 형식으로 맞춥니다. 코드 없음은 `N.C.`.
6. **음**: 줄·프렛 배정 결과를 `notes[{beat, duration, string, fret}]`로 적습니다. 1번 줄이 가장 높은 음입니다. 주법(`technique`)은 MVP에서 비웁니다.
7. **악보 정보**: `SheetMusic`에 `bpm`(비트 간격 평균), `instrumentType`, `sheetTypes`(타브·코드)를 채웁니다. `originalKey`는 코드 분포로 추정하거나 비워 둡니다.

## 9. Spring Boot와 연동

```
[Vue] 업로드 ─▶ [Spring] 파일 저장 + 작업(job) 생성(대기) ─▶ Redis 큐
                                                          │
                     [Python 워커: FastAPI + RQ/Celery] ◀─┘
                     분리 → 비트 → 코드 → 음 → 줄·프렛 → noteData JSON
                                                          │
[Spring] 결과 받아 SheetMusic·TabData 저장, 상태 완료 ◀───┘ (콜백 또는 결과 큐)
   └─ 알림(Notification, 실시간 SSE)으로 "악보가 완성됐어요"
```

- **비동기 필수**: 4분 곡이 CPU로 수 분, GPU로 30초~1분 걸려서 업로드 요청 안에서 처리하지 않습니다.
- **상태**: 대기 → 진행(단계별 %) → 완료 / 실패. 화면은 상태 조회 API를 주기적으로 부르거나 알림으로 받습니다.
- **큐**: 이미 쓰는 Redis를 그대로 씁니다. 워커는 별도 Python 서버(도커)로 띄웁니다.
- **GPU**: 처음엔 CPU 워커로 시작하고, 느리면 Modal(사용한 초만 과금, T4 기준 곡당 약 $0.01~0.02 추정)이나 Replicate로 분리 단계만 옮깁니다.
- **실패 처리**: 단계별 타임아웃, 2회 재시도, 실패 사유(형식·길이·처리 오류) 에러 코드.

## 10. 비용 추정 (곡당, 직접 측정 필요)

| 방식 | 시간 | 비용 |
| --- | --- | --- |
| 자체 CPU 워커 | 5~8분 | 서버 비용만 |
| 자체 + GPU 서버리스 | 약 1분 | 약 $0.01~0.02 |
| Music.AI 조합 (분리·코드·비트·채보) | 빠름 | 약 $0.9 |
| Klangio | 빠름 | 약 $0.2 (Startup 기준) |

## 11. 저작권 (법률 자문 아님)

상용곡 음원을 처리하고 만든 악보를 다른 사람과 공유하면 복제·2차적저작물 문제가 될 수 있습니다. MVP에서는 다음을 지킵니다.

- 만든 악보는 올린 본인(과 그 사람이 셋리스트로 공유한 밴드)만 봅니다. 공개 검색·공유 기능은 넣지 않습니다.
- 원본 음원은 처리가 끝나면 지우거나 짧게 보관합니다.
- 약관에 "권리를 가진 음원만 올린다"와 신고 시 내리는 절차를 둡니다.

## 12. 구현 범위 (#53)

**1단계 (MVP)**

- 업로드: MP3·WAV·M4A, 용량·길이 제한(예: 20MB, 6분), 파일 머리로 형식 판별
- 작업 API: 생성, 상태 조회(대기·진행·완료·실패), 실패 시 다시 시도
- Python 워커: Demucs → Beat This! → BTC(장·단3화음) → Basic Pitch → DP 줄·프렛
- 결과: `SheetMusic` + 마디별 `TabData`, "AI 초안" 표시, 완료 알림
- 악기: **베이스 먼저**(단음이라 정확도가 높음), 그다음 기타

**2단계**

- 코드 큰 어휘(7th 등), 키 추정, 섹션 이름
- 타브 직접 수정 화면
- 품질이 부족하면 Klangio API로 바꾸거나 함께 쓰기

**대안**

- 타브 정확도가 계속 부족하면 범위를 **코드 악보 + 마디·박자**로 줄이고 타브는 베이스만 제공합니다.

## 13. 확인하지 못한 항목

FretNet·TART·autochord·Chordino·BeatNet 라이선스, Basic Pitch CPU 속도, BTC 사전학습 가중치 포함 여부, LALAL.AI·AudioShake API 단가, Music.AI 타브 출력 여부. 구현 전에 샘플 곡 3~5개로 속도와 정확도를 직접 재 봅니다.

## 출처

- Demucs: https://github.com/facebookresearch/demucs · Replicate: https://replicate.ai/pricing
- Music.AI: https://music.ai/pricing , https://music.ai/docs/faq/
- AudioShake: https://developer.audioshake.ai/api/overview
- Basic Pitch: https://github.com/spotify/basic-pitch · YourMT3: https://github.com/mimbres/YourMT3
- TabCNN·일렉 강건성: https://arxiv.org/html/2405.14679v3 · FretNet: https://arxiv.org/pdf/2212.03023 · SynthTab: https://arxiv.org/pdf/2309.09085
- TART: https://arxiv.org/pdf/2510.02597
- MIDI-to-Tab: https://arxiv.org/html/2408.05024v1 · FretPath: https://musescore.org/en/project/fretpath
- BTC: https://github.com/jayg996/BTC-ISMIR19 · madmom: https://github.com/CPJKU/madmom
- Essentia 라이선스: https://essentia.upf.edu/licensing_information.html · autochord: https://pypi.org/project/autochord/0.1.2
- Beat This!: https://github.com/CPJKU/beat_this
- Klangio API: https://klang.io/api
