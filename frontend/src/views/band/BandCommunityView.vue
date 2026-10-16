<template>
  <div class="page">
    <AppHeader />
    <main>
      <nav class="toolbar">
        <RouterLink to="/home">← 합주실</RouterLink>
        <RouterLink :to="boardPath">게시판 목록</RouterLink>
      </nav>
      <header class="heading">
        <div><span class="eyebrow">COMMUNITY</span><h1>{{ band?.name || '밴드' }} 게시판</h1></div>
        <RouterLink v-if="!editing" class="button primary" :to="{ path: boardPath, query: { new: '1' } }">글쓰기</RouterLink>
      </header>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="loading" role="status">불러오는 중…</p>
      <button v-else-if="loadFailed" @click="load">다시 불러오기</button>

      <form v-else-if="editing" class="surface editor" @submit.prevent="save">
        <h2>{{ postId ? '글 수정' : '새 글' }}</h2>
        <label>제목<input v-model="draft.title" maxlength="100" required :disabled="busy" placeholder="제목을 입력해 주세요"></label>
        <label>내용<textarea v-model="draft.content" maxlength="10000" rows="12" required :disabled="busy" placeholder="밴드원에게 이야기해 주세요"></textarea></label>
        <label>첨부 파일 <small>사진·영상·오디오·PDF 한 개, 최대 10MB</small>
          <input type="file" :accept="BAND_ATTACHMENT_ACCEPT" :disabled="busy" @change="selectFile">
        </label>
        <p v-if="file">선택한 파일: {{ file.name }} <button type="button" :disabled="busy" @click="clearFile">선택 취소</button></p>
        <label v-if="post?.attachment && !file" class="checkbox"><input v-model="removeFile" type="checkbox" :disabled="busy">기존 첨부 삭제: {{ post.attachment.name }}</label>
        <div class="actions"><button class="primary" :disabled="busy">{{ busy ? '저장 중…' : '저장' }}</button><button type="button" :disabled="busy" @click="cancelEdit">취소</button></div>
      </form>

      <template v-else-if="post">
        <article class="surface detail">
          <span v-if="post.notice" class="tag">공지</span>
          <h2>{{ post.title }}</h2>
          <div class="author"><ProfileAvatar :src="post.author.profileImageUrl" :size="36" /><div><strong>{{ post.author.nickname }}</strong><time>{{ formatDate(post.createdAt) }}</time></div></div>
          <p class="body">{{ post.content }}</p>
          <BandPostAttachment :attachment="post.attachment" />
          <div class="actions">
            <button v-if="band.myPosition === 'LEADER'" :disabled="busy" @click="toggleNotice">{{ post.notice ? '공지 해제' : '공지로 지정' }}</button>
            <RouterLink v-if="post.mine" class="button" :to="{ path: boardPath, query: { post: post.id, edit: '1' } }">수정</RouterLink>
            <button v-if="post.mine" :disabled="busy" @click="removePost">글 삭제</button>
          </div>
        </article>

        <section class="surface comment-section">
          <h2>댓글 {{ comments.filter(c => !c.deleted).length }}</h2>
          <p v-if="commentsError" class="error" role="alert">{{ commentsError }} <button @click="refreshComments">다시 불러오기</button></p>
          <template v-for="root in roots" :key="root.id">
            <div v-for="comment in [root, ...replies(root.id)]" :key="comment.id" class="comment" :class="{ reply: comment.parentId }">
              <template v-if="comment.deleted"><p class="muted">삭제된 댓글</p></template>
              <template v-else>
                <div class="author"><ProfileAvatar :src="comment.author?.profileImageUrl" :size="28" /><strong>{{ comment.author?.nickname }}</strong><time>{{ formatDate(comment.createdAt) }}</time></div>
                <form v-if="commentEditId === comment.id" @submit.prevent="saveComment(comment)">
                  <textarea v-model="commentEditText" maxlength="500" required rows="3" :disabled="busy" aria-label="댓글 수정"></textarea>
                  <button :disabled="busy">수정 저장</button><button type="button" :disabled="busy" @click="commentEditId = null">취소</button>
                </form>
                <p v-else class="comment-content">{{ comment.content }}</p>
              </template>
              <div class="actions small">
                <button v-if="!comment.parentId" :disabled="busy" @click="replyTo = root">답글</button>
                <template v-if="comment.mine && !comment.deleted">
                  <button :disabled="busy" @click="editComment(comment)">수정</button>
                  <button :disabled="busy" @click="removeComment(comment)">삭제</button>
                </template>
              </div>
            </div>
          </template>
          <form class="comment-form" @submit.prevent="submitComment">
            <p v-if="replyTo">{{ replyTo.author?.nickname || '삭제된 댓글' }}에 답글 <button type="button" :disabled="busy" @click="replyTo = null">취소</button></p>
            <label>{{ replyTo ? '답글' : '댓글' }}<textarea v-model="commentText" maxlength="500" required rows="3" :disabled="busy" placeholder="댓글을 남겨 주세요"></textarea></label>
            <button class="primary" :disabled="busy || !commentText.trim()">{{ busy ? '처리 중…' : '등록' }}</button>
          </form>
        </section>
      </template>

      <template v-else>
        <p v-if="!page.content.length" class="surface muted">아직 게시글이 없어요. 첫 글을 남겨 보세요.</p>
        <RouterLink v-for="row in page.content" :key="row.id" class="surface list-row" :to="{ path: boardPath, query: { post: row.id } }">
          <div class="summary"><span v-if="row.notice" class="tag">공지</span><h2>{{ row.title }}</h2><p>{{ row.content }}</p><span class="muted">{{ row.author.nickname }} · {{ formatDate(row.createdAt) }} · 댓글 {{ row.commentCount }}<template v-if="row.attachment"> · 📎 {{ row.attachment.name }}</template></span></div>
          <BandPostAttachment v-if="row.attachment && /^(image|video)\//.test(row.attachment.type)" :attachment="row.attachment" compact />
        </RouterLink>
        <nav v-if="page.totalPages > 1" class="pagination" aria-label="게시판 페이지">
          <button :disabled="page.number === 0" @click="goPage(page.number - 1)">이전</button>
          <span>{{ page.number + 1 }} / {{ page.totalPages }}</span>
          <button :disabled="page.number + 1 >= page.totalPages" @click="goPage(page.number + 1)">다음</button>
        </nav>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import AppHeader from '../../components/AppHeader.vue'
import ProfileAvatar from '../../components/ProfileAvatar.vue'
import BandPostAttachment from '../../components/band/BandPostAttachment.vue'
import { getBand } from '../../api/band'
import { listBandPosts, getBandPost, createBandPost, updateBandPost, deleteBandPost, setBandNotice,
  listBandComments, addBandComment, updateBandComment, deleteBandComment, uploadBandAttachment,
  removeBandAttachment, BAND_ATTACHMENT_ACCEPT, BAND_ATTACHMENT_MAX_BYTES } from '../../api/bandBoard'

const route = useRoute()
const router = useRouter()
const bandId = computed(() => Number(route.params.id))
const boardPath = computed(() => `/bands/${bandId.value}/community`)
const postId = computed(() => Number(route.query.post) || null)
const editing = computed(() => route.query.new === '1' || route.query.edit === '1')
const band = ref(null)
const page = ref({ content: [], number: 0, totalPages: 0 })
const post = ref(null)
const comments = ref([])
const roots = computed(() => comments.value.filter(c => c.parentId === null))
const replies = (id) => comments.value.filter(c => c.parentId === id)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const loadFailed = ref(false)
const commentsError = ref('')
const draft = reactive({ title: '', content: '' })
const file = ref(null)
const fileInput = ref(null)
const removeFile = ref(false)
const savedId = ref(null)
const commentText = ref('')
const replyTo = ref(null)
const commentEditId = ref(null)
const commentEditText = ref('')
const message = (e) => e.response?.data?.message || '요청을 처리하지 못했어요. 다시 시도해 주세요.'
const formatDate = (date) => date ? new Date(date).toLocaleString('ko-KR') : ''
let sequence = 0

async function load() {
  const current = ++sequence
  const id = bandId.value
  const selected = postId.value
  const isEditing = editing.value
  loading.value = true
  error.value = ''
  loadFailed.value = false
  commentsError.value = ''
  post.value = null
  comments.value = []
  commentText.value = ''
  replyTo.value = null
  commentEditId.value = null
  file.value = null
  fileInput.value = null
  removeFile.value = false
  savedId.value = null
  draft.title = ''
  draft.content = ''
  try {
    const nextBand = await getBand(id)
    if (current !== sequence) return
    band.value = nextBand
    if (selected) {
      const nextPost = await getBandPost(id, selected)
      if (current !== sequence) return
      if (isEditing && !nextPost.mine) throw new Error('다른 멤버의 글은 수정할 수 없어요.')
      post.value = nextPost
      draft.title = nextPost.title
      draft.content = nextPost.content
      if (!isEditing) await refreshComments(current)
    } else if (!isEditing) {
      const number = Math.max(0, Number(route.query.page) || 0)
      const nextPage = await listBandPosts(id, number)
      if (current !== sequence) return
      if (number > 0 && !nextPage.content.length) {
        await router.replace({ path: boardPath.value, query: { page: Math.max(0, nextPage.totalPages - 1) } })
        return
      }
      page.value = nextPage
    }
  } catch (e) {
    if (current === sequence) {
      error.value = e.response ? message(e) : e.message || message(e)
      loadFailed.value = true
    }
  } finally {
    if (current === sequence) loading.value = false
  }
}

function selectFile(event) {
  fileInput.value = event.target
  const selected = event.target.files?.[0]
  error.value = ''
  if (selected && (selected.size > BAND_ATTACHMENT_MAX_BYTES || !BAND_ATTACHMENT_ACCEPT.split(',').includes(selected.type))) {
    error.value = '사진·영상·오디오·PDF 파일을 10MB까지 첨부할 수 있어요.'
    event.target.value = ''
    file.value = null
    return
  }
  file.value = selected || null
  if (selected) removeFile.value = false
}
function clearFile() {
  file.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function save() {
  if (busy.value || !draft.title.trim() || !draft.content.trim()) return
  busy.value = true
  error.value = ''
  let contentSaved = false
  try {
    const payload = { title: draft.title.trim(), content: draft.content.trim() }
    const id = savedId.value || postId.value
    const result = id ? await updateBandPost(bandId.value, id, payload) : await createBandPost(bandId.value, payload)
    savedId.value = result.id
    contentSaved = true
    if (file.value) await uploadBandAttachment(bandId.value, result.id, file.value)
    else if (removeFile.value && result.attachment) await removeBandAttachment(bandId.value, result.id)
    busy.value = false
    await router.replace({ path: boardPath.value, query: { post: result.id } })
  } catch (e) {
    error.value = (contentSaved ? '글은 저장됐지만 첨부 처리가 실패했어요. 다시 저장하면 같은 글에 재시도합니다. ' : '') + message(e)
  } finally { busy.value = false }
}

function cancelEdit() {
  const id = savedId.value || postId.value
  router.push(id ? { path: boardPath.value, query: { post: id } } : boardPath.value)
}
const goPage = (number) => router.push({ path: boardPath.value, query: { page: number } })

async function action(work) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try { await work() }
  catch (e) { error.value = message(e) }
  finally { busy.value = false }
}
const toggleNotice = () => action(async () => { post.value = await setBandNotice(bandId.value, postId.value, !post.value.notice) })
function removePost() {
  if (!window.confirm('이 글을 삭제할까요?')) return
  action(async () => {
    await deleteBandPost(bandId.value, postId.value)
    busy.value = false
    await router.replace(boardPath.value)
  })
}

async function refreshComments(current = sequence) {
  commentsError.value = ''
  try {
    const result = await listBandComments(bandId.value, postId.value)
    if (current === sequence) comments.value = result
  } catch (e) {
    if (current === sequence) commentsError.value = message(e)
  }
}
function submitComment() {
  if (!commentText.value.trim()) return
  action(async () => {
    await addBandComment(bandId.value, postId.value, { content: commentText.value.trim(), parentId: replyTo.value?.id || null })
    commentText.value = ''
    replyTo.value = null
    await refreshComments()
  })
}
function editComment(comment) {
  commentEditId.value = comment.id
  commentEditText.value = comment.content
}
function saveComment(comment) {
  if (!commentEditText.value.trim()) return
  action(async () => {
    await updateBandComment(bandId.value, postId.value, comment.id, commentEditText.value.trim())
    commentEditId.value = null
    await refreshComments()
  })
}
function removeComment(comment) {
  if (!window.confirm('이 댓글을 삭제할까요?')) return
  action(async () => {
    await deleteBandComment(bandId.value, postId.value, comment.id)
    if (replyTo.value?.id === comment.id) replyTo.value = null
    await refreshComments()
  })
}

// 저장 요청 도중 화면을 떠나 중복 저장하거나 잘못된 글을 조작하지 않도록 한다
onBeforeRouteLeave(() => !busy.value)
onBeforeRouteUpdate(() => !busy.value)
watch(() => route.fullPath, load, { immediate: true })
</script>

<style scoped>
.page { min-height: 100vh; background: #f8f9fb; color: #343a40; }
main { max-width: 960px; margin: auto; padding: 28px 24px 64px; }
.toolbar, .heading, .actions, .pagination { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.toolbar { margin-bottom: 28px; justify-content: space-between; }
.toolbar a { color: #868e96; font-size: .85rem; }
.heading { justify-content: space-between; margin-bottom: 24px; }
.eyebrow { font-weight: 800; font-size: .75rem; color: #868e96; }
h1 { margin: 6px 0 0; font-size: 1.5rem; }
h2 { font-size: 1.1rem; overflow-wrap: anywhere; }
button, .button { border: 1px solid #dee2e6; background: #fff; color: #495057; padding: 8px 13px; border-radius: 9px; font: inherit; font-size: .8rem; cursor: pointer; text-decoration: none; }
.primary { background: #2d5bff; color: #fff; border-color: #2d5bff; }
button:disabled { opacity: .5; cursor: not-allowed; }
.surface { background: #fff; border: 1px solid #e9ecef; border-radius: 18px; padding: 24px; margin-bottom: 14px; }
.list-row { display: flex; gap: 20px; align-items: center; text-decoration: none; color: inherit; }
.list-row:hover { border-color: #2d5bff; }
.summary { flex: 1; min-width: 0; }
.summary h2 { margin: 6px 0; }
.summary p { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; font-size: .85rem; color: #868e96; }
.tag { display: inline-block; background: #2d5bff; color: #fff; font-size: .7rem; font-weight: 800; border-radius: 5px; padding: 3px 7px; }
.muted, time, small { color: #868e96; font-size: .75rem; overflow-wrap: anywhere; }
.author { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: .85rem; }
.author time { display: block; margin-top: 3px; }
.body, .comment-content { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; }
.body { margin: 28px 0; }
.detail .actions { margin-top: 24px; }
.editor { display: flex; flex-direction: column; gap: 18px; }
label { display: flex; flex-direction: column; gap: 8px; font-size: .85rem; font-weight: 700; }
input, textarea { box-sizing: border-box; width: 100%; border: 1px solid #dee2e6; border-radius: 9px; padding: 12px; font: inherit; color: inherit; background: #fff; }
textarea { resize: vertical; }
.checkbox { flex-direction: row; align-items: center; overflow-wrap: anywhere; }
.checkbox input { width: auto; }
.error { color: #c92a2a; background: #fff5f5; border-radius: 10px; padding: 12px; font-size: .85rem; }
.comment { padding: 18px 0; border-bottom: 1px solid #f1f3f5; }
.reply { margin-left: 28px; }
.small { gap: 6px; }
.small button { padding: 4px 8px; font-size: .7rem; }
.comment-form { display: grid; gap: 12px; margin-top: 24px; }
.comment-form > button { justify-self: end; }
.pagination { justify-content: center; margin-top: 24px; font-size: .85rem; }
@media (max-width: 640px) {
  main { padding: 20px 14px 40px; }
  .surface { padding: 18px; }
  .list-row { gap: 12px; }
  .reply { margin-left: 16px; }
}
</style>
