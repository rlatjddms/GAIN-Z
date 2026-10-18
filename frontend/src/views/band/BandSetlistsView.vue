<template>
  <SubPageLayout pill="BAND">
    <p v-if="error" role="alert">{{ error }} <button class="btn" @click="loadBand">다시 불러오기</button></p>
    <p v-else-if="!band" class="muted">셋리스트를 불러오는 중…</p>
    <div v-else class="page">
      <header class="page-head"><h1>BAND SETLIST</h1><p>{{ band.name }} · 함께 준비할 곡과 합주 메모를 모아 보세요.</p></header>
      <p class="notice">이 브라우저에 저장돼요. 밴드 멤버와의 공유는 아직 지원하지 않아요.</p>
      <p v-if="saveError" class="save-error" role="alert">{{ saveError }}</p>
      <p v-if="notice" class="status" role="status">{{ notice }}</p>
      <div class="workspace" :inert="busy" :aria-busy="busy">
        <WsCard :title="`셋리스트 ${lists.length}`" class="sidebar">
            <button class="btn add-list" :disabled="busy" @click="openCreate">+ 셋리스트 만들기</button>
            <div class="list-tools"><button class="text-btn" :aria-pressed="favoritesOnly" @click="favoritesOnly = !favoritesOnly">{{ favoritesOnly ? '★ 즐겨찾기' : '☆ 즐겨찾기' }}</button><button class="text-btn" @click="selecting = !selecting; checkedIds = []">{{ selecting ? '선택 취소' : '선택' }}</button></div>
            <div v-if="selecting" class="bulk-tools"><span>{{ checkedIds.length }}개 선택</span><button class="text-btn danger" :disabled="!checkedIds.length || busy" @click="askDelete(checkedIds)">선택 삭제</button></div>
            <SmoothReorder v-if="visibleLists.length" :items="visibleLists" :disabled="busy || selecting || favoritesOnly" class="list-nav" @reorder="reorderLists">
              <template #default="{ item: list, start, move }">
                <div class="list-entry" :class="{ active: selectedId === list.id }">
                  <input v-if="selecting" v-model="checkedIds" type="checkbox" :value="list.id" :aria-label="`${list.title} 선택`">
                  <button v-else-if="!favoritesOnly" class="drag-handle list-handle" :disabled="busy" :aria-label="`${list.title} 셋리스트 순서 변경`" @pointerdown="start" @keydown.up.prevent="move(-1)" @keydown.down.prevent="move(1)">⠿</button>
                  <button class="list-link" :aria-pressed="selectedId === list.id" @click="selectList(list.id)"><strong>{{ list.title }}</strong><span>{{ list.songs.length }}곡</span></button>
                  <button class="star" :class="{ on: list.favorite }" :disabled="busy" :aria-pressed="list.favorite" :aria-label="`${list.title} 즐겨찾기`" @click="toggleFavorite(list)">{{ list.favorite ? '★' : '☆' }}</button>
                </div>
              </template>
            </SmoothReorder>
            <p v-else class="muted">{{ favoritesOnly ? '즐겨찾기한 셋리스트가 없어요.' : '아직 셋리스트가 없어요.' }}</p>
        </WsCard>
        <WsCard :title="current?.title || '합주할 곡'" class="detail">
          <template v-if="selected" #actions>
            <button class="btn" :disabled="busy" @click="addSong">+ 곡 추가</button>
            <button class="btn" :disabled="busy" @click="openRename">이름 변경</button>
            <button class="btn danger" :disabled="busy" @click="askDelete([selected.id])">삭제</button>
          </template>
          <template v-if="current">
            <div class="summary"><span>총 <b>{{ selected.songs.length }}</b>곡 <em v-if="newSongCount">작성 중 {{ newSongCount }}곡</em></span><button class="text-btn" @click="toggleAllSongs">{{ allExpanded ? '모두 접기' : '모두 펼치기' }}</button></div>
            <SmoothReorder v-if="current.songs.length" :items="current.songs" :disabled="busy" class="songs" @reorder="reorderSongs">
              <template #default="{ item: song, index, start, move }">
                <article class="song-card" :class="{ 'song-editing': drafts[song.id] }">
                  <div class="song-head">
                    <button class="drag-handle" :aria-label="`${song.title || '새 곡'} 순서 변경`" title="끌어서 이동 · 위아래 방향키로 이동" @pointerdown="start" @keydown.up.prevent="move(-1)" @keydown.down.prevent="move(1)">⠿</button>
                    <span class="number">{{ String(index + 1).padStart(2, '0') }}</span>
                    <button class="song-toggle" :aria-expanded="expandedIds.includes(song.id)" :aria-controls="`song-${song.id}`" @click="toggleSong(song.id)"><span class="song-name"><strong>{{ song.title || '새 곡' }}</strong><span>{{ drafts[song.id] ? '작성 중' : (song.artist || '아티스트 미입력') }}</span></span><svg class="chevron" :class="{ open: expandedIds.includes(song.id) }" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 9 7 7 7-7" /></svg></button>
                    <div class="song-actions">
                      <template v-if="drafts[song.id]"><button class="text-btn" @click="cancelSong(song.id)">취소</button><button class="btn primary" @click="saveSong(song)">저장</button></template>
                      <template v-else><button class="text-btn" :aria-label="`${song.title} 수정`" @click="editSong(song)">수정</button><button class="text-btn danger" :aria-label="`${song.title} 삭제`" @click="removeSong(song)">삭제</button></template>
                    </div>
                  </div>
                  <div class="song-collapse" :class="{ expanded: expandedIds.includes(song.id) }" :inert="!expandedIds.includes(song.id)"><div class="collapse-inner"><div :id="`song-${song.id}`" class="song-body">
                    <template v-if="drafts[song.id]">
                      <div class="song-fields"><label>곡 제목<input v-model="song.title" maxlength="120" placeholder="곡 제목을 적어주세요" @keydown.enter.prevent="saveSong(song)"></label><label>아티스트<input v-model="song.artist" maxlength="120" placeholder="아티스트 이름을 적어주세요" @keydown.enter.prevent="saveSong(song)"></label></div>
                      <label class="sheet-field">보관함 악보 연결<select :value="song.sheetId ?? ''" :disabled="sheetsLoading" @change="linkSheet(song, $event.target.value)"><option value="">{{ sheetsLoading ? '악보를 불러오는 중…' : sheetsError || '연결하지 않음' }}</option><option v-for="sheet in sheetOptions(song)" :key="sheet.id" :value="sheet.id">{{ sheet.title }}{{ sheet.artist ? ` · ${sheet.artist}` : '' }}</option></select></label>
                      <fieldset class="participants"><legend>함께 준비할 멤버 <span>{{ song.memberIds.length }}명</span></legend><div class="member-options"><button v-for="member in band.members" :key="member.memberId" type="button" class="member-option" :class="{ chosen: song.memberIds.includes(member.memberId) }" :aria-pressed="song.memberIds.includes(member.memberId)" @click="toggleMember(song, member.memberId)"><span>{{ member.character?.nickname || '멤버' }}</span><small>{{ partLabel(member.part) }}</small><span v-if="song.memberIds.includes(member.memberId)" aria-hidden="true">✓</span></button></div></fieldset>
                      <label class="memo">합주 메모<textarea v-model="song.memo" rows="2" maxlength="500" placeholder="합주 메모를 적어주세요"></textarea></label>
                    </template>
                    <template v-else>
                      <div class="read-section"><h4>연결한 악보</h4><router-link v-if="song.sheetId" class="sheet-link" :to="`/sheets/${song.sheetId}`">{{ song.sheetTitle || '악보' }} 보기 →</router-link><p v-else class="muted">연결한 악보가 없어요.</p></div>
                      <div class="read-section"><h4>함께 준비할 멤버</h4><div v-if="song.memberIds.length" class="member-options"><span v-for="id in song.memberIds" :key="id" class="member-tag">{{ memberName(id) }}<small>{{ memberPart(id) }}</small></span></div><p v-else class="muted">선택한 멤버가 없어요.</p></div>
                      <div class="read-section"><h4>합주 메모</h4><p class="read-memo" :class="{ muted: !song.memo }">{{ song.memo || '등록된 메모가 없어요.' }}</p></div>
                    </template>
                    <div class="attachments"><div class="attachment-head"><span>첨부파일 <small>{{ song.files.length }}</small></span><label v-if="drafts[song.id]" class="btn file-picker">+ 파일 첨부<input type="file" multiple accept="image/*,video/*,audio/*,.pdf,.musicxml,.mxl,.gp,.gp3,.gp4,.gp5,.gpx,.gpif" :aria-label="`${song.title || '새 곡'} 파일 첨부`" @change="attachFiles(song, $event)"></label></div><p v-if="!song.files.length" class="file-hint">{{ drafts[song.id] ? '악보, 영상, 음원 파일을 첨부할 수 있어요.' : '첨부된 파일이 없어요.' }}</p><ul v-else class="file-list"><li v-for="file in song.files" :key="file.id"><a :href="file.url" :download="file.name">{{ file.name }} <small>{{ formatSize(file.size) }}</small></a><button v-if="drafts[song.id]" class="remove-file" :aria-label="`${file.name} 첨부 해제`" @click="song.files = song.files.filter(item => item.id !== file.id)">×</button></li></ul></div>
                  </div></div></div>
                </article>
              </template>
            </SmoothReorder>
            <div v-else class="empty"><span class="empty-icon">🎶</span><strong>아직 등록한 곡이 없어요</strong><p>곡을 추가하고 곡마다 내용을 저장해 보세요.</p><button class="btn" @click="addSong">+ 첫 곡 추가</button></div>
          </template>

          <div v-else class="empty"><span class="empty-icon">🎼</span><strong>우리 밴드의 다음 합주를 준비해요</strong><p>왼쪽 목록에서 셋리스트를 만들어 주세요.</p></div>
        </WsCard>
      </div>
    </div>
    <Teleport to="body">
      <dialog ref="createDialog" class="setlist-dialog" aria-labelledby="create-title" @click="closeOnBackdrop($event, createDialog)" @close="listTitle = ''"><form @submit.prevent="submitList"><div class="dialog-head"><h2 id="create-title">{{ renaming ? '셋리스트 이름 변경' : '새 셋리스트' }}</h2><button type="button" class="close-btn" aria-label="닫기" @click="createDialog.close()">×</button></div><label>셋리스트 이름<input v-model="listTitle" autofocus required maxlength="80" placeholder="셋리스트 이름을 적어주세요"></label><div class="form-actions"><button type="button" class="btn" @click="createDialog.close()">취소</button><button class="btn primary" :disabled="!listTitle.trim() || busy">{{ renaming ? '저장' : '만들기' }}</button></div></form></dialog>
      <dialog ref="deleteDialog" class="setlist-dialog" aria-labelledby="delete-title" @click="closeOnBackdrop($event, deleteDialog)"><h2 id="delete-title">셋리스트 {{ deleteIds.length }}개를 삭제할까요?</h2><p class="delete-description">선택한 셋리스트의 곡 목록과 메모, 첨부파일이 함께 삭제돼요.</p><div class="form-actions"><button class="btn" autofocus :disabled="busy" @click="deleteDialog.close()">취소</button><button class="btn danger" :disabled="busy" @click="deleteLists">삭제</button></div></dialog>
    </Teleport>
  </SubPageLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import SmoothReorder from '../../components/band/SmoothReorder.vue'
import { getBand } from '../../api/band'
import { readLocalSetlists, writeLocalSetlists } from '../../api/setlistLocal'
import { getWorkspace } from '../../api/sheet'
import { apiErrorMessage } from '../../api/practice'
import { useAuthStore } from '../../stores/auth'
import { useBandStore } from '../../stores/band'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
const route = useRoute()
const store = useBandStore()
const auth = useAuthStore()
const band = ref(null), error = ref(''), saveError = ref(''), notice = ref(''), busy = ref(false)
const lists = ref([]), selectedId = ref(null), drafts = ref({}), songOrder = ref([])
const listTitle = ref(''), renaming = ref(false)
const createDialog = ref(null), deleteDialog = ref(null), deleteIds = ref([])
const expandedIds = ref([]), favoritesOnly = ref(false), selecting = ref(false), checkedIds = ref([])
const sheets = ref(null), sheetsLoading = ref(false), sheetsError = ref('')
const urls = new Set()
let request = 0
let storageKey = ''
const selected = computed(() => lists.value.find(list => list.id === selectedId.value))
// 저장한 곡에 곡별 초안을 겹쳐 표시해 다른 곡을 저장해도 작성 중인 내용을 유지한다.
const current = computed(() => {
  if (!selected.value) return null
  const savedIds = new Set(selected.value.songs.map(song => song.id))
  const songs = [...selected.value.songs.map(song => drafts.value[song.id] || song), ...Object.values(drafts.value).filter(song => !savedIds.has(song.id))]
  const byId = new Map(songs.map(song => [song.id, song]))
  return { ...selected.value, songs: [...songOrder.value.filter(id => byId.has(id)).map(id => byId.get(id)), ...songs.filter(song => !songOrder.value.includes(song.id))] }
})
const newSongCount = computed(() => Object.keys(drafts.value).filter(id => !selected.value?.songs.some(song => song.id === id)).length)
const visibleLists = computed(() => lists.value.filter(list => !favoritesOnly.value || list.favorite))
const allExpanded = computed(() => current.value?.songs.length && current.value.songs.every(song => expandedIds.value.includes(song.id)))
const cloneSong = song => ({ ...song, memberIds: [...song.memberIds], files: song.files.map(file => ({ ...file })) })
const serializeSong = song => ({ id: song.id, title: song.title, artist: song.artist, memo: song.memo, sheetId: song.sheetId ?? null, sheetTitle: song.sheetTitle ?? '', memberIds: [...song.memberIds], files: song.files.map(file => ({ id: file.id, name: file.name, size: file.size, blob: file.blob })) })
const serializable = list => ({ id: list.id, title: list.title, favorite: list.favorite, songs: list.songs.map(serializeSong) })
const songJson = song => JSON.stringify(serializeSong(song), (key, value) => key === 'blob' ? undefined : value)
const dirty = computed(() => Object.keys(drafts.value).length > 0)
const partLabel = part => INSTRUMENT_OPTIONS.find(option => option.value === part)?.label || part
const memberName = id => band.value.members.find(member => member.memberId === id)?.character?.nickname || '탈퇴한 멤버'
const memberPart = id => partLabel(band.value.members.find(member => member.memberId === id)?.part) || ''
// 보관함 악보는 곡을 처음 편집할 때 한 번만 불러온다.
async function loadSheets() {
  if (sheets.value || sheetsLoading.value) return
  sheetsLoading.value = true
  sheetsError.value = ''
  try { sheets.value = await getWorkspace({ sort: 'title' }) }
  catch { sheetsError.value = '악보를 불러오지 못했어요' }
  finally { sheetsLoading.value = false }
}
// 보관함에서 빠진 악보도 이미 연결돼 있으면 선택지에 남긴다.
function sheetOptions(song) {
  const options = sheets.value || []
  return song.sheetId && !options.some(sheet => sheet.id === song.sheetId) ? [{ id: song.sheetId, title: song.sheetTitle || '연결한 악보' }, ...options] : options
}
function linkSheet(song, value) {
  const sheet = sheets.value?.find(item => item.id === Number(value))
  song.sheetId = sheet ? sheet.id : null
  song.sheetTitle = sheet ? sheet.title : ''
  if (sheet && !song.title.trim()) song.title = sheet.title
  if (sheet && !song.artist.trim()) song.artist = sheet.artist || ''
}
function objectUrl(blob) { const url = URL.createObjectURL(blob); urls.add(url); return url }
function cleanupUrls() {
  const used = new Set([...lists.value.flatMap(list => list.songs), ...Object.values(drafts.value)].flatMap(song => song.files.map(file => file.url)))
  for (const url of urls) if (!used.has(url)) { URL.revokeObjectURL(url); urls.delete(url) }
}
async function loadBand() {
  const token = ++request
  band.value = null
  error.value = ''
  try {
    const [result, profile] = await Promise.all([getBand(Number(route.params.id)), auth.profile ? Promise.resolve(auth.profile) : auth.fetchProfile()])
    if (!profile?.id) throw new Error('계정 정보를 불러오지 못했어요.')
    const key = `${profile.id}:${result.id}`
    const saved = await readLocalSetlists(key)
    if (token !== request) return
    storageKey = key
    lists.value = saved.map(list => ({ ...list, songs: list.songs.map(song => ({ ...song, files: song.files.map(file => ({ ...file, url: objectUrl(file.blob) })) })) }))
    selectedId.value = lists.value[0]?.id ?? null
    band.value = result
    store.upsertBand(result, true)
    store.isBandMode = true
  } catch (e) { if (token === request) error.value = apiErrorMessage(e, '셋리스트를 불러오지 못했어요.') }
}
async function persist(next) {
  if (busy.value) return false
  busy.value = true
  saveError.value = ''
  try { await writeLocalSetlists(storageKey, next.map(serializable)); lists.value = next; return true }
  catch { saveError.value = '저장하지 못했어요. 브라우저 저장 공간과 설정을 확인하고 다시 시도해 주세요.'; return false }
  finally { busy.value = false }
}
function discardAllowed() { return !dirty.value || window.confirm('저장하지 않은 곡이 있어요. 변경사항을 버릴까요?') }
function resetEditor() { drafts.value = {}; songOrder.value = []; expandedIds.value = []; cleanupUrls() }
function selectList(id) { if (id === selectedId.value || busy.value || !discardAllowed()) return; resetEditor(); selectedId.value = id; notice.value = saveError.value = '' }
function addSong() {
  if (!selected.value || busy.value) return
  const song = { id: crypto.randomUUID(), title: '', artist: '', memo: '', sheetId: null, sheetTitle: '', memberIds: [], files: [] }
  songOrder.value = [...current.value.songs.map(item => item.id), song.id]
  drafts.value[song.id] = song
  expandedIds.value.push(song.id)
  notice.value = saveError.value = ''
  loadSheets()
}
function editSong(song) { drafts.value[song.id] = cloneSong(song); if (!expandedIds.value.includes(song.id)) expandedIds.value.push(song.id); notice.value = saveError.value = ''; loadSheets() }
function cancelSong(id) {
  const saved = selected.value.songs.find(song => song.id === id)
  const draft = drafts.value[id]
  const changed = saved ? songJson(draft) !== songJson(saved) : draft.title || draft.artist || draft.memo || draft.sheetId || draft.memberIds.length || draft.files.length
  if (changed && !window.confirm('이 곡의 저장하지 않은 변경사항을 버릴까요?')) return
  delete drafts.value[id]
  if (!saved) { songOrder.value = songOrder.value.filter(item => item !== id); expandedIds.value = expandedIds.value.filter(item => item !== id) }
  saveError.value = ''; cleanupUrls()
}
async function saveSong(song) {
  if (!song.title.trim()) { saveError.value = '곡 제목을 적어주세요.'; return }
  const savedSong = cloneSong(song)
  savedSong.title = savedSong.title.trim()
  savedSong.artist = savedSong.artist.trim()
  // 이 곡만 저장하고, 다른 곡의 작성 중인 값은 브라우저 저장소에 섞지 않는다.
  const savedById = new Map(selected.value.songs.map(item => [item.id, item]))
  savedById.set(song.id, savedSong)
  const songs = current.value.songs.map(item => savedById.get(item.id)).filter(Boolean)
  if (await persist(lists.value.map(list => list.id === selectedId.value ? { ...list, songs } : list))) {
    delete drafts.value[song.id]
    cleanupUrls()
    notice.value = `${savedSong.title} 곡을 저장했어요.`
  }
}
async function removeSong(song) {
  if (await persist(lists.value.map(list => list.id === selectedId.value ? { ...list, songs: list.songs.filter(item => item.id !== song.id) } : list))) {
    songOrder.value = songOrder.value.filter(id => id !== song.id)
    expandedIds.value = expandedIds.value.filter(id => id !== song.id)
    cleanupUrls()
  }
}
function openCreate() { renaming.value = false; listTitle.value = ''; createDialog.value.showModal() }
function openRename() { renaming.value = true; listTitle.value = selected.value.title; createDialog.value.showModal() }
function closeOnBackdrop(event, dialog) {
  if (event.target !== dialog) return
  const r = dialog.getBoundingClientRect()
  if (event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom) dialog.close()
}
async function submitList() {
  if (!listTitle.value.trim()) return
  if (renaming.value) {
    if (await persist(lists.value.map(list => list.id === selectedId.value ? { ...list, title: listTitle.value.trim() } : list))) createDialog.value.close()
    return
  }
  if (!discardAllowed()) return
  const list = { id: crypto.randomUUID(), title: listTitle.value.trim(), favorite: false, songs: [] }
  if (await persist([...lists.value, list])) { resetEditor(); favoritesOnly.value = false; selectedId.value = list.id; createDialog.value.close() }
}
function askDelete(ids) { if (busy.value) return; deleteIds.value = [...ids]; deleteDialog.value.showModal() }
async function deleteLists() {
  if (deleteIds.value.includes(selectedId.value) && !discardAllowed()) return
  if (await persist(lists.value.filter(list => !deleteIds.value.includes(list.id)))) {
    if (deleteIds.value.includes(selectedId.value)) { resetEditor(); selectedId.value = lists.value[0]?.id ?? null }
    checkedIds.value = []; deleteDialog.value.close(); cleanupUrls()
  }
}
async function toggleFavorite(list) { await persist(lists.value.map(item => item.id === list.id ? { ...item, favorite: !item.favorite } : item)) }
async function reorderLists(ids) { await persist(ids.map(id => lists.value.find(list => list.id === id))) }
async function reorderSongs(ids) {
  const savedById = new Map(selected.value.songs.map(song => [song.id, song]))
  const songs = ids.map(id => savedById.get(id)).filter(Boolean)
  if (await persist(lists.value.map(list => list.id === selectedId.value ? { ...list, songs } : list))) songOrder.value = ids
}
function toggleSong(id) { expandedIds.value = expandedIds.value.includes(id) ? expandedIds.value.filter(item => item !== id) : [...expandedIds.value, id] }
function toggleAllSongs() { expandedIds.value = allExpanded.value ? [] : current.value.songs.map(song => song.id) }
function toggleMember(song, id) { song.memberIds = song.memberIds.includes(id) ? song.memberIds.filter(memberId => memberId !== id) : [...song.memberIds, id] }
function attachFiles(song, event) { for (const file of event.target.files) song.files.push({ id: crypto.randomUUID(), name: file.name, size: file.size, blob: file, url: objectUrl(file) }); event.target.value = '' }
const formatSize = size => size < 1024 * 1024 ? `${Math.max(1, Math.round(size / 1024))} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`
function beforeUnload(event) { if (dirty.value || busy.value) { event.preventDefault(); event.returnValue = '' } }
onBeforeRouteLeave(() => !busy.value && discardAllowed())
onBeforeRouteUpdate(() => !busy.value && discardAllowed())
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
watch(() => route.params.id, () => { resetEditor(); lists.value = []; selectedId.value = null; cleanupUrls(); checkedIds.value = []; saveError.value = notice.value = ''; createDialog.value?.close(); deleteDialog.value?.close(); loadBand() }, { immediate: true })
onBeforeUnmount(() => { request++; window.removeEventListener('beforeunload', beforeUnload); urls.forEach(url => URL.revokeObjectURL(url)) })
</script>


<style scoped>
.page,.page *,.setlist-dialog,.setlist-dialog * { box-sizing:border-box; }
.page { display:flex; flex-direction:column; gap:16px; }
.page-head h1 { margin:0; font-size:1.3rem; font-weight:900; }
.page-head p,.muted,.empty p { color:#868e96; font-size:.8rem; line-height:1.7; }
.page-head p { margin:8px 0 0; }
.notice { margin:0; color:#868e96; font-size:.75rem; line-height:1.6; }
.save-error,.status { margin:0; padding:10px 14px; border-radius:10px; font-size:.8rem; background:#f0f4ff; color:#495057; }
.save-error { color:#c92a2a; background:#fff5f5; }
.workspace { display:grid; grid-template-columns:250px minmax(0,1fr); gap:20px; align-items:start; }
.sidebar,.detail { min-width:0; }
.sidebar :deep(.ws-card-head) { gap:4px; }
.detail :deep(.ws-card-head) { align-items:center; flex-wrap:wrap; }
.detail :deep(.ws-card-head h3) { min-width:0; overflow-wrap:anywhere; flex:1 1 160px; }
.detail :deep(.actions) { flex-wrap:wrap; gap:8px; }
.btn { display:inline-flex; justify-content:center; align-items:center; min-height:34px; border:1px solid #e9ecef; background:#fff; color:#495057; font-family:inherit; font-size:.76rem; font-weight:700; line-height:1.4; padding:8px 12px; border-radius:9px; cursor:pointer; text-align:center; }
.btn.primary { background:#2d5bff; color:#fff; border-color:#2d5bff; }
.btn:hover:not(:disabled) { background:#f8f9fa; }
.btn.primary:hover:not(:disabled) { background:#244de0; }
button:disabled { opacity:.4; cursor:default; }
.danger { color:#e03131; }
button:focus-visible,a:focus-visible { outline:2px solid #adb5bd; outline-offset:2px; }
.add-list { width:100%; margin-bottom:12px; color:#495057; background:#f8f9fa; border-style:dashed; }
.list-tools,.bulk-tools { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:12px; font-size:.73rem; color:#868e96; }
.text-btn { border:0; background:transparent; padding:4px 0; font-family:inherit; font-size:.74rem; cursor:pointer; }
.list-nav { gap:8px; }
.list-entry { display:flex; align-items:center; gap:5px; min-width:0; padding:8px 7px; border:1px solid #e9ecef; border-radius:12px; background:#fff; }
.list-entry.active { background:#f8faff; border-color:#dce3f5; }
.list-link { display:flex; flex-direction:column; flex:1; min-width:0; gap:6px; text-align:left; border:0; background:transparent; font-family:inherit; padding:4px 0; cursor:pointer; overflow-wrap:anywhere; color:#343a40; }
.list-link strong { font-size:.79rem; }
.list-link span { color:#868e96; font-size:.7rem; }
.star { background:transparent; border:0; color:#ced4da; font-size:20px; cursor:pointer; padding:3px; }
.star.on { color:#fab005; }
.summary { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:8px 16px; padding:10px 12px; background:#f8f9fa; border-radius:10px; color:#868e96; font-size:.75rem; }
.summary b { color:#495057; }
.summary em { margin-left:8px; color:#868e96; font-size:.7rem; font-style:normal; }
.empty { display:flex; flex-direction:column; align-items:center; text-align:center; padding:36px 14px; margin-top:16px; border:1px dashed #e9ecef; border-radius:14px; gap:8px; }
.empty-icon { font-size:2rem; margin-bottom:8px; }
.empty strong { font-size:.9rem; color:#495057; }
.empty p { margin:0 0 8px; }
.edit-toolbar { display:flex; gap:16px; align-items:flex-end; flex-wrap:wrap; margin-top:18px; }
.title-field { flex:1; max-width:340px; min-width:160px; }
.song-form,.song-fields { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; max-width:480px; }
.song-form { margin-top:16px; padding:16px; background:#f8f9fa; border-radius:12px; }
label { display:flex; flex-direction:column; gap:7px; min-width:0; font-size:.75rem; font-weight:500; color:#868e96; }
input:not([type=checkbox]):not([type=file]),textarea { display:block; width:100%; min-width:0; max-width:100%; border:1px solid #e9ecef; padding:8px 10px; border-radius:8px; background:#fff; color:#343a40; font-family:inherit; font-size:.8rem; font-weight:400; line-height:1.6; outline:none; box-shadow:none; }
input:not([type=checkbox]):not([type=file]):focus,textarea:focus { border-color:#adb5bd; outline:none; box-shadow:none; }
input::placeholder,textarea::placeholder { color:#adb5bd; opacity:1; font-weight:400; }
textarea { resize:vertical; min-height:64px; }
.memo,.sheet-field { max-width:480px; }
select { width:100%; min-width:0; border:1px solid #e9ecef; padding:8px 10px; border-radius:8px; background:#fff; color:#343a40; font-family:inherit; font-size:.8rem; line-height:1.6; outline:none; }
select:focus { border-color:#adb5bd; }
.sheet-link { color:#2d5bff; font-size:.8rem; font-weight:700; text-decoration:none; overflow-wrap:anywhere; }
.form-actions { display:flex; justify-content:flex-end; flex-wrap:wrap; gap:8px; grid-column:1 / -1; }
.songs { margin-top:16px; }
.song-card { border:1px solid #e9ecef; border-radius:12px; background:#fff; }
.song-editing { border-color:#d3d9e0; }
.song-actions { display:flex; align-items:center; gap:10px; flex-shrink:0; }
.song-head { display:flex; align-items:center; gap:10px; padding:12px 14px; }
.drag-handle { border:0; background:transparent; color:#adb5bd; border-radius:7px; width:26px; height:32px; flex-shrink:0; font-size:22px; cursor:grab; touch-action:none; user-select:none; }
.drag-handle:active { cursor:grabbing; }
.list-handle { width:18px; font-size:18px; padding:0; }
.number { font-size:.8rem; color:#868e96; font-weight:700; }
.song-toggle { display:flex; flex:1; min-width:0; align-items:center; justify-content:space-between; gap:10px; border:0; background:transparent; text-align:left; font-family:inherit; cursor:pointer; padding:0; color:#343a40; }
.song-name { display:flex; flex-direction:column; gap:4px; flex:1; min-width:0; overflow-wrap:anywhere; }
.song-name strong { font-size:.86rem; }
.song-name>span { font-size:.72rem; color:#868e96; }
.chevron { flex-shrink:0; color:#868e96; transition:transform .2s ease; }
.chevron.open { transform:rotate(180deg); }
.song-collapse { display:grid; grid-template-rows:0fr; transition:grid-template-rows .22s ease; }
.song-collapse.expanded { grid-template-rows:1fr; }
.collapse-inner { overflow:hidden; min-height:0; }
.song-body { border-top:1px solid #f1f3f5; padding:16px; display:flex; flex-direction:column; gap:18px; }
.participants { min-width:0; border:0; margin:0; padding:0; }
legend,.read-section h4 { padding:0; margin:0 0 10px; font-size:.75rem; font-weight:600; color:#868e96; }
legend span { margin-left:6px; font-weight:400; }
.member-options { display:flex; flex-wrap:wrap; gap:7px; }
.member-option,.member-tag { display:inline-flex; align-items:center; gap:7px; max-width:100%; padding:7px 10px; border:1px solid #e9ecef; border-radius:20px; font-family:inherit; font-size:.74rem; background:#fff; color:#495057; line-height:1.5; overflow-wrap:anywhere; }
.member-option { cursor:pointer; }
.member-option.chosen { border-color:#c9d6ff; background:#f0f4ff; color:#365bb8; }
.member-option small,.member-tag small { color:#adb5bd; font-size:.66rem; }
.member-tag { background:#f8f9fa; border:0; }
.read-section p { margin:0; }
.read-memo { white-space:pre-wrap; overflow-wrap:anywhere; line-height:1.8; font-size:.82rem; color:#495057; }
.attachment-head { display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap; font-size:.75rem; color:#868e96; }
.attachment-head small { margin-left:4px; }
.file-picker { position:relative; overflow:hidden; }
.file-picker input { position:absolute; inset:0; width:100%; height:100%; opacity:0; cursor:pointer; }
.file-picker:focus-within { outline:1px solid #adb5bd; }
.file-hint { font-size:.73rem; color:#adb5bd; line-height:1.6; margin:10px 0 0; }
.file-list { list-style:none; padding:0; margin:10px 0 0; display:flex; flex-direction:column; gap:6px; }
.file-list li { display:flex; align-items:center; gap:10px; padding:8px 10px; background:#f8f9fa; border-radius:8px; }
.file-list a { flex:1; min-width:0; overflow-wrap:anywhere; color:#495057; font-size:.76rem; text-decoration:none; line-height:1.6; }
.file-list small { color:#adb5bd; margin-left:6px; }
.remove-file,.close-btn { border:0; background:transparent; color:#868e96; font-size:22px; cursor:pointer; min-width:28px; min-height:28px; }
.setlist-dialog { width:calc(100% - 32px); max-width:400px; max-height:calc(100dvh - 48px); overflow-y:auto; padding:24px; border:1px solid #e9ecef; border-radius:18px; color:#343a40; font-family:'Pretendard',sans-serif; box-shadow:0 20px 80px #0002; }
.setlist-dialog::backdrop { background:rgba(20,28,40,.4); }
.dialog-head { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:20px; }
.setlist-dialog h2 { font-size:1rem; margin:0; }
.setlist-dialog .form-actions { margin-top:22px; }
.delete-description { font-size:.82rem; line-height:1.7; color:#868e96; }
@media(max-width:760px) { .workspace { grid-template-columns:1fr; } }
@media(max-width:480px) { .song-form,.song-fields { grid-template-columns:1fr; } .song-head { padding:12px 10px; gap:7px; } .detail :deep(.actions) { gap:6px; } .song-body { padding:14px; } }
@media(prefers-reduced-motion:reduce) { .song-collapse,.chevron { transition:none; } }
</style>
