<template>
  <form id="community-post-form" class="editor" @submit.prevent="save">
    <div class="author">
      <ProfileAvatar :src="author.profileImageUrl" :size="40" />
      <div><strong>{{ author.nickname }}</strong><span>{{ band.name }}에 {{ noticeOnly ? '등록된 글이에요' : '이야기를 남겨 보세요' }}</span></div>
    </div>

    <p v-if="noticeOnly" class="notice-hint">다른 멤버의 글은 공지 설정만 바꿀 수 있어요.</p>
    <label class="sr-only" for="post-title">제목</label>
    <input id="post-title" v-model="draft.title" class="title-input" placeholder="제목을 입력해 주세요" maxlength="100" required :readonly="noticeOnly" :disabled="saving">
    <label class="sr-only" for="post-content">내용</label>
    <MentionInput :members="members" id="post-content" v-model="draft.content" class="content-input" placeholder="합주 이야기, 연습 영상, 함께 나누고 싶은 소식을 적어 주세요." maxlength="10000" rows="7" required :readonly="noticeOnly" :disabled="saving" />

    <section class="attachments-section" aria-label="첨부 파일">
      <div class="section-head"><span><BoardIcon name="attach" :size="17" /> 첨부 파일 <b>{{ retained.length + picked.length }}</b></span><small>최대 8개 · 파일당 10MB</small></div>
      <div v-if="retained.length || picked.length" class="file-grid">
        <div v-for="item in retained" :key="item.id" class="file-tile">
          <BandPostAttachment :attachment="item" tile />
          <button v-if="!noticeOnly" type="button" class="remove-file" :aria-label="`${item.name} 첨부 삭제`" :disabled="saving" @click="retained = retained.filter(f => f.id !== item.id)"><BoardIcon name="close" :size="14" /></button>
          <div class="file-caption"><span>{{ item.name }}</span><small>등록됨</small></div>
        </div>
        <div v-for="item in picked" :key="item.key" class="file-tile">
          <div class="local-preview">
            <img v-if="item.file.type.startsWith('image/')" :src="item.url" :alt="item.file.name">
            <video v-else-if="item.file.type.startsWith('video/')" :src="item.url" controls preload="auto" playsinline :aria-label="item.file.name" />
            <div v-else class="file-symbol"><BoardIcon name="file" :size="32" /><span>{{ item.file.type.startsWith('audio/') ? '오디오' : 'PDF' }}</span></div>
          </div>
          <button type="button" class="remove-file" :aria-label="`${item.file.name} 선택 취소`" :disabled="saving" @click="unpick(item)"><BoardIcon name="close" :size="14" /></button>
          <div class="file-caption"><span>{{ item.file.name }}</span><small>{{ fileSize(item.file.size) }}</small></div>
        </div>
      </div>
      <label v-if="!noticeOnly && retained.length + picked.length < BAND_ATTACHMENT_MAX_COUNT" class="dropzone" :class="{ over: dragOver, compact: retained.length + picked.length }"
             @dragover.prevent="dragOver = !saving" @dragleave.prevent="dragOver = false" @drop.prevent="onDrop">
        <input ref="fileInput" type="file" multiple :accept="BAND_ATTACHMENT_ACCEPT" :disabled="saving" aria-label="첨부 파일 선택" @change="onPick">
        <span class="upload-icon"><BoardIcon name="image" :size="23" /></span>
        <span class="drop-copy"><strong>{{ retained.length + picked.length ? '파일 더 추가하기' : '사진이나 영상을 끌어 놓으세요' }}</strong><span>또는 <b>파일 선택</b> · 사진, 영상, 오디오, PDF</span></span>
        <BoardIcon name="plus" :size="20" />
      </label>
      <p v-if="fileError" class="error" role="alert">{{ fileError }}</p>
    </section>

    <label v-if="canNotice" class="notice-option">
      <span class="pin-icon"><BoardIcon name="pin" :size="19" /></span>
      <span class="notice-copy"><strong>공지로 등록</strong><span>커뮤니티 상단에 고정해 밴드원에게 알려요.</span></span>
      <input v-model="draft.notice" type="checkbox" :disabled="saving" role="switch" aria-label="공지로 등록">
      <span class="switch" aria-hidden="true" />
    </label>
    <section v-if="canNotice && draft.notice" class="notice-schedule" aria-label="공지 기간">
      <div class="period-row">
        <span>고정 기간</span>
        <div class="period-options" role="radiogroup" aria-label="공지 기간 선택">
          <label><input v-model="draft.endMode" type="radio" value="none" :disabled="saving"><span>무기한</span></label>
          <label><input v-model="draft.endMode" type="radio" value="scheduled" :disabled="saving"><span>종료일 지정</span></label>
        </div>
        <div v-if="draft.endMode === 'scheduled'" class="period-end">
          <label class="sr-only" for="notice-end">공지 종료 일시</label>
          <input id="notice-end" v-model="draft.noticeEnd" type="datetime-local" :min="localDateTime(Date.now() + 60000)" :disabled="saving" required>
        </div>
      </div>
      <p v-if="draft.endMode === 'scheduled'" class="period-hint">종료 후 일반 글로 전환</p>
    </section>
    <div v-if="saving" class="save-progress" role="status"><span :style="{ width: `${progress}%` }" /><p>{{ progress < 100 ? `첨부 올리는 중 ${progress}%` : '게시글을 저장하고 있어요…' }}</p></div>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
  </form>
</template>

<script setup>
import { computed, reactive, ref, watchEffect, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../../stores/auth'
import MentionInput from './MentionInput.vue'
import { mentionMembers, localDateTime } from '../../constants/bandBoard'
import ProfileAvatar from '../ProfileAvatar.vue'
import BoardIcon from './BoardIcon.vue'
import BandPostAttachment from './BandPostAttachment.vue'
import { saveBandPost, setBandNotice, BAND_ATTACHMENT_ACCEPT, BAND_ATTACHMENT_MAX_BYTES, BAND_ATTACHMENT_MAX_COUNT } from '../../api/bandBoard'

const props = defineProps({ band: { type: Object, required: true }, post: { type: Object, default: null } })
const emit = defineEmits(['saved', 'state'])
const auth = useAuthStore()
const members = computed(() => mentionMembers(props.band))
const canNotice = computed(() => props.band.myPosition === 'LEADER')
const noticeOnly = computed(() => props.post && !props.post.mine)
const author = computed(() => props.post?.author || { nickname: auth.character?.nickname || '나', profileImageUrl: auth.profile?.profileImageUrl })
const draft = reactive({ title: props.post?.title || '', content: props.post?.content || '', notice: props.post?.notice || false, endMode: props.post?.noticeExpiresAt ? 'scheduled' : 'none', noticeEnd: localDateTime(props.post?.noticeExpiresAt) })
const initial = JSON.stringify(draft)
const originalFiles = props.post?.attachments || (props.post?.attachment ? [{ ...props.post.attachment, id: props.post.attachment.id || 'legacy' }] : [])
const retained = ref([...originalFiles])
const picked = ref([])
const fileInput = ref(null)
const dragOver = ref(false)
const fileError = ref('')
const error = ref('')
const saving = ref(false)
const progress = ref(0)
let key = 0
const dirty = computed(() => JSON.stringify(draft) !== initial || retained.value.length !== originalFiles.length || picked.value.length > 0)
const expiryError = computed(() => {
  if (!canNotice.value || !draft.notice || draft.endMode !== 'scheduled') return ''
  if (!draft.noticeEnd) return '공지 종료 날짜와 시간을 선택해 주세요.'
  return !Number.isFinite(Date.parse(draft.noticeEnd)) || Date.parse(draft.noticeEnd) <= Date.now() ? '현재보다 이후의 날짜와 시간을 선택해 주세요.' : ''
})
const valid = computed(() => !expiryError.value && draft.title.trim() && draft.content.trim() && (!noticeOnly.value || canNotice.value))
watchEffect(() => emit('state', { busy: saving.value, dirty: dirty.value, valid: !!valid.value }))
const fileSize = size => size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(size / 1024))} KB`

function addFiles(files) {
  if (saving.value || noticeOnly.value) return
  fileError.value = ''
  const added = [...files]
  if (retained.value.length + picked.value.length + added.length > BAND_ATTACHMENT_MAX_COUNT) {
    fileError.value = '첨부 파일은 최대 8개까지 선택할 수 있어요.'
    return
  }
  if (added.some(file => file.size > BAND_ATTACHMENT_MAX_BYTES || !BAND_ATTACHMENT_ACCEPT.split(',').includes(file.type))) {
    fileError.value = '사진·영상·오디오·PDF 파일을 파일당 10MB까지 첨부할 수 있어요.'
    return
  }
  picked.value.push(...added.map(file => ({ key: ++key, file, url: URL.createObjectURL(file) })))
}
function onPick(event) { addFiles(event.target.files || []); event.target.value = '' }
function onDrop(event) { dragOver.value = false; addFiles(event.dataTransfer.files || []) }
function unpick(item) { URL.revokeObjectURL(item.url); picked.value = picked.value.filter(f => f.key !== item.key) }
onBeforeUnmount(() => picked.value.forEach(item => URL.revokeObjectURL(item.url)))

async function save() {
  if (!valid.value || saving.value) return
  const expiresAt = canNotice.value && draft.notice && draft.endMode === 'scheduled' ? new Date(draft.noticeEnd).toISOString() : null
  if (expiresAt && Date.parse(expiresAt) <= Date.now()) { error.value = '공지 종료 시각이 지났어요. 새 날짜와 시간을 선택해 주세요.'; return }
  saving.value = true
  error.value = ''
  progress.value = 0
  try {
    const result = noticeOnly.value
      ? await setBandNotice(props.band.id, props.post.id, draft.notice, expiresAt)
      : await saveBandPost(props.band.id, props.post?.id, {
        title: draft.title.trim(), content: draft.content.trim(), notice: canNotice.value ? draft.notice : null,
        keepAttachmentIds: retained.value.map(item => item.id), noticeExpiresAt: expiresAt,
      }, picked.value.map(item => item.file), percent => { progress.value = percent })
    saving.value = false
    emit('state', { busy: false, dirty: false, valid: true })
    emit('saved', result)
  } catch (e) {
    error.value = e.response?.data?.message || '저장하지 못했어요. 입력한 내용과 첨부를 유지했으니 다시 시도해 주세요.'
  } finally { saving.value = false }
}
</script>

<style scoped>
* { box-sizing: border-box; }
.editor { padding: 28px 32px 32px; }
.author { display: flex; align-items: center; gap: 11px; margin-bottom: 24px; }
.author strong { display: block; font-size: 14px; font-weight: 800; }
.author span { display: block; color: var(--c-muted); font-size: 12px; margin-top: 5px; }
.title-input, :deep(.content-input) { width: 100%; border: 0; border-radius: 0; background: transparent; color: #212529; font-family: inherit; outline: none; }
.title-input { font-size: 23px; font-weight: 800; padding: 0 0 18px; border-bottom: 1px solid var(--c-line-soft); }
.title-input::placeholder { color: var(--c-faint); }
:deep(.content-input) { padding: 20px 0; font-size: 14px; line-height: 1.85; resize: vertical; min-height: 190px; }
.content-input::placeholder { color: var(--c-faint); }
.title-input:focus { border-bottom-color: var(--c-primary-line); }
.attachments-section { margin-top: 8px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 12px; }
.section-head > span { display: flex; align-items: center; gap: 6px; color: var(--c-text-2); font-size: 12px; font-weight: 800; }
.section-head b { color: var(--c-primary-text); }
.section-head small { color: var(--c-muted); font-size: 11px; }
.dropzone { display: flex; align-items: center; gap: 14px; padding: 22px 20px; background: #fafbfe; border: 1px dashed #cdd5e4; border-radius: 14px; cursor: pointer; transition: .15s; }
.dropzone:hover, .dropzone.over, .dropzone:focus-within { background: var(--c-primary-soft); border-color: var(--c-primary); }
.dropzone input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.upload-icon { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 12px; background: var(--c-primary-soft); color: var(--c-primary-text); flex: none; }
.drop-copy { flex: 1; min-width: 0; }
.drop-copy strong { display: block; font-size: 13px; font-weight: 700; }
.drop-copy > span { display: block; font-size: 11px; color: var(--c-muted); margin-top: 6px; }
.drop-copy b { color: var(--c-primary-text); font-weight: 700; }
.dropzone > svg { color: var(--c-faint); }
.dropzone.compact { padding: 14px; margin-top: 12px; }
.file-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.file-tile { position: relative; min-width: 0; border: 1px solid var(--c-line); border-radius: 12px; overflow: hidden; }
.local-preview { height: 124px; background: #f4f6f8; }
.local-preview img, .local-preview video { width: 100%; height: 100%; display: block; object-fit: cover; }
.local-preview video { object-fit: contain; background: #20242c; }
.file-symbol { height: 100%; display: grid; place-content: center; justify-items: center; gap: 8px; color: var(--c-muted); font-size: 11px; }
.remove-file { position: absolute; top: 7px; right: 7px; width: 24px; height: 24px; display: grid; place-items: center; border: 0; border-radius: 50%; background: rgba(25,30,40,.7); color: white; cursor: pointer; }
.file-caption { display: flex; flex-direction: column; gap: 4px; padding: 10px; }
.file-caption span { font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; }
.file-caption small { color: var(--c-faint); font-size: 10px; }
.notice-option { display: flex; align-items: center; gap: 12px; padding: 18px 0 0; margin-top: 24px; border-top: 1px solid var(--c-line-soft); cursor: pointer; }
.pin-icon { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 10px; background: var(--c-primary-soft); color: var(--c-primary-text); }
.notice-copy { flex: 1; }
.notice-copy strong { display: block; font-size: 13px; }
.notice-copy > span { display: block; margin-top: 4px; color: var(--c-muted); font-size: 11px; }
.notice-option input { position: absolute; opacity: 0; width: 1px; height: 1px; }
.switch { width: 36px; height: 21px; background: #dee2e6; border-radius: 20px; padding: 3px; transition: background .15s; }
.switch::after { content: ''; display: block; width: 15px; height: 15px; border-radius: 50%; background: #fff; box-shadow: 0 1px 3px #0002; transition: transform .15s; }
input:checked + .switch { background: var(--c-primary); }
input:checked + .switch::after { transform: translateX(15px); }
input:focus-visible + .switch { outline: 3px solid var(--c-primary-line); outline-offset: 3px; }
.error { padding: 10px 14px; border-radius: var(--radius-md); background: var(--c-danger-soft); color: var(--c-danger); font-size: 0.8rem; font-weight: 700; line-height: 1.6; }
.notice-schedule { margin: 14px 0 0 48px; }
.period-row { display: flex; align-items: center; justify-content: flex-start; gap: 10px; }
.period-row > span { flex: none; white-space: nowrap; color: #7b8491; font-size: 12px; }
.period-options { flex: none; white-space: nowrap; display: inline-flex; gap: 2px; padding: 3px; border-radius: 9px; background: #f1f3f6; }
.period-options label { position: relative; cursor: pointer; }
.period-options input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.period-options span { display: block; padding: 7px 11px; border-radius: 6px; color: #858e9b; font-size: 11px; font-weight: 600; }
.period-options input:checked + span { color: #343f52; background: white; box-shadow: 0 1px 3px #17243b10; }
.period-options input:focus-visible + span { outline: 2px solid var(--c-primary-line); outline-offset: 1px; }
.period-options input:disabled + span { opacity: .5; cursor: default; }
.period-end { flex: 1; min-width: 0; max-width: 240px; }
.period-end > input { box-sizing: border-box; width: 100%; min-width: 0; padding: 7px 10px; border: 1px solid #e1e6ee; border-radius: 9px; background: #fff; color: var(--c-text-2); font: inherit; font-size: 13px; outline: none; }
.period-end > input:focus { border-color: var(--c-primary-line); box-shadow: 0 0 0 3px var(--c-primary)08; }
.period-hint { margin: 7px 0 0; color: #939ba7; font-size: 10px; line-height: 1.6; }
.notice-hint { color: #62708c; background: #f5f7fb; border-radius: 10px; padding: 12px; font-size: 12px; }
.save-progress { margin-top: 20px; height: 3px; background: #edf0f5; border-radius: 3px; }
.save-progress > span { display: block; height: 3px; background: var(--c-primary); border-radius: 3px; }
.save-progress p { color: var(--c-muted); font-size: 11px; margin-top: 8px; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0,0,0,0); }
@media(max-width: 600px) {
  .notice-schedule { margin-left: 0; }
  .period-row { gap: 6px; }
  .period-row > span { font-size: 10px; }
  .period-options span { padding: 7px 6px; font-size: 10px; }
  .period-end > input { padding: 7px 5px; font-size: 11px; }
  .editor { padding: 22px 20px 28px; }
  .title-input { font-size: 21px; }
  :deep(.content-input) { min-height: 160px; }
  .file-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dropzone { padding: 16px 12px; gap: 10px; }
  .dropzone > svg { display: none; }
  .drop-copy strong { font-size: 12px; }
}
</style>
