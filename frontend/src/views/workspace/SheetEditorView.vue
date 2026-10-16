<template>
  <SubPageLayout>
  <div class="page">
    <router-link :to="editing ? `/sheets/${itemId}` : '/sheets'" class="crumb">‹ {{ editing ? '악보로 돌아가기' : '악보 보관함' }}</router-link>

    <div class="page-head">
      <div>
        <h2>{{ editing ? '악보 수정' : '악보 쓰기' }}</h2>
        <p>코드는 [C] 처럼 대괄호로 가사 앞에 써요. 저장하면 내 보관함에 담겨요.</p>
      </div>
      <div class="head-actions">
        <button class="btn primary" :disabled="!canSave" @click="save">{{ saving ? '저장 중…' : '저장' }}</button>
      </div>
    </div>

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="editing && !loaded" class="loading">불러오는 중…</div>

    <div v-else class="layout">
      <WsCard title="곡 정보 · 악보">
        <div class="fields">
          <label class="field wide">
            <span>곡 제목 <em>*</em></span>
            <input v-model="form.title" maxlength="100" placeholder="예: 밤편지">
          </label>
          <label class="field wide">
            <span>아티스트</span>
            <input v-model="form.artist" maxlength="100" placeholder="예: 아이유">
          </label>
          <label class="field">
            <span>원래 키 <em>*</em></span>
            <select v-model="form.originalKey">
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
            <input v-model.number="form.bpm" type="number" min="20" max="300" inputmode="numeric" placeholder="모르면 비워 두기">
          </label>
          <label class="field wide">
            <span>악기 <em>*</em></span>
            <select v-model="form.instrumentType">
              <option v-for="o in INSTRUMENT_OPTIONS" :key="o.value" :value="o.value">{{ o.emoji }} {{ o.label }}</option>
            </select>
          </label>
        </div>

        <label class="field content-field">
          <span>악보 <small>{{ form.content.length.toLocaleString() }} / 20,000</small></span>
          <textarea
            v-model="form.content"
            maxlength="20000"
            spellcheck="false"
            :placeholder="PLACEHOLDER"
          ></textarea>
        </label>
        <ul class="help">
          <li><code>[C]</code> <code>[Am7]</code> <code>[D/F#]</code> 코드는 대괄호로, 바로 뒤 가사 위에 표시돼요</li>
          <li><code># 후렴</code> 처럼 #으로 시작하는 줄은 구간 이름이에요</li>
          <li>빈 줄은 그대로 줄 띄움이 돼요</li>
        </ul>
        <p v-if="problem" class="hint">{{ problem }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </WsCard>

      <WsCard title="미리보기" class="preview-card">
        <ChordSheet
          :content="form.content"
          :original-key="form.originalKey"
          empty-text="왼쪽에 악보를 쓰면 여기 보여요."
        />
      </WsCard>
    </div>
  </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import ChordSheet from '../../components/workspace/ChordSheet.vue'
import { getWorkspaceItem, createSheet, updateSheet } from '../../api/sheet'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { MAJOR_KEYS, MINOR_KEYS } from '../../music/transpose'
import { useAuthStore } from '../../stores/auth'

const PLACEHOLDER = `# 1절
[G]이 밤 그날의 [D]반딧불을
[Em]당신의 창 [C]가까이 보낼게요

# 후렴
[C]사랑한다는 [D]말이에요`

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const itemId = route.params.id
const editing = !!itemId

const form = reactive({
  title: '',
  artist: '',
  originalKey: 'C',
  bpm: null,
  instrumentType: auth.character?.instrumentType || 'GUITAR',
  content: '',
})
const sheetId = ref(null)
const loaded = ref(false)
const loadError = ref('')
const saving = ref(false)
const error = ref('')
// 마지막으로 저장(불러온) 내용. 바뀌었는지 비교용
const original = ref(JSON.stringify(form))

onMounted(async () => {
  if (!editing) return
  try {
    const item = await getWorkspaceItem(itemId)
    if (!item.sheet.mine) {
      loadError.value = '직접 쓴 악보만 고칠 수 있어요.'
      return
    }
    const s = item.sheet
    sheetId.value = s.id
    Object.assign(form, {
      title: s.title, artist: s.artist || '', originalKey: s.originalKey || 'C',
      bpm: s.bpm, instrumentType: s.instrumentType, content: s.content || '',
    })
    original.value = JSON.stringify(form)
    loaded.value = true
  } catch (e) {
    loadError.value = `악보를 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
})

const dirty = computed(() => JSON.stringify(form) !== original.value)
const problem = computed(() => {
  if (!form.title.trim()) return '곡 제목을 입력해 주세요.'
  if (form.bpm !== null && form.bpm !== '' && (form.bpm < 20 || form.bpm > 300)) return 'BPM은 20~300 사이로 입력해 주세요.'
  return ''
})
const canSave = computed(() => !saving.value && !problem.value && dirty.value && !loadError.value)

const save = async () => {
  saving.value = true
  error.value = ''
  try {
    const body = { ...form, title: form.title.trim(), artist: form.artist.trim(), bpm: form.bpm || null }
    if (editing) {
      await updateSheet(sheetId.value, body)
      original.value = JSON.stringify(form)
      router.replace(`/sheets/${itemId}`)
    } else {
      const item = await createSheet(body)
      original.value = JSON.stringify(form)
      router.replace(`/sheets/${item.id}`)
    }
  } catch (e) {
    error.value = `저장하지 못했어요. ${apiErrorMessage(e)}`
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
.head-actions { display: flex; gap: 8px; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit; cursor: pointer;
  font-weight: 800; font-size: 0.85rem; padding: 10px 22px; border-radius: 12px;
}
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.preview-card { position: sticky; top: 16px; max-height: calc(100vh - 32px); overflow-y: auto; }

.fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 14px; }
.field { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.field.wide { grid-column: 1 / -1; }
.field > span { font-size: 0.75rem; font-weight: 800; color: #495057; display: flex; justify-content: space-between; }
.field em { font-style: normal; color: #e03131; }
.field small { font-weight: 700; color: #adb5bd; }
.field input, .field select, .field textarea {
  width: 100%; box-sizing: border-box; border: 1px solid #e9ecef; border-radius: 10px; background: #fff;
  padding: 10px 12px; font-family: inherit; font-size: 0.85rem; color: #212529; outline: none;
}
.field input:focus, .field select:focus, .field textarea:focus { border-color: #2d5bff; }
.content-field textarea {
  min-height: 360px; resize: vertical; line-height: 1.7;
  font-family: 'D2Coding', ui-monospace, Consolas, monospace; font-size: 0.85rem;
}
.help { margin: 10px 0 0; padding-left: 18px; font-size: 0.72rem; color: #868e96; line-height: 1.8; }
code { background: #f1f3f5; border-radius: 4px; padding: 1px 5px; font-size: 0.7rem; color: #e8590c; font-weight: 700; }
.hint { margin: 10px 0 0; font-size: 0.75rem; color: #e8590c; font-weight: 700; }
.error { margin: 10px 0 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading { padding: 48px; text-align: center; font-size: 0.85rem; color: #adb5bd; }

@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
  .preview-card { position: static; max-height: none; }
  .content-field textarea { min-height: 240px; }
}
@media (max-width: 480px) {
  .fields { grid-template-columns: 1fr; }
}
</style>
