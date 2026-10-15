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
            <button class="edit-char-btn" @click="goToEditCharacter" title="캐릭터 수정">
              <svg viewBox="0 0 24 24" width="13" height="13" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              캐릭터 수정
            </button>
          </CharacterStage>

          <section class="quick-grid grid-3">
            <button class="card-btn">
              <div class="c-icon">📄</div>
              <div class="c-text"><strong>AI TRANSCRIBER</strong><span>코드 / 오선보 / 타브 악보</span></div>
            </button>
            <button class="card-btn">
              <div class="c-icon">📂</div>
              <div class="c-text"><strong>WORK BENCH</strong><span>악보 편집 및 보관함</span></div>
            </button>
            <button class="card-btn">
              <div class="c-icon">🕒</div>
              <div class="c-text"><strong>ROUTINE TRACKER</strong><span>연습 루틴 및 기록장</span></div>
            </button>
          </section>

          <section class="footer-grid grid-2">
            <div class="data-card">
              <div class="card-title">MY PICK CASE <span>›</span></div>
              <div class="pick-box">
                <div v-for="i in 6" :key="i" :class="['pick', { locked: i > 3 }]">
                  {{ i <= 3 ? '🔺' : '🔒' }}
                </div>
              </div>
            </div>
            <div class="data-card">
              <div class="card-title">PRACTICE LOG <span>›</span></div>
              <div class="streak">{{ currentStreak }}일 연속 연습 중! 🔥</div>
              <div class="fret-board">
                <div v-for="i in 35" :key="i" :class="['fret', { on: i % 5 === 0 }]"></div>
              </div>
            </div>
          </section>
        </template>

        <template v-else>
          <nav class="band-nav">
            <div class="band-tabs-row">
              <div class="band-tabs-left">
                <button v-for="band in myBands" :key="band.id"
                        :class="{ active: selectedBandId === band.id }"
                        @click="selectedBandId = band.id">
                  {{ band.name }}
                </button>
              </div>

              <button class="match-make-btn">
                <span class="plus-icon">+</span>
                <span class="btn-lbl">BAND MATCHING</span>
              </button>
            </div>
          </nav>

          <section class="stage band-stage">
            <div class="band-info">
              <h2>{{ activeBand.name }}의 합주실</h2>
              <p>{{ activeBand.members.length }}명의 멤버가 참여 중</p>
            </div>
            <div class="member-list">
              <div v-for="m in activeBand.members" :key="m.id" class="m-item">
                <div class="m-avatar">{{ m.emoji }}</div>
                <div class="m-role">{{ m.role }}</div>
              </div>
            </div>
          </section>

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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppHeader from '../components/AppHeader.vue'
import CharacterStage from '../components/CharacterStage.vue'

const router = useRouter()
const authStore = useAuthStore()
const isBandMode = ref(false)
const selectedBandId = ref(1)

const currentStreak = computed(() => authStore.character?.currentStreak ?? 0)

const goToEditCharacter = () => {
  router.push('/character-create?edit=true')
}

const myBands = ref([
  { id: 1, name: '락앤롤 베이비', members: [{id:1, emoji:'🥁', role:'DR'}, {id:2, emoji:'🎤', role:'VO'}, {id:3, emoji:'🎸', role:'GT1'}, {id:4, emoji:'🎸', role:'GT2'}]},
  { id: 2, name: '락스타들', members: [{id:1, emoji:'🎸', role:'GT'}, {id:2, emoji:'🎹', role:'KB'}]}
])
const activeBand = computed(() => myBands.value.find(b => b.id === selectedBandId.value))
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
.pick-box { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.pick { background: #f8f9fa; height: 64px; display: flex; align-items: center; justify-content: center; border-radius: 14px; border: 1px solid #f1f3f5; font-size: 1.2rem; }
.pick.locked { opacity: 0.3; }

.streak { color: #ff922b; font-weight: 800; margin-bottom: 12px; font-size: 0.9rem; }
.fret-board { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.fret { background: #e9ecef; height: 14px; border-radius: 3px; }
.fret.on { background: #2d5bff; }

/* 밴드 전용 */
.band-info h2 { font-size: 1.5rem; font-weight: 800; }
.member-list { display: flex; justify-content: center; gap: 24px; margin-top: 30px; }
.m-avatar { font-size: 2.4rem; margin-bottom: 8px; transition: transform 0.2s; }
.m-item:hover .m-avatar { transform: scale(1.1); }
.m-role { font-size: 0.7rem; font-weight: 800; background: #f1f3f5; padding: 3px 10px; border-radius: 6px; color: #495057; }

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
</style>