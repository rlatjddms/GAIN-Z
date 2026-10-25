<template>
  <SubPageLayout pill="BAND" :back-to="{ path: '/home', query: { band: $route.params.id } }" back-label="합주실"
                 :title="band ? 'LAYER JAM' : ''" name="레이어 잼" :subtitle="band ? `${band.name} · 각자 녹음한 파트를 한 겹씩 쌓아 비대면으로 합주해요` : ''">
    <p v-if="error" role="alert">{{ error }} <button class="btn" @click="loadBand">다시 불러오기</button></p>
    <p v-else-if="!band" class="muted">레이어 잼을 불러오는 중…</p>
    <div v-else class="page">
      <p v-if="saveError" class="save-error" role="alert">{{ saveError }}</p>
      <p v-if="notice" class="status" role="status">{{ notice }}</p>
      <div class="workspace">
        <WsCard :title="`잼 ${jams.length}`" class="sidebar" :class="{ collapsed: fold.collapsed.value }">
          <!-- 휴대폰: 목록 접기·펼치기 (#254) -->
          <template v-if="fold.narrow.value && jams.length" #actions>
            <button class="text-btn fold" :aria-expanded="!fold.collapsed.value" @click="fold.toggle">{{ fold.collapsed.value ? '펼치기' : '접기' }}<ChevronDown class="chevron" :class="{ open: !fold.collapsed.value }" :size="15" :stroke-width="2.4" aria-hidden="true" /></button>
          </template>
          <button v-if="fold.collapsed.value" class="list-summary" @click="fold.toggle">
            <strong>{{ jam?.title || '잼을 골라 주세요' }}</strong><span>잼 {{ jams.length }}개 보기</span>
          </button>
          <template v-else>
          <button class="btn add-jam" :disabled="busy || recording" @click="openForm(null)">+ 잼 만들기</button>
          <!-- 목록 검색 (#254): 잼 이름이나 연결한 곡 제목 -->
          <label v-if="jams.length" class="list-search">
            <Search :size="14" :stroke-width="2.4" aria-hidden="true" />
            <input v-model="jamQuery" type="search" placeholder="잼·곡 이름 검색" aria-label="잼 검색" maxlength="40">
          </label>
          <!-- 셋리스트 목록과 같은 도구: 즐겨찾기만 보기, 여러 개 골라 지우기 (만든 멤버·리더만 지울 수 있어서 그 잼만 고를 수 있음) -->
          <div v-if="jams.length" class="list-tools">
            <button class="chip" :class="{ on: favoritesOnly }" :aria-pressed="favoritesOnly" @click="favoritesOnly = !favoritesOnly"><Star :size="14" :stroke-width="2.2" :fill="favoritesOnly ? 'currentColor' : 'none'" /> 즐겨찾기</button>
            <button class="chip" :class="{ on: selecting }" :disabled="recording" @click="selecting = !selecting; checkedIds = []">{{ selecting ? '선택 취소' : '선택' }}</button>
          </div>
          <div v-if="selecting" class="bulk-tools"><span>{{ checkedIds.length }}개 선택</span><button class="chip danger" :disabled="!checkedIds.length || busy" @click="askDelete({ kind: 'bulk', ids: [...checkedIds] })">선택 삭제</button></div>
          <ul v-if="visibleJams.length" class="jam-list">
            <li v-for="item in visibleJams" :key="item.id" class="jam-row">
              <input v-if="selecting" v-model="checkedIds" type="checkbox" :value="item.id" :disabled="!item.canManage"
                     :aria-label="`${item.title} 선택`" :title="item.canManage ? '' : '만든 멤버나 리더만 지울 수 있어요'">
              <button class="jam-link" :class="{ active: item.id === selectedId }" :aria-pressed="item.id === selectedId" :disabled="recording" @click="selectJam(item.id)">
                <strong>{{ item.title }}</strong>
                <span>{{ item.bpm }} BPM · 트랙 {{ item.trackCount }}</span>
                <small v-if="item.song">셋리스트 · {{ item.song.title }}</small>
              </button>
              <button v-if="item.canManage && !selecting" class="rename" :disabled="busy || recording" :aria-label="`${item.title} 이름 변경`" title="이름 변경" @click="openForm(item)"><Pencil :size="14" :stroke-width="2.2" /></button>
              <!-- 수정할 수 없는 잼도 카드 폭이 같게 연필 자리를 비워 둔다 -->
              <span v-else-if="!selecting" class="rename-space" aria-hidden="true"></span>
              <button class="star" :class="{ on: item.favorite }" :disabled="busy" :aria-pressed="item.favorite" :aria-label="`${item.title} 즐겨찾기`" @click="toggleFavorite(item)"><Star :size="16" :stroke-width="2.2" :fill="item.favorite ? 'currentColor' : 'none'" /></button>
            </li>
          </ul>
          <p v-else class="muted">{{ jamQuery.trim() && jams.length ? `'${jamQuery.trim()}' 잼이 없어요.` : favoritesOnly && jams.length ? '즐겨찾기한 잼이 없어요.' : '아직 잼이 없어요. 합주할 곡으로 잼을 만들어 보세요.' }}</p>
          </template>
        </WsCard>

        <WsCard :title="jam?.title || '레이어 잼'" class="detail">
          <template v-if="jam?.canManage" #actions>
            <button class="btn" :disabled="busy || recording" @click="openForm(jam)">수정</button>
            <button class="btn danger" :disabled="busy || recording" @click="askDelete({ kind: 'jam' })">삭제</button>
          </template>
          <div v-if="jamLoading" class="muted">잼을 불러오는 중…</div>
          <EmptyState v-else-if="!jam" icon="mic" title="잼을 골라 주세요" text="멤버가 올린 트랙을 들으며 내 파트를 녹음해 쌓을 수 있어요." />
          <template v-else>
            <p class="jam-meta">{{ jam.bpm }} BPM<template v-if="jam.song"> · 셋리스트 <router-link :to="`/bands/${band.id}/setlists?item=${jam.song.setlistItemId}`">{{ jam.song.title }}</router-link></template> · {{ jam.creatorName || '멤버' }}님이 만듦</p>
            <details class="guide">
              <summary>레이어 잼은 이렇게 써요</summary>
              <ol>
                <li>첫 멤버는 <b>메트로놈</b>(박자마다 울리는 틱 소리)을 들으며 자기 파트를 녹음해 올려요.</li>
                <li>다음 멤버는 먼저 올라온 트랙을 들으며 녹음하거나, 따로 녹음한 음원 파일을 올려요.</li>
                <li>트랙마다 볼륨을 맞추고, <b>M</b>(뮤트)으로 트랙을 끄거나 <b>S</b>(솔로)로 그 트랙만 골라 들어요.</li>
                <li>다 쌓이면 <b>합친 음원 내려받기</b>로 한 파일(WAV)로 받아요. 메트로놈 소리는 들어가지 않아요.</li>
              </ol>
            </details>

            <div class="transport" role="group" aria-label="재생">
              <button class="btn primary play" :disabled="recording || !canPlay" :aria-label="playing ? '정지' : '재생'" @click="togglePlay">{{ playing ? '■ 정지' : '▶ 재생' }}</button>
              <button class="btn" :disabled="recording" aria-label="처음으로" @click="rewind"><SkipBack :size="16" :stroke-width="2.4" /></button>
              <span class="time" aria-live="off"><b data-testid="position">{{ formatTime(position) }}</b> / {{ formatTime(totalLength) }}</span>
              <input class="seek" type="range" min="0" :max="Math.max(totalLength, 0.1)" step="0.1" :value="position" :disabled="recording || !totalLength" aria-label="재생 위치" @input="seek(Number($event.target.value))">
              <label class="check" :title="`${jam.bpm} BPM으로 박자마다 틱 소리를 내요. 합친 음원에는 들어가지 않아요.`"><input v-model="metronomeOn" type="checkbox" @change="restart"> 메트로놈</label>
              <button class="btn" :disabled="busy || recording || !mixable" @click="downloadMix">합친 음원 내려받기</button>
            </div>

            <ul v-if="tracks.length" class="legend">
              <li><span class="toggle-chip">M</span>뮤트: 끄기</li>
              <li><span class="toggle-chip solo">S</span>솔로: 이 트랙만 듣기</li>
              <li class="legend-note">둘 다 내 화면에만 적용돼요</li>
            </ul>
            <ul class="tracks" aria-label="트랙">
              <li v-for="track in tracks" :key="track.id" class="track" :class="{ silent: !isAudible(track) }">
                <div class="track-name">
                  <strong>{{ track.nickname || '탈퇴한 멤버' }}</strong>
                  <small>{{ partLabel(track.part) }} · {{ formatTime(track.durationMs / 1000) }}</small>
                  <small v-if="track.status === 'loading'" class="muted">불러오는 중…</small>
                  <small v-else-if="track.status === 'error'" class="track-error">재생할 수 없는 파일이에요</small>
                </div>
                <div class="track-controls">
                  <button class="toggle" :class="{ on: track.muted }" :aria-pressed="track.muted" :aria-label="`${trackLabel(track)} 음소거`" title="뮤트: 이 트랙 소리를 꺼요 (내 화면에만 적용)" @click="toggleMute(track)">M</button>
                  <button class="toggle solo" :class="{ on: soloIds.includes(track.id) }" :aria-pressed="soloIds.includes(track.id)" :aria-label="`${trackLabel(track)} 솔로`" title="솔로: 이 트랙만 들려요 (내 화면에만 적용)" @click="toggleSolo(track.id)">S</button>
                  <label class="volume"><span>볼륨 {{ track.volume }}%</span><input type="range" min="0" :max="JAM_VOLUME_MAX" step="1" :value="track.volume" :aria-label="`${trackLabel(track)} 볼륨`" @input="changeVolume(track, Number($event.target.value))" @change="saveTrack(track, { volume: track.volume })"></label>
                  <label class="offset"><span>시작 위치(ms)</span><input type="number" :min="JAM_OFFSET_MIN_MS" :max="JAM_OFFSET_MAX_MS" step="10" :value="track.offsetMs" :disabled="recording" :aria-label="`${trackLabel(track)} 시작 위치`" @change="changeOffset(track, $event.target.value, true)"></label>
                  <button v-if="track.canDelete" class="remove" :disabled="busy || recording" :aria-label="`${trackLabel(track)} 트랙 삭제`" @click="askDelete({ kind: 'track', track })">×</button>
                </div>
              </li>
              <li v-if="take" class="track take">
                <div class="track-name"><strong>{{ takeLabel }}</strong><small>올리기 전 · {{ take.file ? `${take.file.name} · ` : '' }}{{ formatTime(take.buffer.duration) }}</small></div>
                <div class="track-controls">
                  <label class="volume"><span>볼륨 {{ take.volume }}%</span><input type="range" min="0" :max="JAM_VOLUME_MAX" step="1" :value="take.volume" :aria-label="`${takeLabel} 볼륨`" @input="changeVolume(take, Number($event.target.value))"></label>
                  <div class="nudge" role="group" :aria-label="`${takeLabel} 시작 위치 미세 조정`">
                    <button class="btn" :disabled="uploading" @click="nudgeTake(-1000)">−1초</button>
                    <button class="btn" :disabled="uploading" @click="nudgeTake(-50)">−50</button>
                    <button class="btn" :disabled="uploading" @click="nudgeTake(-10)">−10</button>
                    <label class="offset"><span>시작 위치(ms)</span><input type="number" step="10" :value="take.offsetMs" :disabled="uploading" :aria-label="`${takeLabel} 시작 위치`" @change="changeOffset(take, $event.target.value)"></label>
                    <button class="btn" :disabled="uploading" @click="nudgeTake(10)">+10</button>
                    <button class="btn" :disabled="uploading" @click="nudgeTake(50)">+50</button>
                    <button class="btn" :disabled="uploading" @click="nudgeTake(1000)">+1초</button>
                  </div>
                </div>
                <p class="take-hint">{{ take.file ? '파일 처음(0초)이 곡 시작에 놓여 있어요. 파일 앞에 빈 부분이 있으면 −로 당겨 주세요.' : '' }} 재생해서 다른 트랙과 박자가 맞는지 들어 보고, 어긋나면 시작 위치를 옮겨 주세요. 늦게 들리면 −, 빠르게 들리면 +.</p>
                <div class="take-actions">
                  <span v-if="uploading" class="muted" role="status">올리는 중 {{ uploadProgress }}%</span>
                  <button class="btn" :disabled="uploading" @click="discardTake(true)">버리기</button>
                  <button class="btn primary" :disabled="uploading" @click="uploadTake">트랙 올리기</button>
                </div>
              </li>
              <li v-if="!tracks.length && !take" class="muted no-tracks">아직 트랙이 없어요. 메트로놈을 들으며 첫 파트를 녹음해 보세요.</li>
            </ul>

            <section class="recorder" aria-labelledby="recorder-title">
              <h4 id="recorder-title">내 파트 올리기</h4>
              <p v-if="trackLimitReached" class="hint">트랙은 잼마다 {{ JAM_MAX_TRACKS }}개까지 쌓을 수 있어요.</p>
              <div class="add-ways">
                <div class="way">
                  <strong><FileUp :size="16" :stroke-width="2.2" /> 음원 파일 올리기</strong>
                  <p class="hint">녹음 앱이나 녹음기로 따로 녹음한 MP3·WAV·M4A·OGG·WebM 파일을 30MB까지 올릴 수 있어요.</p>
                  <label class="btn file-picker" :class="{ disabled: fileDisabled }">파일 고르기<input type="file" :accept="JAM_FILE_ACCEPT" :disabled="fileDisabled" aria-label="음원 파일 고르기" @change="pickFile"></label>
                </div>
                <div class="way">
                  <strong><Mic :size="16" :stroke-width="2.2" /> 여기서 바로 녹음</strong>
                  <p class="hint"><Headphones :size="14" :stroke-width="2.2" /> 이어폰을 끼고 녹음하세요. 스피커로 들으면 반주가 함께 녹음돼요.</p>
                  <button v-if="!micReady" class="btn" :disabled="busy" @click="connectMic">마이크 연결</button>
                  <template v-else>
                    <div class="latency">
                      <label><span>녹음 지연 보정 <b>{{ latencyMs }}ms</b></span><input type="range" min="0" :max="LATENCY_MAX_MS" step="5" :value="latencyMs" :disabled="recording || measuring" aria-label="녹음 지연 보정" @input="setLatency(Number($event.target.value))"></label>
                      <small>소리가 이어폰에 닿고 마이크로 다시 들어오는 데 걸리는 시간만큼 녹음을 앞당겨 붙여요. 기기 추정값 {{ latencyEstimate }}ms. 녹음이 늦게 붙으면 값을 키워 주세요.</small>
                      <button class="btn" :disabled="recording || measuring || busy" @click="measure">{{ measuring ? '측정 중…' : '지연 측정' }}</button>
                      <small>스피커로 딱 소리를 6번 내며 녹음해서, 소리를 낸 시각과 녹음된 시각의 차이로 지연을 자동으로 맞춰요. 이어폰을 빼고 조용한 곳에서 해 주세요.</small>
                    </div>
                <div class="record-row">
                  <label class="check"><input v-model="countInOn" type="checkbox" :disabled="recording"> 한 마디 카운트인</label>
                  <button v-if="!recording" class="btn record" :disabled="busy || measuring || uploading || trackLimitReached" @click="startRecording">● 녹음 시작</button>
                  <button v-else class="btn record on" @click="stopRecording">■ 녹음 끝내기</button>
                  <!-- 카운트인 중에는 녹음 시작까지 남은 박을 4·3·2·1로 크게 (#234) -->
                  <span v-if="recording && countInBeat" class="count-in" role="status" :aria-label="`녹음 시작까지 ${countInLeft}박`">{{ countInLeft }}</span>
                  <span v-else-if="recording" class="rec-time" role="status">녹음 중 {{ formatTime(position) }}</span>
                </div>
                  </template>
                </div>
              </div>
            </section>
          </template>
        </WsCard>
      </div>
    </div>

    <dialog ref="formDialog" class="jam-dialog" aria-labelledby="jam-form-title" @close="formError = ''">
      <form method="dialog" novalidate @submit.prevent="saveForm">
        <h3 id="jam-form-title">{{ form.id ? '잼 수정' : '잼 만들기' }}</h3>
        <fieldset class="song-picker">
          <legend>셋리스트 곡 연결 (선택)</legend>
          <div v-if="form.song" class="picked">
            <span><b>{{ form.song.title }}</b><small v-if="form.song.setlist"> · {{ form.song.setlist }}</small></span>
            <button type="button" class="text-btn" @click="form.song = null">연결 해제</button>
          </div>
          <template v-else>
            <input v-model="songQuery" type="search" placeholder="곡 제목, 아티스트 또는 셋리스트 이름을 입력해 주세요" aria-label="셋리스트 곡 검색">
            <p v-if="songsLoading" class="muted">곡 목록을 불러오는 중…</p>
            <p v-else-if="!setlistSongs.length" class="muted">셋리스트에 담긴 곡이 없어요.</p>
            <template v-else-if="songQuery.trim()">
              <p v-if="!songMatches.length" class="muted">찾는 곡이 없어요.</p>
              <ul v-else class="song-results" aria-label="검색한 곡">
                <li v-for="song in songMatches.slice(0, SONG_RESULT_LIMIT)" :key="song.id">
                  <button type="button" class="song-option" @click="pickSong(song)">
                    <span><b>{{ song.title }}</b><small v-if="song.artist"> · {{ song.artist }}</small></span>
                    <small>{{ song.setlist }}</small>
                  </button>
                </li>
              </ul>
              <p v-if="songMatches.length > SONG_RESULT_LIMIT" class="muted more">{{ songMatches.length - SONG_RESULT_LIMIT }}곡이 더 있어요. 조금 더 자세히 입력해 주세요.</p>
            </template>
          </template>
        </fieldset>
        <label>잼 이름<input v-model="form.title" maxlength="60" placeholder="잼 이름을 입력해 주세요" required></label>
        <label>BPM<input v-model.number="form.bpm" type="number" min="40" max="240" required></label>
        <p v-if="formError" class="save-error" role="alert">{{ formError }}</p>
        <div class="dialog-actions"><button type="button" class="btn" @click="formDialog.close()">취소</button><button class="btn primary" :disabled="busy">{{ form.id ? '저장' : '만들기' }}</button></div>
      </form>
    </dialog>

    <dialog ref="deleteDialog" class="jam-dialog" aria-labelledby="jam-delete-title">
      <h3 id="jam-delete-title">{{ deleteTarget?.kind === 'bulk' ? `잼 ${deleteTarget.ids.length}개를 삭제할까요?` : deleteTarget?.kind === 'jam' ? '잼을 삭제할까요?' : '트랙을 삭제할까요?' }}</h3>
      <p class="muted">{{ deleteTarget?.kind === 'track' ? '올린 녹음 파일이 지워져요.' : '잼의 모든 트랙이 함께 지워져요.' }}</p>
      <div class="dialog-actions"><button class="btn" @click="deleteDialog.close()">취소</button><button class="btn danger solid" :disabled="busy" @click="confirmDelete">삭제</button></div>
    </dialog>
  </SubPageLayout>
</template>

<script setup>
import { SkipBack, FileUp, Mic, Headphones, Star, Pencil, Search, ChevronDown } from 'lucide-vue-next'
import { useCollapsibleList } from '../../components/band/useCollapsibleList'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import { getBand } from '../../api/band'
import { listSetlists } from '../../api/setlist'
import { createJam, deleteJam, deleteJamTrack, getJam, listJams, loadJamTrackFile, setJamFavorite, updateJam, updateJamTrack, uploadJamTrack } from '../../api/jam'
import { apiErrorMessage } from '../../api/practice'
import { useBandStore } from '../../stores/band'
import { useDialogStore } from '../../stores/dialog'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { JamEngine } from '../../audio/JamEngine'
import {
  JAM_MAX_TRACKS, JAM_OFFSET_MAX_MS, JAM_OFFSET_MIN_MS, JAM_TRACK_MAX_BYTES, JAM_VOLUME_MAX, LATENCY_MAX_MS,
  JAM_FILE_ACCEPT, audibleTracks, clamp, formatTime, recordedOffsetMs, recordingExtension, timelineLength,
} from '../../audio/jamAudio'

const LATENCY_KEY = 'gainz.jam.latencyMs'
const route = useRoute()
const router = useRouter()
const dialog = useDialogStore()
const store = useBandStore()
const engine = new JamEngine()
const band = ref(null), error = ref(''), saveError = ref(''), notice = ref(''), busy = ref(false)
const favoritesOnly = ref(false), selecting = ref(false), checkedIds = ref([])
// 잼 이름이나 연결한 곡 제목으로 거른다 (#254)
const jamQuery = ref('')
const visibleJams = computed(() => {
  const q = jamQuery.value.trim().toLocaleLowerCase()
  return jams.value.filter(item => (!favoritesOnly.value || item.favorite)
    && (!q || item.title.toLocaleLowerCase().includes(q) || (item.song?.title || '').toLocaleLowerCase().includes(q)))
})
const fold = useCollapsibleList()
const jams = ref([]), selectedId = ref(null), jam = ref(null), jamLoading = ref(false), tracks = ref([]), soloIds = ref([])
const playing = ref(false), position = ref(0), metronomeOn = ref(false), countInOn = ref(true)
const micReady = ref(false), latencyMs = ref(readLatency() ?? 0), latencyEstimate = ref(0), measuring = ref(false)
const recording = ref(false), take = ref(null), uploading = ref(false), uploadProgress = ref(0)
// 한 마디 카운트인 박 수, 지금 몇 번째 박인지 (카운트인 중이 아니면 0)
const COUNT_IN_BEATS = 4
const countInBeat = ref(0)
const countInLeft = computed(() => COUNT_IN_BEATS - countInBeat.value + 1)
const formDialog = ref(null), form = ref({}), formError = ref(''), setlistSongs = ref([]), songQuery = ref(''), songsLoading = ref(false)
const deleteDialog = ref(null), deleteTarget = ref(null)
let session = null, frame = 0, request = 0, jamRequest = 0

const bandId = () => band.value.id
const partLabel = part => INSTRUMENT_OPTIONS.find(option => option.value === part)?.label || part || ''
const trackLabel = track => `${track.nickname || '탈퇴한 멤버'} ${partLabel(track.part)}`
// 재생·병합에 쓰는 트랙: 불러온 트랙 + 올리기 전 녹음
const mixTracks = computed(() => [...tracks.value.filter(track => track.buffer), ...(take.value ? [take.value] : [])])
const totalLength = computed(() => timelineLength(mixTracks.value.map(track => ({ offsetMs: track.offsetMs, duration: track.buffer.duration }))))
const audibleIds = computed(() => new Set(audibleTracks(mixTracks.value, soloIds.value).map(track => track.id)))
const isAudible = track => audibleIds.value.has(track.id)
const mixable = computed(() => audibleIds.value.size > 0)
const canPlay = computed(() => mixable.value || metronomeOn.value)
const trackLimitReached = computed(() => tracks.value.length >= JAM_MAX_TRACKS)
const fileDisabled = computed(() => busy.value || recording.value || measuring.value || uploading.value || trackLimitReached.value)
const takeLabel = computed(() => take.value?.file ? '불러온 파일' : '새 녹음')
// 띄어쓰기·대소문자와 상관없이 곡 제목·아티스트·셋리스트 이름으로 찾는다. 곡이 많아도 입력한 것만 몇 개 보여 준다
const SONG_RESULT_LIMIT = 6
const normalize = text => (text || '').toLowerCase().replace(/\s+/g, '')
const songMatches = computed(() => {
  const query = normalize(songQuery.value)
  if (!query) return []
  return setlistSongs.value.filter(song => [song.title, song.artist, song.setlist].some(text => normalize(text).includes(query)))
})

function readLatency() {
  try { const value = localStorage.getItem(LATENCY_KEY); return value === null ? null : clamp(Number(value) || 0, 0, LATENCY_MAX_MS) } catch { return null }
}
function setLatency(value) {
  latencyMs.value = clamp(Math.round(value), 0, LATENCY_MAX_MS)
  try { localStorage.setItem(LATENCY_KEY, String(latencyMs.value)) } catch { /* 저장 못 해도 이번 화면에서는 쓴다 */ }
}

async function run(work, fallback = '저장하지 못했어요. 잠시 후 다시 시도해 주세요.') {
  if (busy.value) return false
  busy.value = true
  saveError.value = ''
  try { await work(); return true }
  catch (e) { saveError.value = apiErrorMessage(e, fallback); return false }
  finally { busy.value = false }
}

async function loadBand() {
  const token = ++request
  band.value = null
  error.value = ''
  try {
    const id = Number(route.params.id)
    const [result, list] = await Promise.all([getBand(id), listJams(id)])
    if (token !== request) return
    jams.value = list
    band.value = result
    store.upsertBand(result, true)
    store.isBandMode = true
    const wanted = Number(route.query.jam)
    const first = list.find(item => item.id === wanted) || list[0]
    if (first) selectJam(first.id, true)
  } catch (e) { if (token === request) error.value = apiErrorMessage(e, '레이어 잼을 불러오지 못했어요.') }
}

/* ---------- 잼 고르기·트랙 불러오기 ---------- */

async function leaveAllowed() {
  if (recording.value) return dialog.confirm({ title: '녹음 중이에요', text: '녹음을 버리고 나갈까요?', okText: '버리고 나가기', danger: true })
  return !take.value || dialog.confirm({ title: '올리지 않은 녹음이 있어요', text: '녹음을 버릴까요?', okText: '버리기', danger: true })
}
async function selectJam(id, force = false) {
  if (!force && id === selectedId.value) { fold.collapseIfNarrow(); return }
  if (!force && !await leaveAllowed()) return
  if (!force) fold.collapseIfNarrow()
  if (recording.value) await cancelRecording()
  stopPlayback(true)
  discardTake()
  soloIds.value = []
  selectedId.value = id
  saveError.value = notice.value = ''
  if (Number(route.query.jam) !== id) router.replace({ query: { ...route.query, jam: id } })
  const token = ++jamRequest
  jamLoading.value = true
  try {
    const result = await getJam(bandId(), id)
    if (token !== jamRequest) return
    showJam(result)
  } catch (e) {
    if (token === jamRequest) { jam.value = null; saveError.value = apiErrorMessage(e, '잼을 불러오지 못했어요.') }
  } finally { if (token === jamRequest) jamLoading.value = false }
}
function showJam(result) {
  jam.value = result
  tracks.value = result.tracks.map(track => ({ ...track, muted: false, buffer: null, status: 'loading' }))
  tracks.value.forEach(track => loadTrack(track, jamRequest))
  syncSummary()
}
async function loadTrack(track, token) {
  try {
    const buffer = await engine.decode(await loadJamTrackFile(track.url))
    if (token !== jamRequest) return
    const target = tracks.value.find(item => item.id === track.id)
    if (target) Object.assign(target, { buffer, status: 'ready' })
  } catch {
    const target = tracks.value.find(item => item.id === track.id)
    if (target && token === jamRequest) target.status = 'error'
  }
}
// 목록 카드의 트랙 수·제목을 지금 잼에 맞춘다
function syncSummary() {
  const item = jams.value.find(entry => entry.id === jam.value?.id)
  if (item) Object.assign(item, { title: jam.value.title, bpm: jam.value.bpm, song: jam.value.song, trackCount: tracks.value.length })
}

/* ---------- 재생 ---------- */

function tick() {
  position.value = engine.position
  countInBeat.value = recording.value && countInOn.value ? engine.countInBeat(jam.value?.bpm, COUNT_IN_BEATS) : 0
  if (playing.value) frame = requestAnimationFrame(tick)
}
async function startPlayback(from) {
  await engine.play(mixTracks.value, soloIds.value, from, { bpm: jam.value.bpm, metronome: metronomeOn.value })
  playing.value = true
  cancelAnimationFrame(frame)
  frame = requestAnimationFrame(tick)
}
function stopPlayback(toStart = false) {
  engine.stop(toStart)
  playing.value = false
  cancelAnimationFrame(frame)
  position.value = engine.position
}
engine.onEnded = () => { playing.value = false; cancelAnimationFrame(frame); position.value = 0 }
function togglePlay() { if (playing.value) stopPlayback(); else startPlayback(position.value >= totalLength.value && totalLength.value ? 0 : position.value) }
function rewind() { if (playing.value) startPlayback(0); else { engine.seek(0); position.value = 0 } }
function seek(value) { if (playing.value) startPlayback(value); else { engine.seek(value); position.value = value } }
// 음소거·솔로·시작 위치가 바뀌면 지금 위치부터 다시 예약한다
function restart() { if (playing.value) startPlayback(engine.position) }

/* ---------- 믹서 ---------- */

function changeVolume(track, value) {
  track.volume = clamp(value, 0, JAM_VOLUME_MAX)
  engine.setVolume(track.id, track.volume)
}
function toggleMute(track) {
  track.muted = !track.muted
  restart()
}
function toggleSolo(id) {
  soloIds.value = soloIds.value.includes(id) ? soloIds.value.filter(item => item !== id) : [...soloIds.value, id]
  restart()
}
function changeOffset(track, value, save = false) {
  track.offsetMs = clamp(Math.round(Number(value) || 0), JAM_OFFSET_MIN_MS, JAM_OFFSET_MAX_MS)
  restart()
  if (save) saveTrack(track, { offsetMs: track.offsetMs })
}
function nudgeTake(ms) { changeOffset(take.value, take.value.offsetMs + ms) }
// 볼륨·시작 위치는 멤버 모두가 함께 듣는 믹스라 바로 저장한다. 뮤트·솔로는 내 화면에서만 바꾼다
async function saveTrack(track, changes) {
  const jamId = jam.value.id
  try {
    const saved = await updateJamTrack(bandId(), jamId, track.id, changes)
    if (jam.value?.id === jamId) Object.assign(track, { volume: saved.volume, offsetMs: saved.offsetMs })
  } catch (e) { saveError.value = apiErrorMessage(e, '믹서 설정을 저장하지 못했어요.') }
}

async function downloadMix() {
  await run(async () => {
    notice.value = '음원을 합치는 중이에요…'
    const blob = await engine.mixdown(mixTracks.value, soloIds.value)
    if (!blob) throw new Error('empty')
    const url = URL.createObjectURL(blob)
    Object.assign(document.createElement('a'), { href: url, download: `${jam.value.title}.wav` }).click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    notice.value = '들리는 트랙을 합친 음원(WAV)을 내려받았어요.'
  }, '음원을 합치지 못했어요.')
  if (saveError.value) notice.value = ''
}

/* ---------- 녹음 ---------- */

async function connectMic() {
  saveError.value = ''
  try {
    await engine.openMic()
    micReady.value = true
    latencyEstimate.value = engine.estimatedLatencyMs()
    if (readLatency() === null) setLatency(latencyEstimate.value)
  } catch (e) {
    saveError.value = e?.name === 'NotAllowedError' ? '마이크 권한을 허용해 주세요. 주소창 옆 권한 설정에서 바꿀 수 있어요.' : '마이크를 열지 못했어요. 마이크가 연결되어 있는지 확인해 주세요.'
  }
}
async function measure() {
  stopPlayback()
  measuring.value = true
  saveError.value = notice.value = ''
  try {
    const result = await engine.measureLatency()
    if (result === null) saveError.value = '클릭 소리를 찾지 못했어요. 이어폰을 빼고 스피커 음량을 높인 뒤 다시 측정해 주세요.'
    else { setLatency(result); notice.value = `녹음 지연을 ${latencyMs.value}ms로 맞췄어요.` }
  } catch { saveError.value = '지연을 측정하지 못했어요.' }
  finally { measuring.value = false }
}
async function startRecording() {
  if (take.value && !await dialog.confirm({ title: '다시 녹음할까요?', text: '올리지 않은 녹음은 버려져요.', okText: '다시 녹음', danger: true })) return
  discardTake()
  stopPlayback(true)
  saveError.value = notice.value = ''
  try {
    session = await engine.record(tracks.value.filter(track => track.buffer), soloIds.value, { bpm: jam.value.bpm, metronome: metronomeOn.value, countIn: countInOn.value ? COUNT_IN_BEATS : 0 })
    recording.value = true
    playing.value = true
    frame = requestAnimationFrame(tick)
  } catch { session = null; saveError.value = '녹음을 시작하지 못했어요.' }
}
async function stopRecording() {
  if (!session) return
  const { recordStart, timelineStart } = session
  const blob = await finishSession()
  try {
    const buffer = await engine.decode(await blob.arrayBuffer())
    const offsetMs = clamp(recordedOffsetMs(recordStart, timelineStart, latencyMs.value), JAM_OFFSET_MIN_MS, JAM_OFFSET_MAX_MS)
    take.value = { id: 'take', blob, file: null, buffer, offsetMs, volume: 100, muted: false }
    notice.value = '녹음했어요. 재생해서 들어 보고 트랙을 올려 주세요.'
  } catch { saveError.value = '녹음한 소리를 읽지 못했어요. 다시 녹음해 주세요.' }
}
async function finishSession() {
  const current = session
  session = null
  recording.value = false
  playing.value = false
  countInBeat.value = 0
  cancelAnimationFrame(frame)
  const blob = await current.stop()
  position.value = 0
  return blob
}
async function cancelRecording() { if (session) await finishSession() }
// 따로 녹음한 음원은 0초를 곡 시작에 두고, 들어 보며 시작 위치를 맞춘다
async function pickFile(event) {
  const file = event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (take.value && !await dialog.confirm({ title: '이 파일로 바꿀까요?', text: '올리지 않은 트랙은 버려져요.', okText: '바꾸기', danger: true })) return
  saveError.value = notice.value = ''
  if (file.size > JAM_TRACK_MAX_BYTES) { saveError.value = '음원 파일은 30MB까지 올릴 수 있어요.'; return }
  stopPlayback()
  discardTake()
  try {
    const buffer = await engine.decode(await file.arrayBuffer())
    take.value = { id: 'take', blob: file, file, buffer, offsetMs: 0, volume: 100, muted: false }
    notice.value = '파일을 불러왔어요. 재생해서 다른 트랙과 맞는지 들어 보고 올려 주세요.'
  } catch { saveError.value = '재생할 수 없는 파일이에요. MP3·WAV·M4A·OGG·WebM 파일을 골라 주세요.' }
}
// ask가 없으면 바로(동기로) 버린다
async function discardTake(ask = false) {
  if (ask && !await dialog.confirm({ title: '녹음을 버릴까요?', okText: '버리기', danger: true })) return
  if (take.value && playing.value) stopPlayback()
  take.value = null
}
async function uploadTake() {
  const current = take.value
  if (current.blob.size > JAM_TRACK_MAX_BYTES) { saveError.value = '녹음 파일이 30MB를 넘어요. 조금 짧게 녹음해 주세요.'; return }
  stopPlayback()
  uploading.value = true
  uploadProgress.value = 0
  saveError.value = ''
  const jamId = jam.value.id
  try {
    const file = current.file || new File([current.blob], `layer-jam-${Date.now()}.${recordingExtension(current.blob.type)}`, { type: current.blob.type })
    const saved = await uploadJamTrack(bandId(), jamId, { offsetMs: current.offsetMs, volume: current.volume, durationMs: Math.round(current.buffer.duration * 1000) }, file, value => { uploadProgress.value = value })
    if (jam.value?.id !== jamId) return
    tracks.value.push({ ...saved, muted: false, buffer: current.buffer, status: 'ready' })
    take.value = null
    syncSummary()
    notice.value = '트랙을 올렸어요.'
  } catch (e) { saveError.value = apiErrorMessage(e, '트랙을 올리지 못했어요. 잠시 후 다시 시도해 주세요.') }
  finally { uploading.value = false }
}

/* ---------- 잼 만들기·수정·삭제 ---------- */

async function openForm(target) {
  form.value = target ? { id: target.id, title: target.title, bpm: target.bpm, song: target.song ? { ...target.song } : null } : { id: null, title: '', bpm: 100, song: null }
  formError.value = ''
  songQuery.value = ''
  formDialog.value.showModal()
  songsLoading.value = true
  try {
    const lists = await listSetlists(bandId())
    setlistSongs.value = lists.flatMap(list => list.items.map(item => ({ id: item.id, title: item.title, artist: item.artist || '', setlist: list.title })))
    const linked = setlistSongs.value.find(song => song.id === form.value.song?.setlistItemId)
    if (linked) form.value.song.setlist = linked.setlist
  } catch { setlistSongs.value = [] }
  finally { songsLoading.value = false }
}
function pickSong(song) {
  form.value.song = { setlistItemId: song.id, title: song.title, setlist: song.setlist }
  songQuery.value = ''
  if (!form.value.title.trim()) form.value.title = song.title
}
async function saveForm() {
  const title = form.value.title.trim()
  const bpm = Number(form.value.bpm)
  if (!title) { formError.value = '잼 이름을 적어 주세요.'; return }
  if (!Number.isInteger(bpm) || bpm < 40 || bpm > 240) { formError.value = 'BPM은 40에서 240 사이로 적어 주세요.'; return }
  const payload = { title, bpm, setlistItemId: form.value.song?.setlistItemId ?? null }
  const id = form.value.id
  let result = null
  const ok = await run(async () => { result = id ? await updateJam(bandId(), id, payload) : await createJam(bandId(), payload) }, id ? '잼을 저장하지 못했어요.' : '잼을 만들지 못했어요.')
  if (!ok) { formError.value = saveError.value; saveError.value = ''; return }
  formDialog.value.close()
  if (id) {
    // 목록 연필로 고르지 않은 잼을 고쳤으면 목록 줄만 바꾼다 (#254)
    if (id === selectedId.value) {
      Object.assign(jam.value, { title: result.title, bpm: result.bpm, song: result.song })
      syncSummary()
      restart()
    } else {
      const item = jams.value.find(entry => entry.id === id)
      if (item) Object.assign(item, { title: result.title, bpm: result.bpm, song: result.song })
    }
    notice.value = '잼을 저장했어요.'
  } else {
    jams.value.unshift({ id: result.id, title: result.title, bpm: result.bpm, song: result.song, trackCount: 0, creatorName: result.creatorName })
    await selectJam(result.id)
    notice.value = '잼을 만들었어요. 첫 파트를 녹음해 보세요.'
  }
}
function askDelete(target) { deleteTarget.value = target; deleteDialog.value.showModal() }
async function toggleFavorite(item) {
  await run(async () => Object.assign(item, await setJamFavorite(bandId(), item.id, !item.favorite)), '즐겨찾기를 바꾸지 못했어요.')
}
// 고른 잼 여러 개 지우기: 하나씩 지우고, 지운 것만 목록에서 뺀다
async function deleteChecked(ids) {
  const removed = []
  const ok = await run(async () => {
    for (const id of ids) {
      await deleteJam(bandId(), id)
      removed.push(id)
    }
  }, '일부 잼을 삭제하지 못했어요.')
  deleteDialog.value.close()
  jams.value = jams.value.filter(item => !removed.includes(item.id))
  checkedIds.value = checkedIds.value.filter(id => !removed.includes(id))
  if (removed.includes(jam.value?.id)) {
    stopPlayback(true)
    discardTake()
    jam.value = null
    tracks.value = []
    selectedId.value = null
    router.replace({ query: {} })
  }
  if (ok) {
    selecting.value = false
    notice.value = `잼 ${removed.length}개를 삭제했어요.`
  }
}
async function confirmDelete() {
  const target = deleteTarget.value
  if (target.kind === 'bulk') return deleteChecked(target.ids)
  const jamId = jam.value.id
  const ok = await run(async () => {
    if (target.kind === 'jam') await deleteJam(bandId(), jamId)
    else await deleteJamTrack(bandId(), jamId, target.track.id)
  }, '삭제하지 못했어요.')
  deleteDialog.value.close()
  if (!ok) return
  if (target.kind === 'track') {
    stopPlayback()
    tracks.value = tracks.value.filter(track => track.id !== target.track.id)
    soloIds.value = soloIds.value.filter(id => id !== target.track.id)
    syncSummary()
    notice.value = '트랙을 삭제했어요.'
    return
  }
  stopPlayback(true)
  discardTake()
  jams.value = jams.value.filter(item => item.id !== jamId)
  jam.value = null
  tracks.value = []
  selectedId.value = null
  const next = jams.value[0]
  if (next) await selectJam(next.id, true)
  else router.replace({ query: {} })
  notice.value = '잼을 삭제했어요.'
}

function beforeUnload(event) { if (recording.value || take.value) { event.preventDefault(); event.returnValue = '' } }
onBeforeRouteLeave(() => leaveAllowed())
watch(() => route.params.id, () => { if (route.name === 'band-jams') loadBand() }, { immediate: true })
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
  cancelAnimationFrame(frame)
  engine.close()
})
</script>

<style scoped>
.page,.page *,.jam-dialog,.jam-dialog * { box-sizing:border-box; }
.page { display:flex; flex-direction:column; gap:16px; }
.muted { color:var(--c-muted); font-size:.8rem; line-height:1.7; }
.status { margin: 0; padding: 10px 14px; border-radius: var(--radius-md); background: var(--c-primary-soft); color: var(--c-primary-text); font-size: 0.8rem; font-weight: 700; line-height: 1.6; }
.save-error { margin: 0; padding: 10px 14px; border-radius: var(--radius-md); background: var(--c-danger-soft); color: var(--c-danger); font-size: 0.8rem; font-weight: 700; line-height: 1.6; }
.workspace { display:grid; grid-template-columns:250px minmax(0,1fr); gap:20px; align-items:start; }
.sidebar,.detail { min-width:0; }
.detail :deep(.ws-card-head) { align-items:center; flex-wrap:wrap; }
.detail :deep(.ws-card-head h3) { min-width:0; overflow-wrap:anywhere; flex:1 1 160px; }
button:disabled { opacity:.4; cursor:default; }
.danger { color:var(--c-danger); }
button:focus-visible,a:focus-visible { outline:2px solid var(--c-primary-line); outline-offset:2px; }
.add-jam { width:100%; margin-bottom:12px; background:var(--c-bg); border-style:dashed; }
.jam-list { list-style:none; margin:0; padding:0; display:flex; flex-direction:column; gap:8px; }
.jam-row { display:flex; align-items:center; gap:6px; }
.jam-row input[type='checkbox'] { width:16px; height:16px; flex:none; }
.jam-row .jam-link { flex:1; min-width:0; }
.list-tools,.bulk-tools { display:flex; align-items:center; justify-content:space-between; gap:8px; margin:0 0 12px; font-size:.73rem; color:var(--c-muted); }
.text-btn { display:inline-flex; align-items:center; gap:4px; border:0; background:transparent; padding:4px 0; font-family:inherit; font-size:.74rem; color:var(--c-text-2); cursor:pointer; }
.text-btn[aria-pressed='true'] { color:#f59f00; }
.text-btn.danger { color:var(--c-danger); }
.text-btn:disabled { opacity:.45; cursor:default; }
.star { display:inline-flex; background:transparent; border:0; color:#ced4da; cursor:pointer; padding:3px; flex:none; }
.star.on { color:#fab005; }
.rename { flex:none; display:flex; padding:5px; border:0; border-radius:8px; background:transparent; color:var(--c-faint); cursor:pointer; }
.rename:hover:not(:disabled) { color:var(--c-text-2); background:var(--c-line-soft); }
.rename-space { flex:none; width:24px; }
/* 목록 검색·접기 (#254, 셋리스트와 같은 모양) */
.list-search { display:flex; align-items:center; gap:7px; height:36px; padding:0 10px; margin-bottom:10px; border:1px solid var(--c-line); border-radius:10px; background:var(--c-bg); color:var(--c-faint); }
.list-search:focus-within { border-color:var(--c-primary); background:#fff; }
.list-search input { flex:1; min-width:0; border:none; outline:none; background:none; font:inherit; font-size:16px; color:var(--c-text); }
.fold { display:inline-flex; align-items:center; gap:3px; }
.chevron { transition:transform .2s; }
.chevron.open { transform:rotate(180deg); }
.list-summary { width:100%; display:flex; align-items:center; justify-content:space-between; gap:10px; padding:10px 12px; border:1px solid var(--c-primary-line); border-radius:12px; background:var(--c-primary-soft); font:inherit; text-align:left; color:var(--c-primary-text); cursor:pointer; }
.list-summary strong { min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:.82rem; }
.list-summary span { flex:none; font-size:.72rem; font-weight:700; color:var(--c-muted); }
.jam-link { width:100%; display:flex; flex-direction:column; gap:5px; padding:10px 12px; text-align:left; border:1px solid var(--c-line); border-radius:12px; background:#fff; font-family:inherit; color:#343a40; cursor:pointer; overflow-wrap:anywhere; }
.jam-link.active { background:var(--c-primary-soft); border-color:var(--c-primary-line); }
.jam-link strong { font-size:.79rem; }
.jam-link span,.jam-link small { color:var(--c-muted); font-size:.7rem; }
.jam-meta { margin:0 0 14px; color:var(--c-muted); font-size:.76rem; }
.jam-meta a { color: var(--c-primary-text); font-weight:700; text-decoration:none; }
.transport { display:flex; align-items:center; flex-wrap:wrap; gap:10px; padding:12px; background:var(--c-bg); border-radius:12px; }
.play { min-width:84px; }
.time { font-size:.78rem; color:var(--c-muted); font-variant-numeric:tabular-nums; }
.time b { color:#343a40; }
.seek { flex:1 1 160px; min-width:120px; accent-color:var(--c-primary); }
.check { display:inline-flex; align-items:center; gap:6px; font-size:.76rem; color:var(--c-text-2); cursor:pointer; }
.tracks { list-style:none; margin:14px 0 0; padding:0; display:flex; flex-direction:column; gap:8px; }
.track { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:10px 16px; padding:12px 14px; border:1px solid var(--c-line); border-radius:12px; background:#fff; }
.track.silent { background:var(--c-bg); }
.track.silent .track-name { opacity:.55; }
.track.take { border-color:#ffc9c9; background:#fff8f8; }
.track-name { display:flex; flex-direction:column; gap:3px; min-width:120px; flex:1 1 140px; }
.track-name strong { font-size:.8rem; color:#343a40; overflow-wrap:anywhere; }
.track-name small { font-size:.7rem; color:var(--c-muted); }
.track-error { color:var(--c-danger) !important; }
.track-controls { display:flex; align-items:center; flex-wrap:wrap; gap:8px 12px; }
.toggle { width:30px; height:30px; border:1px solid var(--c-line); border-radius:8px; background:#fff; color:var(--c-muted); font-family:inherit; font-weight:800; font-size:.75rem; cursor:pointer; }
.toggle.on { background:#ff6b6b; border-color:#ff6b6b; color:#fff; }
.toggle.solo.on { background:#fab005; border-color:#fab005; }
.volume,.offset,.latency label { display:flex; flex-direction:column; gap:3px; font-size:.68rem; color:var(--c-muted); }
.volume input { width:120px; accent-color:var(--c-primary); }
.offset input { width:96px; border:1px solid var(--c-line); padding:5px 8px; border-radius:8px; font-family:inherit; font-size:.76rem; color:#343a40; outline:none; }
.remove { border:0; background:transparent; color:var(--c-faint); font-size:20px; cursor:pointer; padding:2px 6px; }
.remove:hover:not(:disabled) { color:var(--c-danger); }
.count-in { display:inline-flex; align-items:center; justify-content:center; width:44px; height:44px; border-radius:50%; background:#fff0f0; color:#e03131; font-size:1.5rem; font-weight:900; line-height:1; font-variant-numeric:tabular-nums; }
.nudge { display:flex; align-items:flex-end; flex-wrap:wrap; gap:6px; }
.nudge .btn { min-height:30px; padding:5px 9px; }
.take-hint { flex-basis:100%; margin:0; font-size:.7rem; color:var(--c-muted); }
.take-actions { flex-basis:100%; display:flex; justify-content:flex-end; align-items:center; flex-wrap:wrap; gap:8px; }
.no-tracks { padding:18px; text-align:center; border:1px dashed var(--c-line); border-radius:12px; }
.recorder { margin-top:18px; padding:16px; border-radius:12px; background:var(--c-bg); display:flex; flex-direction:column; align-items:flex-start; gap:10px; }
.recorder h4 { margin:0; font-size:.85rem; color:#343a40; }
.hint { margin:0; font-size:.74rem; color:var(--c-muted); }
.latency { display:flex; flex-direction:column; align-items:flex-start; gap:6px; width:100%; max-width:420px; }
.latency label { width:100%; }
.latency label b { color:#343a40; }
.latency input { width:100%; accent-color:var(--c-primary); }
.latency small { font-size:.7rem; color:var(--c-muted); }
.record-row { display:flex; align-items:center; flex-wrap:wrap; gap:12px; }
.record { color:var(--c-danger); border-color:#ffc9c9; }
.record.on { background:var(--c-danger); border-color:var(--c-danger); color:#fff; }
/* 공통 .btn 회색 hover 대신 녹음 색을 유지한다 */
.record:hover:not(:disabled) { background:var(--c-danger-soft); }
.record.on:hover:not(:disabled) { background:#c92a2a; border-color:#c92a2a; }
.rec-time { font-size:.78rem; color:var(--c-danger); font-weight:700; font-variant-numeric:tabular-nums; }
.jam-dialog { width:min(420px, calc(100vw - 32px)); border:0; border-radius:var(--radius-xl); padding:24px; font-family:var(--font-sans); color:var(--c-text); box-shadow:var(--shadow-lg); }
.jam-dialog::backdrop { background:rgba(15,23,42,.45); }
.jam-dialog h3 { margin:0 0 14px; font-size:1rem; }
.jam-dialog form { display:flex; flex-direction:column; gap:12px; }
.jam-dialog label { display:flex; flex-direction:column; gap:6px; font-size:.75rem; color:var(--c-muted); }
.jam-dialog input,.jam-dialog select { width:100%; border:1px solid var(--c-line); padding:8px 10px; border-radius:8px; background:#fff; color:#343a40; font-family:inherit; font-size:.8rem; outline:none; box-shadow:none; }
.jam-dialog input::placeholder { color:var(--c-faint); opacity:1; }
/* 입력칸은 다른 화면처럼 테두리 색만 바꾼다 */
.offset input:focus,.jam-dialog input:focus,.jam-dialog select:focus { border-color:var(--c-primary); outline:none; box-shadow:none; }
.guide { margin:0 0 14px; padding:10px 14px; border:1px solid var(--c-line); border-radius:12px; font-size:.76rem; color:var(--c-text-2); }
.guide summary { cursor:pointer; font-weight:700; color: var(--c-primary-text); }
.guide ol { margin:10px 0 2px; padding-left:20px; display:flex; flex-direction:column; gap:6px; line-height:1.6; }
.legend { list-style:none; margin:12px 0 0; padding:0; display:flex; flex-wrap:wrap; align-items:center; gap:4px 14px; font-size:.72rem; color:var(--c-muted); }
.legend li { display:inline-flex; align-items:center; gap:6px; white-space:nowrap; }
.legend .legend-note { color:var(--c-faint); }
.toggle-chip { display:inline-flex; align-items:center; justify-content:center; width:20px; height:20px; border-radius:6px; background:#ff6b6b; color:#fff; font-weight:800; font-size:.65rem; }
.toggle-chip.solo { background:#fab005; }
.add-ways { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; width:100%; }
.way { display:flex; flex-direction:column; align-items:flex-start; gap:8px; min-width:0; padding:14px; border-radius:10px; background:#fff; border:1px solid var(--c-line); }
.way > strong { font-size:.8rem; color:#343a40; }
.file-picker { position:relative; overflow:hidden; }
.file-picker input { position:absolute; inset:0; opacity:0; cursor:pointer; }
.file-picker.disabled { opacity:.4; }
.file-picker:focus-within { outline:2px solid var(--c-primary-line); outline-offset:2px; }
.song-picker { margin:0; padding:0; border:0; min-width:0; display:flex; flex-direction:column; gap:8px; }
.song-picker legend { padding:0; margin-bottom:6px; font-size:.75rem; color:var(--c-muted); }
.picked { display:flex; align-items:center; justify-content:space-between; gap:8px; padding:8px 10px; border-radius:8px; background:var(--c-primary-soft); font-size:.78rem; color:#343a40; }
.picked small { color:var(--c-muted); }
.text-btn { border:0; background:transparent; padding:2px 0; font-family:inherit; font-size:.74rem; color:var(--c-muted); cursor:pointer; flex-shrink:0; }
.song-results { list-style:none; margin:0; padding:4px; border:1px solid var(--c-line); border-radius:10px; }
.song-option { width:100%; display:flex; justify-content:space-between; align-items:baseline; gap:10px; padding:8px 10px; border:0; border-radius:8px; background:transparent; font-family:inherit; text-align:left; cursor:pointer; color:#343a40; font-size:.78rem; }
.song-option span { min-width:0; overflow-wrap:anywhere; }
.song-option small { color:var(--c-muted); font-size:.7rem; flex-shrink:0; }
.song-option:hover,.song-option:focus-visible { background:var(--c-primary-soft); }
.more { margin:0; font-size:.72rem; }
@media (max-width: 860px) {
  .workspace { grid-template-columns:minmax(0,1fr); }
  .track { align-items:flex-start; position:relative; }
  /* 줄이 바뀌면 삭제(×)가 아래로 밀려나서 카드 오른쪽 위에 둔다 (#234) */
  .remove { position:absolute; top:6px; right:6px; }
  .track-name { padding-right:28px; }
  .volume input { width:100px; }
  .add-ways { grid-template-columns:minmax(0,1fr); }
}
/* 휴대폰: 팝업은 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  .jam-dialog { width: 100%; max-width: none; margin: auto 0 0; border-radius: var(--radius-xl) var(--radius-xl) 0 0; padding-bottom: calc(24px + env(safe-area-inset-bottom)); }
}
</style>
