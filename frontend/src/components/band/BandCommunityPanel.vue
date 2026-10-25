<template>
  <section ref="panel" class="community-panel">
    <BandCommunityFeed
      :mine="mine" :notices="notices" :posts="posts" :total="total" :loading="loading && !rows.length"
      :searching="searching" :search-open="searchOpen"
      @open="open" @write="write" @mine="toggleMine" @edit="edit" @delete="remove" @search-toggle="searchOpen = !searchOpen"
    >
      <template #search>
        <BandBoardSearch v-if="searchOpen || searching" :open="searchOpen" :q="filters.q" :from="filters.from" :to="filters.to" @search="search" @close="searchOpen = false" />
      </template>
    </BandCommunityFeed>
    <p v-if="error" class="error" role="alert">{{ error }} <button @click="load">다시 불러오기</button></p>
    <PageNav :page="page" :total-pages="totalPages" @change="goPage" />
    <BandPostDialog v-if="dialogMode" :band="band" :mode="dialogMode" :post-id="selectedPost" @close="close" @edit="edit" @saved="saved" @changed="load" />
  </section>
</template>

<script>
// 마지막으로 불러온 목록을 기억한다 (#233). 셋리스트 등에 다녀와 홈이 새로 그려질 때 빈 카드(불러오는 중) 대신
// 기억한 글을 바로 보여 주고 뒤에서 새로 불러온다. 밴드·쪽·검색 조건마다 따로.
// <script setup> 안에 두면 화면이 그려질 때마다 새로 만들어져서 파일 수준에 둔다
const lastLoaded = new Map()
</script>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isActiveNotice } from '../../constants/bandBoard'
import { useNoticeClock } from './useNoticeClock'
import { listBandPosts, deleteBandPost } from '../../api/bandBoard'
import BandCommunityFeed from './BandCommunityFeed.vue'
import BandBoardSearch from './BandBoardSearch.vue'
import BandPostDialog from './BandPostDialog.vue'
import PageNav from '../ui/PageNav.vue'
import { useDialogStore } from '../../stores/dialog'
import { useLiveRefresh } from '../../composables/useLiveRefresh'

// 한 쪽 글 수: 3열(넓은 화면)·2열(휴대폰) 모두 줄이 딱 맞게
const PAGE_SIZE = 12
const DATE = /^\d{4}-\d{2}-\d{2}$/

const props = defineProps({ band: { type: Object, required: true } })
const route = useRoute()
const router = useRouter()
const panel = ref(null)
const mine = computed(() => route.query.mine === '1')
// 쪽·검색 조건은 주소에 둔다 (새로고침·뒤로 가기에도 그대로) (#188)
const page = computed(() => Math.max(0, (Number(route.query.page) || 1) - 1))
const filters = computed(() => {
  const from = DATE.test(route.query.from) ? route.query.from : null
  const to = DATE.test(route.query.to) ? route.query.to : from
  return { q: typeof route.query.q === 'string' ? route.query.q : '', from, to }
})
const searching = computed(() => !!(filters.value.q || filters.value.from))
const searchOpen = ref(false)

const rows = ref([])
const loading = ref(false)
const error = ref('')
const regularTotal = ref(0)
const totalPages = ref(0)
const noticeRows = ref([])
const now = useNoticeClock(() => noticeRows.value, () => load())
const total = computed(() => regularTotal.value + notices.value.length)
const notices = computed(() => noticeRows.value.filter(p => isActiveNotice(p, now.value)))
const posts = computed(() => rows.value.filter(p => !notices.value.some(n => n.id === p.id)))
const selectedPost = computed(() => Number(route.query.post) || null)
const dialogMode = computed(() => route.query.compose === '1' ? 'new' : selectedPost.value ? (route.query.edit === '1' ? 'edit' : 'detail') : null)
let sequence = 0
function showList(list) {
  noticeRows.value = list.noticeRows
  rows.value = list.rows
  regularTotal.value = list.regularTotal
  totalPages.value = list.totalPages
}
async function fetchNotices() {
  const first = await listBandPosts(props.band.id, { page: 0, size: 20, notice: true, mine: mine.value })
  const rest = await Promise.all(Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, i) => listBandPosts(props.band.id, { page: i + 1, size: 20, notice: true, mine: mine.value })))
  return [first, ...rest].flatMap(result => result.content)
}
const cacheKey = () => JSON.stringify([props.band.id, page.value, mine.value, filters.value])
async function load() {
  const current = ++sequence
  const key = cacheKey()
  const cached = lastLoaded.get(key)
  if (cached) showList(cached)
  loading.value = true
  error.value = ''
  try {
    // 검색 중에는 공지도 일반 글처럼 결과에 섞는다 (위에 따로 고정하지 않음)
    const [pinned, result] = await Promise.all([
      searching.value ? [] : fetchNotices(),
      listBandPosts(props.band.id, { page: page.value, size: PAGE_SIZE, notice: searching.value ? null : false, mine: mine.value, ...filters.value }),
    ])
    if (current !== sequence) return
    const list = { noticeRows: pinned, rows: result.content, regularTotal: result.totalElements, totalPages: result.totalPages }
    showList(list)
    lastLoaded.set(key, list)
    // 글이 지워져 쪽이 줄었으면 마지막 쪽으로
    if (result.totalPages > 0 && page.value >= result.totalPages) goPage(result.totalPages - 1, false)
  } catch (e) { if (current === sequence) error.value = e.response?.data?.message || '게시글을 불러오지 못했어요.' }
  finally { if (current === sequence) loading.value = false }
}
const listQuery = () => ({
  ...(mine.value ? { mine: '1' } : {}),
  ...(filters.value.q ? { q: filters.value.q } : {}),
  ...(filters.value.from ? { from: filters.value.from, to: filters.value.to } : {}),
  ...(page.value > 0 ? { page: String(page.value + 1) } : {}),
})
const query = extra => ({ path: '/home', query: { band: props.band.id, ...listQuery(), ...extra } })
const write = () => router.push(query({ compose: '1' }))
const open = post => router.push(query({ post: post.id }))
const edit = post => router.push(query({ post: post.id, edit: '1' }))
const close = () => router.push(query({}))
async function saved(post) { await router.replace(query({ post: post.id })); await load() }
async function toggleMine() { await router.push(query({ mine: mine.value ? undefined : '1', page: undefined })) }
function goPage(p, scroll = true) {
  router.push(query({ page: p > 0 ? String(p + 1) : undefined }))
  if (scroll) panel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
function search({ q, from, to }) {
  router.push(query({ q: q || undefined, from: from || undefined, to: from ? to : undefined, page: undefined }))
}
async function remove(post) {
  if (!await useDialogStore().confirm({ title: '게시글을 삭제할까요?', okText: '삭제', danger: true })) return
  try { await deleteBandPost(props.band.id, post.id); await load() }
  catch (e) { error.value = e.response?.data?.message || '글을 삭제하지 못했어요. 다시 시도해 주세요.' }
}
watch(() => props.band.id, () => { rows.value = []; noticeRows.value = []; regularTotal.value = 0; totalPages.value = 0 })
watch(() => [props.band.id, mine.value, page.value, filters.value.q, filters.value.from, filters.value.to], () => load(), { immediate: true })
// 앱으로 돌아왔을 때·새 공지나 댓글 알림이 왔을 때 글 목록을 다시 (#224)
useLiveRefresh(load, { types: ['NOTICE', 'COMMENT', 'REPLY', 'MENTION'] })
</script>

<style scoped>
.community-panel { scroll-margin-top: 84px; }
.error { padding: 10px 14px; border-radius: var(--radius-md); background: var(--c-danger-soft); color: var(--c-danger); font-size: 0.8rem; font-weight: 700; line-height: 1.6; }
.error button { border: 0; background: none; color: var(--c-primary-text); font: inherit; cursor: pointer; }
</style>
