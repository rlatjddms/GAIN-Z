<template>
  <SubPageLayout pill="BAND" :back-to="{ path: '/home', query: { band: $route.params.id } }" back-label="합주실"
                 :title="band ? 'BAND SETLIST' : ''" name="셋리스트" :subtitle="band ? `${band.name} · 함께 준비할 곡과 합주 메모를 모아요` : ''">
    <p v-if="error" role="alert">{{ error }} <button class="btn" @click="loadBand">다시 불러오기</button></p>
    <p v-else-if="!band" class="muted">셋리스트를 불러오는 중…</p>
    <div v-else class="page">
      <div class="workspace" :inert="busy" :aria-busy="busy">
        <WsCard :title="`셋리스트 ${lists.length}`" class="sidebar" :class="{ collapsed: fold.collapsed.value }">
            <!-- 추가는 제목 줄에 두어 목록을 접어도 보인다 (#293). 휴대폰은 목록 접기·펼치기도 (#254) -->
            <template #actions>
              <button v-if="fold.narrow.value && lists.length" class="text-btn fold" :aria-expanded="!fold.collapsed.value" @click="fold.toggle">{{ fold.collapsed.value ? '펼치기' : '접기' }}<ChevronDown class="chevron" :class="{ open: !fold.collapsed.value }" :size="15" :stroke-width="2.4" aria-hidden="true" /></button>
              <button class="btn add-list" :disabled="busy" aria-label="셋리스트 추가" @click="openCreate"><Plus :size="15" :stroke-width="2.6" aria-hidden="true" />추가</button>
            </template>
            <button v-if="fold.collapsed.value" class="list-summary" @click="fold.toggle">
              <strong>{{ selected?.title || '셋리스트를 골라 주세요' }}</strong><span>셋리스트 {{ lists.length }}개 보기</span>
            </button>
            <template v-else>
            <!-- 목록 검색 (#254): 셋리스트 이름이나 안의 곡 제목 -->
            <label v-if="lists.length" class="list-search">
              <Search :size="14" :stroke-width="2.4" aria-hidden="true" />
              <input v-model="listQuery" type="search" placeholder="셋리스트·곡 이름 검색" aria-label="셋리스트 검색" maxlength="40">
            </label>
            <div class="list-tools"><button class="chip" :class="{ on: favoritesOnly }" :aria-pressed="favoritesOnly" @click="favoritesOnly = !favoritesOnly"><Star :size="14" :stroke-width="2.2" :fill="favoritesOnly ? 'currentColor' : 'none'" /> 즐겨찾기</button><button class="chip" :class="{ on: selecting }" @click="selecting = !selecting; checkedIds = []">{{ selecting ? '선택 취소' : '선택' }}</button></div>
            <div v-if="selecting" class="bulk-tools"><span>{{ checkedIds.length }}개 선택</span><button class="chip danger" :disabled="!checkedIds.length || busy" @click="askDelete(checkedIds)">선택 삭제</button></div>
            <SmoothReorder v-if="visibleLists.length" :items="visibleLists" :disabled="busy || selecting || favoritesOnly || searchingLists" class="list-nav" @reorder="reorderLists">
              <template #default="{ item: list, start, move }">
                <div class="list-entry" :class="{ active: selectedId === list.id }">
                  <input v-if="selecting" v-model="checkedIds" type="checkbox" :value="list.id" :aria-label="`${list.title} 선택`">
                  <button v-else-if="!favoritesOnly && !searchingLists" class="drag-handle list-handle" :disabled="busy" :aria-label="`${list.title} 셋리스트 순서 변경`" @pointerdown="start" @keydown.up.prevent="move(-1)" @keydown.down.prevent="move(1)">⠿</button>
                  <button class="list-link" :aria-pressed="selectedId === list.id" @click="selectList(list.id)"><strong>{{ list.title }}</strong><span>{{ list.songs.length }}곡</span></button>
                  <button v-if="!selecting" class="rename" :disabled="busy" :aria-label="`${list.title} 이름 변경`" title="이름 변경" @click="openRename(list)"><Pencil :size="14" :stroke-width="2.2" /></button>
                  <button class="star" :class="{ on: list.favorite }" :disabled="busy" :aria-pressed="list.favorite" :aria-label="`${list.title} 즐겨찾기`" @click="toggleFavorite(list)"><Star :size="16" :stroke-width="2.2" :fill="list.favorite ? 'currentColor' : 'none'" /></button>
                </div>
              </template>
            </SmoothReorder>
            <p v-else class="muted">{{ searchingLists ? `'${listQuery.trim()}' 셋리스트가 없어요.` : favoritesOnly ? '즐겨찾기한 셋리스트가 없어요.' : '아직 셋리스트가 없어요.' }}</p>
            </template>
        </WsCard>
        <WsCard :title="current?.title || '합주할 곡'" class="detail">
          <template v-if="selected" #actions>
            <button class="btn" :disabled="busy" @click="addSong">+ 곡 추가</button>
            <button class="btn" :disabled="busy" @click="openRename(selected)">이름 변경</button>
            <button class="btn danger" :disabled="busy" @click="askDelete([selected.id])">삭제</button>
          </template>
          <template v-if="current">
            <div class="summary"><span>총 <b>{{ selected.songs.length }}</b>곡 <em v-if="newSongCount">작성 중 {{ newSongCount }}곡</em></span><button class="text-btn" @click="toggleAllSongs">{{ allExpanded ? '모두 접기' : '모두 펼치기' }}</button></div>
            <SmoothReorder v-if="current.songs.length" :items="current.songs" :disabled="busy" class="songs" @reorder="reorderSongs">
              <template #default="{ item: song, index, start, move }">
                <article :id="`song-card-${song.id}`" class="song-card" :class="{ 'song-editing': drafts[song.id], focused: focusedId === song.id }">
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
                      <div class="song-fields"><label>곡 제목<input v-model="song.title" maxlength="120" placeholder="곡 제목을 적어주세요" @keydown.enter.prevent="!$event.isComposing && saveSong(song)"></label><label>아티스트<input v-model="song.artist" maxlength="120" placeholder="아티스트 이름을 적어주세요" @keydown.enter.prevent="!$event.isComposing && saveSong(song)"></label></div>
                      <label class="sheet-field">보관함 악보 연결<select :value="song.sheetId ?? ''" :disabled="sheetsLoading" @change="linkSheet(song, $event.target.value)"><option value="">{{ sheetsLoading ? '악보를 불러오는 중…' : sheetsError || '연결하지 않음' }}</option><option v-for="sheet in sheetOptions(song)" :key="sheet.id" :value="sheet.sheetId">{{ sheet.title }}{{ sheet.artist ? ` · ${sheet.artist}` : '' }}</option></select></label>
                      <fieldset class="participants"><legend>함께 준비할 멤버 <span>{{ song.memberIds.length }}명</span></legend><div class="member-options"><button v-for="member in band.members" :key="member.memberId" type="button" class="member-option" :class="{ chosen: song.memberIds.includes(member.memberId) }" :aria-pressed="song.memberIds.includes(member.memberId)" @click="toggleMember(song, member.memberId)"><span>{{ member.character?.nickname || '멤버' }}</span><small>{{ partLabel(member.part) }}</small><Check v-if="song.memberIds.includes(member.memberId)" :size="13" :stroke-width="3" aria-hidden="true" /></button></div></fieldset>
                      <label class="memo">합주 메모<textarea v-model="song.memo" rows="2" maxlength="500" placeholder="합주 메모를 적어주세요"></textarea></label>
                    </template>
                    <template v-else>
                      <div class="read-section"><h4>연결한 악보</h4><template v-if="song.sheetId"><button class="sheet-link" :disabled="busy" @click="openSheet(song)">{{ song.sheetTitle || '악보' }} 보기 <ChevronRight :size="14" :stroke-width="2.4" /></button></template><p v-else class="muted">연결한 악보가 없어요.</p></div>
                      <div class="read-section"><h4>함께 준비할 멤버</h4><div v-if="song.memberIds.length" class="member-options"><span v-for="id in song.memberIds" :key="id" class="member-tag">{{ memberName(id) }}<small>{{ memberPart(id) }}</small></span></div><p v-else class="muted">선택한 멤버가 없어요.</p></div>
                      <div class="read-section"><h4>합주 메모</h4><p class="read-memo" :class="{ muted: !song.memo }">{{ song.memo || '등록된 메모가 없어요.' }}</p></div>
                      <div v-if="jamsFor(song.id).length" class="read-section"><h4>레이어 잼</h4><div class="jam-links"><router-link v-for="jam in jamsFor(song.id)" :key="jam.id" :to="`/bands/${band.id}/jams?jam=${jam.id}`" class="btn jam-go"><Mic :size="14" :stroke-width="2.2" /> {{ jam.title }} <small>트랙 {{ jam.trackCount }}</small></router-link></div></div>
                    </template>
                    <div class="attachments"><div class="attachment-head"><span>첨부파일 <small>{{ song.files.length }}</small></span><label v-if="drafts[song.id]" class="btn file-picker">+ 파일 첨부<input type="file" multiple :accept="BAND_ATTACHMENT_ACCEPT" :aria-label="`${song.title || '새 곡'} 파일 첨부`" @change="attachFiles(song, $event)"></label></div><p v-if="!song.files.length" class="file-hint">{{ drafts[song.id] ? 'PDF 악보, 사진, 영상, 음원을 10MB까지 첨부할 수 있어요.' : '첨부된 파일이 없어요.' }}</p><ul v-else class="file-list"><li v-for="file in song.files" :key="file.id"><a :href="file.local ? file.url : '#'" :download="file.name" @click="openFile($event, file)">{{ file.name }} <small v-if="file.size">{{ formatSize(file.size) }}</small></a><button v-if="drafts[song.id]" class="remove-file" :aria-label="`${file.name} 첨부 해제`" @click="song.files = song.files.filter(item => item.id !== file.id)">×</button></li></ul></div>
                  </div></div></div>
                </article>
              </template>
            </SmoothReorder>
            <EmptyState v-else class="empty" icon="music" title="아직 등록한 곡이 없어요" text="곡을 추가하고 곡마다 내용을 저장해 보세요."><template #actions><button class="btn" @click="addSong">+ 첫 곡 추가</button></template></EmptyState>
          </template>

          <EmptyState v-else icon="music" title="우리 밴드의 다음 합주를 준비해요" text="왼쪽 목록에서 셋리스트를 만들어 주세요." />
        </WsCard>
      </div>
    </div>
    <Teleport to="body">
      <dialog ref="createDialog" class="setlist-dialog" aria-labelledby="create-title" @click="closeOnBackdrop($event, createDialog)" @close="listTitle = ''"><form @submit.prevent="submitList"><div class="dialog-head"><h2 id="create-title">{{ renaming ? '셋리스트 이름 변경' : '새 셋리스트' }}</h2><button type="button" class="close-btn" aria-label="닫기" @click="createDialog.close()">×</button></div><label>셋리스트 이름<input v-model="listTitle" autofocus required maxlength="80" placeholder="셋리스트 이름을 적어주세요"></label><div class="dialog-actions"><button type="button" class="btn" @click="createDialog.close()">취소</button><button class="btn primary" :disabled="!listTitle.trim() || busy">{{ renaming ? '저장' : '추가' }}</button></div></form></dialog>
      <dialog ref="sheetDialog" class="setlist-dialog sheet-dialog" aria-labelledby="sheet-title" @click="closeOnBackdrop($event, sheetDialog)" @close="sheetSong = null"><h2 id="sheet-title">보관함에 저장하시겠습니까?</h2><p class="delete-description">{{ sheetSong?.sheetTitle || '이 악보' }} 악보를 내 보관함에 담고 열어요. 다음부터는 바로 열려요.</p><div class="dialog-actions"><button class="btn" autofocus :disabled="busy" @click="sheetDialog.close()">아니요</button><button class="btn primary" :disabled="busy" @click="confirmAddSheet">예</button></div></dialog>
      <dialog ref="deleteDialog" class="setlist-dialog" aria-labelledby="delete-title" @click="closeOnBackdrop($event, deleteDialog)"><h2 id="delete-title">셋리스트 {{ deleteIds.length }}개를 삭제할까요?</h2><p class="delete-description">선택한 셋리스트의 곡 목록과 메모, 첨부파일이 함께 삭제돼요.</p><div class="dialog-actions"><button class="btn" autofocus :disabled="busy" @click="deleteDialog.close()">취소</button><button class="btn danger solid" :disabled="busy" @click="deleteLists">삭제</button></div></dialog>
    </Teleport>
  </SubPageLayout>
</template>

<script setup>
import { ChevronRight, ChevronDown, Star, Check, Mic, Pencil, Plus, Search } from 'lucide-vue-next'
import { useCollapsibleList } from '../../components/band/useCollapsibleList'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import SmoothReorder from '../../components/band/SmoothReorder.vue'
import { getBand } from '../../api/band'
import { BAND_ATTACHMENT_ACCEPT, BAND_ATTACHMENT_MAX_BYTES, BAND_ATTACHMENT_MAX_COUNT, loadBandAttachment } from '../../api/bandBoard'
import { listJams } from '../../api/jam'
import { createSetlist, deleteSetlist, deleteSetlistItem, listSetlists, reorderSetlistItems, reorderSetlists, saveSetlistItem, updateSetlist } from '../../api/setlist'
import { addToWorkspace, getWorkspace } from '../../api/sheet'
import { apiErrorMessage } from '../../api/practice'
import { useBandStore } from '../../stores/band'
import { useDialogStore } from '../../stores/dialog'
import { useToastStore } from '../../stores/toast'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
const route = useRoute()
const router = useRouter()
const dialog = useDialogStore()
const toast = useToastStore()
const store = useBandStore()
const band = ref(null), error = ref(''), busy = ref(false)
const lists = ref([]), selectedId = ref(null), drafts = ref({}), songOrder = ref([])
const listTitle = ref(''), renaming = ref(false)
// 이름을 바꿀 셋리스트 (목록에서 고르지 않은 셋리스트도 바꿀 수 있다, #233)
const renameId = ref(null)
const createDialog = ref(null), deleteDialog = ref(null), deleteIds = ref([])
const expandedIds = ref([]), favoritesOnly = ref(false), selecting = ref(false), checkedIds = ref([])
const sheets = ref(null), sheetsLoading = ref(false), sheetsError = ref('')
const sheetDialog = ref(null), sheetSong = ref(null)
const urls = new Set()
const jams = ref([]), focusedId = ref(null)
const jamsFor = songId => jams.value.filter(jam => jam.song?.setlistItemId === songId)
let request = 0
const bandId = () => band.value.id
const selected = computed(() => lists.value.find(list => list.id === selectedId.value))
// 저장한 곡에 곡별 초안을 겹쳐 표시해 다른 곡을 저장해도 작성 중인 내용을 유지한다.
const current = computed(() => {
  if (!selected.value) return null
  const savedIds = new Set(selected.value.songs.map(song => song.id))
  const songs = [...selected.value.songs.map(song => drafts.value[song.id] || song), ...Object.values(drafts.value).filter(song => !savedIds.has(song.id))]
  const byId = new Map(songs.map(song => [song.id, song]))
  return { ...selected.value, songs: [...songOrder.value.filter(id => byId.has(id)).map(id => byId.get(id)), ...songs.filter(song => !songOrder.value.includes(song.id))] }
})
const newSongCount = computed(() => Object.values(drafts.value).filter(draft => !selected.value?.songs.some(song => song.id === draft.id)).length)
// 셋리스트 이름이나 안의 곡 제목으로 거른다 (#254). 거르는 동안은 순서를 못 바꾼다
const listQuery = ref('')
const searchingLists = computed(() => !!listQuery.value.trim())
const visibleLists = computed(() => {
  const q = listQuery.value.trim().toLocaleLowerCase()
  return lists.value.filter(list => (!favoritesOnly.value || list.favorite)
    && (!q || list.title.toLocaleLowerCase().includes(q) || list.songs.some(song => (song.title || '').toLocaleLowerCase().includes(q))))
})
const fold = useCollapsibleList()
const allExpanded = computed(() => current.value?.songs.length && current.value.songs.every(song => expandedIds.value.includes(song.id)))
const cloneSong = song => ({ ...song, memberIds: [...song.memberIds], files: song.files.map(file => ({ ...file })) })
const songJson = song => JSON.stringify([song.title, song.artist, song.memo, song.sheetId, song.memberIds, song.files.map(file => file.id)])
const dirty = computed(() => Object.keys(drafts.value).length > 0)
const partLabel = part => INSTRUMENT_OPTIONS.find(option => option.value === part)?.label || part
const memberName = id => band.value.members.find(member => member.memberId === id)?.character?.nickname || '탈퇴한 멤버'
const memberPart = id => partLabel(band.value.members.find(member => member.memberId === id)?.part) || ''
// 서버 곡 → 화면 곡. 서버에 올라간 첨부는 remote, 새로 고른 파일은 local로 구분한다.
const toSong = item => ({
  id: item.id, title: item.title, artist: item.artist || '', memo: item.memo || '',
  sheetId: item.sheet?.sheetId ?? null, sheetTitle: item.sheet?.title || '', workspaceItemId: item.sheet?.workspaceItemId ?? null,
  memberIds: [...item.memberIds], files: item.attachments.map(file => ({ id: file.id, name: file.name, type: file.type, url: file.url, remote: true })),
})
const toList = list => ({ id: list.id, title: list.title, favorite: list.favorite, songs: list.items.map(toSong) })
function replaceList(result) { lists.value = lists.value.map(list => list.id === result.id ? toList(result) : list) }
// 보관함 악보는 곡을 처음 편집할 때 한 번만 불러온다.
async function loadSheets() {
  if (sheets.value || sheetsLoading.value) return
  sheetsLoading.value = true
  sheetsError.value = ''
  try { sheets.value = await getWorkspace({ sort: 'title' }) }
  catch { sheetsError.value = '악보를 불러오지 못했어요' }
  finally { sheetsLoading.value = false }
}
// 다른 멤버가 연결해 내 보관함에 없는 악보도 선택지에 남긴다.
function sheetOptions(song) {
  const options = sheets.value || []
  return song.sheetId && !options.some(sheet => sheet.sheetId === song.sheetId) ? [{ id: `linked-${song.sheetId}`, sheetId: song.sheetId, title: song.sheetTitle || '연결한 악보' }, ...options] : options
}
function linkSheet(song, value) {
  const sheet = sheetOptions(song).find(item => item.sheetId === Number(value))
  song.sheetId = sheet ? sheet.sheetId : null
  song.sheetTitle = sheet ? sheet.title : ''
  song.workspaceItemId = sheet && typeof sheet.id === 'number' ? sheet.id : null
  if (sheet && !song.title.trim()) song.title = sheet.title
  if (sheet && !song.artist.trim()) song.artist = sheet.artist || ''
}
// 내 보관함에 있는 악보는 바로 열고, 없으면 담을지 먼저 묻는다.
function openSheet(song) {
  if (song.workspaceItemId) return router.push(`/sheets/${song.workspaceItemId}`)
  sheetSong.value = song
  sheetDialog.value.showModal()
}
// 같은 악보를 쓰는 곡에도 보관함 항목을 기억해 다음부터 바로 연다.
function rememberWorkspaceItem(sheetId, workspaceItemId) {
  for (const song of [...lists.value.flatMap(list => list.songs), ...Object.values(drafts.value)]) {
    if (song.sheetId === sheetId) song.workspaceItemId = workspaceItemId
  }
}
async function confirmAddSheet() {
  const song = sheetSong.value
  let itemId = null
  const ok = await run(async () => {
    try { itemId = (await addToWorkspace(song.sheetId)).id }
    catch (e) {
      // 이미 담겨 있으면 목록을 새로 받아 보관함 항목을 찾는다.
      if (e?.response?.status !== 409) throw e
      const saved = (await listSetlists(bandId())).flatMap(list => list.items).find(item => item.sheet?.sheetId === song.sheetId)
      itemId = saved?.sheet?.workspaceItemId
      if (!itemId) throw e
    }
    rememberWorkspaceItem(song.sheetId, itemId)
  }, '악보를 보관함에 담지 못했어요.')
  sheetDialog.value.close()
  // 저장 중에는 화면 이동이 막히므로 끝난 뒤에 연다.
  if (ok) router.push(`/sheets/${itemId}`)
}
function objectUrl(blob) { const url = URL.createObjectURL(blob); urls.add(url); return url }
function cleanupUrls() {
  const used = new Set(Object.values(drafts.value).flatMap(song => song.files.filter(file => file.local).map(file => file.url)))
  for (const url of urls) if (!used.has(url)) { URL.revokeObjectURL(url); urls.delete(url) }
}
// 서버 첨부는 로그인 토큰이 필요해서 받아서 저장한다.
async function openFile(event, file) {
  if (file.local) return
  event.preventDefault()
  try {
    const url = await loadBandAttachment(`${file.url}?download=true`)
    const link = Object.assign(document.createElement('a'), { href: url, download: file.name })
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) { toast.error(apiErrorMessage(e, '첨부파일을 받지 못했어요.')) }
}
// 레이어 잼에서 넘어오면 그 곡이 든 셋리스트를 열고 곡을 펼쳐 보여 준다
async function focusSong(itemId) {
  router.replace({ query: { ...route.query, item: undefined } })
  const list = lists.value.find(entry => entry.songs.some(song => song.id === itemId))
  if (!list) { toast.error('연결한 곡이 셋리스트에서 지워졌어요.'); return }
  selectedId.value = list.id
  expandedIds.value = [itemId]
  focusedId.value = itemId
  await nextTick()
  document.getElementById(`song-card-${itemId}`)?.scrollIntoView({ block: 'center' })
  setTimeout(() => { if (focusedId.value === itemId) focusedId.value = null }, 2000)
}
async function loadBand() {
  const token = ++request
  band.value = null
  error.value = ''
  try {
    const id = Number(route.params.id)
    // 레이어 잼을 못 불러와도 셋리스트는 보여 준다
    const [result, saved, jamList] = await Promise.all([getBand(id), listSetlists(id), listJams(id).catch(() => [])])
    if (token !== request) return
    lists.value = saved.map(toList)
    jams.value = Array.isArray(jamList) ? jamList : []
    selectedId.value = lists.value[0]?.id ?? null
    band.value = result
    store.upsertBand(result, true)
    store.isBandMode = true
    if (route.query.item) focusSong(Number(route.query.item))
  } catch (e) { if (token === request) error.value = apiErrorMessage(e, '셋리스트를 불러오지 못했어요.') }
}
async function run(work, fallback = '저장하지 못했어요. 잠시 후 다시 시도해 주세요.') {
  if (busy.value) return false
  busy.value = true
  try { await work(); return true }
  catch (e) { toast.error(apiErrorMessage(e, fallback)); return false }
  finally { busy.value = false }
}
async function discardAllowed() { return !dirty.value || dialog.confirm({ title: '저장하지 않은 곡이 있어요', text: '변경사항을 버릴까요?', okText: '버리기', danger: true }) }
function resetEditor() { drafts.value = {}; songOrder.value = []; expandedIds.value = []; cleanupUrls() }
async function selectList(id) {
  if (id === selectedId.value) { fold.collapseIfNarrow(); return }
  if (busy.value || !await discardAllowed()) return
  resetEditor(); selectedId.value = id
  fold.collapseIfNarrow()
}
function addSong() {
  if (!selected.value || busy.value) return
  const song = { id: crypto.randomUUID(), title: '', artist: '', memo: '', sheetId: null, sheetTitle: '', workspaceItemId: null, memberIds: [], files: [] }
  songOrder.value = [...current.value.songs.map(item => item.id), song.id]
  drafts.value[song.id] = song
  expandedIds.value.push(song.id)
  loadSheets()
}
function editSong(song) { drafts.value[song.id] = cloneSong(song); if (!expandedIds.value.includes(song.id)) expandedIds.value.push(song.id); loadSheets() }
async function cancelSong(id) {
  const saved = selected.value.songs.find(song => song.id === id)
  const draft = drafts.value[id]
  const changed = saved ? songJson(draft) !== songJson(saved) : draft.title || draft.artist || draft.memo || draft.sheetId || draft.memberIds.length || draft.files.length
  if (changed && !await dialog.confirm({ title: '이 곡의 변경사항을 버릴까요?', text: '저장하지 않은 내용이 사라져요.', okText: '버리기', danger: true })) return
  delete drafts.value[id]
  if (!saved) { songOrder.value = songOrder.value.filter(item => item !== id); expandedIds.value = expandedIds.value.filter(item => item !== id) }
  cleanupUrls()
}
async function saveSong(song) {
  if (!song.title.trim()) { toast.error('곡 제목을 적어주세요.'); return }
  const setlistId = selectedId.value
  const isNew = !selected.value.songs.some(item => item.id === song.id)
  const before = new Set(selected.value.songs.map(item => item.id))
  const payload = { title: song.title.trim(), artist: song.artist.trim(), memo: song.memo, sheetId: song.sheetId, memberIds: song.memberIds, keepAttachmentIds: song.files.filter(file => file.remote).map(file => file.id) }
  // 이 곡만 저장하고, 다른 곡의 작성 중인 값은 그대로 둔다.
  await run(async () => {
    let result = await saveSetlistItem(bandId(), setlistId, isNew ? null : song.id, payload, song.files.filter(file => file.local).map(file => file.blob))
    if (isNew) {
      const created = result.items.find(item => !before.has(item.id))
      songOrder.value = songOrder.value.map(id => id === song.id ? created.id : id)
      expandedIds.value = expandedIds.value.map(id => id === song.id ? created.id : id)
      // 작성하던 자리 그대로 서버 순서를 맞춘다.
      const savedIds = result.items.map(item => item.id)
      const desired = [...songOrder.value.filter(id => savedIds.includes(id)), ...savedIds.filter(id => !songOrder.value.includes(id))]
      if (desired.join() !== savedIds.join()) result = await reorderSetlistItems(bandId(), setlistId, desired)
    }
    delete drafts.value[song.id]
    replaceList(result)
    cleanupUrls()
    toast.success(`${payload.title} 곡을 저장했어요.`)
  })
}
async function removeSong(song) {
  if (!await dialog.confirm({ title: `${song.title} 곡을 삭제할까요?`, okText: '삭제', danger: true })) return
  await run(async () => {
    replaceList(await deleteSetlistItem(bandId(), selectedId.value, song.id))
    songOrder.value = songOrder.value.filter(id => id !== song.id)
    expandedIds.value = expandedIds.value.filter(id => id !== song.id)
  }, '곡을 삭제하지 못했어요.')
}
function openCreate() { renaming.value = false; listTitle.value = ''; createDialog.value.showModal() }
function openRename(list) { renaming.value = true; renameId.value = list.id; listTitle.value = list.title; createDialog.value.showModal() }
function closeOnBackdrop(event, dialog) {
  if (event.target !== dialog) return
  const r = dialog.getBoundingClientRect()
  if (event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom) dialog.close()
}
async function submitList() {
  const title = listTitle.value.trim()
  if (!title) return
  if (renaming.value) {
    if (await run(async () => replaceList(await updateSetlist(bandId(), renameId.value, { title })))) createDialog.value.close()
    return
  }
  if (!await discardAllowed()) return
  await run(async () => {
    const list = toList(await createSetlist(bandId(), title))
    lists.value = [...lists.value, list]
    resetEditor(); favoritesOnly.value = false; selectedId.value = list.id; createDialog.value.close()
  })
}
function askDelete(ids) { if (busy.value) return; deleteIds.value = [...ids]; deleteDialog.value.showModal() }
async function deleteLists() {
  if (deleteIds.value.includes(selectedId.value) && !await discardAllowed()) return
  const removed = []
  await run(async () => { for (const id of deleteIds.value) { await deleteSetlist(bandId(), id); removed.push(id) } }, '셋리스트를 삭제하지 못했어요.')
  // 일부만 지워졌어도 지운 것은 목록에서 뺀다.
  lists.value = lists.value.filter(list => !removed.includes(list.id))
  if (removed.includes(selectedId.value)) { resetEditor(); selectedId.value = lists.value[0]?.id ?? null }
  checkedIds.value = checkedIds.value.filter(id => !removed.includes(id))
  if (removed.length === deleteIds.value.length) deleteDialog.value.close()
}
async function toggleFavorite(list) { await run(async () => replaceList(await updateSetlist(bandId(), list.id, { favorite: !list.favorite }))) }
async function reorderLists(ids) { await run(async () => { lists.value = (await reorderSetlists(bandId(), ids)).map(toList) }, '순서를 바꾸지 못했어요.') }
async function reorderSongs(ids) {
  const savedIds = selected.value.songs.map(song => song.id)
  const next = ids.filter(id => savedIds.includes(id))
  if (next.join() === savedIds.join()) { songOrder.value = ids; return }
  if (await run(async () => replaceList(await reorderSetlistItems(bandId(), selectedId.value, next)), '순서를 바꾸지 못했어요.')) songOrder.value = ids
}
function toggleSong(id) { expandedIds.value = expandedIds.value.includes(id) ? expandedIds.value.filter(item => item !== id) : [...expandedIds.value, id] }
function toggleAllSongs() { expandedIds.value = allExpanded.value ? [] : current.value.songs.map(song => song.id) }
function toggleMember(song, id) { song.memberIds = song.memberIds.includes(id) ? song.memberIds.filter(memberId => memberId !== id) : [...song.memberIds, id] }
function attachFiles(song, event) {
  for (const file of event.target.files) {
    if (song.files.length >= BAND_ATTACHMENT_MAX_COUNT) { toast.error(`첨부파일은 곡마다 ${BAND_ATTACHMENT_MAX_COUNT}개까지 올릴 수 있어요.`); break }
    if (file.size > BAND_ATTACHMENT_MAX_BYTES) { toast.error(`${file.name}: 10MB까지 첨부할 수 있어요.`); continue }
    song.files.push({ id: crypto.randomUUID(), name: file.name, size: file.size, blob: file, url: objectUrl(file), local: true })
  }
  event.target.value = ''
}
const formatSize = size => size < 1024 * 1024 ? `${Math.max(1, Math.round(size / 1024))} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`
function beforeUnload(event) { if (dirty.value || busy.value) { event.preventDefault(); event.returnValue = '' } }
onBeforeRouteLeave(() => !busy.value && discardAllowed())
onBeforeRouteUpdate(() => !busy.value && discardAllowed())
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
watch(() => route.params.id, () => { resetEditor(); lists.value = []; selectedId.value = null; cleanupUrls(); checkedIds.value = []; createDialog.value?.close(); deleteDialog.value?.close(); sheetDialog.value?.close(); loadBand() }, { immediate: true })
onBeforeUnmount(() => { request++; window.removeEventListener('beforeunload', beforeUnload); urls.forEach(url => URL.revokeObjectURL(url)) })
</script>


<style scoped>
.page,.page *,.setlist-dialog,.setlist-dialog * { box-sizing:border-box; }
.page { display:flex; flex-direction:column; gap:16px; }
.muted { color:var(--c-muted); font-size:.8rem; line-height:1.7; }
.notice { margin:0; color:var(--c-muted); font-size:.75rem; line-height:1.6; }
.workspace { display:grid; grid-template-columns:250px minmax(0,1fr); gap:20px; align-items:start; }
.sidebar,.detail { min-width:0; }
.sidebar :deep(.ws-card-head) { gap:4px; }
.detail :deep(.ws-card-head) { align-items:center; flex-wrap:wrap; }
.detail :deep(.ws-card-head h3) { min-width:0; overflow-wrap:anywhere; flex:1 1 160px; }
.detail :deep(.actions) { flex-wrap:wrap; gap:8px; }
button:disabled { opacity:.4; cursor:default; }
.danger { color:var(--c-danger); }
button:focus-visible,a:focus-visible { outline:2px solid var(--c-primary-line); outline-offset:2px; }
.add-list { display:inline-flex; align-items:center; gap:4px; padding:6px 12px; font-size:.8rem; font-weight:800; color:var(--c-primary-text); white-space:nowrap; }
.list-tools,.bulk-tools { display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:12px; font-size:.73rem; color:var(--c-muted); }
.text-btn { border:0; background:transparent; padding:4px 0; font-family:inherit; font-size:.74rem; cursor:pointer; }
.list-nav { gap:8px; }
.list-entry { display:flex; align-items:center; gap:5px; min-width:0; padding:8px 7px; border:1px solid var(--c-line); border-radius:12px; background:#fff; }
.list-entry.active { background:var(--c-primary-soft); border-color:var(--c-primary-line); }
.list-link { display:flex; flex-direction:column; flex:1; min-width:0; gap:6px; text-align:left; border:0; background:transparent; font-family:inherit; padding:4px 0; cursor:pointer; overflow-wrap:anywhere; color:#343a40; }
.list-link strong { font-size:.79rem; }
.list-link span { color:var(--c-muted); font-size:.7rem; }
.star { background:transparent; border:0; color:#ced4da; font-size:20px; cursor:pointer; padding:3px; }
/* 목록 검색·접기 (#254). 검색칸도 label이라 이 화면의 label 규칙(세로 쌓기)을 가로로 되돌린다 */
.list-search { display:flex; flex-direction:row; align-items:center; gap:7px; height:36px; padding:0 10px; margin-bottom:10px; border:1px solid var(--c-line); border-radius:10px; background:var(--c-bg); color:var(--c-faint); }
.list-search:focus-within { border-color:var(--c-primary); background:#fff; }
/* 아래 입력칸 공통 규칙(input:not(…))보다 우선하도록 .sidebar를 붙인다 */
.sidebar .list-search input[type=search] { flex:1; width:auto; min-width:0; height:100%; padding:0; border:none; border-radius:0; outline:none; background:none; font:inherit; font-size:16px; line-height:normal; color:var(--c-text); }
.fold { display:inline-flex; align-items:center; gap:3px; }
.list-summary { width:100%; display:flex; align-items:center; justify-content:space-between; gap:10px; padding:10px 12px; border:1px solid var(--c-primary-line); border-radius:12px; background:var(--c-primary-soft); font:inherit; text-align:left; color:var(--c-primary-text); cursor:pointer; }
.list-summary strong { min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:.82rem; }
.list-summary span { flex:none; font-size:.72rem; font-weight:700; color:var(--c-muted); }
.rename { flex:none; display:flex; padding:5px; border:0; border-radius:8px; background:transparent; color:var(--c-faint); cursor:pointer; }
.rename:hover:not(:disabled) { color:var(--c-text-2); background:var(--c-line-soft); }
.star.on { color:#fab005; }
.summary { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:8px 16px; padding:10px 12px; background:var(--c-bg); border-radius:10px; color:var(--c-muted); font-size:.75rem; }
.summary b { color:var(--c-text-2); }
.summary em { margin-left:8px; color:var(--c-muted); font-size:.7rem; font-style:normal; }
.empty { margin-top:16px; }
.song-form,.song-fields { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; max-width:480px; }
.song-form { margin-top:16px; padding:16px; background:var(--c-bg); border-radius:12px; }
label { display:flex; flex-direction:column; gap:7px; min-width:0; font-size:.75rem; font-weight:500; color:var(--c-muted); }
input:not([type=checkbox]):not([type=file]),textarea { display:block; width:100%; min-width:0; max-width:100%; border:1px solid var(--c-line); padding:8px 10px; border-radius:8px; background:#fff; color:#343a40; font-family:inherit; font-size:.8rem; font-weight:400; line-height:1.6; outline:none; box-shadow:none; }
input:not([type=checkbox]):not([type=file]):focus,textarea:focus { border-color:var(--c-primary); outline:none; box-shadow:none; }
input::placeholder,textarea::placeholder { color:var(--c-faint); opacity:1; font-weight:400; }
textarea { resize:vertical; min-height:64px; }
.memo,.sheet-field { max-width:480px; }
select { width:100%; min-width:0; border:1px solid var(--c-line); padding:8px 10px; border-radius:8px; background:#fff; color:#343a40; font-family:inherit; font-size:.8rem; line-height:1.6; outline:none; }
select:focus { border-color:var(--c-primary); }
.sheet-link { border:0; background:transparent; padding:0; font-family:inherit; cursor:pointer; text-align:left; color: var(--c-primary-text); font-size:.8rem; font-weight:700; overflow-wrap:anywhere; }
.songs { margin-top:16px; }
.song-card { border:1px solid var(--c-line); border-radius:12px; background:#fff; transition:border-color .3s, box-shadow .3s; }
.song-card.focused { border-color:var(--c-primary); box-shadow:0 0 0 3px color-mix(in srgb, var(--c-primary) 12%, transparent); }
.jam-links { display:flex; flex-wrap:wrap; gap:8px; }
.jam-go { gap:6px; text-decoration:none; }
.jam-go small { color:var(--c-muted); font-weight:500; }
.song-editing { border-color:#d3d9e0; }
.song-actions { display:flex; align-items:center; gap:10px; flex-shrink:0; }
.song-head { display:flex; align-items:center; gap:10px; padding:12px 14px; }
.drag-handle { border:0; background:transparent; color:var(--c-faint); border-radius:7px; width:26px; height:32px; flex-shrink:0; font-size:22px; cursor:grab; touch-action:none; user-select:none; }
.drag-handle:active { cursor:grabbing; }
.list-handle { width:18px; font-size:18px; padding:0; }
.number { font-size:.8rem; color:var(--c-muted); font-weight:700; }
.song-toggle { display:flex; flex:1; min-width:0; align-items:center; justify-content:space-between; gap:10px; border:0; background:transparent; text-align:left; font-family:inherit; cursor:pointer; padding:0; color:#343a40; }
.song-name { display:flex; flex-direction:column; gap:4px; flex:1; min-width:0; overflow-wrap:anywhere; }
.song-name strong { font-size:.86rem; }
.song-name>span { font-size:.72rem; color:var(--c-muted); }
.chevron { flex-shrink:0; color:var(--c-muted); transition:transform .2s ease; }
.chevron.open { transform:rotate(180deg); }
.song-collapse { display:grid; grid-template-rows:0fr; transition:grid-template-rows .22s ease; }
.song-collapse.expanded { grid-template-rows:1fr; }
.collapse-inner { overflow:hidden; min-height:0; }
.song-body { border-top:1px solid var(--c-line-soft); padding:16px; display:flex; flex-direction:column; gap:18px; }
.participants { min-width:0; border:0; margin:0; padding:0; }
legend,.read-section h4 { padding:0; margin:0 0 10px; font-size:.75rem; font-weight:600; color:var(--c-muted); }
legend span { margin-left:6px; font-weight:400; }
.member-options { display:flex; flex-wrap:wrap; gap:7px; }
.member-option,.member-tag { display:inline-flex; align-items:center; gap:7px; max-width:100%; padding:7px 10px; border:1px solid var(--c-line); border-radius:20px; font-family:inherit; font-size:.74rem; background:#fff; color:var(--c-text-2); line-height:1.5; overflow-wrap:anywhere; }
.member-option { cursor:pointer; }
.member-option.chosen { border-color:var(--c-primary-line); background:var(--c-primary-soft); color:var(--c-primary-text); }
.member-option small,.member-tag small { color:var(--c-faint); font-size:.66rem; }
.member-tag { background:var(--c-bg); border:0; }
.read-section p { margin:0; }
.read-memo { white-space:pre-wrap; overflow-wrap:anywhere; line-height:1.8; font-size:.82rem; color:var(--c-text-2); }
.attachment-head { display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:wrap; font-size:.75rem; color:var(--c-muted); }
.attachment-head small { margin-left:4px; }
.file-picker { position:relative; overflow:hidden; }
.file-picker input { position:absolute; inset:0; width:100%; height:100%; opacity:0; cursor:pointer; }
.file-picker:focus-within { outline:1px solid var(--c-primary); }
.file-hint { font-size:.73rem; color:var(--c-faint); line-height:1.6; margin:10px 0 0; }
.file-list { list-style:none; padding:0; margin:10px 0 0; display:flex; flex-direction:column; gap:6px; }
.file-list li { display:flex; align-items:center; gap:10px; padding:8px 10px; background:var(--c-bg); border-radius:8px; }
.file-list a { flex:1; min-width:0; overflow-wrap:anywhere; color:var(--c-text-2); font-size:.76rem; text-decoration:none; line-height:1.6; }
.file-list small { color:var(--c-faint); margin-left:6px; }
.remove-file,.close-btn { border:0; background:transparent; color:var(--c-muted); font-size:22px; cursor:pointer; min-width:28px; min-height:28px; }
.setlist-dialog { width:calc(100% - 32px); max-width:400px; max-height:calc(100dvh - 48px); overflow-y:auto; padding:24px; border:0; border-radius:var(--radius-xl); color:var(--c-text); font-family:var(--font-sans); box-shadow:var(--shadow-lg); }
.setlist-dialog::backdrop { background:rgba(15,23,42,.45); }
.dialog-head { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:20px; }
.setlist-dialog h2 { font-size:1rem; margin:0; }
.setlist-dialog .dialog-actions { margin-top:22px; }
.delete-description { font-size:.82rem; line-height:1.7; color:var(--c-muted); }
.sheet-dialog { max-width:340px; }
@media (max-width: 860px) { .workspace { grid-template-columns:1fr; } }
@media (max-width: 420px) { .song-form,.song-fields { grid-template-columns:1fr; } .song-head { padding:12px 10px; gap:7px; } .detail :deep(.actions) { gap:6px; } .song-body { padding:14px; } }
@media(prefers-reduced-motion:reduce) { .song-collapse,.chevron { transition:none; } }
/* 휴대폰: 팝업은 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  /* 휴대폰: 만들기 버튼은 목록 아래로 (#233) */
  .sidebar { display:flex; flex-direction:column; }
  .setlist-dialog { width: 100%; max-width: none; margin: auto 0 0; border-radius: var(--radius-xl) var(--radius-xl) 0 0; padding-bottom: calc(24px + env(safe-area-inset-bottom)); }
}
</style>
