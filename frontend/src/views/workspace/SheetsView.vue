<template>
  <SubPageLayout>
  <div class="page">
    <div class="page-head">
      <h2>악보 보관함</h2>
      <div class="head-actions">
        <router-link to="/transcribe" class="btn">✨ AI로 악보 만들기</router-link>
        <router-link to="/sheets/new" class="btn primary">+ 악보 올리기</router-link>
      </div>
    </div>

    <WsCard :title="`내 악보${items.length ? ` ${items.length}` : ''}`">
      <template #actions>
        <div class="filters">
          <button class="chip" :class="{ on: !favoriteOnly }" @click="favoriteOnly = false">전체</button>
          <button class="chip" :class="{ on: favoriteOnly }" @click="favoriteOnly = true">★ 즐겨찾기</button>
          <select v-model="sort" class="sort" aria-label="정렬">
            <option value="recent">최근 담은 순</option>
            <option value="title">제목 순</option>
          </select>
          <button v-if="items.length || selecting" class="chip" :class="{ on: selecting }" @click="toggleSelecting">
            {{ selecting ? '선택 취소' : '선택' }}
          </button>
        </div>
      </template>

      <!-- 선택할 때만 보이는 일괄 작업 줄 -->
      <div v-if="selecting" class="select-bar">
        <label class="check-all">
          <input type="checkbox" :checked="allSelected" :indeterminate.prop="someSelected" :disabled="!items.length || bulkBusy" @change="toggleAll">
          <span>{{ selected.size ? `${selected.size}개 선택` : '전체 선택' }}</span>
        </label>
        <div class="bulk-actions">
          <button class="chip" :disabled="!selected.size || bulkBusy" @click="bulkFavorite(true)">즐겨찾기 추가</button>
          <button class="chip" :disabled="!selected.size || bulkBusy" @click="bulkFavorite(false)">즐겨찾기 해제</button>
          <button class="chip danger" :disabled="!selected.size || bulkBusy" @click="bulkDelete">삭제</button>
        </div>
      </div>

      <p v-if="error" class="error">{{ error }} <button class="text-btn" @click="load">다시 불러오기</button></p>
      <div v-else-if="loading && !items.length" class="loading">불러오는 중…</div>

      <ul v-else-if="items.length" class="list" :class="{ selecting }">
        <li v-for="item in items" :key="item.id" :class="{ checked: selected.has(item.id) }">
          <input
            v-if="selecting"
            type="checkbox"
            class="item-check"
            :checked="selected.has(item.id)"
            :disabled="bulkBusy"
            :aria-label="`${item.title} 선택`"
            @change="toggleOne(item.id)"
          >
          <router-link
            v-slot="{ href, navigate }"
            :to="`/sheets/${item.id}`"
            custom
          >
          <a :href="href" class="row" @click="onRowClick($event, item, navigate)">
            <span class="cover">
              <SheetPageView
                v-if="item.coverPageId"
                :sheet-id="item.sheetId"
                :page-id="item.coverPageId"
                content-type="image/png"
                thumb
              />
              <span v-else class="cover-text">{{ item.pageCount ? 'PDF' : '♪' }}</span>
            </span>
            <span class="info">
              <strong>{{ item.title }}</strong>
              <span class="meta">
                {{ item.artist || '아티스트 미입력' }} · {{ instrumentLabel(item.instrumentType) }}
                <template v-if="item.sheetType"> · {{ sheetTypeLabel(item.sheetType) }}</template>
                <template v-if="item.pageCount > 1"> · {{ item.pageCount }}장</template>
              </span>
            </span>
            <span v-if="item.originalKey" class="key-badge" :class="{ changed: item.customKey }" :title="item.customKey ? `원래 ${item.originalKey} → 연습 ${item.customKey}` : '원래 키'">
              {{ item.customKey ? `${item.originalKey}→${item.customKey}` : item.originalKey }}
            </span>
            <span v-if="item.hasMemo" class="memo-mark" title="메모 있음">📝</span>
          </a>
          </router-link>
          <button
            v-if="!selecting"
            class="star"
            :class="{ on: item.favorite }"
            :aria-label="item.favorite ? '즐겨찾기 해제' : '즐겨찾기'"
            :aria-pressed="item.favorite"
            @click="toggleFavorite(item)"
          >{{ item.favorite ? '★' : '☆' }}</button>
        </li>
      </ul>

      <div v-else-if="favoriteOnly" class="empty">
        <div class="empty-icon">☆</div>
        <strong>즐겨찾기한 악보가 없어요</strong>
        <p>악보 옆 별을 누르면 여기 모아 볼 수 있어요.</p>
      </div>
      <div v-else class="empty">
        <div class="empty-icon">📂</div>
        <strong>아직 담아 둔 악보가 없어요</strong>
        <p>갖고 있는 악보 사진·스캔·PDF를 올려 보세요. 메모를 남기고, 다른 키로 칠 코드도 알려 드려요.<br>AI로 만든 악보도 여기에 모여요.</p>
        <router-link to="/sheets/new" class="btn primary">+ 첫 악보 올리기</router-link>
      </div>
    </WsCard>
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import SheetPageView from '../../components/workspace/SheetPageView.vue'
import { getWorkspace, updateWorkspaceItem, removeWorkspaceItem } from '../../api/sheet'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { sheetTypeLabel } from '../../constants/sheet'

const items = ref([])
const loading = ref(false)
const error = ref('')
const favoriteOnly = ref(false)
const sort = ref('recent')

const instrumentLabel = (type) => INSTRUMENT_OPTIONS.find((o) => o.value === type)?.label || type

let token = 0
const load = async () => {
  const mine = ++token
  loading.value = true
  error.value = ''
  try {
    const result = await getWorkspace({ favorite: favoriteOnly.value, sort: sort.value })
    if (mine === token) {
      items.value = result
      // 목록에서 사라진 악보는 선택에서도 뺀다
      const ids = new Set(result.map((i) => i.id))
      selected.value = new Set([...selected.value].filter((id) => ids.has(id)))
    }
  } catch (e) {
    if (mine === token) error.value = `악보를 불러오지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    if (mine === token) loading.value = false
  }
}

// 별은 바로 바꿔 보이고, 실패하면 되돌린다
const toggleFavorite = async (item) => {
  const next = !item.favorite
  item.favorite = next
  try {
    await updateWorkspaceItem(item.id, { isFavorite: next })
    if (favoriteOnly.value && !next) items.value = items.value.filter((i) => i.id !== item.id)
  } catch (e) {
    item.favorite = !next
    error.value = `즐겨찾기를 바꾸지 못했어요. ${apiErrorMessage(e)}`
  }
}

/* ---------- 선택해서 한꺼번에 (선택할 때만 체크박스가 보인다) ---------- */
const selecting = ref(false)
const selected = ref(new Set())
const bulkBusy = ref(false)
const allSelected = computed(() => items.value.length > 0 && selected.value.size === items.value.length)
const someSelected = computed(() => selected.value.size > 0 && !allSelected.value)

const toggleSelecting = () => {
  selecting.value = !selecting.value
  selected.value = new Set()
}
const toggleOne = (id) => {
  const next = new Set(selected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}
const toggleAll = () => {
  selected.value = allSelected.value ? new Set() : new Set(items.value.map((i) => i.id))
}
// 선택 중에는 줄을 눌러도 들어가지 않고 선택만 바꾼다
const onRowClick = (e, item, navigate) => {
  if (!selecting.value) return navigate(e)
  e.preventDefault()
  if (!bulkBusy.value) toggleOne(item.id)
}

// 여러 개를 동시에 보내고 목록을 다시 불러온다. 실패가 있으면 개수를 알려 주고 선택 상태를 유지한다
const runBulk = async (ids, action, failText) => {
  bulkBusy.value = true
  const results = await Promise.allSettled(ids.map(action))
  const failed = results.filter((r) => r.status === 'rejected')
  selected.value = new Set()
  if (!failed.length) selecting.value = false
  await load()
  if (failed.length) error.value = `${failed.length}개를 ${failText}. ${apiErrorMessage(failed[0].reason)}`
  bulkBusy.value = false
}

const bulkFavorite = (favorite) =>
  runBulk([...selected.value], (id) => updateWorkspaceItem(id, { isFavorite: favorite }),
    favorite ? '즐겨찾기에 추가하지 못했어요' : '즐겨찾기에서 해제하지 못했어요')

const bulkDelete = () => {
  const ids = [...selected.value]
  if (!window.confirm(`선택한 악보 ${ids.length}개를 삭제할까요?\n직접 올린 악보는 파일과 메모도 함께 삭제돼요.`)) return
  return runBulk(ids, (id) => removeWorkspaceItem(id), '삭제하지 못했어요')
}

watch([favoriteOnly, sort], load)
onMounted(load)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.head-actions { display: flex; gap: 8px; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit;
  font-weight: 800; font-size: 0.8rem; padding: 10px 14px; border-radius: 12px; text-decoration: none;
}
a.btn:hover { border-color: var(--c-primary); color: var(--c-primary-text); }
.btn.primary { background: var(--c-primary); border-color: var(--c-primary); color: #fff; }
a.btn.primary:hover { color: #fff; background: #1f4ae0; }

.filters { display: flex; gap: 6px; align-items: center; }
.chip {
  border: 1px solid #e9ecef; background: #fff; color: #868e96; font-family: inherit; cursor: pointer;
  font-size: 0.7rem; font-weight: 800; padding: 5px 10px; border-radius: 8px;
}
.chip.on { background: var(--c-primary-soft); border-color: var(--c-primary-line); color: var(--c-primary-text); }
.sort {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit;
  font-size: 0.7rem; font-weight: 800; padding: 5px 6px; border-radius: 8px; cursor: pointer;
}

.chip:disabled { opacity: 0.4; cursor: not-allowed; }
.chip.danger:not(:disabled) { color: #e03131; border-color: #ffc9c9; background: #fff5f5; }

.select-bar {
  display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap;
  margin-bottom: 10px; padding: 8px 12px; border-radius: 12px; background: #f8f9fa;
}
.check-all { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 0.8rem; font-weight: 800; color: #495057; }
.bulk-actions { display: flex; gap: 6px; flex-wrap: wrap; }
input[type='checkbox'] { width: 18px; height: 18px; margin: 0; accent-color: var(--c-primary); cursor: pointer; flex: none; }

.list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.list li { display: flex; align-items: center; gap: 4px; border: 1px solid #f1f3f5; border-radius: 14px; transition: border-color 0.15s, background 0.15s; }
.list li:hover { border-color: var(--c-primary-line); }
.list.selecting li { padding-left: 12px; }
.list li.checked { border-color: var(--c-primary-line); background: #f8faff; }
.list.selecting .row { padding-right: 12px; }
.row { flex: 1; min-width: 0; display: flex; align-items: center; gap: 14px; padding: 8px 6px 8px 8px; text-decoration: none; color: inherit; }
.cover {
  flex: none; width: 46px; height: 60px; border-radius: 6px; overflow: hidden; border: 1px solid #f1f3f5;
  background: #f8f9fa; display: flex; align-items: center; justify-content: center;
}
.cover-text { font-size: 0.65rem; font-weight: 900; color: #e03131; }
.key-badge {
  flex: none; padding: 5px 8px; border-radius: 8px; text-align: center; white-space: nowrap;
  background: #f8f9fa; color: #495057; font-weight: 900; font-size: 0.75rem;
}
.key-badge.changed { background: #fff4e6; color: #e8590c; }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.info strong { font-size: 0.95rem; font-weight: 800; color: #212529; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { font-size: 0.75rem; color: #868e96; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.memo-mark { flex: none; font-size: 0.9rem; }
.star {
  flex: none; width: 44px; height: 44px; border: none; background: none; cursor: pointer;
  font-size: 1.2rem; color: #ced4da; font-family: inherit;
}
.star.on { color: #fab005; }
.star:hover { color: #fab005; }

.loading { padding: 32px; text-align: center; font-size: 0.85rem; color: #adb5bd; }
.error { margin: 0 0 12px; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.text-btn { border: none; background: none; color: var(--c-primary-text); font-weight: 800; cursor: pointer; font-family: inherit; font-size: inherit; }

.empty {
  border: 2px dashed #e9ecef; border-radius: 16px; padding: 48px 20px;
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px;
}
.empty-icon { font-size: 2rem; margin-bottom: 4px; color: #ced4da; }
.empty strong { font-size: 0.95rem; font-weight: 800; color: #495057; }
.empty p { margin: 0 0 8px; font-size: 0.8rem; color: #adb5bd; line-height: 1.6; }

.chip, .sort { white-space: nowrap; }

@media (max-width: 560px) {
  .page-head { align-items: flex-start; flex-direction: column; }
  /* 좁은 화면: 카드 제목 아래로 필터를 내린다 */
  .page :deep(.ws-card-head) { flex-direction: column; align-items: flex-start; }
  .filters { flex-wrap: wrap; }
}
</style>
