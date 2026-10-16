<template>
  <Teleport to="body">
    <dialog ref="dialog" class="post-dialog" aria-labelledby="community-dialog-title" @cancel.prevent="close" @click="onBackdrop">
      <header class="dialog-toolbar">
        <div class="dialog-context"><span>{{ band.name }}</span><h2 id="community-dialog-title">{{ mode === 'new' ? '새 글 작성' : mode === 'edit' ? (post?.mine ? '글 수정' : '공지 설정') : '밴드 이야기' }}</h2></div>
        <button v-if="mode !== 'detail'" class="publish" type="submit" form="community-post-form" :disabled="editorState.busy || !editorState.valid || loading">{{ editorState.busy ? '저장 중…' : mode === 'new' ? '등록하기' : '저장하기' }}<BoardIcon name="arrow" :size="15" /></button>
        <button type="button" class="close-button" aria-label="게시글 창 닫기" :disabled="busy" @click="close"><BoardIcon name="close" /></button>
      </header>
      <div ref="scrollBody" class="dialog-scroll">
        <div v-if="loading" class="load-state" role="status"><span class="loading-line" /><span class="loading-line short" /><span class="loading-box" /></div>
        <div v-else-if="error" class="load-state"><p role="alert">{{ error }}</p><button @click="load">다시 불러오기</button></div>
        <BandPostEditor v-else-if="mode !== 'detail'" :key="`${mode}-${postId}`" :band="band" :post="post" @state="editorState = $event" @saved="saved" />
        <BandPostDetail v-else-if="post" :key="post.id" :band="band" :post="post" @edit="$emit('edit', post)" @deleted="deleted" @changed="$emit('changed')" @busy="detailBusy = $event" />
      </div>
    </dialog>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import { getBandPost } from '../../api/bandBoard'
import BoardIcon from './BoardIcon.vue'
import BandPostEditor from './BandPostEditor.vue'
import BandPostDetail from './BandPostDetail.vue'

const props = defineProps({ band: { type: Object, required: true }, mode: String, postId: { type: Number, default: null } })
const emit = defineEmits(['close', 'saved', 'edit', 'changed'])
const dialog = ref(null)
const scrollBody = ref(null)
const post = ref(null)
const loading = ref(false)
const error = ref('')
const editorState = ref({ busy: false, dirty: false, valid: false })
const detailBusy = ref(false)
const busy = computed(() => editorState.value.busy || detailBusy.value)
let sequence = 0
let allowNavigation = false
const previousOverflow = document.body.style.overflow
onMounted(() => { dialog.value.showModal(); document.body.style.overflow = 'hidden' })
onBeforeUnmount(() => { sequence++; document.body.style.overflow = previousOverflow; dialog.value?.close() })

function mayLeave() {
  if (allowNavigation) return true
  if (busy.value) return false
  return !editorState.value.dirty || window.confirm('작성 중인 내용이 있어요. 저장하지 않고 닫을까요?')
}
function close() {
  if (!mayLeave()) return
  allowNavigation = true
  emit('close')
}
function onBackdrop(event) {
  if (event.target !== dialog.value) return
  const rect = dialog.value.getBoundingClientRect()
  if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) close()
}
async function load() {
  const current = ++sequence
  allowNavigation = false
  editorState.value = { busy: false, dirty: false, valid: false }
  error.value = ''
  post.value = null
  if (props.mode === 'new') { loading.value = false; return }
  loading.value = true
  try {
    const result = await getBandPost(props.band.id, props.postId)
    if (current !== sequence) return
    if (props.mode === 'edit' && !result.mine && props.band.myPosition !== 'LEADER') throw new Error('수정 권한이 없는 글이에요.')
    post.value = result
  } catch (e) { if (current === sequence) error.value = e.response?.data?.message || e.message || '글을 불러오지 못했어요.' }
  finally { if (current === sequence) loading.value = false }
  await nextTick()
  if (current === sequence && scrollBody.value) scrollBody.value.scrollTop = 0
}
function saved(result) { allowNavigation = true; emit('saved', result) }
function deleted() { allowNavigation = true; emit('changed'); emit('close') }
onBeforeRouteLeave(mayLeave)
onBeforeRouteUpdate(mayLeave)
watch(() => [props.mode, props.postId], load, { immediate: true })
</script>

<style scoped>
* { box-sizing: border-box; }
.post-dialog { width: min(740px, calc(100vw - 48px)); max-width: none; max-height: min(880px, calc(100dvh - 100px)); padding: 0; border: 1px solid #e8ebf0; border-radius: 22px; color: #212529; background: white; font-family: 'Pretendard', sans-serif; box-shadow: 0 28px 100px #15233d30; overflow: hidden; }
.post-dialog[open] { display: flex; flex-direction: column; }
.post-dialog::backdrop { background: #17243b55; backdrop-filter: blur(2px); }
.dialog-toolbar { display: flex; align-items: center; gap: 14px; flex: none; padding: 18px 24px; border-bottom: 1px solid #f0f2f5; background: #fff; }
.dialog-context { flex: 1; min-width: 0; }
.dialog-context > span { display: block; color: #868e96; font-size: 10px; font-weight: 700; margin-bottom: 5px; }
.dialog-context h2 { font-size: 14px; font-weight: 800; margin: 0; }
.close-button { display: grid; place-items: center; width: 32px; height: 32px; border: none; border-radius: 9px; background: #f6f7f9; color: #868e96; cursor: pointer; }
.close-button:hover { background: #edf0f4; color: #212529; }
.publish { display: flex; align-items: center; gap: 8px; background: #2d5bff; color: #fff; border: 0; border-radius: 10px; padding: 10px 14px; font: inherit; font-size: 12px; font-weight: 800; cursor: pointer; }
.publish:hover { background: #214ce8; }
.publish:disabled { background: #edf0f6; color: #adb5bd; cursor: default; }
.dialog-scroll { overflow-y: auto; overscroll-behavior: contain; min-height: 0; }
.load-state { padding: 32px; min-height: 280px; color: #868e96; font-size: 13px; }
.load-state button { border: 0; color: #2d5bff; background: #f0f4ff; border-radius: 8px; padding: 9px 14px; cursor: pointer; font: inherit; }
.loading-line, .loading-box { display: block; height: 14px; background: #f1f3f6; border-radius: 6px; margin-bottom: 16px; }
.loading-line { width: 50%; }
.loading-line.short { width: 30%; }
.loading-box { height: 150px; margin-top: 28px; }
@media(max-width: 600px) {
  .post-dialog { width: calc(100vw - 20px); max-height: calc(100dvh - 28px); border-radius: 18px; }
  .dialog-toolbar { padding: 15px 18px; gap: 10px; }
  .publish { padding: 9px 12px; }
}
</style>
