<template>
  <!-- 게시판 검색 (#188): 평소에는 걸린 조건만 작은 칩으로, 펼치면 검색어·날짜 한 줄 -->
  <div class="board-search">
    <form v-if="open" class="search-row" @submit.prevent="submit">
      <label class="keyword">
        <Search :size="15" :stroke-width="2.2" aria-hidden="true" />
        <input
          ref="input"
          :value="text"
          type="search"
          enterkeyhint="search"
          maxlength="50"
          placeholder="제목·내용·작성자"
          aria-label="검색어"
          @input="text = $event.target.value"
        />
      </label>
      <div ref="dateWrap" class="date-wrap">
        <button type="button" class="date-btn" :class="{ on: from }" :aria-expanded="calendarOpen" @click="calendarOpen = !calendarOpen">
          <CalendarDays :size="15" :stroke-width="2.2" aria-hidden="true" />
          <span>{{ from ? rangeLabel : '날짜' }}</span>
        </button>
        <DateRangeCalendar v-if="calendarOpen" class="calendar-pop" :from="from" :to="to" @change="pickDate" @close="calendarOpen = false" />
      </div>
      <button type="submit" class="go">검색</button>
      <button type="button" class="close" aria-label="검색 닫기" @click="close"><X :size="16" :stroke-width="2.2" /></button>
    </form>

    <div v-if="active" class="chips" aria-label="걸린 검색 조건">
      <span v-if="q" class="chip">‘{{ q }}’<button type="button" :aria-label="`검색어 ${q} 지우기`" @click="emit('search', { q: '', from, to })"><X :size="12" :stroke-width="2.6" /></button></span>
      <span v-if="from" class="chip">{{ rangeLabel }}<button type="button" aria-label="날짜 조건 지우기" @click="emit('search', { q, from: null, to: null })"><X :size="12" :stroke-width="2.6" /></button></span>
      <button type="button" class="clear" @click="emit('search', { q: '', from: null, to: null })">모두 지우기</button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { CalendarDays, Search, X } from 'lucide-vue-next'
import DateRangeCalendar from '../ui/DateRangeCalendar.vue'

const props = defineProps({
  open: Boolean,
  q: { type: String, default: '' },
  from: { type: String, default: null },
  to: { type: String, default: null },
})
// search: { q, from, to } / close: 검색 줄 접기
const emit = defineEmits(['search', 'close'])

// 검색어 칸. v-model 대신 input 이벤트로 받아 한글 조합 중인 마지막 글자도 바로 들어간다
const text = ref(props.q)
const input = ref(null)
const calendarOpen = ref(false)
// 달력 바깥을 누르면 닫는다 (#292, 연습 화면 달력과 같게)
const dateWrap = ref(null)
const onDocPointer = (e) => {
  if (calendarOpen.value && dateWrap.value && !dateWrap.value.contains(e.target)) calendarOpen.value = false
}
onMounted(() => document.addEventListener('pointerdown', onDocPointer))
onBeforeUnmount(() => document.removeEventListener('pointerdown', onDocPointer))
const active = computed(() => !!(props.q || props.from))

watch(() => props.q, (v) => { text.value = v })
watch(() => props.open, async (v) => {
  if (!v) { calendarOpen.value = false; return }
  await nextTick()
  input.value?.focus()
})

const short = (key) => { const [, m, d] = key.split('-').map(Number); return `${m}.${d}` }
const rangeLabel = computed(() => (!props.from ? '' : props.from === props.to ? short(props.from) : `${short(props.from)} ~ ${short(props.to)}`))

const submit = () => {
  calendarOpen.value = false
  emit('search', { q: text.value.trim(), from: props.from, to: props.to })
}
// 날짜는 고르자마자 적용 (지금 칸의 검색어도 함께)
const pickDate = ({ from, to }) => {
  emit('search', { q: text.value.trim(), from, to })
  if (from === null || from !== to) calendarOpen.value = false
}
const close = () => { calendarOpen.value = false; emit('close') }
</script>

<style scoped>
.board-search { margin-bottom: 12px; }
.board-search:empty { display: none; }
.search-row { display: flex; align-items: center; gap: 6px; }
.keyword {
  flex: 1; min-width: 0; display: flex; align-items: center; gap: 6px; height: 38px; padding: 0 12px;
  border: 1px solid var(--c-line); border-radius: 10px; background: #fff; color: var(--c-faint);
}
.keyword:focus-within { border-color: var(--c-primary); }
.keyword input { flex: 1; min-width: 0; border: none; outline: none; background: none; font: inherit; font-size: 16px; color: var(--c-text); }
.date-wrap { position: relative; flex: none; }
.date-btn, .go, .close {
  height: 38px; display: flex; align-items: center; gap: 5px; padding: 0 12px; border: 1px solid var(--c-line); border-radius: 10px;
  background: #fff; font: inherit; font-size: 0.8rem; font-weight: 700; color: var(--c-text-2); cursor: pointer; white-space: nowrap;
}
.date-btn.on { border-color: var(--c-primary-line); background: var(--c-primary-soft); color: var(--c-primary-text); }
.go { border-color: var(--c-primary); color: var(--c-primary-text); }
.close { width: 38px; justify-content: center; padding: 0; border-color: transparent; color: var(--c-faint); }
.calendar-pop { position: absolute; top: calc(100% + 6px); right: 0; z-index: 50; }

.chips { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.search-row + .chips { margin-top: 8px; }
.board-search > .chips:first-child { margin-top: 0; }
.chip {
  display: inline-flex; align-items: center; gap: 4px; padding: 4px 6px 4px 10px; border-radius: 999px;
  background: var(--c-primary-soft); color: var(--c-primary-text); font-size: 0.75rem; font-weight: 700;
}
.chip button { display: flex; padding: 2px; border: none; border-radius: 50%; background: none; color: inherit; cursor: pointer; }
.chip button:hover { background: color-mix(in srgb, var(--c-primary) 15%, transparent); }
.clear { border: none; background: none; padding: 0 4px; font: inherit; font-size: 0.72rem; font-weight: 700; color: var(--c-muted); cursor: pointer; }

/* 휴대폰: 검색 버튼은 키보드의 검색 키로 대신하고, 날짜는 아이콘만 */
@media (max-width: 640px) {
  .go { display: none; }
  .date-btn span { display: none; }
  .date-btn.on span { display: inline; }
  .calendar-pop { right: -44px; }
}
</style>
