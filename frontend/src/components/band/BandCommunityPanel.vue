<template>
  <section class="community-panel">
    <BandCommunityFeed :mine="mine" :notices="notices" :posts="posts" :total="total" :loading="loading && !rows.length" @open="open" @write="write" @mine="toggleMine" @edit="edit" @delete="remove" />
    <p v-if="error" class="error" role="alert">{{ error }} <button @click="load">다시 불러오기</button></p>
    <button v-if="hasMore" class="load-more" :disabled="loading" @click="loadMore">{{ loading ? '불러오는 중…' : '이야기 더 보기' }} <BoardIcon name="plus" :size="15" /><span>{{ rows.length }} / {{ regularTotal }}</span></button>
    <BandPostDialog v-if="dialogMode" :band="band" :mode="dialogMode" :post-id="selectedPost" @close="close" @edit="edit" @saved="saved" @changed="load" />
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isActiveNotice } from '../../constants/bandBoard'
import { useNoticeClock } from './useNoticeClock'
import { listBandPosts, deleteBandPost } from '../../api/bandBoard'
import BandCommunityFeed from './BandCommunityFeed.vue'
import BandPostDialog from './BandPostDialog.vue'
import BoardIcon from './BoardIcon.vue'
const props = defineProps({ band: { type: Object, required: true } })
const route = useRoute()
const router = useRouter()
const mine = computed(() => route.query.mine === '1')
const rows = ref([])
const loading = ref(false)
const error = ref('')
const regularTotal = ref(0)
const noticeRows = ref([])
const now = useNoticeClock(() => noticeRows.value, () => load())
const total = computed(() => regularTotal.value + notices.value.length)
const page = ref(0)
const totalPages = ref(0)
const notices = computed(() => noticeRows.value.filter(p => isActiveNotice(p, now.value)))
const posts = computed(() => rows.value.filter(p => !notices.value.some(n => n.id === p.id)))
const hasMore = computed(() => page.value + 1 < totalPages.value)
const selectedPost = computed(() => Number(route.query.post) || null)
const dialogMode = computed(() => route.query.compose === '1' ? 'new' : selectedPost.value ? (route.query.edit === '1' ? 'edit' : 'detail') : null)
let sequence = 0
async function fetchNotices() {
  const first = await listBandPosts(props.band.id, 0, 20, true, mine.value)
  const rest = await Promise.all(Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, i) => listBandPosts(props.band.id, i + 1, 20, true, mine.value)))
  return [first, ...rest].flatMap(result => result.content)
}
async function load() {
  const current = ++sequence
  loading.value = true
  error.value = ''
  try {
    const [pinned, results] = await Promise.all([fetchNotices(), Promise.all(Array.from({ length: page.value + 1 }, (_, index) => listBandPosts(props.band.id, index, 7, false, mine.value)))])
    if (current !== sequence) return
    noticeRows.value = pinned
    rows.value = results.flatMap(result => result.content)
    regularTotal.value = results[0].totalElements
    totalPages.value = results[0].totalPages
    page.value = Math.max(0, Math.min(page.value, totalPages.value - 1))
  } catch (e) { if (current === sequence) error.value = e.response?.data?.message || '게시글을 불러오지 못했어요.' }
  finally { if (current === sequence) loading.value = false }
}
async function loadMore() {
  if (loading.value || !hasMore.value) return
  const current = ++sequence
  loading.value = true
  error.value = ''
  try {
    const result = await listBandPosts(props.band.id, page.value + 1, 7, false, mine.value)
    if (current !== sequence) return
    rows.value = [...rows.value, ...result.content.filter(next => !rows.value.some(row => row.id === next.id))]
    page.value = result.number
    regularTotal.value = result.totalElements
    totalPages.value = result.totalPages
  } catch (e) { if (current === sequence) error.value = e.response?.data?.message || '게시글을 불러오지 못했어요.' }
  finally { if (current === sequence) loading.value = false }
}
const query = extra => ({ path: '/home', query: { band: props.band.id, ...(mine.value ? { mine: '1' } : {}), ...extra } })
const write = () => router.push(query({ compose: '1' }))
const open = post => router.push(query({ post: post.id }))
const edit = post => router.push(query({ post: post.id, edit: '1' }))
const close = () => router.push(query({}))
async function saved(post) { await router.replace(query({ post: post.id })); await load() }
async function toggleMine() { await router.push(query({ mine: mine.value ? undefined : '1' })) }
async function remove(post) {
  if (!window.confirm('게시글을 삭제할까요?')) return
  try { await deleteBandPost(props.band.id, post.id); await load() }
  catch (e) { error.value = e.response?.data?.message || '글을 삭제하지 못했어요. 다시 시도해 주세요.' }
}
watch(() => [props.band.id, mine.value], () => { page.value = 0; rows.value = []; noticeRows.value = []; regularTotal.value = 0; load() }, { immediate: true })
</script>

<style scoped>
.community-panel { scroll-margin-top: 84px; }
.load-more { display: flex; justify-content: center; align-items: center; gap: 8px; width: 100%; margin-top: 14px; padding: 13px; border: 1px solid #e9ecef; border-radius: 12px; background: #fff; color: #687385; font-family: inherit; font-size: 12px; font-weight: 700; cursor: pointer; }
.load-more:hover { color: var(--c-primary-text); border-color: #cdd7ff; }
.load-more span { color: #adb5bd; font-size: 10px; font-weight: 500; margin-left: 4px; }
.error { color: #d94841; background: #fff5f5; padding: 12px; font-size: 12px; border-radius: 10px; }
.error button { border: 0; background: none; color: var(--c-primary-text); font: inherit; cursor: pointer; }
</style>
