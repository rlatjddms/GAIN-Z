<template>
  <div v-if="attachment" class="attachment" :class="{ compact }">
    <span v-if="loading" class="status">첨부 불러오는 중…</span>
    <button v-else-if="error" class="status" @click.stop="load">첨부 다시 불러오기</button>
    <template v-else-if="url">
      <img v-if="attachment.type.startsWith('image/')" :src="url" :alt="attachment.name" @error="error = true">
      <video v-else-if="attachment.type.startsWith('video/')" :src="url" controls preload="metadata" playsinline
             :aria-label="attachment.name" @click.stop @error="error = true" />
      <audio v-else-if="!compact && attachment.type.startsWith('audio/')" :src="url" controls :aria-label="attachment.name" @click.stop @error="error = true" />
      <a v-if="!compact || !isVisual" :href="url" :download="attachment.name" @click.stop>📎 {{ attachment.name }}</a>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { loadBandAttachment } from '../../api/bandBoard'

const props = defineProps({ attachment: { type: Object, default: null }, compact: Boolean })
const url = ref(null)
const loading = ref(false)
const error = ref(false)
const isVisual = computed(() => /^(image|video)\//.test(props.attachment?.type || ''))
let sequence = 0
function release() {
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = null
}
async function load() {
  const current = ++sequence
  release()
  error.value = false
  loading.value = false
  if (!props.attachment) return
  loading.value = true
  try {
    const next = await loadBandAttachment(props.attachment.url)
    if (current !== sequence) { URL.revokeObjectURL(next); return }
    url.value = next
  } catch {
    if (current === sequence) error.value = true
  } finally {
    if (current === sequence) loading.value = false
  }
}
watch(() => props.attachment?.url, load, { immediate: true })
onBeforeUnmount(() => { sequence++; release() })
</script>

<style scoped>
.attachment { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; min-width: 0; }
img, video { display: block; max-width: 100%; max-height: 480px; border-radius: 12px; background: #f1f3f5; object-fit: contain; }
audio { max-width: 100%; }
a { color: #495057; font-size: .8rem; overflow-wrap: anywhere; }
.compact { width: 112px; height: 84px; flex: none; overflow: hidden; border-radius: 10px; }
.compact img, .compact video { width: 112px; height: 84px; }
.compact img { object-fit: cover; }
.status { color: #868e96; font: inherit; font-size: .75rem; }
</style>
