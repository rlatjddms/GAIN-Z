<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="!busy && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="music-title">
          <h2 id="music-title">요즘 듣는 음악</h2>
          <p class="sub">친구와 밴드 멤버가 내 프로필에서 보고 30초 미리듣기를 할 수 있어요.</p>

          <!-- 곡 검색 (#187): 고르면 아래 칸이 채워지고 커버·미리듣기가 붙는다 -->
          <div class="search">
            <label class="search-box">
              <Search :size="15" :stroke-width="2.4" aria-hidden="true" />
              <input
                ref="searchInput"
                :value="term"
                type="search"
                enterkeyhint="search"
                maxlength="60"
                placeholder="곡 제목이나 가수로 찾기"
                aria-label="곡 검색"
                :disabled="busy"
                @input="term = $event.target.value"
                @keydown.enter.prevent="runSearch"
              >
            </label>
            <p v-if="searching" class="search-note">찾는 중…</p>
            <p v-else-if="searchError" class="search-note bad">{{ searchError }}</p>
            <p v-else-if="searched && !results.length" class="search-note">찾는 곡이 없어요. 아래 '직접 입력하기'로 적어도 돼요.</p>
            <ul v-if="results.length" class="results">
              <li v-for="r in results" :key="r.id">
                <button type="button" class="result" :class="{ picked: isPicked(r) }" @click="pick(r)">
                  <img v-if="r.artworkUrl" :src="r.artworkUrl" alt="" class="cover" loading="lazy">
                  <span v-else class="cover empty"><Music :size="16" /></span>
                  <span class="r-text"><strong>{{ r.title }}</strong><span>{{ r.artist }}<template v-if="r.album"> · {{ r.album }}</template></span></span>
                </button>
                <PreviewButton v-if="r.previewUrl" :url="r.previewUrl" />
              </li>
            </ul>
          </div>

          <div v-if="artworkUrl" class="picked-card">
            <img :src="artworkUrl" alt="" class="cover">
            <span class="r-text"><strong>{{ title || '제목' }}</strong><span>{{ artist }}</span></span>
            <PreviewButton v-if="previewUrl" :url="previewUrl" />
            <button type="button" class="unpick" aria-label="고른 곡의 커버·미리듣기 빼기" :disabled="busy" @click="unpick"><X :size="15" :stroke-width="2.4" /></button>
          </div>

          <form @submit.prevent="save">
            <!-- 검색이 먼저, 직접 적기는 접어 둔다 (#228). 고른 곡은 제목·가수를 고칠 때만 연다 -->
            <button type="button" class="manual-toggle" :aria-expanded="manualOpen" :disabled="busy" @click="toggleManual">
              {{ artworkUrl ? '제목·가수 고치기' : '직접 입력하기' }}
              <ChevronDown class="chevron" :class="{ open: manualOpen }" :size="15" :stroke-width="2.4" aria-hidden="true" />
            </button>
            <div v-if="manualOpen" class="manual">
              <p v-if="artworkUrl" class="manual-label">영어 제목을 한국어로 바꿔도 돼요</p>
              <label class="field">
                <span>곡 제목</span>
                <input ref="titleInput" v-model="title" type="text" :maxlength="PROFILE_MUSIC_TITLE_MAX" placeholder="예) Hysteria" :disabled="busy">
              </label>
              <label class="field">
                <span>아티스트</span>
                <input v-model="artist" type="text" :maxlength="PROFILE_MUSIC_ARTIST_MAX" placeholder="예) Muse" :disabled="busy">
              </label>
              <label class="field">
                <span>링크 <em>선택</em></span>
                <input v-model="url" type="url" :maxlength="PROFILE_MUSIC_URL_MAX" placeholder="유튜브·스포티파이 링크" :disabled="busy">
              </label>
              <p v-if="url.trim() && !urlValid" class="hint bad">http:// 또는 https://로 시작하는 링크만 넣을 수 있어요.</p>
            </div>
            <p v-if="error" class="error">{{ error }}</p>

            <div class="actions">
              <button v-if="current" type="button" class="btn ghost" :disabled="busy" @click="remove">지우기</button>
              <span class="spacer"></span>
              <button type="button" class="btn" :disabled="busy" @click="emit('close')">취소</button>
              <button type="submit" class="btn primary" :disabled="busy || !valid">{{ busy ? '저장 중…' : '저장' }}</button>
            </div>
          </form>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 요즘 듣는 음악 등록·수정 팝업 (홈 음악 칩에서 연다). 저장하면 내 정보(authStore.profile)를 바로 바꾼다
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { ChevronDown, Music, Search, X } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import PreviewButton from './PreviewButton.vue'
import {
  updateProfileMusic, deleteProfileMusic, searchSongs,
  PROFILE_MUSIC_TITLE_MAX, PROFILE_MUSIC_ARTIST_MAX, PROFILE_MUSIC_URL_MAX,
} from '../api/user'
import { apiErrorMessage } from '../api/practice'
import { useDialogStore } from '../stores/dialog'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const authStore = useAuthStore()
const current = computed(() => authStore.profile?.profileMusic || null)

const title = ref('')
const artist = ref('')
const url = ref('')
// 곡 검색으로 골랐을 때만 있다
const artworkUrl = ref(null)
const previewUrl = ref(null)
const busy = ref(false)
const error = ref('')
const titleInput = ref(null)
const searchInput = ref(null)
// 직접 입력 칸 열기 (검색으로 고른 곡이면 제목·가수 고치기)
const manualOpen = ref(false)
const toggleManual = async () => {
  manualOpen.value = !manualOpen.value
  if (!manualOpen.value) return
  await nextTick()
  titleInput.value?.focus()
}

const urlValid = computed(() => /^https?:\/\/\S+$/i.test(url.value.trim()))
const valid = computed(() => title.value.trim() && artist.value.trim() && (!url.value.trim() || urlValid.value))

/* ---------- 곡 검색: 입력을 멈추면 잠시 뒤 찾는다 ----------
   v-model은 한글을 조합하는 동안 값을 늦게 바꿔서 마지막 글자가 빠진 채로 찾게 되니, input 이벤트로 바로 받는다 */
const term = ref('')
const results = ref([])
const searching = ref(false)
const searched = ref(false)
const searchError = ref('')
let searchTimer = null
let searchAbort = null

const runSearch = async () => {
  clearTimeout(searchTimer)
  searchAbort?.abort()
  const q = term.value.trim()
  if (!q) { results.value = []; searched.value = false; searchError.value = ''; return }
  searchAbort = new AbortController()
  const signal = searchAbort.signal
  searching.value = true
  searchError.value = ''
  try {
    results.value = await searchSongs(q, signal)
    searched.value = true
  } catch (e) {
    // 새 검색으로 이전 요청을 끊은 것 (브라우저 fetch는 AbortError, 서버로 찾을 때 axios는 CanceledError)
    if (e.name === 'AbortError' || e.name === 'CanceledError') return
    results.value = []
    searchError.value = '곡을 찾지 못했어요. 잠시 후 다시 해 보거나 아래에 직접 적어 주세요.'
  } finally {
    if (!signal.aborted) searching.value = false
  }
}
watch(term, () => { clearTimeout(searchTimer); searchTimer = setTimeout(runSearch, 400) })
onBeforeUnmount(() => { clearTimeout(searchTimer); searchAbort?.abort() })

const isPicked = (r) => !!artworkUrl.value && artworkUrl.value === r.artworkUrl && previewUrl.value === r.previewUrl
const pick = (r) => {
  title.value = r.title
  artist.value = r.artist
  url.value = r.url || ''
  artworkUrl.value = r.artworkUrl
  previewUrl.value = r.previewUrl
}
const unpick = () => { artworkUrl.value = null; previewUrl.value = null }

watch(() => props.open, async (open) => {
  if (!open) return
  title.value = current.value?.title || ''
  artist.value = current.value?.artist || ''
  url.value = current.value?.url || ''
  artworkUrl.value = current.value?.artworkUrl || null
  previewUrl.value = current.value?.previewUrl || null
  // 예전에 직접 적은 곡(커버 없음)이면 입력 칸을 열어 둔다
  manualOpen.value = !!current.value && !current.value.artworkUrl
  term.value = ''
  results.value = []
  searched.value = false
  searchError.value = ''
  error.value = ''
  await nextTick()
  searchInput.value?.focus()
})

const run = async (action, fallback) => {
  busy.value = true
  error.value = ''
  try {
    authStore.profile = await action()
    emit('close')
  } catch (e) {
    error.value = apiErrorMessage(e, fallback)
  } finally {
    busy.value = false
  }
}

const save = () => {
  if (!valid.value || busy.value) return
  run(() => updateProfileMusic({
    title: title.value.trim(), artist: artist.value.trim(), url: url.value.trim() || null,
    artworkUrl: artworkUrl.value, previewUrl: previewUrl.value,
  }), '음악을 저장하지 못했어요.')
}

const remove = async () => {
  if (busy.value || !await useDialogStore().confirm({ title: '요즘 듣는 음악을 지울까요?', okText: '지우기', danger: true })) return
  run(() => deleteProfileMusic(), '음악을 지우지 못했어요.')
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 420px; max-height: calc(100dvh - 32px); overflow-y: auto; box-sizing: border-box;
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 24px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; }
.sub { margin: 0 0 16px; font-size: 0.8rem; color: var(--c-muted); }

.search { margin-bottom: 14px; }
.search-box {
  display: flex; align-items: center; gap: 8px; padding: 0 14px; height: 44px;
  border: 1px solid var(--c-line); border-radius: 12px; background: var(--c-bg); color: var(--c-faint);
}
.search-box:focus-within { border-color: var(--c-primary); background: #fff; }
.search-box input { flex: 1; min-width: 0; border: none; outline: none; background: none; font: inherit; font-size: 16px; color: var(--c-text); }
.search-note { margin: 8px 2px 0; font-size: 0.75rem; color: var(--c-muted); }
.search-note.bad { color: var(--c-danger); }
.results { list-style: none; margin: 8px 0 0; padding: 0; max-height: 236px; overflow-y: auto; border: 1px solid var(--c-line); border-radius: 12px; }
.results li { display: flex; align-items: center; gap: 8px; padding-right: 10px; }
.results li + li { border-top: 1px solid var(--c-line-soft); }
.result {
  flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; padding: 8px 10px;
  border: none; background: none; font: inherit; text-align: left; cursor: pointer; color: var(--c-text);
}
.result:hover { background: var(--c-line-soft); }
.result.picked { background: var(--c-primary-soft); }
.cover { flex: none; width: 40px; height: 40px; border-radius: 8px; object-fit: cover; background: var(--c-line-soft); }
.cover.empty { display: flex; align-items: center; justify-content: center; color: var(--c-faint); }
.r-text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.r-text strong, .r-text span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.r-text strong { font-size: 0.86rem; font-weight: 800; }
.r-text span { font-size: 0.74rem; color: var(--c-muted); }
.picked-card {
  display: flex; align-items: center; gap: 10px; margin-bottom: 14px; padding: 10px 12px;
  border: 1px solid var(--c-primary-line); border-radius: 14px; background: var(--c-primary-soft);
}
.unpick { flex: none; display: flex; padding: 6px; border: none; border-radius: 8px; background: none; color: var(--c-muted); cursor: pointer; }
.unpick:hover { background: color-mix(in srgb, var(--c-primary) 12%, transparent); }
.manual-toggle {
  display: inline-flex; align-items: center; gap: 4px; padding: 4px 2px; border: none; background: none;
  font: inherit; font-size: 0.8rem; font-weight: 800; color: var(--c-text-2); cursor: pointer;
}
.manual-toggle:hover:not(:disabled) { color: var(--c-primary-text); }
.chevron { transition: transform 0.2s; }
.chevron.open { transform: rotate(180deg); }
.manual { margin-top: 8px; }
.manual-label { margin: 0 0 8px; font-size: 0.72rem; font-weight: 700; color: var(--c-faint); }

.field { display: flex; flex-direction: column; gap: 6px; margin-top: 12px; }
.manual-label + .field { margin-top: 0; }
.field span { font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); }
.field em { font-style: normal; font-weight: 700; color: var(--c-faint); margin-left: 4px; }
.field input {
  border: 1px solid var(--c-line); border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: var(--c-bg);
}
.field input:focus { border-color: var(--c-primary); background: #fff; }
.hint { margin: 6px 0 0; font-size: 0.72rem; font-weight: 700; line-height: 1.5; }
.hint.bad { color: #e8590c; }
.error { margin: 12px 0 0; font-size: 0.8rem; font-weight: 700; line-height: 1.5; color: var(--c-danger); }
.actions { display: flex; align-items: center; gap: 8px; margin-top: 20px; }
.spacer { flex: 1; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
/* 휴대폰: 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .modal {
    width: 100%; max-width: none; max-height: 92dvh;
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    padding-bottom: calc(24px + env(safe-area-inset-bottom));
  }
  .field input { font-size: 16px; }
}
</style>
