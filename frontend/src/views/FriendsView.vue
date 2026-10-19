<template>
  <SubPageLayout pill="FRIENDS">
    <div class="page">
      <div class="page-head">
        <h2>친구</h2>
        <p>밴드 멤버에게 바로, 또는 친구 코드로 친구를 맺어요</p>
      </div>
      <p v-if="actionError" class="error">{{ actionError }}</p>

      <!-- 내 친구 코드 + 코드로 신청 -->
      <WsCard title="친구 추가">
        <div class="code-box">
          <span class="label">내 친구 코드</span>
          <div class="code-row">
            <span class="code">{{ myCode || '········' }}</span>
            <button class="btn small" :disabled="!myCode" @click="copyCode">{{ copied ? '복사했어요' : '복사' }}</button>
            <button class="btn small ghost" :disabled="!myCode || codeBusy" @click="renewCode">새 코드 받기</button>
          </div>
          <p class="hint">이 코드를 친구에게 알려 주세요. 새 코드를 받으면 예전 코드로는 신청할 수 없어요.</p>
        </div>

        <form class="request-form" @submit.prevent="sendRequest">
          <input
            v-model="codeInput"
            type="text"
            :maxlength="FRIEND_CODE_LENGTH"
            placeholder="친구 코드 입력"
            aria-label="친구 코드"
            :disabled="sending"
            @input="codeInput = codeInput.toUpperCase().replace(/\s/g, '')"
          >
          <button type="submit" class="btn primary" :disabled="sending || codeInput.length !== FRIEND_CODE_LENGTH">
            {{ sending ? '보내는 중…' : '친구 신청' }}
          </button>
        </form>
        <p v-if="requestError" class="field-error">{{ requestError }}</p>
        <p v-if="requestStatus" class="saved">{{ requestStatus }}</p>
      </WsCard>

      <!-- 같은 밴드 멤버: 코드 없이 바로 신청 -->
      <WsCard v-if="mateRows.length" :title="`같은 밴드 멤버 ${mateRows.length}`">
        <ul class="people">
          <li v-for="m in mateRows" :key="m.userId" class="person">
            <ProfileAvatar :src="m.profileImageUrl" :size="44" :alt="`${m.name} 프로필 사진`" />
            <div class="who">
              <strong>{{ m.name }}</strong>
              <span>{{ m.bandNames.join(', ') }} · {{ levelText(m) }}</span>
            </div>
            <div class="actions">
              <button v-if="m.received" class="btn small primary" :disabled="busyId === m.received.requestId" @click="accept(m.received)">수락</button>
              <button v-else-if="m.sent" class="btn small ghost" disabled>신청함</button>
              <button v-else class="btn small primary" :disabled="mateBusyId === m.userId" @click="requestMate(m)">
                {{ mateBusyId === m.userId ? '보내는 중…' : '친구 신청' }}
              </button>
            </div>
          </li>
        </ul>
      </WsCard>

      <!-- 받은 신청 -->
      <WsCard v-if="received.length" :title="`받은 신청 ${received.length}`">
        <ul class="people">
          <li v-for="r in received" :key="r.requestId" class="person">
            <ProfileAvatar :src="r.user.profileImageUrl" :size="44" :alt="`${r.user.name} 프로필 사진`" />
            <div class="who">
              <strong>{{ r.user.name }}</strong>
              <span>{{ levelText(r.user) }} · {{ agoText(r.createdAt) }}</span>
            </div>
            <div class="actions">
              <button class="btn small primary" :disabled="busyId === r.requestId" @click="accept(r)">수락</button>
              <button class="btn small ghost" :disabled="busyId === r.requestId" @click="reject(r)">거절</button>
            </div>
          </li>
        </ul>
      </WsCard>

      <!-- 친구 목록 -->
      <WsCard :title="`내 친구 ${friends.length}`">
        <p v-if="loadError" class="error">{{ loadError }}</p>
        <div v-else-if="!loaded" class="loading">불러오는 중…</div>
        <p v-else-if="!friends.length" class="empty">아직 친구가 없어요. 친구 코드로 신청해 보세요!</p>
        <ul v-else class="people">
          <li v-for="f in friends" :key="f.userId" class="friend-row">
            <button class="person clickable" title="프로필 보기" @click="selectedId = f.userId">
              <ProfileAvatar :src="f.profileImageUrl" :size="44" :alt="`${f.name} 프로필 사진`" />
              <div class="who">
                <strong>{{ f.name }}</strong>
                <span>{{ levelText(f) }}</span>
              </div>
            </button>
            <button class="remove-btn" :disabled="removingId === f.userId" @click="unfriend(f)">{{ removingId === f.userId ? '삭제 중…' : '삭제' }}</button>
          </li>
        </ul>
      </WsCard>

      <!-- 보낸 신청 -->
      <WsCard v-if="sent.length" :title="`보낸 신청 ${sent.length}`">
        <ul class="people">
          <li v-for="r in sent" :key="r.requestId" class="person">
            <ProfileAvatar :src="r.user.profileImageUrl" :size="44" :alt="`${r.user.name} 프로필 사진`" />
            <div class="who">
              <strong>{{ r.user.name }}</strong>
              <span>수락을 기다리는 중 · {{ agoText(r.createdAt) }}</span>
            </div>
            <div class="actions">
              <button class="btn small ghost" :disabled="busyId === r.requestId" @click="cancel(r)">취소</button>
            </div>
          </li>
        </ul>
      </WsCard>
    </div>

    <FriendProfileModal ref="profileModal" :user-id="selectedId" @close="selectedId = null" @remove="unfriend" />
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SubPageLayout from '../components/workspace/SubPageLayout.vue'
import WsCard from '../components/workspace/WsCard.vue'
import ProfileAvatar from '../components/ProfileAvatar.vue'
import FriendProfileModal from '../components/friend/FriendProfileModal.vue'
import {
  getMyFriendCode, regenerateFriendCode, sendFriendRequest,
  getReceivedRequests, getSentRequests, acceptFriendRequest, deleteFriendRequest,
  getFriends, removeFriend, getBandMates, sendFriendRequestToBandMate, FRIEND_CODE_LENGTH,
} from '../api/friend'
import { apiErrorMessage } from '../api/practice'

const myCode = ref('')
const friends = ref([])
const received = ref([])
const sent = ref([])
const bandMates = ref([])
const loaded = ref(false)
const loadError = ref('')
const actionError = ref('')
const busyId = ref(null)
const selectedId = ref(null)

const levelText = (user) => (user.character ? `${user.character.nickname} · Lv.${user.character.level ?? 1}` : '캐릭터 없음')

const agoText = (at) => {
  const d = new Date(at)
  if (Number.isNaN(d.getTime())) return ''
  const minutes = Math.floor((Date.now() - d.getTime()) / 60000)
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${minutes}분 전`
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}시간 전`
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

const loadAll = async () => {
  loadError.value = ''
  try {
    const [code, friendList, receivedList, sentList, mates] = await Promise.all([
      getMyFriendCode(), getFriends(), getReceivedRequests(), getSentRequests(), getBandMates(),
    ])
    myCode.value = code.code
    bandMates.value = mates
    friends.value = friendList
    received.value = receivedList
    sent.value = sentList
  } catch (e) {
    loadError.value = apiErrorMessage(e, '친구 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    loaded.value = true
  }
}

onMounted(loadAll)

/* ---------- 내 친구 코드 ---------- */
const copied = ref(false)
const codeBusy = ref(false)

const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(myCode.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  } catch {
    // 클립보드를 못 쓰는 환경이면 직접 복사하게
    window.prompt('친구 코드를 복사해 주세요', myCode.value)
  }
}

const renewCode = async () => {
  if (!window.confirm('새 친구 코드를 받을까요? 예전 코드로는 더 이상 신청할 수 없어요.')) return
  codeBusy.value = true
  try {
    myCode.value = (await regenerateFriendCode()).code
  } catch (e) {
    actionError.value = apiErrorMessage(e, '새 코드를 받지 못했어요.')
  } finally {
    codeBusy.value = false
  }
}

/* ---------- 같은 밴드 멤버 ---------- */
// 이미 친구인 멤버는 빼고, 신청 상태를 붙인다
const mateRows = computed(() => {
  const friendIds = new Set(friends.value.map((f) => f.userId))
  return bandMates.value
    .filter((m) => !friendIds.has(m.userId))
    .map((m) => ({
      ...m,
      sent: sent.value.find((r) => r.user.userId === m.userId) || null,
      received: received.value.find((r) => r.user.userId === m.userId) || null,
    }))
})

const mateBusyId = ref(null)
const requestMate = async (mate) => {
  if (mateBusyId.value) return
  mateBusyId.value = mate.userId
  actionError.value = ''
  try {
    const request = await sendFriendRequestToBandMate(mate.userId)
    sent.value = [request, ...sent.value]
  } catch (e) {
    actionError.value = apiErrorMessage(e, '친구 신청을 보내지 못했어요.')
    await loadAll()
  } finally {
    mateBusyId.value = null
  }
}

/* ---------- 친구 신청 ---------- */
const codeInput = ref('')
const sending = ref(false)
const requestError = ref('')
const requestStatus = ref('')

const sendRequest = async () => {
  if (sending.value || codeInput.value.length !== FRIEND_CODE_LENGTH) return
  sending.value = true
  requestError.value = ''
  requestStatus.value = ''
  try {
    const request = await sendFriendRequest(codeInput.value)
    sent.value = [request, ...sent.value]
    requestStatus.value = `${request.user.name} 님에게 친구 신청을 보냈어요`
    codeInput.value = ''
  } catch (e) {
    requestError.value = apiErrorMessage(e, '친구 신청을 보내지 못했어요.')
  } finally {
    sending.value = false
  }
}

const runAction = async (requestId, action, fallback) => {
  busyId.value = requestId
  actionError.value = ''
  try {
    await action()
  } catch (e) {
    actionError.value = apiErrorMessage(e, fallback)
    // 상대가 먼저 취소했을 수도 있으니 최신 상태로
    await loadAll()
  } finally {
    busyId.value = null
  }
}

const accept = (r) => runAction(r.requestId, async () => {
  const friend = await acceptFriendRequest(r.requestId)
  received.value = received.value.filter((x) => x.requestId !== r.requestId)
  friends.value = [...friends.value, friend].sort((a, b) => a.name.localeCompare(b.name, 'ko'))
}, '신청을 수락하지 못했어요.')

const reject = (r) => runAction(r.requestId, async () => {
  if (!window.confirm(`${r.user.name} 님의 신청을 거절할까요?`)) return
  await deleteFriendRequest(r.requestId)
  received.value = received.value.filter((x) => x.requestId !== r.requestId)
}, '신청을 거절하지 못했어요.')

const cancel = (r) => runAction(r.requestId, async () => {
  if (!window.confirm(`${r.user.name} 님에게 보낸 신청을 취소할까요?`)) return
  await deleteFriendRequest(r.requestId)
  sent.value = sent.value.filter((x) => x.requestId !== r.requestId)
}, '신청을 취소하지 못했어요.')

/* ---------- 친구 삭제 (목록·프로필 팝업 공통) ---------- */
const profileModal = ref(null)
const removingId = ref(null)

const unfriend = async (friend) => {
  if (removingId.value) return
  if (!window.confirm(`${friend.name} 님을 친구에서 삭제할까요?`)) return
  removingId.value = friend.userId
  actionError.value = ''
  profileModal.value?.setRemoving(true)
  try {
    await removeFriend(friend.userId)
    friends.value = friends.value.filter((f) => f.userId !== friend.userId)
    if (selectedId.value === friend.userId) selectedId.value = null
  } catch (e) {
    const message = apiErrorMessage(e, '친구를 삭제하지 못했어요.')
    if (selectedId.value === friend.userId) profileModal.value?.setError(message)
    else actionError.value = message
  } finally {
    removingId.value = null
    profileModal.value?.setRemoving(false)
  }
}
</script>

<style scoped>
.page { max-width: 640px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.85rem; font-weight: 700; color: #adb5bd; }

.code-box { padding-bottom: 16px; border-bottom: 1px solid #f1f3f5; }
.label { font-size: 0.75rem; font-weight: 800; color: #868e96; }
.code-row { display: flex; align-items: center; gap: 6px; margin-top: 6px; flex-wrap: wrap; }
.code {
  font-family: 'Courier New', monospace; font-size: 1.25rem; font-weight: 900; letter-spacing: 3px;
  color: #2d5bff; background: #f0f4ff; padding: 6px 12px; border-radius: 10px; margin-right: 4px;
}
.hint { margin: 8px 0 0; font-size: 0.72rem; font-weight: 700; color: #adb5bd; line-height: 1.5; }

.request-form { display: flex; gap: 8px; margin-top: 16px; }
.request-form input {
  flex: 1; min-width: 0; border: 1px solid #e9ecef; border-radius: 10px; padding: 9px 12px; background: #f8f9fa;
  font-family: 'Courier New', monospace; font-size: 0.95rem; font-weight: 800; letter-spacing: 2px; outline: none;
}
.request-form input:focus { border-color: #2d5bff; background: #fff; }

.people { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.people > li + li { border-top: 1px solid #f1f3f5; }
.person { display: flex; align-items: center; gap: 12px; padding: 10px 2px; }
.friend-row { display: flex; align-items: center; gap: 4px; }
.remove-btn {
  flex: none; border: 1px solid #e9ecef; background: #fff; border-radius: 10px; padding: 7px 11px; cursor: pointer;
  font-family: inherit; font-size: 0.75rem; font-weight: 800; color: #868e96; white-space: nowrap; transition: 0.15s;
}
.remove-btn:hover:not(:disabled) { color: #e03131; border-color: #ffc9c9; background: #fff5f5; }
.remove-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.person.clickable {
  flex: 1; min-width: 0; border: none; background: none; cursor: pointer; text-align: left; font-family: inherit;
  border-radius: 12px; transition: background 0.15s;
}
.person.clickable:hover { background: #f8f9fa; }
.who { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.who strong { font-size: 0.9rem; font-weight: 900; color: #343a40; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.who span { font-size: 0.75rem; font-weight: 700; color: #868e96; }
.actions { display: flex; gap: 4px; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit; cursor: pointer;
  font-weight: 800; font-size: 0.8rem; padding: 8px 12px; border-radius: 10px; white-space: nowrap;
}
.btn:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.btn.small { padding: 7px 10px; font-size: 0.75rem; }
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn.primary:hover:not(:disabled) { color: #fff; background: #1f4ae0; }
.btn.ghost { border-color: transparent; color: #868e96; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.field-error { margin: 8px 0 0; font-size: 0.72rem; font-weight: 700; color: #e03131; }
.saved { margin: 8px 0 0; font-size: 0.75rem; font-weight: 800; color: #2f9e44; }
.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading, .empty { padding: 12px 0; margin: 0; font-size: 0.85rem; color: #adb5bd; font-weight: 700; }
</style>
