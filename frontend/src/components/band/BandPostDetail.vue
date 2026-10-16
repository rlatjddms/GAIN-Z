<template>
  <article ref="article" class="post-detail" @click="dismissMenus" @keydown.esc="escapeMenu">
    <div class="article-body">
      <div class="author-row">
        <ProfileAvatar :src="post.author?.profileImageUrl" :size="42" :alt="`${post.author?.nickname} 프로필 사진`" />
        <div class="author-info"><strong>{{ post.author?.nickname || '멤버' }}</strong><time :datetime="post.createdAt">{{ postDate(post.createdAt) }}</time></div>
        <details v-if="post.mine || band.myPosition === 'LEADER'" class="more-menu">
          <summary aria-label="게시글 더보기"><BoardIcon name="more" /></summary>
          <div class="menu-items">
            <button v-if="post.mine" :disabled="busy" @click="$emit('edit')"><BoardIcon name="edit" :size="16" />글 수정</button>
            <button v-if="!post.mine && band.myPosition === 'LEADER'" @click="$emit('edit')"><BoardIcon name="pin" :size="16" />공지 설정</button>
            <button v-if="post.mine" class="danger" :disabled="busy" @click="removePost"><BoardIcon name="trash" :size="16" />글 삭제</button>
          </div>
        </details>
      </div>
      <span v-if="activeNotice" class="notice-tag"><BoardIcon name="pin" :size="13" />공지<span v-if="post.noticeExpiresAt"> · {{ postDate(post.noticeExpiresAt) }}까지</span></span>
      <h1 class="post-title">{{ post.title }}</h1>
      <p class="post-content"><MentionText :text="post.content" :members="members" /></p>
      <div v-if="visuals.length" class="media-gallery" :class="{ single: visuals.length === 1 }">
        <div v-for="(item, index) in visuals" :key="item.id" class="gallery-item"><BandPostAttachment :attachment="item" hide-download expandable @expand="viewerIndex = index" /><button v-if="item.type.startsWith('video/')" class="media-open" @click="viewerIndex = index">크게 보기</button></div>
      </div>
      <div v-if="documents.length" class="document-list"><BandPostAttachment v-for="item in documents" :key="item.id" :attachment="item" /></div>
      <BandMediaViewer v-if="viewerIndex !== null" :items="visuals" :initial-index="viewerIndex" :band-id="band.id" :post-id="post.id" @close="viewerIndex = null" />
      <p v-if="error" class="error" role="alert">{{ error }}</p>
    </div>

    <section class="comments-section" aria-label="댓글">
      <h2><BoardIcon name="comment" :size="18" />댓글 <b>{{ comments.filter(c => !c.deleted).length }}</b></h2>
      <p v-if="loading" class="quiet" role="status">댓글을 불러오는 중…</p>
      <p v-else-if="commentsError" class="error" role="alert">{{ commentsError }} <button @click="loadComments">다시 불러오기</button></p>
      <p v-else-if="!comments.length" class="empty-comments">가장 먼저 이야기를 나눠 보세요.</p>
      <template v-for="comment in orderedComments" :key="comment.id"><div class="comment" :class="{ reply: comment.parentId, deleted: comment.deleted }">
        <template v-if="comment.deleted"><p class="quiet">삭제된 댓글이에요.</p></template>
        <template v-else>
          <ProfileAvatar :src="comment.author?.profileImageUrl" :size="comment.parentId ? 24 : 30" />
          <div class="comment-main">
            <div class="comment-meta"><strong>{{ comment.author?.nickname }}</strong><time :datetime="comment.createdAt">{{ postTimeAgo(comment.createdAt) }}</time>
              <details v-if="comment.mine" class="more-menu comment-menu"><summary :aria-label="`${comment.author?.nickname} 댓글 더보기`"><BoardIcon name="more" :size="16" /></summary><div class="menu-items"><button :disabled="busy" @click="editComment(comment)">수정</button><button class="danger" :disabled="busy" @click="removeComment(comment)">삭제</button></div></details>
            </div>
            <form v-if="editId === comment.id" class="edit-comment" @submit.prevent="saveComment(comment)"><MentionInput :members="members" v-model="editText" maxlength="500" required :disabled="busy" aria-label="댓글 수정" rows="2" /><div><button type="button" :disabled="busy" @click="editId = null">취소</button><button class="text-primary" :disabled="busy || !editText.trim()">저장</button></div></form>
            <p v-else class="comment-content"><MentionText :text="comment.content" :members="members" /></p>
            <div class="comment-actions"><button class="reply-button" :disabled="busy" @click="startReply(comment)">답글 달기</button><button v-if="!comment.parentId && replies(comment.id).length" class="reply-toggle" :aria-expanded="expanded.has(comment.id)" @click="toggleReplies(comment.id)">{{ expanded.has(comment.id) ? '답글 접기' : `답글 ${replies(comment.id).length}개 보기` }}</button></div>
          </div>
        </template>
        <div v-if="comment.deleted && !comment.parentId" class="comment-actions"><button class="reply-button" :disabled="busy" @click="startReply(comment)">답글 달기</button><button v-if="!comment.parentId && replies(comment.id).length" class="reply-toggle" :aria-expanded="expanded.has(comment.id)" @click="toggleReplies(comment.id)">{{ expanded.has(comment.id) ? '답글 접기' : `답글 ${replies(comment.id).length}개 보기` }}</button></div>
      </div></template>
      <form class="comment-composer" @submit.prevent="submitComment">
        <ProfileAvatar :src="auth.profile?.profileImageUrl" :size="32" />
        <div class="composer-box">
          <div v-if="replyTo" class="reply-target"><span>@{{ replyTo.author?.nickname || '삭제된 댓글' }}에게 답글</span><button type="button" aria-label="답글 취소" :disabled="busy" @click="replyTo = null"><BoardIcon name="close" :size="13" /></button></div>
          <MentionInput :members="members" ref="commentInput" v-model="commentText" :aria-label="replyTo ? '답글' : '댓글'" :maxlength="500 - (replyTo?.author?.nickname ? replyTo.author.nickname.length + 2 : 0)" rows="2" :disabled="busy" placeholder="댓글을 남겨 주세요. @로 멤버를 태그할 수 있어요." />
          <div class="composer-footer"><span>{{ submittedText.length }} / 500</span><button :disabled="busy || !commentText.trim() || submittedText.length > 500">{{ busy ? '등록 중' : '등록' }}<BoardIcon name="arrow" :size="14" /></button></div>
        </div>
      </form>
    </section>
  </article>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useNoticeClock } from './useNoticeClock'
import MentionInput from './MentionInput.vue'
import MentionText from './MentionText.vue'
import BandMediaViewer from './BandMediaViewer.vue'
import ProfileAvatar from '../ProfileAvatar.vue'
import BoardIcon from './BoardIcon.vue'
import BandPostAttachment from './BandPostAttachment.vue'
import { attachmentsOf, postDate, postTimeAgo, mentionMembers, isActiveNotice } from '../../constants/bandBoard'
import { deleteBandPost, listBandComments, addBandComment, updateBandComment, deleteBandComment } from '../../api/bandBoard'

const props = defineProps({ post: { type: Object, required: true }, band: { type: Object, required: true } })
const emit = defineEmits(['edit', 'deleted', 'changed', 'busy'])
const auth = useAuthStore()
const noticeNow = useNoticeClock(() => [props.post])
const activeNotice = computed(() => isActiveNotice(props.post, noticeNow.value))
const article = ref(null)
const members = computed(() => mentionMembers(props.band))
const viewerIndex = ref(null)
const expanded = ref(new Set())
const replies = id => comments.value.filter(c => c.parentId === id)
function toggleReplies(id) { const next = new Set(expanded.value); next.has(id) ? next.delete(id) : next.add(id); expanded.value = next }
function dismissMenus(event) {
  const selected = event.target.closest('details')
  article.value?.querySelectorAll('details[open]').forEach(menu => {
    if (menu !== selected || event.target.closest('button')) menu.open = false
  })
}
function escapeMenu(event) {
  const opened = article.value?.querySelectorAll('details[open]')
  if (!opened?.length) return
  event.preventDefault()
  event.stopPropagation()
  opened.forEach(menu => { menu.open = false; menu.querySelector('summary')?.focus() })
}
const visuals = computed(() => attachmentsOf(props.post).filter(f => /^(image|video)\//.test(f.type)))
const documents = computed(() => attachmentsOf(props.post).filter(f => !/^(image|video)\//.test(f.type)))
const comments = ref([])
const orderedComments = computed(() => comments.value.filter(c => c.parentId === null).flatMap(root => [root, ...(expanded.value.has(root.id) ? replies(root.id) : [])]))
const loading = ref(false)
const commentsError = ref('')
const busy = ref(false)
const error = ref('')
const commentText = ref('')
const commentInput = ref(null)
const replyTo = ref(null)
const submittedText = computed(() => (replyTo.value?.author?.nickname ? '@' + replyTo.value.author.nickname + ' ' : '') + commentText.value.trim())
const editId = ref(null)
const editText = ref('')
let active = true
const message = e => e.response?.data?.message || '처리하지 못했어요. 다시 시도해 주세요.'
onBeforeUnmount(() => { active = false })
async function loadComments() {
  loading.value = true
  commentsError.value = ''
  try {
    const rows = await listBandComments(props.band.id, props.post.id)
    if (active) comments.value = rows
  } catch (e) { if (active) commentsError.value = message(e) }
  finally { if (active) loading.value = false }
}
onMounted(loadComments)
async function action(work) {
  if (busy.value) return
  busy.value = true
  emit('busy', true)
  error.value = ''
  try { await work() }
  catch (e) { error.value = message(e) }
  finally { busy.value = false; emit('busy', false) }
}
function removePost() {
  if (!window.confirm('게시글을 삭제할까요?')) return
  action(async () => { await deleteBandPost(props.band.id, props.post.id); emit('deleted') })
}
async function startReply(comment) { replyTo.value = comment; await nextTick(); commentInput.value?.focus() }
function submitComment() {
  if (!commentText.value.trim() || submittedText.value.length > 500) return
  action(async () => {
    await addBandComment(props.band.id, props.post.id, { content: submittedText.value, parentId: replyTo.value ? (replyTo.value.parentId || replyTo.value.id) : null })
    if (replyTo.value) expanded.value = new Set([...expanded.value, replyTo.value.parentId || replyTo.value.id])
    commentText.value = ''
    replyTo.value = null
    await loadComments()
    emit('changed')
  })
}
function editComment(comment) { editId.value = comment.id; editText.value = comment.content }
function saveComment(comment) {
  if (!editText.value.trim()) return
  action(async () => { await updateBandComment(props.band.id, props.post.id, comment.id, editText.value.trim()); editId.value = null; await loadComments() })
}
function removeComment(comment) {
  if (!window.confirm('댓글을 삭제할까요?')) return
  action(async () => {
    await deleteBandComment(props.band.id, props.post.id, comment.id)
    if (replyTo.value?.id === comment.id) replyTo.value = null
    await loadComments()
    emit('changed')
  })
}
</script>

<style scoped>
* { box-sizing: border-box; }
.article-body { padding: 28px 32px 30px; }
.author-row { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.author-info { flex: 1; min-width: 0; }
.author-info strong { display: block; font-size: 14px; font-weight: 800; }
.author-info time { display: block; font-size: 11px; color: #868e96; margin-top: 6px; }
.post-title { margin: 0 0 20px; font-size: 25px; font-weight: 800; line-height: 1.5; letter-spacing: -.6px; overflow-wrap: anywhere; }
.post-content { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 14px; line-height: 1.9; color: #495057; margin: 0 0 24px; }
.notice-tag { display: inline-flex; align-items: center; gap: 4px; color: #2d5bff; background: #eff3ff; border-radius: 6px; padding: 4px 7px; font-size: 10px; font-weight: 800; margin-bottom: 10px; }
.more-menu { position: relative; margin-left: auto; flex: none; }
.more-menu summary { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 9px; color: #526071; cursor: pointer; list-style: none; }
.more-menu summary::-webkit-details-marker { display: none; }
.more-menu summary:hover, .more-menu[open] summary { background: #f1f3f5; color: #343a40; }
.menu-items { position: absolute; top: 36px; right: 0; z-index: 10; min-width: 132px; padding: 5px; border: 1px solid #e9ecef; border-radius: 12px; background: #fff; box-shadow: 0 6px 24px #1c2c4a14; }
.menu-items button { width: 100%; display: flex; align-items: center; gap: 9px; padding: 10px; background: none; border: none; border-radius: 7px; font: inherit; font-size: 12px; text-align: left; color: #495057; cursor: pointer; white-space: nowrap; }
.menu-items button:hover { background: #f6f7f9; }
.menu-items .danger { color: #e05252; }
.media-gallery { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.media-gallery.single { grid-template-columns: minmax(0, 1fr); }
.document-list { margin-top: 14px; display: grid; gap: 10px; }
.comments-section { padding: 24px 32px 30px; background: #fafbfc; border-top: 1px solid #edf0f3; }
.comments-section h2 { display: flex; align-items: center; gap: 7px; margin: 0 0 20px; font-size: 13px; font-weight: 800; }
.comments-section h2 b { color: #2d5bff; margin-left: 2px; }
.comment { display: flex; gap: 10px; padding: 8px 0; }
.comment.reply { margin-left: 40px; padding: 6px 0; }
.comment-main { flex: 1; min-width: 0; }
.comment-meta { display: flex; align-items: center; gap: 9px; min-height: 22px; }
.comment-meta strong { font-size: 12px; font-weight: 800; }
.comment-meta time { font-size: 10px; color: #adb5bd; }
.comment-menu summary { width: 24px; height: 24px; }
.comment-menu .menu-items { top: 26px; min-width: 90px; }
.comment-content { margin: 4px 0 7px; font-size: 13px; line-height: 1.65; color: #495057; white-space: pre-wrap; overflow-wrap: anywhere; }
.reply-button { padding: 0; background: none; border: none; font-family: inherit; font-size: 12px; font-weight: 500; color: #858b94; cursor: pointer; }
.reply-button:hover { color: #2d5bff; }
.deleted { display: block; padding-left: 40px; }
.quiet, .empty-comments { font-size: 12px; color: #adb5bd; }
.empty-comments { padding: 8px 0 20px; }
.comment-composer { display: flex; align-items: flex-start; gap: 10px; margin-top: 20px; }
.composer-box { flex: 1; min-width: 0; border: 1px solid #e3e7ee; background: #fff; border-radius: 13px; padding: 12px 14px; }
.composer-box:focus-within { border-color: #a7b9ff; box-shadow: 0 0 0 3px #2d5bff06; }
.composer-box :deep(textarea) { width: 100%; padding: 0; border: 0; outline: none; resize: vertical; min-height: 42px; font-family: inherit; font-size: 12px; color: #343a40; line-height: 1.7; background: transparent; }
.composer-box :deep(textarea)::placeholder { color: #adb5bd; }
.composer-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.composer-footer > span { color: #c0c7d1; font-size: 10px; }
.composer-footer button { display: flex; align-items: center; gap: 5px; background: #2d5bff; border: 0; border-radius: 7px; padding: 6px 10px; color: white; font-family: inherit; font-size: 11px; font-weight: 800; cursor: pointer; }
.composer-footer button:disabled { background: #edf0f5; color: #adb5bd; cursor: default; }
.reply-target { display: flex; align-items: center; gap: 8px; color: #2d5bff; font-size: 11px; margin-bottom: 10px; }
.reply-target button { display: grid; place-items: center; border: 0; background: #eef2ff; border-radius: 4px; color: #2d5bff; cursor: pointer; }
.edit-comment :deep(textarea) { width: 100%; border: 1px solid #dfe5ee; border-radius: 8px; padding: 9px; font: inherit; font-size: 12px; resize: vertical; }
.edit-comment > div { display: flex; justify-content: flex-end; gap: 10px; margin: 6px 0; }
.edit-comment button { border: 0; background: none; font: inherit; font-size: 11px; color: #868e96; cursor: pointer; }
.edit-comment .text-primary { color: #2d5bff; font-weight: 800; }
.error { color: #d94841; background: #fff5f5; border-radius: 10px; padding: 12px; font-size: 12px; line-height: 1.6; }
@media(max-width: 600px) {
  .article-body { padding: 22px 20px 26px; }
  .post-title { font-size: 22px; }
  .comments-section { padding: 22px 20px; }
  .comment.reply { margin-left: 24px; }
  .media-gallery { gap: 8px; }
}
.reply-toggle, .media-open { border: 1px solid #d5ddec; border-radius: 8px; padding: 7px 10px; font: inherit; font-size: 12px; font-weight: 700; background: #fff; color: #405372; cursor: pointer; }
.reply-toggle { display: block; margin: 0; border: 0; background: transparent; color: #2d5bff; padding: 5px 0; }
.comment-actions { display: flex; align-items: center; gap: 14px; margin-top: 3px; }
.reply-target > span { background: #edf2ff; padding: 5px 8px; border-radius: 7px; font-weight: 700; }
.gallery-item { position: relative; min-width: 0; }.media-open { position: absolute; right: 8px; top: 8px; background: #ffffffed; }
</style>
