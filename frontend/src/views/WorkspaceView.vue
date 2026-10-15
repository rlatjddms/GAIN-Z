<template>
  <div class="workspace-app">
    <AppHeader>
      <div class="page-pill">SOLO · WORKSPACE</div>
    </AppHeader>

    <main class="main-body">
      <div class="content-wrapper">
        <!-- 내 캐릭터 요약 -->
        <section class="summary-card" :style="{ '--accent': tier.accent, '--accent-soft': tier.accentSoft }">
          <div class="mini-char">
            <PixelCharacter :character="authStore.character" :scale="3" />
          </div>
          <div class="summary-body">
            <div class="summary-head">
              <strong>Lv.{{ level }} {{ nickname }}</strong>
              <span class="tier-badge">{{ tier.name }}</span>
            </div>
            <div class="exp-bar"><div class="exp-fill" :style="{ width: `${progress}%` }"></div></div>
            <div class="exp-text">{{ expLabel }}</div>
          </div>
          <button class="start-btn" @click="selectTab('practice')">연습 시작</button>
        </section>

        <!-- 탭 -->
        <nav ref="tabsRef" class="ws-tabs" role="tablist">
          <button
            v-for="tab in TABS"
            :key="tab.key"
            role="tab"
            :aria-selected="activeTab === tab.key"
            :class="{ active: activeTab === tab.key }"
            @click="selectTab(tab.key)"
          >
            <span class="tab-icon">{{ tab.icon }}</span>{{ tab.label }}
          </button>
        </nav>

        <!-- 탭 내용: 각 기능은 이후 작업에서 이 자리에 들어온다 -->
        <section class="ws-panel" role="tabpanel">
          <div class="panel-head">
            <div class="panel-icon">{{ current.icon }}</div>
            <div>
              <h2>{{ current.title }}</h2>
              <p>{{ current.description }}</p>
            </div>
          </div>
          <div class="coming-soon">
            <span class="soon-badge">준비 중</span>
            <ul>
              <li v-for="item in current.upcoming" :key="item">{{ item }}</li>
            </ul>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppHeader from '../components/AppHeader.vue'
import PixelCharacter from '../components/PixelCharacter.vue'
import { MAX_LEVEL, clampLevel, getLevelTier, getRequiredExp, getExpProgress } from '../constants/levelTier'

const TABS = [
  {
    key: 'practice', label: '연습', icon: '🎸', title: 'PRACTICE',
    description: '타이머로 연습 시간을 재고, 끝나면 달성률과 함께 기록해요. 기록할 때마다 경험치가 쌓여요.',
    upcoming: ['연습 타이머 (시작 · 일시정지 · 종료)', '달성률 입력 후 연습 기록', '획득 경험치와 레벨업 결과 확인'],
  },
  {
    key: 'routine', label: '루틴', icon: '✅', title: 'ROUTINE TRACKER',
    description: '나만의 연습 루틴을 만들고 매일 체크해요.',
    upcoming: ['루틴 추가 · 수정 · 끄기', '오늘의 루틴 진행도 체크', '지난 날짜 기록 보기'],
  },
  {
    key: 'log', label: '기록', icon: '🌱', title: 'PRACTICE LOG',
    description: '날짜별 연습량을 잔디로 보고, 연속으로 연습한 날을 확인해요.',
    upcoming: ['연습량 잔디 (최근 1년)', '연속 연습 일수와 총 연습 시간', '날짜별 연습 시간 · 달성률'],
  },
  {
    key: 'sheets', label: '악보', icon: '📂', title: 'WORK BENCH',
    description: '악보를 보관함에 담고 메모와 키를 저장해요. AI로 음원에서 악보를 만드는 기능도 여기에 들어와요.',
    upcoming: ['악보 보관함 · 즐겨찾기', '개인 메모와 키 변조', '타브 · 코드 악보 보기', 'AI 악보 생성'],
  },
  {
    key: 'picks', label: '피크', icon: '🔺', title: 'MY PICK CASE',
    description: '업적을 달성하면 커스텀 피크를 모을 수 있어요.',
    upcoming: ['피크 도감 (희귀도별)', '획득 조건 보기', '새 피크 획득 연출'],
  },
]
const TAB_KEYS = TABS.map((t) => t.key)

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 탭은 주소(?tab=)에 담아서, 홈 카드에서 바로 원하는 탭으로 들어오고 새로고침해도 유지되게 한다
const activeTab = computed(() => (TAB_KEYS.includes(route.query.tab) ? route.query.tab : 'practice'))
const current = computed(() => TABS.find((t) => t.key === activeTab.value))
const selectTab = (key) => {
  if (key !== activeTab.value) router.replace({ query: { ...route.query, tab: key } })
}

// 좁은 화면에서는 탭이 가로로 스크롤되므로, 선택된 탭이 보이도록 맞춰 준다
const tabsRef = ref(null)
const revealActiveTab = async () => {
  await nextTick()
  tabsRef.value?.querySelector('button.active')?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
}
watch(activeTab, revealActiveTab)
onMounted(revealActiveTab)

const level = computed(() => clampLevel(authStore.character?.level ?? 1))
const exp = computed(() => Math.max(0, Number(authStore.character?.exp) || 0))
const nickname = computed(() => authStore.character?.nickname || '방구석 락스타')
const tier = computed(() => getLevelTier(level.value))
const progress = computed(() => getExpProgress(level.value, exp.value))
const expLabel = computed(() => {
  if (level.value >= MAX_LEVEL) return 'MAX LEVEL'
  return `EXP ${exp.value} / ${getRequiredExp(level.value)}`
})
</script>

<style scoped>
* { box-sizing: border-box; }
button { -webkit-appearance: none; appearance: none; outline: none; font-family: inherit; }

.workspace-app {
  width: 100%; min-height: 100vh;
  background: #f8f9fa; color: #1a1a1a;
  font-family: 'Pretendard', sans-serif;
}
.page-pill {
  background: #f1f3f5; color: #2d5bff; font-weight: 800; font-size: 0.8rem;
  padding: 8px 18px; border-radius: 12px; letter-spacing: 0.5px; white-space: nowrap;
}

.main-body { padding: 32px 20px 48px; }
.content-wrapper { max-width: 1040px; margin: 0 auto; }

/* 캐릭터 요약 */
.summary-card {
  display: flex; align-items: center; gap: 20px;
  background: #fff; border: 1px solid #e9ecef; border-radius: 24px;
  padding: 16px 24px; margin-bottom: 24px;
}
.mini-char {
  flex: none; width: 96px; height: 108px; border-radius: 18px;
  background: var(--accent-soft); display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.summary-body { flex: 1; min-width: 0; }
.summary-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.summary-head strong { font-size: 1.05rem; font-weight: 800; }
.tier-badge {
  font-size: 0.7rem; font-weight: 800; color: var(--accent); background: var(--accent-soft);
  padding: 3px 10px; border-radius: 6px;
}
.exp-bar { height: 10px; background: #f1f3f5; border-radius: 6px; overflow: hidden; }
.exp-fill { height: 100%; background: var(--accent); border-radius: 6px; transition: width 0.4s; }
.exp-text { margin-top: 6px; font-size: 0.75rem; font-weight: 700; color: #868e96; }
.start-btn {
  flex: none; border: none; background: #2d5bff; color: #fff; font-weight: 800; font-size: 0.9rem;
  padding: 12px 22px; border-radius: 14px; cursor: pointer; transition: background 0.2s, transform 0.2s;
}
.start-btn:hover { background: #1f47e0; transform: translateY(-2px); }

/* 탭: 아래 패널과 이어지는 폴더 탭 모양 (홈의 밴드 탭과 같은 톤) */
.ws-tabs {
  display: flex; gap: 0; position: relative; z-index: 2; margin-bottom: -1px;
  overflow-x: auto; scrollbar-width: none;
}
.ws-tabs::-webkit-scrollbar { display: none; }
.ws-tabs button {
  flex: none; display: flex; align-items: center; gap: 6px;
  background: #f1f3f5; border: 1px solid #dee2e6; border-bottom: none;
  padding: 10px 22px; border-radius: 16px 16px 0 0; cursor: pointer;
  color: #adb5bd; font-size: 0.9rem; font-weight: 700; transition: background 0.2s, color 0.2s;
}
.ws-tabs button:hover { color: #495057; }
.ws-tabs button.active {
  background: #fff; color: #1a1a1a; font-weight: 800;
  border-color: #e9ecef; border-bottom: 1px solid #fff;
}
.tab-icon { font-size: 1rem; }

.ws-panel {
  background: #fff; border: 1px solid #e9ecef; border-radius: 0 28px 28px 28px;
  padding: 36px 40px; min-height: 360px; position: relative; z-index: 1;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.02);
}
.panel-head { display: flex; align-items: flex-start; gap: 18px; margin-bottom: 28px; }
.panel-icon {
  flex: none; font-size: 1.8rem; background: #f1f5ff; width: 56px; height: 56px;
  display: flex; align-items: center; justify-content: center; border-radius: 16px;
}
.panel-head h2 { margin: 2px 0 6px; font-size: 1.15rem; font-weight: 900; letter-spacing: 0.3px; }
.panel-head p { margin: 0; font-size: 0.9rem; color: #868e96; line-height: 1.5; }

.coming-soon {
  border: 2px dashed #e9ecef; border-radius: 20px; padding: 24px 28px; background: #fcfcfd;
}
.soon-badge {
  display: inline-block; font-size: 0.7rem; font-weight: 800; color: #ff922b; background: #fff4e6;
  padding: 4px 10px; border-radius: 6px; margin-bottom: 14px;
}
.coming-soon ul { margin: 0; padding-left: 18px; color: #495057; font-size: 0.9rem; line-height: 1.9; }

@media (max-width: 640px) {
  .main-body { padding: 20px 16px 40px; }
  .summary-card { flex-wrap: wrap; padding: 16px; gap: 14px; }
  .start-btn { width: 100%; }
  .ws-tabs button { padding: 9px 16px; }
  .ws-panel { padding: 24px 20px; border-radius: 0 20px 20px 20px; }
}
</style>
