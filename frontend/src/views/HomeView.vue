<template>
  <div class="gain-z-app" :class="{ 'band-mode': isBandMode }">
    <AppHeader>
      <div class="tab-switcher">
        <button @click="isBandMode = false" :class="{ active: !isBandMode }">SOLO</button>
        <button @click="isBandMode = true" :class="{ active: isBandMode }">BAND</button>
      </div>
    </AppHeader>

    <main class="main-body">
      <div class="content-wrapper">

        <template v-if="!isBandMode">
          <CharacterStage :character="authStore.character">
            <button class="edit-char-btn" @click="router.push('/wardrobe')" title="옷장: 외형·의상·배경 바꾸기">
              <svg viewBox="0 0 24 24" width="13" height="13" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 3l3 2 3-2 5 3-2 4-2-1v12H8V9L6 10 4 6z"></path>
              </svg>
              옷장
            </button>
          </CharacterStage>

          <section class="quick-grid grid-3">
            <button class="card-btn" @click="router.push('/practice')">
              <div class="c-icon">🕒</div>
              <div class="c-text"><strong>ROUTINE TRACKER</strong><span>연습 루틴 및 기록장</span></div>
            </button>
            <button class="card-btn" @click="router.push('/sheets')">
              <div class="c-icon">📂</div>
              <div class="c-text"><strong>WORK BENCH</strong><span>악보 편집 및 보관함</span></div>
            </button>
            <button class="card-btn" @click="router.push('/transcribe')">
              <div class="c-icon">📄</div>
              <div class="c-text"><strong>AI TRANSCRIBER</strong><span>코드 / 오선보 / 타브 악보</span></div>
            </button>
          </section>

          <section class="footer-grid grid-2">
            <div class="data-card">
              <button class="card-title link" @click="isRecordsOpen = true">
                MY RECORDS <span class="record-count">0 / {{ PLACEHOLDER_RECORDS.length }} ›</span>
              </button>
              <!-- 레코드 선반: 실제 보유 레코드는 #47·#48에서 연결 -->
              <div class="shelf" @click="isRecordsOpen = true">
                <div v-for="(row, ri) in SHELF_ROWS" :key="ri" class="shelf-row">
                  <div class="shelf-items">
                    <RecordIcon v-for="(type, i) in row" :key="i" :type="type" locked :size="56" />
                  </div>
                  <div class="shelf-board"></div>
                </div>
              </div>
            </div>
            <PracticeLogCard />
          </section>

          <RecordsModal :open="isRecordsOpen" @close="isRecordsOpen = false" />
        </template>

        <template v-else>
          <nav class="band-nav">
            <div class="band-tabs-row">
              <div class="band-tabs-left">
                <button v-for="band in bandStore.bands" :key="band.id"
                        :class="{ active: activeBand?.id === band.id }"
                        @click="bandStore.selectedBandId = band.id">
                  <span v-if="band.myPosition === 'LEADER'" class="crown" title="내가 리더">👑</span>{{ band.name }}
                </button>
                <button v-if="bandStore.loaded" class="add-band-tab"
                        :disabled="bandStore.bands.length >= MAX_JOINED_BANDS"
                        :title="bandStore.bands.length >= MAX_JOINED_BANDS ? `밴드는 ${MAX_JOINED_BANDS}개까지 들어갈 수 있어요` : '밴드 만들기 · 초대 코드로 가입'"
                        @click="openStart('create')">+</button>
              </div>

              <button class="match-make-btn" title="밴드 매칭은 준비 중이에요">
                <span class="plus-icon">+</span>
                <span class="btn-lbl">BAND MATCHING</span>
              </button>
            </div>
          </nav>

          <section v-if="!bandStore.loaded" class="stage band-stage band-empty">
            <p v-if="bandStore.error" class="empty-text">
              {{ bandStore.error }}
              <button class="text-btn" @click="bandStore.fetchMyBands()">다시 불러오기</button>
            </p>
            <p v-else class="empty-text">밴드를 불러오는 중…</p>
          </section>

          <section v-else-if="!activeBand" class="stage band-stage band-empty">
            <div class="empty-icon">🎸🥁🎤</div>
            <h2>아직 들어간 밴드가 없어요</h2>
            <p class="empty-text">밴드를 새로 만들거나, 리더에게 받은 초대 코드로 가입해 보세요.</p>
            <div class="empty-actions">
              <button class="pill-btn primary" @click="openStart('create')">밴드 만들기</button>
              <button class="pill-btn" @click="openStart('join')">초대 코드로 가입</button>
            </div>
          </section>

          <section v-else class="stage band-stage band-room">
            <div class="band-info">
              <div>
                <h2>{{ activeBand.name }}의 합주실</h2>
                <p>{{ activeBand.members.length }}명의 멤버가 참여 중<template v-if="activeBand.description"> · {{ activeBand.description }}</template></p>
              </div>
              <div class="band-tools">
                <button class="tool-btn" :title="`초대 코드 ${activeBand.inviteCode} 복사`" @click="copyInviteCode">
                  <span class="code">{{ activeBand.inviteCode }}</span>{{ copied ? '복사됨' : '초대 코드 복사' }}
                </button>
                <button class="tool-btn" @click="router.push(`/bands/${activeBand.id}/settings`)">⚙ 밴드 설정</button>
              </div>
            </div>

            <div class="band-scene">
              <BandStageScene :members="activeBand.members" />
            </div>

            <div class="member-list">
              <div v-for="m in activeBand.members" :key="m.memberId" class="m-item" :class="{ me: m.me }">
                <div class="m-name">
                  <span v-if="m.position === 'LEADER'" title="리더">👑</span>{{ m.character?.nickname || '알 수 없는 멤버' }}
                </div>
                <div class="m-role">{{ partEmoji(m.part) }} {{ partLabel(m.part) }}<template v-if="m.me"> · 나</template></div>
              </div>
            </div>
          </section>

          <BandStartModal :open="isStartOpen" :initial-tab="startTab"
                          :default-part="authStore.character?.instrumentType || 'GUITAR'"
                          @close="isStartOpen = false" @done="onBandStarted" />

          <div class="band-layout-split">
            <div class="band-menus">
              <button class="card-btn large">
                <div class="c-icon">🎶</div>
                <div class="c-text"><strong>BAND SETLIST</strong><span>파트 분배 및 합주 현황 공유</span></div>
              </button>
              <button class="card-btn large">
                <div class="c-icon">🎙️</div>
                <div class="c-text"><strong>LAYER JAM</strong><span>레이어링 방식 비대면 합주</span></div>
              </button>
            </div>

            <section class="band-lounge">
              <div class="card-title">COMMUNITY</div>
              <div class="lounge-box">
                <div class="lounge-msg"><strong>공지</strong> 이번주 일요일 정기 합주!</div>
                <div class="lounge-msg"><strong>활동</strong> 베이스 레이어 업데이트</div>
                <input type="text" class="lounge-input" placeholder="밴드원에게 메시지 남기기">
              </div>
            </section>
          </div>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppHeader from '../components/AppHeader.vue'
import CharacterStage from '../components/CharacterStage.vue'
import RecordsModal from '../components/workspace/RecordsModal.vue'
import PracticeLogCard from '../components/workspace/PracticeLogCard.vue'
import RecordIcon from '../components/workspace/RecordIcon.vue'
import { PLACEHOLDER_RECORDS } from '../constants/records'
import BandStageScene from '../components/band/BandStageScene.vue'
import BandStartModal from '../components/band/BandStartModal.vue'
import { useBandStore } from '../stores/band'
import { MAX_JOINED_BANDS } from '../api/band'
import { INSTRUMENT_OPTIONS } from '../constants/characterOptions'

const router = useRouter()
const authStore = useAuthStore()
const bandStore = useBandStore()
// SOLO / BAND 탭은 스토어에 둬서 밴드 설정에 다녀와도 BAND 탭으로 돌아온다
const isBandMode = computed({
  get: () => bandStore.isBandMode,
  set: (v) => { bandStore.isBandMode = v },
})

const isRecordsOpen = ref(false)
// 홈 선반에 보여 줄 음반 (위: 카세트·CD, 아래: CD·LP)
const SHELF_ROWS = [
  ['CASSETTE', 'CASSETTE', 'CASSETTE', 'CD'],
  ['CASSETTE', 'CD', 'LP', 'LP'],
]
/* ---------- BAND ---------- */

const activeBand = computed(() => bandStore.selectedBand)

// BAND 탭을 열 때마다 내 밴드를 새로 불러온다 (설정 화면에서 바꾼 내용 반영)
watch(isBandMode, (on) => { if (on) bandStore.fetchMyBands() }, { immediate: true })

const isStartOpen = ref(false)
const startTab = ref('create')
const openStart = (tab) => {
  startTab.value = tab
  isStartOpen.value = true
}
const onBandStarted = (band) => {
  isStartOpen.value = false
  bandStore.upsertBand(band, true)
}

const copied = ref(false)
let copiedTimer = null
const copyInviteCode = async () => {
  try {
    await navigator.clipboard.writeText(activeBand.value.inviteCode)
    copied.value = true
    clearTimeout(copiedTimer)
    copiedTimer = setTimeout(() => { copied.value = false }, 1500)
  } catch {
    window.prompt('초대 코드를 복사해 주세요', activeBand.value.inviteCode)
  }
}
onBeforeUnmount(() => clearTimeout(copiedTimer))

const partOption = (part) => INSTRUMENT_OPTIONS.find((o) => o.value === part)
const partLabel = (part) => partOption(part)?.label || part
const partEmoji = (part) => partOption(part)?.emoji || '🎵'
</script>

<style scoped>
/* 전역 초기화: 클릭 시 나타나는 브라우저 기본 포커스 테두리 제거 */
* { box-sizing: border-box; outline: none; -webkit-tap-highlight-color: transparent; }
button { -webkit-appearance: none; -moz-appearance: none; appearance: none; }
button:focus, button:active, button:focus-visible { 
  outline: none !important; 
  box-shadow: none !important; 
  -webkit-tap-highlight-color: transparent !important;
}
button::-moz-focus-inner { border: 0 !important; }

.gain-z-app {
  width: 100%; min-height: 100vh;
  background: #f8f9fa; color: #1a1a1a;
  font-family: 'Pretendard', sans-serif;
}

/* 헤더 가운데 SOLO / BAND 전환 */
.tab-switcher { background: #f1f3f5; padding: 4px; border-radius: 12px; display: flex; }
.tab-switcher button {
  border: none; background: none; padding: 7px 24px; font-weight: 700;
  border-radius: 9px; cursor: pointer; color: #adb5bd; transition: 0.2s; font-size: 0.85rem;
}
.tab-switcher button.active { background: #fff; color: #2d5bff; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

/* 레이아웃 메인 */
.main-body { padding: 40px 20px; }
.content-wrapper { max-width: 1040px; margin: 0 auto; }

/* 스테이지 공통 */
.stage {
  background: #fff; border: 1px solid #e9ecef; border-radius: 28px;
  padding: 70px 40px; text-align: center; margin-bottom: 24px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.02); position: relative;
  z-index: 1; /* 탭보다 낮게 설정 */
}
/* 탭과 연결되는 밴드 스테이지 상단 모서리 조정 */
.band-stage { border-top-left-radius: 0; border-top-right-radius: 0; }


/* 밴드 매칭 탭 (양쪽 잘림 수정 완료) */
.band-nav { margin-bottom: -1px; position: relative; z-index: 10; }
.band-tabs-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between; /* 양 끝 정렬 */
}
.band-tabs-left {
  display: flex;
  gap: 0; /* 탭 사이 틈 제거하여 일체감 형성 */
}
.band-tabs-left button {
  background: #f1f3f5; border: 1px solid #dee2e6; border-bottom: none;
  padding: 10px 24px; border-radius: 16px 16px 0 0; cursor: pointer; color: #adb5bd; font-size: 0.9rem;
  transition: background 0.2s, color 0.2s; position: relative; top: 1px;
  outline: none;
}
.band-tabs-left button:active {
  border-color: #dee2e6 !important;
  top: 1px !important;
}
/* 선택된 탭: 흰색 배경, z-index를 높여 아래 테두리 덮기 */
.band-tabs-left button.active {
  background: #fff; color: #1a1a1a; font-weight: 800;
  border-color: #e9ecef; border-bottom: 2px solid #fff; /* 흰색 선으로 스테이지 선을 지움 */
  padding-bottom: 11px; z-index: 11;
}

/* 우측 매칭 버튼: z-index를 높여 스테이지 위로 올림 */
.match-make-btn {
  background: #fff; border: 1px solid #2d5bff; border-bottom: none; /* dashed 대신 solid로 변경하여 울렁임 방지 */
  padding: 10px 18px; border-radius: 12px 12px 0 0; cursor: pointer;
  display: flex; align-items: center; gap: 8px;
  position: relative; top: 1px;
  z-index: 11; /* stage 위로 올림 */
  transition: background 0.2s; /* 움직임 효과(all) 제거 */
}
.match-make-btn:hover { background: #f0f4ff; }
.match-make-btn .plus-icon { color: #2d5bff; font-weight: 900; font-size: 1.1rem; }
.match-make-btn .btn-lbl { color: #2d5bff; font-size: 0.75rem; font-weight: 800; letter-spacing: 0.5px; }

/* 그리드 & 카드 */
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.card-btn {
  background: #fff; border: 1px solid #e9ecef; border-radius: 20px;
  padding: 24px; display: flex; align-items: center; gap: 18px;
  cursor: pointer; transition: 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94); text-align: left;
}
.card-btn:hover { border-color: #2d5bff; transform: translateY(-4px); box-shadow: 0 10px 20px rgba(45,91,255,0.06); }
.c-icon { font-size: 1.8rem; background: #f1f5ff; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; border-radius: 14px; }
.c-text strong { display: block; font-size: 0.95rem; margin-bottom: 2px; }
.c-text span { font-size: 0.75rem; color: #868e96; }

/* 데이터 카드 */
.data-card { background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 24px; }
.card-title { font-weight: 800; font-size: 0.85rem; margin-bottom: 18px; display: flex; justify-content: space-between; color: #495057; }
.card-title.link { width: 100%; background: none; border: none; padding: 0; cursor: pointer; font-family: inherit; }
.card-title.link:hover { color: #2d5bff; }
.record-count { font-size: 0.75rem; color: #adb5bd; }
.shelf { cursor: pointer; display: flex; flex-direction: column; gap: 14px; }
.shelf-items { display: flex; justify-content: space-around; align-items: flex-end; padding: 0 8px; }
.shelf-board {
  height: 8px; border-radius: 3px; margin-top: 2px;
  background: #c08a52; box-shadow: inset 0 -3px 0 #8e6035;
}
.shelf:hover .shelf-items { transform: translateY(-2px); }
.shelf-items { transition: transform 0.2s; }


/* 밴드 전용 */
.crown { margin-right: 4px; font-size: 0.8rem; }
.band-tabs-left .add-band-tab { font-weight: 900; font-size: 1rem; padding: 10px 16px; color: #2d5bff; }
.band-tabs-left .add-band-tab:disabled { color: #ced4da; cursor: not-allowed; }

.band-empty { padding: 56px 24px; }
.band-empty h2 { margin: 12px 0 6px; font-size: 1.25rem; font-weight: 800; }
.empty-icon { font-size: 2rem; letter-spacing: 6px; }
.empty-text { margin: 0; font-size: 0.85rem; color: #868e96; }
.empty-actions { display: flex; justify-content: center; gap: 8px; margin-top: 20px; flex-wrap: wrap; }
.pill-btn {
  border: 1px solid #dee2e6; background: #fff; color: #495057; border-radius: 12px; padding: 10px 18px;
  font-family: inherit; font-weight: 800; font-size: 0.85rem; cursor: pointer; transition: 0.2s;
}
.pill-btn:hover { border-color: #2d5bff; color: #2d5bff; }
.pill-btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.pill-btn.primary:hover { background: #1f47e0; color: #fff; }
.text-btn { margin-left: 8px; border: none; background: none; color: #2d5bff; font-weight: 800; cursor: pointer; font-family: inherit; }

.band-room { padding: 24px; text-align: left; }
.band-info { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.band-info h2 { margin: 0 0 4px; font-size: 1.4rem; font-weight: 800; }
.band-info p { margin: 0; font-size: 0.82rem; color: #868e96; }
.band-tools { display: flex; gap: 6px; flex-wrap: wrap; }
.tool-btn {
  border: 1px solid #e9ecef; background: #fff; border-radius: 10px; padding: 7px 12px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #495057; display: flex; align-items: center; gap: 6px;
  transition: 0.2s;
}
.tool-btn:hover { border-color: #2d5bff; color: #2d5bff; }
.tool-btn .code {
  font-family: ui-monospace, Menlo, Consolas, monospace; letter-spacing: 1px;
  background: #f1f3f5; padding: 2px 6px; border-radius: 6px; color: #1a1a1a;
}
.band-scene { margin-top: 16px; border-radius: 18px; overflow: hidden; border: 1px solid #f1f3f5; }

.member-list { display: flex; justify-content: center; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
.m-item { text-align: center; padding: 8px 12px; border-radius: 12px; background: #f8f9fa; min-width: 88px; }
.m-item.me { background: #f0f4ff; }
.m-name { font-size: 0.8rem; font-weight: 800; margin-bottom: 4px; white-space: nowrap; }
.m-role { font-size: 0.7rem; font-weight: 700; color: #868e96; }

.band-layout-split { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.band-menus { display: flex; flex-direction: column; gap: 16px; }
.band-lounge { background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 24px; display: flex; flex-direction: column; }
.lounge-box { margin-top: 8px; flex: 1; }
.lounge-msg { background: #f8f9fa; padding: 12px; border-radius: 12px; font-size: 0.85rem; margin-bottom: 10px; border: 1px solid #f1f3f5; }
.lounge-msg strong { color: #2d5bff; margin-right: 6px; font-weight: 800; }
.lounge-input { width: 100%; border: 1px solid #e9ecef; padding: 12px; border-radius: 12px; background: #fdfdfd; font-size: 0.85rem; outline: none; transition: 0.2s; }
.lounge-input:focus { border-color: #2d5bff; background: #fff; }

.edit-char-btn {
  position: absolute;
  bottom: 16px;
  right: 20px;
  background: #ffffff;
  border: 1px solid #e9ecef;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 700;
  color: #495057;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.2s;
  box-shadow: 0 2px 5px rgba(0,0,0,0.02);
}

.edit-char-btn:hover {
  background: #f1f3f5;
  color: #2d5bff;
  border-color: #2d5bff;
}

/* 좁은 화면: 무대 아래 경험치와 겹치지 않게 옷장 버튼을 무대 밑으로 */
@media (max-width: 640px) {
  .edit-char-btn { position: static; margin: 0 auto 12px; }
}
</style>