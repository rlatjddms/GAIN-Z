<template>
  <!-- 프로필 사진. 사진이 없거나 못 불러오면 기본 프로필 -->
  <span class="profile-avatar" :style="{ width: `${size}px`, height: `${size}px` }">
    <img v-if="url" :src="url" :alt="alt" @error="release">
    <svg v-else class="default" viewBox="0 0 40 40" aria-hidden="true">
      <circle cx="20" cy="15" r="7" />
      <path d="M6 40v-3c1.5-7.5 7.2-11.5 14-11.5S32.5 29.5 34 37v3z" />
    </svg>
  </span>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { loadProfileImageUrl } from '../api/user'

const props = defineProps({
  // 내 정보의 profileImageUrl (없으면 기본 프로필)
  src: { type: String, default: null },
  size: { type: Number, default: 32 },
  alt: { type: String, default: '프로필 사진' },
})

const url = ref(null)

const release = () => {
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = null
}

let token = 0
watch(() => props.src, async (src) => {
  const mine = ++token
  if (!src) { release(); return }
  try {
    const result = await loadProfileImageUrl(src)
    if (mine !== token) { URL.revokeObjectURL(result); return }
    release()
    url.value = result
  } catch {
    if (mine === token) release()
  }
}, { immediate: true })

onBeforeUnmount(() => { token++; release() })
</script>

<style scoped>
.profile-avatar {
  flex: none; display: inline-flex; align-items: center; justify-content: center; overflow: hidden;
  border-radius: 50%; background: var(--c-line); box-shadow: 0 0 0 1px var(--c-line);
}
img { width: 100%; height: 100%; object-fit: cover; display: block; }
.default { width: 100%; height: 100%; fill: var(--c-faint); }
</style>
