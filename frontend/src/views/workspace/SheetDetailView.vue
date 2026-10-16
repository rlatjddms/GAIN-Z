<template>
  <SubPageLayout>
  <div class="page">
    <router-link to="/sheets" class="crumb">‹ 악보 보관함</router-link>

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="!item" class="loading">불러오는 중…</div>

    <template v-else>
      <div class="page-head">
        <div class="title">
          <h2>{{ sheet.title }}</h2>
          <p>{{ sheet.artist || '아티스트 미입력' }} · {{ instrumentLabel }}<template v-if="sheet.bpm"> · {{ sheet.bpm }} BPM</template></p>
        </div>
        <div class="head-actions">
          <button
            class="btn star"
            :class="{ on: item.favorite }"
            :aria-pressed="item.favorite"
            @click="toggleFavorite"
          >{{ item.favorite ? '★ 즐겨찾기' : '☆ 즐겨찾기' }}</button>
          <router-link v-if="sheet.mine" :to="`/sheets/${item.id}/edit`" class="btn">수정</router-link>
          <button class="btn danger" :disabled="removing" @click="remove">빼기</button>
        </div>
      </div>

      <p v-if="actionError" class="error">{{ actionError }}</p>

      <div class="layout">
        <!-- 악보 -->
        <WsCard class="sheet-card">
          <div class="toolbar">
            <div class="key-control" role="group" aria-label="키 바꾸기">
              <button class="step" :disabled="!sheet.originalKey" aria-label="반음 내리기" @click="shiftKey(-1)">−</button>
              <div class="key-now">
                <span class="label">키</span>
                <strong>{{ viewKey || '-' }}</strong>
              </div>
              <button class="step" :disabled="!sheet.originalKey" aria-label="반음 올리기" @click="shiftKey(1)">+</button>
            </div>
            <div class="key-info">
              <template v-if="changed">
                원래 {{ sheet.originalKey }} · {{ offsetLabel }}
                <button class="text-btn" @click="setKey(sheet.originalKey)">원래 키로</button>
              </template>
              <template v-else-if="sheet.originalKey">원래 키</template>
              <span v-if="capo" class="capo">카포 {{ capo }}프렛 + {{ sheet.originalKey }} 코드 모양</span>
              <span v-if="keyStatus" class="saved">{{ keyStatus }}</span>
            </div>
            <div class="font-control" role="group" aria-label="글자 크기">
              <button class="step small" :disabled="fontScale <= 0.8" aria-label="글자 작게" @click="fontScale = +(fontScale - 0.1).toFixed(1)">가</button>
              <button class="step small big" :disabled="fontScale >= 1.6" aria-label="글자 크게" @click="fontScale = +(fontScale + 0.1).toFixed(1)">가</button>
            </div>
          </div>

          <ChordSheet
            :content="sheet.content"
            :original-key="sheet.originalKey"
            :view-key="viewKey"
            :font-scale="fontScale"
            :empty-text="sheet.mine ? '아직 악보 내용이 없어요. 수정에서 코드를 써 보세요.' : '아직 악보 내용이 없어요.'"
          />
        </WsCard>

        <!-- 메모 -->
        <WsCard title="내 메모" class="memo-card">
          <textarea
            v-model="memo"
            class="memo"
            maxlength="2000"
            placeholder="연습하면서 기억할 것 (예: 브릿지는 BPM 80으로 천천히, 2절 끝 쉼표 주의)"
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
    </template>
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import ChordSheet from '../../components/workspace/ChordSheet.vue'
import { getWorkspaceItem, updateWorkspaceItem, removeWorkspaceItem } from '../../api/sheet'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { transposeKey, semitonesBetween, capoFor } from '../../music/transpose'

const route = useRoute()
const router = useRouter()
const itemId = route.params.id

const item = ref(null)
const loadError = ref('')
const actionError = ref('')
const sheet = computed(() => item.value?.sheet || {})
const instrumentLabel = computed(() => INSTRUMENT_OPTIONS.find((o) => o.value === sheet.value.instrumentType)?.label || '')

onMounted(async () => {
  try {
    item.value = await getWorkspaceItem(itemId)
    viewKey.value = item.value.customKey || item.value.sheet.originalKey
    memo.value = item.value.personalMemo || ''
  } catch (e) {
    loadError.value = `악보를 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
})

/* ---------- 키 바꾸기: 바꾸면 잠시 뒤 자동 저장 ---------- */
const viewKey = ref(null)
const keyStatus = ref('')
const changed = computed(() => viewKey.value && viewKey.value !== sheet.value.originalKey)
const offset = computed(() => semitonesBetween(sheet.value.originalKey, viewKey.value))
const offsetLabel = computed(() => `${offset.value > 0 ? '+' : ''}${offset.value}반음`)
const capo = computed(() => (sheet.value.instrumentType === 'GUITAR' ? capoFor(sheet.value.originalKey, viewKey.value) : null))

let keyTimer = null
const shiftKey = (step) => setKey(transposeKey(viewKey.value, step))
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
    if (viewKey.value === key) keyStatus.value = '키 저장됨'
  } catch (e) {
    actionError.value = `키를 저장하지 못했어요. ${apiErrorMessage(e)}`
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

/* ---------- 빼기 ---------- */
const removing = ref(false)
const remove = async () => {
  const message = sheet.value.mine
    ? `「${sheet.value.title}」을(를) 보관함에서 뺄까요?\n직접 쓴 악보라서 악보 내용과 메모도 함께 지워져요.`
    : `「${sheet.value.title}」을(를) 보관함에서 뺄까요?\n메모와 바꾼 키도 함께 지워져요.`
  if (!window.confirm(message)) return
  removing.value = true
  try {
    await removeWorkspaceItem(itemId)
    memo.value = item.value.personalMemo || ''
    router.replace('/sheets')
  } catch (e) {
    actionError.value = `빼지 못했어요. ${apiErrorMessage(e)}`
    removing.value = false
  }
}

onBeforeRouteLeave(() => (memoDirty.value ? window.confirm('저장하지 않은 메모가 있어요. 나갈까요?') : true))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.crumb { align-self: flex-start; font-size: 0.8rem; font-weight: 800; color: #868e96; text-decoration: none; }
.crumb:hover { color: #2d5bff; }

.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.title { min-width: 0; }
.title h2 { margin: 0 0 4px; font-size: 1.4rem; font-weight: 900; word-break: keep-all; }
.title p { margin: 0; font-size: 0.8rem; font-weight: 700; color: #868e96; }
.head-actions { display: flex; gap: 8px; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit; cursor: pointer;
  font-weight: 800; font-size: 0.8rem; padding: 9px 14px; border-radius: 12px; text-decoration: none;
}
.btn:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn.primary:hover:not(:disabled) { color: #fff; background: #1f4ae0; }
.btn.star.on { color: #e67700; border-color: #ffe066; background: #fff9db; }
.btn.danger:hover:not(:disabled) { border-color: #ffa8a8; color: #e03131; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 16px; align-items: start; }
.memo-card { position: sticky; top: 16px; }

.toolbar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding-bottom: 16px; margin-bottom: 18px; border-bottom: 1px solid #f1f3f5;
}
.key-control, .font-control { display: flex; align-items: center; gap: 4px; }
.font-control { margin-left: auto; }
.step {
  width: 36px; height: 36px; border: 1px solid #e9ecef; background: #fff; border-radius: 10px; cursor: pointer;
  font-family: inherit; font-size: 1.1rem; font-weight: 900; color: #495057;
}
.step:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.step:disabled { opacity: 0.4; cursor: not-allowed; }
.step.small { font-size: 0.75rem; }
.step.small.big { font-size: 1rem; }
.key-now { min-width: 60px; display: flex; flex-direction: column; align-items: center; line-height: 1.1; }
.key-now .label { font-size: 0.6rem; font-weight: 800; color: #adb5bd; }
.key-now strong { font-size: 1.25rem; font-weight: 900; color: #e8590c; }
.key-info { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 0.75rem; font-weight: 700; color: #868e96; }
.capo { background: #f8f9fa; border-radius: 6px; padding: 3px 8px; color: #495057; }
.saved { color: #2f9e44; font-weight: 800; font-size: 0.72rem; }
.text-btn { border: none; background: none; padding: 0; color: #2d5bff; font-weight: 800; cursor: pointer; font-family: inherit; font-size: inherit; }

.memo {
  width: 100%; box-sizing: border-box; min-height: 220px; resize: vertical; border: 1px solid #e9ecef; border-radius: 12px;
  padding: 12px; font-family: inherit; font-size: 0.85rem; line-height: 1.6; color: #212529; outline: none;
}
.memo:focus { border-color: #2d5bff; }
.memo-foot { display: flex; align-items: center; gap: 8px; margin-top: 10px; }
.count { font-size: 0.7rem; color: #adb5bd; font-weight: 700; margin-right: auto; }

.loading { padding: 48px; text-align: center; font-size: 0.85rem; color: #adb5bd; }
.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }

@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
  .memo-card { position: static; }
  .memo { min-height: 120px; }
}
</style>
