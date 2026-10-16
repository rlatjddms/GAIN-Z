<template>
  <SubPageLayout>
  <div class="page">
    <router-link :to="editing ? `/sheets/${itemId}` : '/sheets'" class="crumb">‹ {{ editing ? '악보로 돌아가기' : '악보 보관함' }}</router-link>

    <div class="page-head">
      <div>
        <h2>{{ editing ? '악보 정보 수정' : '악보 올리기' }}</h2>
        <p>{{ editing ? '곡 정보를 고치거나 장을 더 올리고 지울 수 있어요.' : '갖고 있는 악보 사진·스캔·PDF를 올리면 내 보관함에 담겨요.' }}</p>
      </div>
      <button class="btn primary" :disabled="!canSave" @click="save">
        {{ saving ? (editing ? '저장 중…' : `올리는 중 ${Math.round(progress * 100)}%`) : (editing ? '저장' : '올리기') }}
      </button>
    </div>

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="editing && !sheet" class="loading">불러오는 중…</div>

    <div v-else class="layout">
      <!-- 파일 -->
      <WsCard :title="editing ? `악보 ${sheet.pages.length}장` : `악보 파일${picked.length ? ` ${picked.length}장` : ''}`">
        <label
          class="drop"
          :class="{ over: dragOver, compact: pageCount > 0 }"
          @dragover.prevent="dragOver = true"
          @dragleave="dragOver = false"
          @drop.prevent="onDrop"
        >
          <input
            ref="fileInput"
            type="file"
            :accept="SHEET_FILE_ACCEPT"
            multiple
            :disabled="busy || pageCount >= MAX_SHEET_PAGES"
            @change="onPick"
          >
          <span class="drop-icon">📄</span>
          <strong>{{ pageCount ? '장 더 올리기' : '악보 파일을 끌어 놓거나 눌러서 고르세요' }}</strong>
          <span class="drop-sub">PNG·JPG·WEBP 이미지, PDF · 한 장 {{ MAX_SHEET_FILE_MB }}MB까지 · 최대 {{ MAX_SHEET_PAGES }}장<br>휴대폰에서는 카메라로 바로 찍을 수 있어요</span>
        </label>
        <p v-if="fileError" class="error">{{ fileError }}</p>
        <div v-if="editing && adding" class="progress"><span :style="{ width: `${progress * 100}%` }"></span></div>

        <!-- 올리기 전: 고른 파일 순서 정하기 -->
        <ol v-if="!editing && picked.length" class="pages">
          <li v-for="(p, i) in picked" :key="p.key">
            <div class="thumb">
              <img v-if="p.preview" :src="p.preview" alt="">
              <span v-else class="pdf">PDF</span>
            </div>
            <div class="page-info">
              <strong>{{ i + 1 }}장</strong>
              <span>{{ p.file.name }} · {{ fileSizeText(p.file.size) }}</span>
            </div>
            <div class="page-actions">
              <button class="icon" :disabled="i === 0 || saving" aria-label="앞으로" @click="move(i, -1)">↑</button>
              <button class="icon" :disabled="i === picked.length - 1 || saving" aria-label="뒤로" @click="move(i, 1)">↓</button>
              <button class="icon danger" :disabled="saving" aria-label="삭제" @click="unpick(i)">✕</button>
            </div>
          </li>
        </ol>

        <!-- 수정: 이미 올린 장 -->
        <ol v-if="editing" class="pages">
          <li v-for="p in sheet.pages" :key="p.id">
            <div class="thumb">
              <SheetPageView :sheet-id="sheet.id" :page-id="p.id" :content-type="p.contentType" thumb />
            </div>
            <div class="page-info">
              <strong>{{ p.pageNumber }}장</strong>
              <span>{{ p.fileName }} · {{ fileSizeText(p.size) }}</span>
            </div>
            <div class="page-actions">
              <button
                class="icon danger"
                :disabled="busy || sheet.pages.length <= 1"
                :title="sheet.pages.length <= 1 ? '마지막 한 장은 지울 수 없어요' : ''"
                aria-label="이 장 지우기"
                @click="removePage(p)"
              >✕</button>
            </div>
          </li>
        </ol>
      </WsCard>

      <!-- 곡 정보 -->
      <WsCard title="곡 정보" class="info-card">
        <div class="fields">
          <label class="field wide">
            <span>곡 제목 <em>*</em></span>
            <input v-model="form.title" maxlength="100" placeholder="제목을 입력해 주세요">
          </label>
          <label class="field wide">
            <span>아티스트</span>
            <input v-model="form.artist" maxlength="100" placeholder="아티스트를 입력해 주세요">
          </label>
          <label class="field">
            <span>악기 <em>*</em></span>
            <select v-model="form.instrumentType">
              <option v-for="o in INSTRUMENT_OPTIONS" :key="o.value" :value="o.value">{{ o.emoji }} {{ o.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>악보 종류 <em>*</em></span>
            <select v-model="form.sheetType">
              <option v-for="o in SHEET_TYPE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>원래 키</span>
            <select v-model="form.originalKey">
              <option value="">모름</option>
              <optgroup label="장조">
                <option v-for="k in MAJOR_KEYS" :key="k" :value="k">{{ k }}</option>
              </optgroup>
              <optgroup label="단조">
                <option v-for="k in MINOR_KEYS" :key="k" :value="k">{{ k }}</option>
              </optgroup>
            </select>
          </label>
          <label class="field">
            <span>BPM</span>
            <input v-model.number="form.bpm" type="number" min="20" max="300" inputmode="numeric" placeholder="BPM을 입력해 주세요">
          </label>
        </div>
        <p class="tip">원래 키를 적어 두면 악보를 볼 때 다른 키로 바꿔 칠 코드를 알려 드려요.</p>
        <p v-if="problem" class="hint">{{ problem }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </WsCard>
    </div>
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import SheetPageView from '../../components/workspace/SheetPageView.vue'
import { getWorkspaceItem, uploadSheet, updateSheet, addSheetPages, deleteSheetPage } from '../../api/sheet'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import {
  SHEET_TYPE_OPTIONS, SHEET_FILE_ACCEPT, MAX_SHEET_PAGES, MAX_SHEET_FILE_MB,
  sheetFileProblem, fileSizeText, titleFromFileName,
} from '../../constants/sheet'
import { MAJOR_KEYS, MINOR_KEYS } from '../../music/transpose'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const itemId = route.params.id
const editing = !!itemId

const form = reactive({
  title: '',
  artist: '',
  instrumentType: auth.character?.instrumentType || 'GUITAR',
  sheetType: 'SCORE',
  originalKey: '',
  bpm: null,
})
// 마지막으로 저장(불러온) 곡 정보. 바뀌었는지 비교용
const original = ref(JSON.stringify(form))
const sheet = ref(null)
const loadError = ref('')
const saving = ref(false)
const adding = ref(false)
const progress = ref(0)
const error = ref('')
const fileError = ref('')
const dragOver = ref(false)
const fileInput = ref(null)

onMounted(async () => {
  if (!editing) return
  try {
    const item = await getWorkspaceItem(itemId)
    if (!item.sheet.mine) {
      loadError.value = '직접 올린 악보만 고칠 수 있어요.'
      return
    }
    const s = item.sheet
    sheet.value = s
    Object.assign(form, {
      title: s.title, artist: s.artist || '', instrumentType: s.instrumentType,
      sheetType: s.sheetTypes?.[0] || 'SCORE', originalKey: s.originalKey || '', bpm: s.bpm,
    })
    original.value = JSON.stringify(form)
  } catch (e) {
    loadError.value = `악보를 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
})

/* ---------- 파일 고르기 ---------- */
const picked = ref([])   // 새로 올릴 때: [{ key, file, preview }]
let seq = 0
const pageCount = computed(() => (editing ? sheet.value?.pages.length || 0 : picked.value.length))
const busy = computed(() => saving.value || adding.value)

const takeFiles = (fileList) => {
  fileError.value = ''
  const files = [...fileList]
  const room = MAX_SHEET_PAGES - pageCount.value
  const problems = files.map(sheetFileProblem).filter(Boolean)
  const ok = files.filter((f) => !sheetFileProblem(f)).slice(0, Math.max(room, 0))
  if (problems.length) fileError.value = problems[0]
  else if (files.length > ok.length) fileError.value = `악보는 최대 ${MAX_SHEET_PAGES}장까지 올릴 수 있어요.`
  return ok
}

const addFiles = (fileList) => {
  const files = takeFiles(fileList)
  if (!files.length) return
  if (editing) {
    uploadMore(files)
    return
  }
  if (!form.title.trim()) form.title = titleFromFileName(files[0].name)
  picked.value.push(...files.map((file) => ({
    key: ++seq,
    file,
    preview: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
  })))
}

const onPick = (e) => { addFiles(e.target.files); e.target.value = '' }
const onDrop = (e) => { dragOver.value = false; if (!busy.value) addFiles(e.dataTransfer.files) }
const move = (i, d) => {
  const list = picked.value
  ;[list[i], list[i + d]] = [list[i + d], list[i]]
}
const unpick = (i) => {
  const [p] = picked.value.splice(i, 1)
  if (p.preview) URL.revokeObjectURL(p.preview)
}
onBeforeUnmount(() => picked.value.forEach((p) => p.preview && URL.revokeObjectURL(p.preview)))

/* ---------- 수정: 장 추가·삭제는 바로 반영 ---------- */
const uploadMore = async (files) => {
  adding.value = true
  progress.value = 0
  try {
    const result = await addSheetPages(sheet.value.id, files, (p) => { progress.value = p })
    sheet.value.pages = result.pages
  } catch (e) {
    fileError.value = `올리지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    adding.value = false
  }
}

const removePage = async (page) => {
  if (!window.confirm(`${page.pageNumber}장을 지울까요?`)) return
  adding.value = true
  try {
    const result = await deleteSheetPage(sheet.value.id, page.id)
    sheet.value.pages = result.pages
  } catch (e) {
    fileError.value = `지우지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    adding.value = false
  }
}

/* ---------- 저장 ---------- */
const dirty = computed(() => JSON.stringify(form) !== original.value || (!editing && picked.value.length > 0))
const problem = computed(() => {
  if (!editing && !picked.value.length) return '악보 파일을 한 장 이상 골라 주세요.'
  if (!form.title.trim()) return '곡 제목을 입력해 주세요.'
  if (form.bpm !== null && form.bpm !== '' && (form.bpm < 20 || form.bpm > 300)) return 'BPM은 20~300 사이로 입력해 주세요.'
  return ''
})
const canSave = computed(() => !busy.value && !problem.value && dirty.value && !loadError.value)

const save = async () => {
  saving.value = true
  progress.value = 0
  error.value = ''
  const info = { ...form, title: form.title.trim(), artist: form.artist.trim() }
  try {
    if (editing) {
      await updateSheet(sheet.value.id, info)
      original.value = JSON.stringify(form)
      router.replace(`/sheets/${itemId}`)
    } else {
      const item = await uploadSheet(info, picked.value.map((p) => p.file), (p) => { progress.value = p })
      original.value = JSON.stringify(form)
      picked.value = []
      router.replace(`/sheets/${item.id}`)
    }
  } catch (e) {
    error.value = `${editing ? '저장' : '올리기'}하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

onBeforeRouteLeave(() => (dirty.value && !loadError.value ? window.confirm('저장하지 않은 내용이 있어요. 나갈까요?') : true))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.crumb { align-self: flex-start; font-size: 0.8rem; font-weight: 800; color: #868e96; text-decoration: none; }
.crumb:hover { color: #2d5bff; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-head h2 { margin: 0 0 4px; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.8rem; font-weight: 700; color: #adb5bd; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit; cursor: pointer;
  font-weight: 800; font-size: 0.85rem; padding: 10px 22px; border-radius: 12px;
}
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.info-card { position: sticky; top: 16px; }

.drop {
  position: relative; display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center;
  padding: 36px 16px; border: 2px dashed #dee2e6; border-radius: 16px; cursor: pointer; transition: all 0.15s;
}
.drop:hover, .drop.over { border-color: #2d5bff; background: #f0f4ff; }
.drop.compact { padding: 16px; }
.drop.compact .drop-icon { display: none; }
.drop input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.drop input:disabled { cursor: not-allowed; }
.drop-icon { font-size: 1.8rem; }
.drop strong { font-size: 0.9rem; font-weight: 800; color: #343a40; }
.drop-sub { font-size: 0.72rem; color: #adb5bd; font-weight: 600; line-height: 1.6; }

.progress { height: 6px; margin-top: 10px; border-radius: 999px; background: #f1f3f5; overflow: hidden; }
.progress span { display: block; height: 100%; background: #2d5bff; transition: width 0.2s; }

.pages { list-style: none; margin: 14px 0 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.pages li { display: flex; align-items: center; gap: 12px; padding: 8px; border: 1px solid #f1f3f5; border-radius: 12px; }
.thumb {
  flex: none; width: 48px; height: 64px; border-radius: 6px; overflow: hidden; background: #f8f9fa;
  display: flex; align-items: center; justify-content: center; border: 1px solid #f1f3f5;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; object-position: top; }
.thumb .pdf { font-size: 0.65rem; font-weight: 900; color: #e03131; }
.page-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.page-info strong { font-size: 0.85rem; font-weight: 800; }
.page-info span { font-size: 0.72rem; color: #868e96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.page-actions { display: flex; gap: 4px; }
.icon {
  width: 32px; height: 32px; border: 1px solid #e9ecef; background: #fff; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-weight: 900; color: #495057;
}
.icon:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.icon.danger:hover:not(:disabled) { border-color: #ffa8a8; color: #e03131; }
.icon:disabled { opacity: 0.35; cursor: not-allowed; }

.fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.field.wide { grid-column: 1 / -1; }
.field > span { font-size: 0.75rem; font-weight: 800; color: #495057; }
.field em { font-style: normal; color: #e03131; }
.field input, .field select {
  width: 100%; box-sizing: border-box; border: 1px solid #e9ecef; border-radius: 10px; background: #fff;
  padding: 10px 12px; font-family: inherit; font-size: 0.85rem; color: #212529; outline: none;
}
.field input:focus, .field select:focus { border-color: #2d5bff; }
.tip { margin: 12px 0 0; font-size: 0.72rem; color: #868e96; line-height: 1.5; }
.hint { margin: 10px 0 0; font-size: 0.75rem; color: #e8590c; font-weight: 700; }
.error { margin: 10px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading { padding: 48px; text-align: center; font-size: 0.85rem; color: #adb5bd; }

@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
  .info-card { position: static; }
}
@media (max-width: 420px) {
  .fields { grid-template-columns: 1fr; }
}
</style>
