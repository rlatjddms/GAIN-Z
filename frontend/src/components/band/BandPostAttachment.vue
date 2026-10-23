<template>
  <div class="attachment" :class="{ compact, tile, visual: isVisual }">
    <div v-if="isVisual || tile" class="media-frame">
      <span v-if="loading" class="media-placeholder"><BoardIcon :name="isVisual ? 'image' : 'file'" :size="26" /></span>
      <div v-else-if="error" class="media-placeholder"><BoardIcon name="file" :size="24" /><button @click.stop="load">다시 불러오기</button></div>
      <template v-else>
        <img :class="{ expandable }" :tabindex="expandable ? 0 : undefined" :role="expandable ? 'button' : undefined" :aria-label="expandable ? `${attachment.name} 크게 보기` : undefined" @click="expandable && $emit('expand')" @keydown.enter.prevent="expandable && $emit('expand')" @keydown.space.prevent="expandable && $emit('expand')" v-if="url && attachment.type.startsWith('image/')" :src="url" :alt="attachment.name" @error="error = true">
        <video v-else-if="url && attachment.type.startsWith('video/')" :src="url" :controls="!compact" preload="auto" playsinline :aria-label="attachment.name" @click.stop @error="error = true" />
        <div v-else class="media-placeholder"><BoardIcon name="file" :size="28" /><span>{{ attachment.type.startsWith('audio/') ? '오디오' : 'PDF' }}</span></div>
        <span v-if="compact && attachment.type.startsWith('video/')" class="play-badge"><BoardIcon name="play" :size="14" /></span>
      </template>
    </div>
    <template v-if="!compact && !tile && !hideDownload">
      <audio v-if="url && attachment.type.startsWith('audio/')" :src="url" controls :aria-label="attachment.name" />
      <div class="file-row" :class="{ document: !isVisual }">
        <span v-if="!isVisual" class="document-icon"><BoardIcon name="file" :size="20" /></span>
        <span class="file-name">{{ attachment.name }}</span>
        <a v-if="url" :href="url" :download="attachment.name" :aria-label="`${attachment.name} 다운로드`" @click.stop><BoardIcon name="download" :size="16" /></a>
        <button v-else :disabled="loading" :aria-label="`${attachment.name} 다운로드`" @click.stop="download"><BoardIcon name="download" :size="16" /></button>
      </div>
      <p v-if="error && !isVisual" class="file-error">파일을 불러오지 못했어요. 다운로드를 눌러 다시 시도해 주세요.</p>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { loadBandAttachment } from '../../api/bandBoard'
import BoardIcon from './BoardIcon.vue'
const props = defineProps({ attachment: { type: Object, required: true }, compact: Boolean, tile: Boolean, expandable: Boolean, hideDownload: Boolean })
defineEmits(['expand'])
const url = ref(null)
const loading = ref(false)
const error = ref(false)
const isVisual = computed(() => /^(image|video)\//.test(props.attachment?.type || ''))
let sequence = 0
function release() { if (url.value) URL.revokeObjectURL(url.value); url.value = null }
async function load() {
  const current = ++sequence
  release()
  error.value = false
  loading.value = true
  try {
    const next = await loadBandAttachment(props.attachment.url)
    if (current !== sequence) { URL.revokeObjectURL(next); return }
    url.value = next
  } catch { if (current === sequence) error.value = true }
  finally { if (current === sequence) loading.value = false }
}
async function download() {
  await load()
  if (!url.value) return
  const link = document.createElement('a')
  link.href = url.value
  link.download = props.attachment.name
  link.click()
}
watch(() => props.attachment.url, () => {
  if (isVisual.value || props.attachment.type.startsWith('audio/')) load()
  else { sequence++; release(); error.value = false; loading.value = false }
}, { immediate: true })
onBeforeUnmount(() => { sequence++; release() })
</script>

<style scoped>
.expandable { cursor: zoom-in; }
.attachment { min-width: 0; }
.media-frame { position: relative; height: 220px; border-radius: 12px; overflow: hidden; background: #f3f5f8; }
img, video { width: 100%; height: 100%; object-fit: contain; display: block; }
video { background: #242832; }
.media-placeholder { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #a2adbf; font-size: 11px; }
.media-placeholder button { border: 0; border-radius: 6px; background: #e9edf4; color: #68788f; padding: 6px 10px; font: inherit; cursor: pointer; }
.file-row { display: flex; align-items: center; gap: 10px; padding: 9px 2px 0; }
.file-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; color: var(--c-muted); }
.file-row a, .file-row button { color: var(--c-muted); display: grid; place-items: center; width: 26px; height: 26px; background: none; border: 0; border-radius: 6px; cursor: pointer; }
.file-row a:hover, .file-row button:hover { color: var(--c-primary-text); background: var(--c-primary-soft); }
.file-row.document { border: 1px solid #e9edf2; border-radius: 12px; padding: 12px 14px; }
.document .file-name { color: var(--c-text-2); font-size: 12px; font-weight: 700; }
.document-icon { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 9px; background: #f1f4f9; color: #8290a7; }
audio { width: 100%; margin-bottom: 8px; }
.file-error { color: #d94841; font-size: 11px; }
.compact { width: 112px; height: 84px; flex: none; }
.compact .media-frame { width: 112px; height: 84px; border-radius: 10px; }
.compact img, .compact video { object-fit: cover; }
.play-badge { position: absolute; inset: 0; display: grid; place-items: center; color: white; background: #0002; pointer-events: none; }
.play-badge svg { background: #15203999; padding: 7px; border-radius: 50%; }
.tile .media-frame { height: 124px; border-radius: 0; }
.tile img { object-fit: cover; }
@media (max-width: 640px) { .media-frame { height: 160px; } }
</style>
