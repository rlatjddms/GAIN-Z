<template>
  <!-- 요즘 듣는 음악 한 줄. 링크가 있으면 눌러서 새 탭으로 연다 -->
  <component
    :is="safeUrl ? 'a' : 'div'"
    class="profile-music"
    :class="{ compact, link: !!safeUrl }"
    v-bind="safeUrl ? { href: safeUrl, target: '_blank', rel: 'noopener noreferrer', title: '새 탭에서 듣기' } : {}"
  >
    <img v-if="music.artworkUrl" :src="music.artworkUrl" alt="" class="cover" loading="lazy">
    <span v-else class="note" aria-hidden="true"><Music :size="13" :stroke-width="2.5" /></span>
    <span class="text">
      <strong>{{ music.title }}</strong>
      <span class="artist">{{ music.artist }}</span>
    </span>
    <PreviewButton v-if="music.previewUrl" :url="music.previewUrl" :small="compact" />
    <svg v-if="safeUrl" class="out" viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line>
    </svg>
  </component>
</template>

<script setup>
import { Music } from 'lucide-vue-next'
import { computed } from 'vue'
import PreviewButton from './PreviewButton.vue'

const props = defineProps({
  // { title, artist, url | null, artworkUrl | null, previewUrl | null } (커버·미리듣기는 곡 검색으로 골랐을 때, #187)
  music: { type: Object, required: true },
  // 목록 안의 작은 한 줄 (목록 줄 전체가 버튼이라 링크는 열지 않는다)
  compact: { type: Boolean, default: false },
})

// 서버에서도 막지만 http/https 링크만 연다
const safeUrl = computed(() => (!props.compact && /^https?:\/\//i.test(props.music?.url || '') ? props.music.url : null))
</script>

<style scoped>
.profile-music {
  display: flex; align-items: center; gap: 10px; min-width: 0; box-sizing: border-box;
  background: #fff; border: 1px solid var(--c-line); border-radius: 16px; padding: 12px 16px;
  color: #343a40; text-decoration: none; transition: border-color 0.15s, background 0.15s;
}
.profile-music.link:hover { border-color: var(--c-primary); background: var(--c-primary-soft); }
.note {
  flex: none; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: var(--c-primary); color: #fff; font-size: 0.95rem; font-weight: 900;
}
.cover { flex: none; width: 36px; height: 36px; border-radius: 8px; object-fit: cover; background: var(--c-line-soft); }
.text { flex: 1; min-width: 0; display: flex; align-items: baseline; gap: 8px; overflow: hidden; }
.text strong, .artist { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text strong { font-size: 0.9rem; font-weight: 900; }
.artist { font-size: 0.8rem; font-weight: 700; color: var(--c-muted); }
.out { flex: none; color: var(--c-faint); }
.profile-music.link:hover .out { color: var(--c-primary-text); }

/* 목록 안 작은 칩 (#228): 커버·제목·가수·미리듣기를 한 덩어리로. 내용만큼만 넓고 넘치면 제목부터 줄인다 */
.profile-music.compact {
  display: inline-flex; align-self: flex-start; max-width: 100%; margin-top: 4px; gap: 6px; padding: 3px 4px 3px 3px;
  background: var(--c-primary-soft); border: none; border-radius: 999px;
}
.profile-music.compact .note { width: 22px; height: 22px; font-size: 0.7rem; }
.profile-music.compact .cover { width: 22px; height: 22px; border-radius: 50%; }
.profile-music.compact .text { flex: 0 1 auto; gap: 5px; }
.profile-music.compact .text strong { flex: 0 1 auto; font-size: 0.74rem; font-weight: 800; color: var(--c-text-2); }
.profile-music.compact .artist { flex: 0 10 auto; font-size: 0.7rem; }
.profile-music.compact:not(:has(.preview-btn)) { padding-right: 10px; }
.profile-music.compact .out { width: 12px; height: 12px; }
</style>
