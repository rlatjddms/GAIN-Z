<template>
  <SubPageLayout back-to="/sheets" back-label="악보 보관함" :title="item ? 'SHEET' : ''" :name="sheet?.title" :subtitle="item ? metaText : ''">
    <template #actions>
          <button
            class="btn star"
            :class="{ on: item.favorite }"
            :aria-pressed="item.favorite"
            @click="toggleFavorite"
          ><Star :size="14" :stroke-width="2.2" :fill="item.favorite ? 'currentColor' : 'none'" /> 즐겨찾기</button>
          <router-link v-if="sheet.mine" :to="`/sheets/${item.id}/edit`" class="btn">수정</router-link>
          <button class="btn danger" :disabled="removing" @click="remove">삭제</button>
    </template>
  <div class="page">
    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="!item" class="loading">불러오는 중…</div>

    <template v-else>

      <p v-if="actionError" class="error">{{ actionError }}</p>

      <div class="layout" :class="{ wide }">
        <!-- 악보 -->
        <WsCard class="sheet-card">
          <div class="toolbar">
            <span class="count">{{ !tabs.length && pages.length ? `${pages.length}장` : '' }}</span>
            <div v-if="!tabs.length && pages.length" class="zoom" role="group" aria-label="확대·축소">
              <button class="step" :disabled="zoom <= ZOOMS[0]" aria-label="축소" @click="setZoom(-1)">−</button>
              <span class="zoom-value">{{ zoom }}%</span>
              <button class="step" :disabled="zoom >= ZOOMS[ZOOMS.length - 1]" aria-label="확대" @click="setZoom(1)">+</button>
            </div>
            <button v-if="!tabs.length && pages.length" class="btn small" :class="{ on: annot.enabled }" :aria-pressed="annot.enabled"
                    :disabled="!annot.loaded" :title="annot.loaded ? '악보 위에 펜·형광펜으로 쓰기' : '필기를 불러오지 못했어요'"
                    @click="annot.enabled = !annot.enabled"><PenLine :size="14" :stroke-width="2.2" /> 필기</button>
            <button class="btn small wide-toggle" @click="wide = !wide">{{ wide ? '메모 보이기' : '악보만 크게' }}</button>
          </div>
          <!-- 필기 도구: 악보를 내려도 위에 붙어 있다. 필기 중엔 장 위에서 스크롤 대신 그려진다 -->
          <AnnotationToolbar v-if="annot.enabled && !tabs.length && pages.length" class="annot-bar" @done="annot.enabled = false" />

          <!-- 마디 악보(타브·코드): 보는 키로 옮겨서 그린다 -->
          <MeasureSheet
            v-if="tabs.length"
            :tabs="tabs"
            :instrument-type="sheet.instrumentType"
            :original-key="sheet.originalKey"
            :view-key="viewKey"
            :bpm="sheet.bpm"
          />

          <div v-else-if="pages.length" class="pages">
            <figure v-for="p in pages" :key="p.id" class="page-frame" :style="{ width: `${zoom}%` }">
              <SheetPageView :sheet-id="sheet.id" :page-id="p.id" :content-type="p.contentType" :alt="`${sheet.title} ${p.pageNumber}장`" annotate />
              <figcaption v-if="pages.length > 1">{{ p.pageNumber }} / {{ pages.length }}</figcaption>
            </figure>
          </div>

          <!-- 예전에 직접 쓴 코드 악보 -->
          <ChordSheet
            v-else-if="sheet.content"
            :content="sheet.content"
            :original-key="sheet.originalKey"
            :view-key="viewKey"
          />
          <p v-else class="empty">아직 악보 파일이 없어요.</p>
        </WsCard>

        <div v-show="!wide" class="side">
          <!-- 키 바꿔 치기 -->
          <WsCard title="키 바꿔 치기">
            <KeyHelper
              v-if="sheet.originalKey"
              :original-key="sheet.originalKey"
              :view-key="viewKey"
              :instrument-type="sheet.instrumentType"
              :status="keyStatus"
              @change="setKey"
            />
            <p v-else class="muted">
              원래 키를 적어 두면 다른 키로 칠 때 바꿔 칠 코드를 알려 드려요.
              <router-link v-if="sheet.mine" :to="`/sheets/${item.id}/edit`" class="text-btn">원래 키 적기 ›</router-link>
            </p>
          </WsCard>

          <!-- 메모 -->
          <WsCard title="내 메모">
            <textarea
              v-model="memo"
              class="memo"
              maxlength="2000"
              placeholder="메모를 입력해 주세요"
            ></textarea>
            <div class="memo-foot">
              <span class="count">{{ memo.length }} / 2000</span>
              <span v-if="memoStatus" class="saved">{{ memoStatus }}</span>
              <button class="btn primary" :disabled="!memoDirty || savingMemo" @click="saveMemo">
                {{ savingMemo ? '저장 중…' : '메모 저장' }}
              </button>
            </div>
          </WsCard>
        </div>
      </div>
    </template>
  </div>
  </SubPageLayout>
</template>

<script setup>
import { Star, PenLine } from 'lucide-vue-next'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import ChordSheet from '../../components/workspace/ChordSheet.vue'
import SheetPageView from '../../components/workspace/SheetPageView.vue'
import MeasureSheet from '../../components/workspace/MeasureSheet.vue'
import KeyHelper from '../../components/workspace/KeyHelper.vue'
import AnnotationToolbar from '../../components/workspace/AnnotationToolbar.vue'
import { useAnnotationStore } from '../../stores/annotation'
import { getWorkspaceItem, updateWorkspaceItem, removeWorkspaceItem } from '../../api/sheet'
import { useDialogStore } from '../../stores/dialog'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { sheetTypeLabel } from '../../constants/sheet'

const route = useRoute()
const router = useRouter()
const dialog = useDialogStore()
const itemId = route.params.id

const item = ref(null)
const loadError = ref('')
const actionError = ref('')
const sheet = computed(() => item.value?.sheet || {})
const pages = computed(() => sheet.value.pages || [])
const tabs = computed(() => sheet.value.tabs || [])
const metaText = computed(() => {
  const s = sheet.value
  return [
    s.artist || '아티스트 미입력',
    INSTRUMENT_OPTIONS.find((o) => o.value === s.instrumentType)?.label,
    sheetTypeLabel(s.sheetTypes?.[0]),
    s.originalKey && `원래 키 ${s.originalKey}`,
    s.bpm && `${s.bpm} BPM`,
  ].filter(Boolean).join(' · ')
})

onMounted(async () => {
  try {
    item.value = await getWorkspaceItem(itemId)
    viewKey.value = item.value.customKey || item.value.sheet.originalKey
    memo.value = item.value.personalMemo || ''
    if (pages.value.length) annot.load(item.value.sheet.id)
  } catch (e) {
    loadError.value = `악보를 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
})

/* ---------- 보기: 확대·축소, 악보만 크게 (이 기기에 기억) ---------- */
const ZOOMS = [50, 60, 70, 80, 90, 100]
const readPref = (key, fallback) => {
  try { return JSON.parse(localStorage.getItem(key)) ?? fallback } catch { return fallback }
}
const writePref = (key, value) => {
  try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* 저장 못 해도 화면은 그대로 */ }
}
const zoom = ref(ZOOMS.includes(readPref('sheetZoom', 100)) ? readPref('sheetZoom', 100) : 100)
const wide = ref(false)
const setZoom = (d) => {
  zoom.value = ZOOMS[Math.min(Math.max(ZOOMS.indexOf(zoom.value) + d, 0), ZOOMS.length - 1)]
  writePref('sheetZoom', zoom.value)
}

/* ---------- 악보 위 필기 (#167): 쓰면 잠시 뒤 자동 저장, 나갈 때 남은 것 저장 ---------- */
const annot = useAnnotationStore()
annot.enabled = false
onBeforeUnmount(() => {
  annot.enabled = false
  annot.flush()
})

/* ---------- 연습 키: 바꾸면 잠시 뒤 자동 저장 ---------- */
const viewKey = ref(null)
const keyStatus = ref('')
let keyTimer = null
const setKey = (key) => {
  viewKey.value = key
  keyStatus.value = ''
  clearTimeout(keyTimer)
  keyTimer = setTimeout(saveKey, 600)
}
const saveKey = async () => {
  keyTimer = null
  const key = viewKey.value
  try {
    const result = await updateWorkspaceItem(itemId, { customKey: key })
    item.value.customKey = result.customKey
    if (viewKey.value === key) keyStatus.value = '저장됨'
  } catch (e) {
    actionError.value = `연습 키를 저장하지 못했어요. ${apiErrorMessage(e)}`
  }
}
// 저장 대기 중에 나가면 바로 저장
onBeforeUnmount(() => { if (keyTimer) { clearTimeout(keyTimer); saveKey() } })

/* ---------- 즐겨찾기 ---------- */
const toggleFavorite = async () => {
  const next = !item.value.favorite
  item.value.favorite = next
  try {
    await updateWorkspaceItem(itemId, { isFavorite: next })
  } catch (e) {
    item.value.favorite = !next
    actionError.value = `즐겨찾기를 바꾸지 못했어요. ${apiErrorMessage(e)}`
  }
}

/* ---------- 메모 ---------- */
const memo = ref('')
const memoStatus = ref('')
const savingMemo = ref(false)
const memoDirty = computed(() => item.value && memo.value !== (item.value.personalMemo || ''))
const saveMemo = async () => {
  savingMemo.value = true
  memoStatus.value = ''
  try {
    const result = await updateWorkspaceItem(itemId, { personalMemo: memo.value })
    item.value.personalMemo = result.personalMemo
    memo.value = result.personalMemo || ''
    memoStatus.value = '저장했어요'
  } catch (e) {
    actionError.value = `메모를 저장하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    savingMemo.value = false
  }
}

/* ---------- 삭제 ---------- */
const removing = ref(false)
const remove = async () => {
  const message = sheet.value.mine
    ? `「${sheet.value.title}」을(를) 삭제할까요?\n올린 파일과 메모도 함께 삭제돼요.`
    : `「${sheet.value.title}」을(를) 보관함에서 삭제할까요?\n메모와 연습 키도 함께 삭제돼요.`
  if (!await dialog.confirm({ title: '악보를 삭제할까요?', text: message, okText: '삭제', danger: true })) return
  removing.value = true
  try {
    await removeWorkspaceItem(itemId)
    memo.value = item.value.personalMemo || ''
    router.replace('/sheets')
  } catch (e) {
    actionError.value = `삭제하지 못했어요. ${apiErrorMessage(e)}`
    removing.value = false
  }
}

onBeforeRouteLeave(() => (memoDirty.value ? dialog.confirm({ title: '저장하지 않은 메모가 있어요', text: '저장하지 않고 나갈까요?', okText: '나가기' }) : true))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.btn.star.on { color: #e67700; border-color: #ffe066; background: #fff9db; }

.layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 16px; align-items: start; }
.layout.wide { grid-template-columns: minmax(0, 1fr); }
.side { position: sticky; top: 16px; display: flex; flex-direction: column; gap: 16px; }

.toolbar { display: flex; align-items: center; gap: 12px; padding-bottom: 14px; margin-bottom: 16px; border-bottom: 1px solid var(--c-line-soft); }
.toolbar .count { margin-right: auto; font-size: 0.75rem; font-weight: 800; color: var(--c-muted); }
/* 위 앱 헤더(64px, 휴대폰 56px) 바로 아래에 붙는다 */
.annot-bar { position: sticky; top: 73px; z-index: 5; margin: -4px 0 16px; }
.zoom { display: flex; align-items: center; gap: 4px; }
.zoom-value { min-width: 44px; text-align: center; font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); font-variant-numeric: tabular-nums; }
.step {
  width: 32px; height: 32px; border: 1px solid var(--c-line); background: #fff; border-radius: 9px; cursor: pointer;
  font-family: inherit; font-size: 1rem; font-weight: 900; color: var(--c-text-2);
}
.step:hover:not(:disabled) { border-color: var(--c-primary); color: var(--c-primary-text); }
.step:disabled { opacity: 0.4; cursor: not-allowed; }

.pages { display: flex; flex-direction: column; align-items: center; gap: 18px; }
.page-frame { margin: 0; width: 100%; max-width: 100%; }
.page-frame :deep(.sheet-page) { border: 1px solid var(--c-line); border-radius: 6px; overflow: hidden; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04); }
figcaption { margin-top: 6px; text-align: center; font-size: 0.7rem; font-weight: 800; color: var(--c-faint); }

.memo {
  width: 100%; box-sizing: border-box; min-height: 180px; resize: vertical; border: 1px solid var(--c-line); border-radius: 12px;
  padding: 12px; font-family: inherit; font-size: 0.85rem; line-height: 1.6; color: #212529; outline: none;
}
.memo:focus { border-color: var(--c-primary); }
.memo-foot { display: flex; align-items: center; gap: 8px; margin-top: 10px; }
.memo-foot .count { font-size: 0.7rem; color: var(--c-faint); font-weight: 700; margin-right: auto; }
.saved { font-size: 0.78rem; font-weight: 800; color: var(--c-success); }
.muted { margin: 0; font-size: 0.78rem; color: var(--c-muted); line-height: 1.6; }
.text-btn { border: none; background: none; padding: 0; color: var(--c-primary-text); font-weight: 800; cursor: pointer; font-family: inherit; font-size: inherit; text-decoration: none; }

.empty { margin: 0; padding: 32px; text-align: center; font-size: 0.85rem; color: var(--c-faint); }
.loading { padding: 32px 16px; text-align: center; font-size: 0.85rem; font-weight: 700; color: var(--c-faint); }
.error { margin: 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }

@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
  .side { position: static; }
  .memo { min-height: 110px; }
  .toolbar .wide-toggle { display: none; }
}
@media (max-width: 640px) {
  .annot-bar { top: 64px; }
}
</style>
