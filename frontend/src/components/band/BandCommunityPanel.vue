<template>
  <div>
    <p v-if="loading" class="status">게시글을 불러오는 중…</p>
    <p v-else-if="error" class="status" role="alert">{{ error }} <button @click="load">다시 불러오기</button></p>
    <BandCommunityFeed v-else :notice="notice" :posts="posts" @open="open" @write="write" @list="list" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listBandPosts } from '../../api/bandBoard'
import BandCommunityFeed from './BandCommunityFeed.vue'

const props = defineProps({ bandId: { type: Number, required: true } })
const router = useRouter()
const rows = ref([])
const loading = ref(false)
const error = ref('')
const notice = computed(() => rows.value.find(p => p.notice) || null)
const posts = computed(() => rows.value.filter(p => p.id !== notice.value?.id).slice(0, 6))
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  error.value = ''
  rows.value = []
  try {
    const [notices, recent] = await Promise.all([
      listBandPosts(props.bandId, 0, 1, true), listBandPosts(props.bandId, 0, 6, false),
    ])
    if (current === sequence) rows.value = [...notices.content, ...recent.content]
  } catch (e) {
    if (current === sequence) error.value = e.response?.data?.message || '게시글을 불러오지 못했어요.'
  } finally {
    if (current === sequence) loading.value = false
  }
}
const list = () => router.push(`/bands/${props.bandId}/community`)
const write = () => router.push({ path: `/bands/${props.bandId}/community`, query: { new: '1' } })
const open = (post) => router.push({ path: `/bands/${props.bandId}/community`, query: { post: post.id } })
watch(() => props.bandId, load, { immediate: true })
</script>

<style scoped>
.status { margin: 24px 0; color: #868e96; font-size: .85rem; }
</style>
