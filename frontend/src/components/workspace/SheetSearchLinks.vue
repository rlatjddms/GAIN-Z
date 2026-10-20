<template>
  <!-- 악보 찾기 (#156): 사이트마다 검색 결과를 새 탭으로 -->
  <div class="search-links" :class="{ disabled: !query }">
    <a v-for="s in SHEET_SEARCH_SITES" :key="s.key" class="chip" :href="query ? s.url(query) : undefined"
       target="_blank" rel="noopener noreferrer" :aria-disabled="!query" :title="query ? `${s.name}에서 '${query}' 찾기` : '곡 제목을 먼저 적어 주세요'"
       @click="!query && $event.preventDefault()">
      {{ s.name }}<span class="note">{{ s.note }}</span><ExternalLink :size="12" :stroke-width="2.2" />
    </a>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ExternalLink } from 'lucide-vue-next'
import { SHEET_SEARCH_SITES, sheetSearchQuery } from '../../constants/sheetSearch'

const props = defineProps({
  title: { type: String, default: '' },
  artist: { type: String, default: '' },
})
const query = computed(() => sheetSearchQuery(props.title, props.artist))
</script>

<style scoped>
.search-links { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { text-decoration: none; }
.note { font-weight: 600; color: var(--c-faint); }
.disabled .chip { opacity: 0.45; cursor: not-allowed; }
</style>
