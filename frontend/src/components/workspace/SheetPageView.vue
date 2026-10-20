<template>
  <!-- 악보 한 장 (이미지 또는 PDF). 파일은 로그인 토큰으로 받아서 보여 준다 -->
  <div class="sheet-page" :class="{ thumb }">
    <div v-if="error" class="state error">{{ thumb ? '!' : '이 장을 불러오지 못했어요.' }}</div>
    <div v-else-if="!url" class="state">{{ thumb ? '' : '불러오는 중…' }}</div>
    <template v-else-if="pdf">
      <div v-if="thumb" class="state pdf">PDF</div>
      <iframe v-else :src="url" :title="alt" class="pdf-frame"></iframe>
    </template>
    <img v-else :src="url" :alt="alt" :loading="thumb ? 'lazy' : 'eager'" draggable="false">
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { loadSheetPageUrl } from '../../api/sheet'
import { isPdf } from '../../constants/sheet'

const props = defineProps({
  sheetId: { type: Number, required: true },
  pageId: { type: Number, required: true },
  contentType: { type: String, default: 'image/png' },
  alt: { type: String, default: '악보' },
  // 목록용 작은 미리보기 (PDF는 글자로만)
  thumb: { type: Boolean, default: false },
})

const url = ref(null)
const error = ref(false)
const pdf = computed(() => isPdf(props.contentType))

const release = () => {
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = null
}

let token = 0
const load = async () => {
  const mine = ++token
  release()
  error.value = false
  if (props.thumb && pdf.value) return
  try {
    const result = await loadSheetPageUrl(props.sheetId, props.pageId)
    if (mine === token) url.value = result
    else URL.revokeObjectURL(result)
  } catch {
    if (mine === token) error.value = true
  }
}

watch(() => [props.sheetId, props.pageId], load, { immediate: true })
onBeforeUnmount(() => { token++; release() })
</script>

<style scoped>
.sheet-page { position: relative; width: 100%; background: #fff; }
img { display: block; width: 100%; height: auto; user-select: none; }
.pdf-frame { display: block; width: 100%; height: 85vh; border: 0; }
.state {
  display: flex; align-items: center; justify-content: center; min-height: 240px;
  font-size: 0.8rem; font-weight: 700; color: var(--c-faint); background: var(--c-bg);
}
.state.error { color: var(--c-danger); }

.thumb { width: 100%; height: 100%; overflow: hidden; background: var(--c-bg); }
.thumb img { width: 100%; height: 100%; object-fit: cover; object-position: top; }
.thumb .state { min-height: 0; height: 100%; font-size: 0.7rem; }
.thumb .state.pdf { color: var(--c-danger); font-weight: 900; letter-spacing: 0.05em; }
</style>
