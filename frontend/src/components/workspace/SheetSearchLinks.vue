<template>
  <!-- 악보 찾기 (#156): 사이트마다 검색 결과를 새 탭으로 -->
  <div class="search-links">
    <a v-for="s in SHEET_SEARCH_SITES" :key="s.key" class="chip" :class="{ off: !usable(s) }" :href="usable(s) ? s.url(query) : undefined"
       target="_blank" rel="noopener noreferrer" :aria-disabled="!usable(s)" :title="tip(s)"
       @click="open(s, $event)">
      {{ s.name }}<span class="note">{{ s.note }}</span><ExternalLink :size="12" :stroke-width="2.2" />
    </a>
    <p v-if="copied" class="copied" role="status">'{{ copied }}'를 복사했어요. Songsterr 검색창에 붙여 넣어 주세요.</p>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { ExternalLink } from 'lucide-vue-next'
import { SHEET_SEARCH_SITES, hasHangul, sheetSearchQuery } from '../../constants/sheetSearch'

const props = defineProps({
  title: { type: String, default: '' },
  artist: { type: String, default: '' },
})
const query = computed(() => sheetSearchQuery(props.title, props.artist))
// 검색어가 없으면 끈다
const usable = () => !!query.value
// 사이트가 한글 검색어를 받지 못하면 (Songsterr) 누를 때 검색어를 복사해 둔다 (#164)
const needsCopy = (s) => s.copyIfHangul && hasHangul(query.value)
const tip = (s) => {
  if (!query.value) return '곡 제목이나 가수를 먼저 입력해 주세요'
  if (needsCopy(s)) return `${s.name}를 열고 검색어를 복사해 둘게요 (한글 검색어는 사이트 검색창에 붙여 넣어야 해요)`
  return `${s.name}에서 '${query.value}' 찾기`
}

const copied = ref('')
let copiedTimer = null
const open = async (s, event) => {
  if (!usable(s)) {
    event.preventDefault()
    return
  }
  if (!needsCopy(s)) return
  try {
    await navigator.clipboard.writeText(query.value)
    copied.value = query.value
    clearTimeout(copiedTimer)
    copiedTimer = setTimeout(() => { copied.value = '' }, 5000)
  } catch {
    // 복사가 안 되는 환경이면 사이트만 연다
  }
}
onBeforeUnmount(() => clearTimeout(copiedTimer))
</script>

<style scoped>
.search-links { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { text-decoration: none; }
.note { font-weight: 600; color: var(--c-faint); }
.chip.off { opacity: 0.45; cursor: not-allowed; }
.copied { flex-basis: 100%; margin: 2px 0 0; font-size: 0.75rem; font-weight: 700; color: var(--c-primary-text); }
</style>
