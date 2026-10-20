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
            <!-- 요즘 듣는 음악: 누르면 등록·수정 팝업, 링크가 있으면 옆 아이콘으로 새 탭 -->
            <div class="music-chip" :class="{ empty: !myMusic }">
              <button class="music-main" :title="myMusic ? '요즘 듣는 음악 바꾸기' : '요즘 듣는 음악 등록'" @click="isMusicOpen = true">
                <span class="note">♪</span>
                <span class="music-text">{{ myMusic ? `${myMusic.title} · ${myMusic.artist}` : '요즘 듣는 음악 등록' }}</span>
              </button>
              <a v-if="myMusicUrl" class="music-link" :href="myMusicUrl" target="_blank" rel="noopener noreferrer" title="새 탭에서 듣기">
                <svg viewBox="0 0 24 24" width="12" height="12" stroke="currentColor" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
              </a>
            </div>
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
                MY RECORDS <span class="record-count">{{ recordsStore.ownedCount }} / {{ recordsStore.total || 12 }} ›</span>
              </button>
              <!-- 레코드 선반: 얻은 레코드(최근 순)를 먼저, 남은 칸은 아직 못 얻은 레코드 실루엣 -->
              <div class="shelf" @click="isRecordsOpen = true">
                <div v-for="(row, ri) in shelfRows" :key="ri" class="shelf-row">
                  <div class="shelf-items">
                    <RecordIcon v-for="item in row" :key="item.key" :type="item.type" :color="item.color"
                                :locked="item.locked" :size="56" :title="item.title" />
                  </div>
                  <div class="shelf-board"></div>
                </div>
              </div>
            </div>
            <PracticeLogCard />
          </section>

          <RecordsModal :open="isRecordsOpen" :focus-key="recordFocus" @close="closeRecords" />
          <ProfileMusicModal :open="isMusicOpen" @close="isMusicOpen = false" />
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
                <button class="tool-btn" @click="router.push(`/bands/${activeBand.id}/settings`)">⚙ 밴드 설정</button>
              </div>
            </div>

            <div class="band-scene">
              <BandStageScene :members="shownMembers" :stage-key="activeBand.stage"
                              :reorderable="canReorder" @reorder="onReorder" />
            </div>
            <p v-if="canReorder" class="scene-hint">캐릭터나 아래 이름표를 끌어서 자리를 바꿀 수 있어요.</p>
            <p v-if="reorderError" class="scene-hint bad">{{ reorderError }}</p>

            <div ref="memberListRef" class="member-list"
                 :class="{ reorderable: canReorder, dragging: !!tagDrag, settling: tagSettling }">
              <div v-for="m in activeBand.members" :key="m.memberId" class="m-item" :data-member-id="m.memberId"
                   :class="{ me: m.me, held: tagDrag?.id === m.memberId }" :style="tagStyle(m.memberId)"
                   @pointerdown="startTagDrag($event, m.memberId)">
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

          <template v-if="activeBand">
            <section class="quick-grid grid-2 band-menus">
              <button class="card-btn" @click="router.push(`/bands/${activeBand.id}/setlists`)">
                <div class="c-icon">🎶</div>
                <div class="c-text"><strong>BAND SETLIST</strong><span>파트 분배 및 합주 현황 공유</span></div>
              </button>
              <button class="card-btn" @click="router.push(`/bands/${activeBand.id}/jams`)">
                <div class="c-icon">🎙️</div>
                <div class="c-text"><strong>LAYER JAM</strong><span>레이어링 방식 비대면 합주</span></div>
              </button>
            </section>

            <BandCommunityPanel :key="activeBand.id" :band="activeBand" />
          </template>
        </template>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppHeader from '../components/AppHeader.vue'
import CharacterStage from '../components/CharacterStage.vue'
import RecordsModal from '../components/workspace/RecordsModal.vue'
import ProfileMusicModal from '../components/ProfileMusicModal.vue'
import PracticeLogCard from '../components/workspace/PracticeLogCard.vue'
import RecordIcon from '../components/workspace/RecordIcon.vue'
import { RECORD_RARITIES, rarityOf, progressRatio } from '../constants/records'
import { useRecordsStore } from '../stores/records'
import BandStageScene from '../components/band/BandStageScene.vue'
import BandStartModal from '../components/band/BandStartModal.vue'
import BandCommunityPanel from '../components/band/BandCommunityPanel.vue'
import { useBandStore } from '../stores/band'
import { MAX_JOINED_BANDS, reorderMembers } from '../api/band'
import { apiErrorMessage } from '../api/practice'
import { INSTRUMENT_OPTIONS } from '../constants/characterOptions'

const router = useRouter()

// 요즘 듣는 음악 (홈 칩을 눌러 팝업에서 등록)
const isMusicOpen = ref(false)
const myMusic = computed(() => authStore.profile?.profileMusic || null)
const myMusicUrl = computed(() => (/^https?:\/\//i.test(myMusic.value?.url || '') ? myMusic.value.url : null))
const route = useRoute()
const authStore = useAuthStore()
const bandStore = useBandStore()
// 게시글 링크를 직접 열어도 기존 BAND 화면과 상단 탭을 유지한다
watch(() => route.query.band, (id) => {
  if (id && Number.isFinite(Number(id))) {
    bandStore.selectedBandId = Number(id)
    bandStore.isBandMode = true
  }
}, { immediate: true })
// SOLO / BAND 탭은 스토어에 둬서 밴드 설정에 다녀와도 BAND 탭으로 돌아온다
const isBandMode = computed({
  get: () => bandStore.isBandMode,
  set: (v) => { bandStore.isBandMode = v },
})

/* ---------- MY RECORDS ---------- */
const recordsStore = useRecordsStore()
const isRecordsOpen = ref(false)
const recordFocus = ref(null)
// 선반 자리 모양은 고정 (위: 카세트 4, 아래: CD 2 · LP 2).
// 자리마다 그 모양의 레코드 중 얻은 것(최근 순)을 먼저, 남은 자리는 못 얻은 것 중 진행도가 높은 것을 놓는다
const SHELF_LAYOUT = [
  ['COMMON', 'COMMON', 'COMMON', 'COMMON'],
  ['RARE', 'RARE', 'LEGENDARY', 'LEGENDARY'],
]

onMounted(() => { recordsStore.load().catch(() => {}) })

const shelfRows = computed(() => {
  const queues = {}
  if (recordsStore.loaded) {
    for (const { key } of RECORD_RARITIES) {
      const same = recordsStore.records.filter((r) => r.rarity === key)
      queues[key] = [
        ...recordsStore.owned.filter((r) => r.rarity === key),
        ...same.filter((r) => !r.owned).sort((x, y) => progressRatio(y.progress) - progressRatio(x.progress)),
      ]
    }
  }
  return SHELF_LAYOUT.map((row, ri) => row.map((rarity, i) => {
    const r = queues[rarity]?.shift()
    if (!r) return { key: `empty-${ri}-${i}`, type: rarityOf(rarity).type, color: undefined, locked: true, title: '' }
    return { key: r.key, type: rarityOf(r.rarity).type, color: r.color, locked: !r.owned, title: r.owned ? r.name : r.condition }
  }))
})

// 알림·획득 연출에서 오면 도감을 연다 (?records=open 또는 ?records=레코드 키)
watch(() => route.query.records, (value) => {
  if (!value) return
  recordFocus.value = value === 'open' ? null : String(value)
  isRecordsOpen.value = true
}, { immediate: true })

const closeRecords = () => {
  isRecordsOpen.value = false
  recordFocus.value = null
  if (route.query.records) router.replace({ query: { ...route.query, records: undefined } })
}
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

// 무대 자리 바꾸기 (리더만). 화면은 먼저 바꾸고, 실패하면 원래 순서로 되돌린다
const canReorder = computed(() => activeBand.value?.myPosition === 'LEADER' && activeBand.value.members.length > 1)
const reorderError = ref('')
const onReorder = async (memberIds) => {
  const band = activeBand.value
  const before = band
  const byId = new Map(band.members.map((m) => [m.memberId, m]))
  bandStore.upsertBand({ ...band, members: memberIds.map((id) => byId.get(id)) })
  reorderError.value = ''
  try {
    bandStore.upsertBand(await reorderMembers(band.id, memberIds))
  } catch (e) {
    bandStore.upsertBand(before)
    reorderError.value = apiErrorMessage(e, '자리를 바꾸지 못했어요.')
  }
}

// 이름표 끌기: 캐릭터처럼 잡은 이름표는 손을 따라오고, 나머지는 비켜 준 자리로 미끄러진다.
// 끄는 동안 DOM 순서는 그대로 두고 transform으로만 움직이며, 무대 캐릭터도 같은 순서로 미리 보여 준다
const memberListRef = ref(null)
// { id, ids: 원래 순서, slots: 원래 자리 가운데 {x, y}, startX, startY, dx, dy, order: 미리 보기 순서 }
const tagDrag = ref(null)
// 놓은 직후 한 번은 애니메이션 없이 새 순서로 (transform을 풀 때 튀지 않게)
const tagSettling = ref(false)

const shownMembers = computed(() => {
  const band = activeBand.value
  if (!band) return []
  if (!tagDrag.value) return band.members
  const byId = new Map(band.members.map((m) => [m.memberId, m]))
  return tagDrag.value.order.map((id) => byId.get(id)).filter(Boolean)
})

const startTagDrag = (e, id) => {
  if (!canReorder.value || e.button > 0) return
  e.preventDefault()
  const slots = new Map()
  memberListRef.value.querySelectorAll('[data-member-id]').forEach((el) => {
    const r = el.getBoundingClientRect()
    slots.set(Number(el.dataset.memberId), { x: r.left + r.width / 2, y: r.top + r.height / 2 })
  })
  const ids = activeBand.value.members.map((m) => m.memberId)
  tagDrag.value = { id, ids, slots, startX: e.clientX, startY: e.clientY, dx: 0, dy: 0, order: ids }
  window.addEventListener('pointermove', onTagMove)
  window.addEventListener('pointerup', endTagDrag)
  window.addEventListener('pointercancel', cancelTagDrag)
}

// 잡은 이름표 가운데에서 가장 가까운 원래 자리로 끼워 넣는다
const onTagMove = (e) => {
  const drag = tagDrag.value
  if (!drag) return
  drag.dx = e.clientX - drag.startX
  drag.dy = e.clientY - drag.startY
  const home = drag.slots.get(drag.id)
  const x = home.x + drag.dx
  const y = home.y + drag.dy
  let target = 0
  let best = Infinity
  drag.ids.forEach((slotId, i) => {
    const c = drag.slots.get(slotId)
    const d = Math.hypot(x - c.x, (y - c.y) * 2)
    if (d < best) { best = d; target = i }
  })
  const order = drag.ids.filter((x2) => x2 !== drag.id)
  order.splice(target, 0, drag.id)
  if (order.some((id2, i) => id2 !== drag.order[i])) drag.order = order
}

const tagStyle = (id) => {
  const drag = tagDrag.value
  if (!drag) return null
  if (id === drag.id) return { transform: `translate(${drag.dx}px, ${drag.dy}px)` }
  const own = drag.slots.get(id)
  const slot = drag.slots.get(drag.ids[drag.order.indexOf(id)])
  return { transform: `translate(${slot.x - own.x}px, ${slot.y - own.y}px)` }
}

const stopTagListening = () => {
  window.removeEventListener('pointermove', onTagMove)
  window.removeEventListener('pointerup', endTagDrag)
  window.removeEventListener('pointercancel', cancelTagDrag)
}

const endTagDrag = () => {
  const drag = tagDrag.value
  stopTagListening()
  if (!drag) return
  const changed = drag.order.some((id, i) => id !== drag.ids[i])
  tagSettling.value = true
  tagDrag.value = null
  if (changed) onReorder(drag.order)
  requestAnimationFrame(() => requestAnimationFrame(() => { tagSettling.value = false }))
}

const cancelTagDrag = () => {
  stopTagListening()
  tagDrag.value = null
}
onBeforeUnmount(stopTagListening)

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
.tab-switcher button.active { background: #fff; color: var(--c-primary-text); box-shadow: 0 2px 8px rgba(0,0,0,0.06); }

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
  background: #fff; border: 1px solid var(--c-primary); border-bottom: none; /* dashed 대신 solid로 변경하여 울렁임 방지 */
  padding: 10px 18px; border-radius: 12px 12px 0 0; cursor: pointer;
  display: flex; align-items: center; gap: 8px;
  position: relative; top: 1px;
  z-index: 11; /* stage 위로 올림 */
  transition: background 0.2s; /* 움직임 효과(all) 제거 */
}
.match-make-btn:hover { background: var(--c-primary-soft); }
.match-make-btn .plus-icon { color: var(--c-primary-text); font-weight: 900; font-size: 1.1rem; }
.match-make-btn .btn-lbl { color: var(--c-primary-text); font-size: 0.75rem; font-weight: 800; letter-spacing: 0.5px; }

/* 그리드 & 카드 */
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.card-btn {
  background: #fff; border: 1px solid #e9ecef; border-radius: 20px;
  padding: 24px; display: flex; align-items: center; gap: 18px;
  cursor: pointer; transition: 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94); text-align: left;
}
.card-btn:hover { border-color: var(--c-primary); transform: translateY(-4px); box-shadow: 0 10px 20px color-mix(in srgb, var(--c-primary) 6%, transparent); }
.c-icon { font-size: 1.8rem; background: var(--c-primary-soft); width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; border-radius: 14px; }
.c-text strong { display: block; font-size: 0.95rem; margin-bottom: 2px; }
.c-text span { font-size: 0.75rem; color: #868e96; }

/* 데이터 카드 */
.data-card { background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 24px; }
.card-title { font-weight: 800; font-size: 0.85rem; margin-bottom: 18px; display: flex; justify-content: space-between; color: #495057; }
.card-title.link { width: 100%; background: none; border: none; padding: 0; cursor: pointer; font-family: inherit; }
.card-title.link:hover { color: var(--c-primary-text); }
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
.band-tabs-left .add-band-tab { font-weight: 900; font-size: 1rem; padding: 10px 16px; color: var(--c-primary-text); }
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
.pill-btn:hover { border-color: var(--c-primary); color: var(--c-primary-text); }
.pill-btn.primary { background: var(--c-primary); border-color: var(--c-primary); color: #fff; }
.pill-btn.primary:hover { background: var(--c-primary-strong); color: #fff; }
.text-btn { margin-left: 8px; border: none; background: none; color: var(--c-primary-text); font-weight: 800; cursor: pointer; font-family: inherit; }

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
.tool-btn:hover { border-color: var(--c-primary); color: var(--c-primary-text); }
.band-scene { margin-top: 16px; border-radius: 18px; overflow: hidden; border: 1px solid #f1f3f5; }
.scene-hint { margin: 8px 0 0; text-align: center; font-size: 0.72rem; color: #adb5bd; font-weight: 600; }
.scene-hint.bad { color: #e03131; font-weight: 700; }

.member-list { display: flex; justify-content: center; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
.m-item { text-align: center; padding: 8px 12px; border-radius: 12px; background: #f8f9fa; min-width: 88px; }
.m-item.me { background: var(--c-primary-soft); }
.member-list.reorderable .m-item { cursor: grab; touch-action: none; user-select: none; }
.member-list.dragging .m-item { cursor: grabbing; transition: transform 0.18s ease; }
.member-list.settling .m-item { transition: none; }
.m-item.held { position: relative; z-index: 2; box-shadow: 0 6px 16px color-mix(in srgb, var(--c-primary) 18%, transparent), 0 0 0 2px var(--c-primary); background: #fff; }
.member-list.dragging .m-item.held { transition: none; }
.m-name { font-size: 0.8rem; font-weight: 800; margin-bottom: 4px; white-space: nowrap; }
.m-role { font-size: 0.7rem; font-weight: 700; color: #868e96; }

.band-menus { margin-bottom: 0; }

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

.music-chip {
  position: absolute; top: 16px; left: 20px; z-index: 4; max-width: min(280px, 34%);
  display: inline-flex; align-items: center; box-sizing: border-box;
  background: rgba(255, 255, 255, 0.92); border: 1px solid #e9ecef; border-radius: 999px;
  transition: border-color 0.15s;
}
.music-chip:hover { border-color: var(--c-primary); }
.music-main {
  display: inline-flex; align-items: center; gap: 6px; min-width: 0; padding: 5px 12px 5px 6px;
  border: none; background: none; cursor: pointer; font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #495057;
}
.music-main:hover { color: var(--c-primary-text); }
.music-chip .note {
  flex: none; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: var(--c-primary); color: #fff; font-size: 0.7rem;
}
.music-chip.empty .music-main { color: #adb5bd; }
.music-chip.empty .note { background: #ced4da; }
.music-text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.music-link {
  flex: none; display: flex; align-items: center; justify-content: center; width: 26px; height: 26px; margin-right: 3px;
  border-radius: 50%; color: #adb5bd; transition: 0.15s;
}
.music-link:hover { background: var(--c-primary-soft); color: var(--c-primary-text); }

.edit-char-btn:hover {
  background: #f1f3f5;
  color: var(--c-primary-text);
  border-color: var(--c-primary);
}

/* 좁은 화면: 무대 아래 경험치와 겹치지 않게 옷장 버튼을 무대 밑으로 */
@media (max-width: 640px) {
  .edit-char-btn { position: static; margin: 0 auto 12px; }
}
</style>
