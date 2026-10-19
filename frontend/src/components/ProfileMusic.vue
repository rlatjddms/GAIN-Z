<template>
  <!-- 요즘 듣는 음악 한 줄. 링크가 있으면 눌러서 새 탭으로 연다 -->
  <component
    :is="safeUrl ? 'a' : 'div'"
    class="profile-music"
    :class="{ compact, link: !!safeUrl }"
    v-bind="safeUrl ? { href: safeUrl, target: '_blank', rel: 'noopener noreferrer', title: '새 탭에서 듣기' } : {}"
  >
    <span class="note" aria-hidden="true">♪</span>
    <span class="text">
      <strong>{{ music.title }}</strong>
      <span class="artist">{{ music.artist }}</span>
    </span>
    <svg v-if="safeUrl" class="out" viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line>
    </svg>
  </component>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // { title, artist, url | null }
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
  background: #fff; border: 1px solid #e9ecef; border-radius: 16px; padding: 12px 16px;
  color: #343a40; text-decoration: none; transition: border-color 0.15s, background 0.15s;
}
.profile-music.link:hover { border-color: #2d5bff; background: #f8faff; }
.note {
  flex: none; width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: #2d5bff; color: #fff; font-size: 0.95rem; font-weight: 900;
}
.text { flex: 1; min-width: 0; display: flex; align-items: baseline; gap: 8px; overflow: hidden; }
.text strong, .artist { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text strong { font-size: 0.9rem; font-weight: 900; }
.artist { font-size: 0.8rem; font-weight: 700; color: #868e96; }
.out { flex: none; color: #adb5bd; }
.profile-music.link:hover .out { color: #2d5bff; }

/* 목록 안 작은 한 줄 */
.profile-music.compact { background: none; border: none; padding: 0; gap: 5px; }
.profile-music.compact .note { width: auto; height: auto; background: none; color: #2d5bff; font-size: 0.75rem; }
.profile-music.compact .text strong { font-size: 0.75rem; font-weight: 800; color: #495057; }
.profile-music.compact .artist { font-size: 0.72rem; }
.profile-music.compact .out { width: 12px; height: 12px; }
</style>
