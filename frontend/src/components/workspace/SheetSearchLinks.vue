<template>
  <!-- 악보 찾기 (#156): 사이트마다 검색 결과를 새 탭으로 -->
  <div class="search-links">
    <a v-for="s in SHEET_SEARCH_SITES" :key="s.key" class="chip" :class="{ off: !usable(s) }" :href="usable(s) ? s.url(query) : undefined"
       target="_blank" rel="noopener noreferrer" :aria-disabled="!usable(s)" :title="tip(s)"
       @click="!usable(s) && $event.preventDefault()">
      {{ s.name }}<span class="note">{{ s.note }}</span><ExternalLink :size="12" :stroke-width="2.2" />
    </a>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ExternalLink } from 'lucide-vue-next'
import { SHEET_SEARCH_SITES, hasHangul, sheetSearchQuery } from '../../constants/sheetSearch'

const props = defineProps({
  title: { type: String, default: '' },
  artist: { type: String, default: '' },
})
const query = computed(() => sheetSearchQuery(props.title, props.artist))
// 검색어가 없거나, 영어 전용 사이트에 한글 검색어면 끈다
const usable = (s) => !!query.value && !(s.latinOnly && hasHangul(query.value))
const tip = (s) => {
  if (!query.value) return '곡 제목이나 가수를 먼저 입력해 주세요'
  if (!usable(s)) return `${s.name}는 영어 제목으로만 찾을 수 있어요`
  return `${s.name}에서 '${query.value}' 찾기`
}
</script>

<style scoped>
.search-links { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { text-decoration: none; }
.note { font-weight: 600; color: var(--c-faint); }
.chip.off { opacity: 0.45; cursor: not-allowed; }
</style>
