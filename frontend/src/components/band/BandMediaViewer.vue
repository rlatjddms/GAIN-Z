<template>
  <Teleport to="body">
    <dialog ref="dialog" class="media-viewer" aria-label="첨부 미디어 상세보기" @cancel.prevent="$emit('close')" @keydown="keyboard">
      <header><div><strong>{{ current.name }}</strong><span>{{ index + 1 }} / {{ items.length }}</span></div><button :disabled="!url || loading" @click="downloadCurrent"><BoardIcon name="download" />저장</button><button :disabled="downloading" @click="downloadAll"><BoardIcon name="download" />{{ downloading ? '준비 중…' : '전체 ZIP 저장' }}</button><button aria-label="미디어 닫기" @click="$emit('close')"><BoardIcon name="close" /></button></header>
      <p v-if="downloadError" class="error" role="alert">{{ downloadError }}</p>
      <div class="stage" :class="{ zoomed }" @touchstart.passive="touchStart" @touchend.passive="touchEnd">
        <button v-if="items.length > 1" class="previous" aria-label="이전 미디어" :disabled="index === 0" @click="move(-1)">‹</button>
        <p v-if="loading" role="status">불러오는 중…</p>
        <div v-else-if="error" role="alert">미디어를 불러오지 못했어요. <button @click="load">다시 시도</button></div>
        <button v-else-if="current.type.startsWith('image/')" class="image-button" :aria-label="zoomed ? '이미지 축소' : '이미지 확대'" :aria-pressed="zoomed" @click="zoomed = !zoomed"><img :src="url" :alt="current.name" @error="error = true"></button>
        <video v-else :key="url" :src="url" controls playsinline :aria-label="current.name" @error="error = true" />
        <button v-if="items.length > 1" class="next" aria-label="다음 미디어" :disabled="index === items.length - 1" @click="move(1)">›</button>
      </div>
      <footer><span>좌우 화살표 또는 밀어서 넘기기 · 사진을 누르면 확대</span><nav aria-label="첨부 미디어 선택"><button v-for="(item, i) in items" :key="item.id" :aria-label="`${i + 1}번째 ${item.name}`" :aria-current="index === i ? 'true' : undefined" @click="index = i">{{ i + 1 }}<BoardIcon :name="item.type.startsWith('image/') ? 'image' : 'play'" :size="14" /></button></nav></footer>
    </dialog>
  </Teleport>
</template>
<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { loadBandAttachment, downloadBandArchive } from '../../api/bandBoard'
import BoardIcon from './BoardIcon.vue'
const props = defineProps({ items: Array, initialIndex: Number, bandId: Number, postId: Number })
defineEmits(['close'])
const dialog = ref(null), index = ref(props.initialIndex || 0), url = ref(null), loading = ref(false), error = ref(false), zoomed = ref(false), downloading = ref(false), downloadError = ref('')
const current = computed(() => props.items[index.value])
const returnFocus = document.activeElement
let sequence = 0, alive = true, touchX = 0, touchY = 0
const downloads = []
onMounted(() => dialog.value.showModal())
onBeforeUnmount(() => { alive = false; sequence++; if (url.value) URL.revokeObjectURL(url.value); downloads.forEach(URL.revokeObjectURL); dialog.value?.close(); returnFocus?.focus() })
async function load() {
  const seq = ++sequence
  loading.value = true; error.value = false; zoomed.value = false
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = null
  try { const next = await loadBandAttachment(current.value.url); if (seq !== sequence) URL.revokeObjectURL(next); else url.value = next }
  catch { if (seq === sequence) error.value = true }
  finally { if (seq === sequence) loading.value = false }
}
function move(delta) { index.value = Math.max(0, Math.min(props.items.length - 1, index.value + delta)) }
function keyboard(event) {
  if (event.target.tagName === 'VIDEO' || zoomed.value) return
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); move(event.key === 'ArrowLeft' ? -1 : 1) }
}
function touchStart(event) { touchX = event.changedTouches[0].clientX; touchY = event.changedTouches[0].clientY }
function touchEnd(event) { const dx = event.changedTouches[0].clientX - touchX; const dy = event.changedTouches[0].clientY - touchY; if (!zoomed.value && event.target.tagName !== 'VIDEO' && Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy)) move(dx < 0 ? 1 : -1) }
function save(url, name) { const a = document.createElement('a'); a.href = url; a.download = name; a.click() }
function downloadCurrent() { if (url.value) save(url.value, current.value.name) }
async function downloadAll() {
  downloading.value = true; downloadError.value = ''
  try { const next = await downloadBandArchive(props.bandId, props.postId); if (!alive) { URL.revokeObjectURL(next); return }; downloads.push(next); save(next, `community-${props.postId}.zip`) }
  catch { if (alive) downloadError.value = '전체 저장에 실패했어요. 다시 눌러 시도해 주세요.' }
  finally { downloading.value = false }
}
watch(index, load, { immediate: true })
</script>
<style scoped>
* { box-sizing: border-box; }
.media-viewer { position: fixed; inset: 0; width: 100vw; height: 100dvh; max-width: none; max-height: none; margin: 0; padding: 0; border: 0; background: #10141c; color: #fff; font-family: 'Pretendard', sans-serif; }
.media-viewer[open] { display: flex; flex-direction: column; }
.media-viewer::backdrop { background: #10141cee; }
header { display: flex; align-items: center; gap: 10px; padding: 18px 24px; background: #1a202b; flex: none; }
header > div { flex: 1; min-width: 0; } header strong { display: block; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } header span { display: block; margin-top: 5px; color: #a5b0c2; font-size: 12px; }
button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; color: #f3f5fa; background: #2b3444; border: 1px solid #465167; border-radius: 10px; padding: 10px 12px; font: inherit; font-size: 12px; cursor: pointer; }
button:disabled { opacity: .35; cursor: default; } button:focus-visible { outline: 2px solid var(--c-primary-line); outline-offset: 3px; }
.stage { flex: 1; min-height: 0; display: flex; justify-content: center; align-items: center; overflow: auto; padding: 20px 64px; }
.image-button { display: flex; padding: 0; width: 100%; height: 100%; border: 0; background: none; cursor: zoom-in; }
.image-button img { width: 100%; height: 100%; }
img, video { display: block; max-width: 100%; max-height: 100%; object-fit: contain; }
.zoomed { display: block; }.zoomed .image-button { display: block; width: 200%; height: 200%; cursor: zoom-out; }.zoomed img { max-width: none; max-height: none; }
.previous, .next { position: absolute; top: 50%; font-size: 32px; width: 42px; height: 54px; z-index: 2; }.previous { left: 12px; }.next { right: 12px; }
footer { flex: none; padding: 12px 20px 20px; text-align: center; color: #a5b0c2; font-size: 11px; } nav { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; margin-top: 12px; } nav button[aria-current] { background: var(--c-primary); border-color: color-mix(in srgb, var(--c-primary) 55%, #fff); }
.error { color: #ffb4b4; padding: 8px 24px; margin: 0; font-size: 12px; }
@media(max-width: 600px) { header { padding: 12px; gap: 6px; } header button { padding: 9px 7px; font-size: 10px; } header button svg { width: 15px; } .stage { padding: 12px 42px; } .previous,.next { width: 30px; height: 44px; padding: 0; }.previous { left: 4px; }.next { right: 4px; } footer { padding-bottom: 16px; } }
</style>
