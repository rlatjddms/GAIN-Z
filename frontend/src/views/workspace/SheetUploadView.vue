<template>
  <SubPageLayout :back-to="editing ? `/sheets/${itemId}` : '/sheets'" :back-label="editing ? '악보' : '악보 보관함'"
                 :title="editing ? 'EDIT SHEET' : 'UPLOAD SHEET'" :name="editing ? '악보 정보 수정' : '악보 올리기'"
                 :subtitle="editing ? (mode === 'chords' ? '코드 악보와 곡 정보를 고칠 수 있어요' : mode === 'tab' ? '타브 글과 곡 정보를 고칠 수 있어요' : '곡 정보를 고치거나 장을 더 올리고 지울 수 있어요') : '악보 사진·PDF를 올리거나, 코드 사이트에서 복사한 코드·타브를 붙여 넣으세요'">
    <template #actions>
      <button class="btn primary" :disabled="!canSave" @click="save">
        {{ saving ? (editing || textMode ? '저장 중…' : `올리는 중 ${Math.round(progress * 100)}%`) : (editing || textMode ? '저장' : '올리기') }}
      </button>
    </template>
  <div class="page">

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="editing && !sheet" class="loading">불러오는 중…</div>

    <template v-else>
    <!-- 새로 만들 때: 파일 올리기 / 코드 붙여넣기 (#157) / 타브 붙여넣기 (#166) -->
    <div v-if="!editing" class="segment mode-switch" role="tablist" aria-label="악보 만드는 방법">
      <button role="tab" :aria-selected="mode === 'files'" :class="{ on: mode === 'files' }" @click="setMode('files')">파일 올리기</button>
      <button role="tab" :aria-selected="mode === 'chords'" :class="{ on: mode === 'chords' }" @click="setMode('chords')">코드 붙여넣기</button>
      <button role="tab" :aria-selected="mode === 'tab'" :class="{ on: mode === 'tab' }" @click="setMode('tab')">타브 붙여넣기</button>
    </div>

    <div class="layout">
      <!-- 코드 붙여넣기: 가사 위 코드 글 → [C]가사 형식, 바로 미리보기 -->
      <WsCard v-if="mode === 'chords'" title="코드 붙여넣기">
        <textarea v-model="chordText" class="chord-input" rows="14" spellcheck="false" :disabled="busy"
                  placeholder="코드 사이트에서 '가사 위에 코드'가 있는 부분을 복사해 붙여 넣으세요.&#10;&#10;[Verse 1]&#10;C        G&#10;이 밤 그날의 반딧불을" />
        <p class="tip">코드 줄은 아래 가사의 같은 칸에 붙이고, [Verse]·Chorus: 같은 줄은 구간 이름이 돼요. 바뀐 결과를 아래에서 확인하고, 이상한 곳은 붙여 넣은 글을 고쳐 주세요.</p>
        <div class="chord-preview">
          <ChordSheet :content="chordContent" :original-key="form.originalKey || null" empty-text="붙여 넣으면 여기에 코드 악보가 보여요." />
        </div>
      </WsCard>

      <!-- 타브 붙여넣기·직접 쓰기 (#166): 글자 타브 → 마디 악보, 타브 렌더러로 바로 미리보기 -->
      <WsCard v-else-if="mode === 'tab'" title="타브 붙여넣기 · 직접 쓰기">
        <textarea v-model="tabText" class="chord-input" rows="14" spellcheck="false" :disabled="busy"
                  :placeholder="TAB_PLACEHOLDER" />
        <p class="tip">코드 사이트의 글자 타브를 붙여 넣거나 직접 써도 돼요. 맨 위 줄이 가장 높은 줄(기타 e, 베이스 G)이고, 마디선 | 사이가 한 마디예요. 음 길이는 칸 간격으로 짐작해서 맞춰요.</p>
        <p v-if="tabResult.strings === 4 && form.instrumentType !== 'BASS'" class="tip warn">4줄 타브예요. 악기를 베이스로 바꾸면 줄 이름이 맞게 보여요.</p>
        <div class="chord-preview">
          <MeasureSheet v-if="tabRows.length" :tabs="tabRows" :instrument-type="form.instrumentType"
                        :original-key="form.originalKey || null" :view-key="form.originalKey || null" :bpm="form.bpm || null" />
          <p v-else class="empty-preview">붙여 넣으면 여기에 타브 악보가 보여요.</p>
        </div>
      </WsCard>

      <!-- 파일 -->
      <WsCard v-else :title="editing ? `악보 ${sheet.pages.length}장` : `악보 파일${picked.length ? ` ${picked.length}장` : ''}`">
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
          <span class="drop-icon"><PixelIcon name="file" :size="48" /></span>
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
              <button class="icon" :disabled="i === 0 || saving" aria-label="앞으로" @click="move(i, -1)"><ArrowUp :size="14" :stroke-width="2.4" /></button>
              <button class="icon" :disabled="i === picked.length - 1 || saving" aria-label="뒤로" @click="move(i, 1)"><ArrowDown :size="14" :stroke-width="2.4" /></button>
              <button class="icon danger" :disabled="saving" aria-label="삭제" @click="unpick(i)"><X :size="14" :stroke-width="2.4" /></button>
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
              ><X :size="14" :stroke-width="2.4" /></button>
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
          <label v-if="mode === 'files'" class="field">
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
    </template>
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ArrowUp, ArrowDown, X } from 'lucide-vue-next'
import PixelIcon from '../../components/PixelIcon.vue'
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import SheetPageView from '../../components/workspace/SheetPageView.vue'
import ChordSheet from '../../components/workspace/ChordSheet.vue'
import MeasureSheet from '../../components/workspace/MeasureSheet.vue'
import { getWorkspaceItem, uploadSheet, createTextSheet, updateSheet, addSheetPages, deleteSheetPage } from '../../api/sheet'
import { importTabText, toTabRows } from '../../music/tabImport'
import { importChordText } from '../../music/chordImport'
import { countChords } from '../../music/chordSheet'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import {
  SHEET_TYPE_OPTIONS, SHEET_FILE_ACCEPT, MAX_SHEET_PAGES, MAX_SHEET_FILE_MB, MAX_CHORD_TEXT,
  sheetFileProblem, fileSizeText, titleFromFileName,
} from '../../constants/sheet'
import { MAJOR_KEYS, MINOR_KEYS } from '../../music/transpose'
import { useAuthStore } from '../../stores/auth'
import { useDialogStore } from '../../stores/dialog'

const route = useRoute()
const router = useRouter()
const dialog = useDialogStore()
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
// 파일 올리기 / 코드 붙여넣기 / 타브 붙여넣기 (고치기는 그 악보에 맞춰 정해짐: 파일 없이 글만 있으면 코드 또는 타브)
const MODES = { chords: 'CHORD', tab: 'TAB' }
const mode = ref(MODES[route.query.mode] ? route.query.mode : 'files')
const textMode = computed(() => mode.value !== 'files')
// 타브 붙여넣기·직접 쓰기 (#166)
const tabText = ref('')
const originalTab = ref('')
const tabResult = computed(() => importTabText(tabText.value))
const tabRows = computed(() => toTabRows(tabResult.value.measures))
const TAB_PLACEHOLDER = [
  '[Intro]',
  'e|-------0-----|-------3-----|',
  'B|-----1---1---|-----0---0---|',
  'G|---0-------0-|---0-------0-|',
  'D|-2-----------|-------------|',
  'A|-------------|-2-----------|',
  'E|-------------|-------------|',
].join(String.fromCharCode(10))
const chordText = ref('')
const originalChords = ref('')
const chordContent = computed(() => importChordText(chordText.value))
const setMode = (m) => {
  mode.value = m
  if (m === 'tab') form.sheetType = 'TAB'
  if (m === 'chords') form.sheetType = 'CHORD'
}
if (textMode.value) form.sheetType = MODES[mode.value]
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
    if (s.content && !s.pages.length) {
      // 글 악보: 종류가 타브면 타브 붙여넣기, 아니면 코드 붙여넣기로 고친다
      if (s.sheetTypes?.[0] === 'TAB') {
        mode.value = 'tab'
        tabText.value = s.content
        originalTab.value = s.content
      } else {
        mode.value = 'chords'
        chordText.value = s.content
        originalChords.value = s.content
      }
    }
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
  if (!await dialog.confirm({ title: `${page.pageNumber}장을 지울까요?`, okText: '지우기', danger: true })) return
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
const dirty = computed(() => JSON.stringify(form) !== original.value
  || (mode.value === 'chords' ? chordText.value !== originalChords.value
    : mode.value === 'tab' ? tabText.value !== originalTab.value
      : (!editing && picked.value.length > 0)))
const problem = computed(() => {
  if (mode.value === 'tab') {
    if (!tabResult.value.noteCount) return '음이 있는 타브를 붙여 넣거나 써 주세요.'
    if (tabText.value.length > MAX_CHORD_TEXT) return `타브는 ${MAX_CHORD_TEXT.toLocaleString()}자까지 저장할 수 있어요.`
  } else if (mode.value === 'chords') {
    if (!countChords(chordContent.value)) return '코드가 있는 글을 붙여 넣어 주세요.'
    if (chordContent.value.length > MAX_CHORD_TEXT) return `코드 악보는 ${MAX_CHORD_TEXT.toLocaleString()}자까지 저장할 수 있어요.`
  } else if (!editing && !picked.value.length) return '악보 파일을 한 장 이상 골라 주세요.'
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
    if (textMode.value) {
      // 코드는 [C]가사 형식으로 바꾼 글, 타브는 붙여 넣은 글 그대로 저장 (보기·고치기 때 다시 읽음)
      const content = mode.value === 'tab' ? tabText.value.replace(/\s+$/, '') : chordContent.value
      const sheetType = MODES[mode.value]
      if (editing) await updateSheet(sheet.value.id, { ...info, sheetType, content })
      const item = editing ? null : await createTextSheet({ ...info, sheetType }, content)
      original.value = JSON.stringify(form)
      originalChords.value = chordText.value
      originalTab.value = tabText.value
      router.replace(`/sheets/${editing ? itemId : item.id}`)
    } else if (editing) {
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

onBeforeRouteLeave(() => (dirty.value && !loadError.value ? dialog.confirm({ title: '저장하지 않은 내용이 있어요', text: '저장하지 않고 나갈까요?', okText: '나가기' }) : true))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }


.mode-switch { margin-bottom: 16px; }
.tip.warn { color: var(--c-warning); font-weight: 700; }
.empty-preview { margin: 0; padding: 24px 0; text-align: center; font-size: 0.85rem; color: var(--c-faint); }
.chord-input {
  width: 100%; box-sizing: border-box; resize: vertical; min-height: 220px; padding: 12px 14px;
  border: 1px solid var(--c-line); border-radius: 12px; outline: none; background: var(--c-surface);
  font-family: ui-monospace, Menlo, Consolas, 'D2Coding', monospace; font-size: 0.85rem; line-height: 1.6; color: var(--c-text);
}
.chord-input:focus { border-color: var(--c-primary); }
.chord-preview { margin-top: 12px; padding: 14px 16px; border: 1px dashed var(--c-line); border-radius: 12px; max-height: 420px; overflow-y: auto; }
.layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.info-card { position: sticky; top: 16px; }

.drop {
  position: relative; display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center;
  padding: 36px 16px; border: 2px dashed #dee2e6; border-radius: 16px; cursor: pointer; transition: all 0.15s;
}
.drop:hover, .drop.over { border-color: var(--c-primary); background: var(--c-primary-soft); }
.drop.compact { padding: 16px; }
.drop.compact .drop-icon { display: none; }
.drop input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.drop input:disabled { cursor: not-allowed; }
.drop-icon { color: var(--c-faint); }
.drop strong { font-size: 0.9rem; font-weight: 800; color: #343a40; }
.drop-sub { font-size: 0.72rem; color: var(--c-faint); font-weight: 600; line-height: 1.6; }

.progress { height: 6px; margin-top: 10px; border-radius: 999px; background: var(--c-line-soft); overflow: hidden; }
.progress span { display: block; height: 100%; background: var(--c-primary); transition: width 0.2s; }

.pages { list-style: none; margin: 14px 0 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.pages li { display: flex; align-items: center; gap: 12px; padding: 8px; border: 1px solid var(--c-line-soft); border-radius: 12px; }
.thumb {
  flex: none; width: 48px; height: 64px; border-radius: 6px; overflow: hidden; background: var(--c-bg);
  display: flex; align-items: center; justify-content: center; border: 1px solid var(--c-line-soft);
}
.thumb img { width: 100%; height: 100%; object-fit: cover; object-position: top; }
.thumb .pdf { font-size: 0.65rem; font-weight: 900; color: var(--c-danger); }
.page-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.page-info strong { font-size: 0.85rem; font-weight: 800; }
.page-info span { font-size: 0.72rem; color: var(--c-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.page-actions { display: flex; gap: 4px; }
.icon {
  width: 32px; height: 32px; border: 1px solid var(--c-line); background: #fff; border-radius: 8px; cursor: pointer;
  font-family: inherit; font-weight: 900; color: var(--c-text-2);
}
.icon:hover:not(:disabled) { border-color: var(--c-primary); color: var(--c-primary-text); }
.icon.danger:hover:not(:disabled) { border-color: #ffa8a8; color: var(--c-danger); }
.icon:disabled { opacity: 0.35; cursor: not-allowed; }

.fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.field.wide { grid-column: 1 / -1; }
.field > span { font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); }
.field em { font-style: normal; color: var(--c-danger); }
.field input, .field select {
  width: 100%; box-sizing: border-box; border: 1px solid var(--c-line); border-radius: 10px; background: #fff;
  padding: 10px 12px; font-family: inherit; font-size: 0.85rem; color: #212529; outline: none;
}
.field input:focus, .field select:focus { border-color: var(--c-primary); }
.tip { margin: 12px 0 0; font-size: 0.72rem; color: var(--c-muted); line-height: 1.5; }
.hint { margin: 10px 0 0; font-size: 0.75rem; color: #e8590c; font-weight: 700; }
.error { margin: 10px 0 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }
.loading { padding: 32px 16px; text-align: center; font-size: 0.85rem; font-weight: 700; color: var(--c-faint); }

@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
  .info-card { position: static; }
}
@media (max-width: 420px) {
  .fields { grid-template-columns: 1fr; }
}
</style>
