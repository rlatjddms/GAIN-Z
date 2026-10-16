<template>
  <SubPageLayout pill="BAND">
    <p v-if="loadError" class="state">{{ loadError }}</p>
    <p v-else-if="!band" class="state">밴드를 불러오는 중…</p>

    <template v-else>
      <header class="page-head">
        <h1>{{ band.name }} 밴드 설정</h1>
        <p>{{ isLeader ? '리더는 밴드 정보와 멤버를 관리할 수 있어요.' : '내 파트를 바꾸거나 밴드를 나갈 수 있어요.' }}</p>
      </header>

      <p v-if="notice" class="notice" :class="{ bad: noticeBad }">{{ notice }}</p>

      <!-- 밴드 정보 -->
      <section class="card">
        <h2>밴드 정보</h2>
        <form v-if="isLeader" @submit.prevent="saveInfo">
          <label class="field">
            <span>밴드 이름</span>
            <input v-model="name" type="text" :maxlength="BAND_NAME_MAX" :disabled="busy">
          </label>
          <label class="field">
            <span>소개</span>
            <textarea v-model="description" rows="2" :maxlength="BAND_DESCRIPTION_MAX" :disabled="busy"
                      placeholder="어떤 음악을 하는 밴드인가요?"></textarea>
          </label>
          <div class="row-end">
            <button type="submit" class="btn primary" :disabled="busy || !infoChanged || !name.trim()">저장</button>
          </div>
        </form>
        <dl v-else class="info">
          <dt>이름</dt><dd>{{ band.name }}</dd>
          <dt>소개</dt><dd>{{ band.description || '소개가 없어요.' }}</dd>
        </dl>
      </section>

      <!-- 초대 코드 -->
      <section class="card">
        <h2>초대 코드</h2>
        <p class="desc">이 코드를 받은 사람은 홈 BAND 탭의 <strong>+</strong> 에서 밴드에 들어올 수 있어요. (최대 {{ MAX_BAND_MEMBERS }}명)</p>
        <div class="code-row">
          <span class="code">{{ band.inviteCode }}</span>
          <button class="btn" :disabled="busy" @click="copyCode">{{ copied ? '복사됨' : '복사' }}</button>
          <button v-if="isLeader" class="btn" :disabled="busy" @click="regenerate">새 코드 받기</button>
        </div>
      </section>

      <!-- 멤버 -->
      <section class="card">
        <h2>멤버 <span class="count">{{ band.members.length }} / {{ MAX_BAND_MEMBERS }}</span></h2>
        <ul class="members">
          <li v-for="m in band.members" :key="m.memberId" class="member" :class="{ me: m.me }">
            <div class="avatar">
              <PixelCharacter :character="looks[m.memberId]"
                              :outfit="m.character?.currentOutfitSpriteKey || 'DEFAULT'" :scale="2" :animate="false" />
            </div>
            <div class="who">
              <strong>
                <span v-if="m.position === 'LEADER'" title="리더">👑 </span>{{ m.character?.nickname || '알 수 없는 멤버' }}
                <em v-if="m.me">나</em>
              </strong>
              <span>Lv.{{ m.character?.level ?? 1 }} · {{ joinedLabel(m.joinedAt) }} 가입</span>
            </div>
            <select class="part-select" :value="m.part" :disabled="busy || !(m.me || isLeader)"
                    :title="m.me || isLeader ? '파트 바꾸기' : '본인이나 리더만 바꿀 수 있어요'"
                    @change="changePart(m, $event.target.value)">
              <option v-for="o in INSTRUMENT_OPTIONS" :key="o.value" :value="o.value">{{ o.emoji }} {{ o.label }}</option>
            </select>
            <div v-if="isLeader && !m.me" class="member-actions">
              <button class="mini" :disabled="busy" @click="makeLeader(m)">리더 넘기기</button>
              <button class="mini danger" :disabled="busy" @click="kick(m)">내보내기</button>
            </div>
          </li>
        </ul>
      </section>

      <!-- 나가기 / 없애기 -->
      <section class="card danger-zone">
        <h2>밴드 나가기{{ isLeader ? ' · 없애기' : '' }}</h2>
        <template v-if="isLeader">
          <p class="desc">
            리더는 다른 멤버에게 리더를 넘긴 뒤에 나갈 수 있어요. 밴드를 없애면 모든 멤버가 밴드에서 빠져요.
          </p>
          <div class="row-end">
            <button class="btn" :disabled="busy || band.members.length > 1"
                    :title="band.members.length > 1 ? '먼저 리더를 넘겨 주세요' : ''" @click="leave">밴드 나가기</button>
            <button class="btn danger" :disabled="busy" @click="removeBand">밴드 없애기</button>
          </div>
        </template>
        <template v-else>
          <p class="desc">나가면 초대 코드로 다시 들어올 수 있어요.</p>
          <div class="row-end">
            <button class="btn danger" :disabled="busy" @click="leave">밴드 나가기</button>
          </div>
        </template>
      </section>
    </template>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import PixelCharacter from '../../components/PixelCharacter.vue'
import {
  getBand, updateBand, deleteBand, regenerateInviteCode, changeMemberPart, removeMember, leaveBand, transferLeader,
  BAND_NAME_MAX, BAND_DESCRIPTION_MAX, MAX_BAND_MEMBERS,
} from '../../api/band'
import { apiErrorMessage } from '../../api/practice'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { useBandStore } from '../../stores/band'

const route = useRoute()
const router = useRouter()
const bandStore = useBandStore()
const bandId = Number(route.params.id)

const band = ref(null)
const loadError = ref('')
const busy = ref(false)
const notice = ref('')
const noticeBad = ref(false)

const name = ref('')
const description = ref('')

const isLeader = computed(() => band.value?.myPosition === 'LEADER')
// 캐릭터 악기는 이 밴드에서 맡은 파트로 (멤버가 바뀔 때만 새로 만든다)
const looks = computed(() => Object.fromEntries((band.value?.members || []).map((m) => [m.memberId, { ...m.character, instrumentType: m.part }])))
const infoChanged = computed(() =>
  band.value && (name.value.trim() !== band.value.name || (description.value.trim() || null) !== (band.value.description || null)))

// API가 돌려준 밴드 상세를 화면과 홈 목록에 함께 반영
const apply = (b) => {
  band.value = b
  name.value = b.name
  description.value = b.description || ''
  bandStore.upsertBand(b)
}

onMounted(async () => {
  try {
    apply(await getBand(bandId))
  } catch (e) {
    loadError.value = apiErrorMessage(e, '밴드를 불러오지 못했어요.')
  }
})

const say = (msg, bad = false) => {
  notice.value = msg
  noticeBad.value = bad
}

// 버튼 동작 공통: 하나씩만, 실패하면 서버 메시지
const run = async (action, okMessage, failMessage) => {
  if (busy.value) return
  busy.value = true
  say('')
  try {
    await action()
    if (okMessage) say(okMessage)
  } catch (e) {
    say(apiErrorMessage(e, failMessage), true)
  } finally {
    busy.value = false
  }
}

const saveInfo = () => run(async () => {
  apply(await updateBand(bandId, { name: name.value.trim(), description: description.value.trim() || null }))
}, '밴드 정보를 저장했어요.', '저장하지 못했어요.')

const regenerate = () => {
  if (!window.confirm('새 초대 코드를 받을까요? 지금 코드는 더 이상 쓸 수 없어요.')) return
  run(async () => apply(await regenerateInviteCode(bandId)), '새 초대 코드를 받았어요.', '코드를 바꾸지 못했어요.')
}

const changePart = (m, part) => {
  if (part === m.part) return
  run(async () => apply(await changeMemberPart(bandId, m.memberId, part)), '파트를 바꿨어요.', '파트를 바꾸지 못했어요.')
}

const nick = (m) => m.character?.nickname || '이 멤버'

const makeLeader = (m) => {
  if (!window.confirm(`${nick(m)}에게 리더를 넘길까요? 넘기면 나는 일반 멤버가 돼요.`)) return
  run(async () => apply(await transferLeader(bandId, m.memberId)), `${nick(m)}이(가) 새 리더가 됐어요.`, '리더를 넘기지 못했어요.')
}

const kick = (m) => {
  if (!window.confirm(`${nick(m)}을(를) 밴드에서 내보낼까요?`)) return
  run(async () => apply(await removeMember(bandId, m.memberId)), `${nick(m)}을(를) 내보냈어요.`, '내보내지 못했어요.')
}

const goHome = () => {
  bandStore.removeBand(bandId)
  router.push('/home')
}

const leave = () => {
  if (!window.confirm(`${band.value.name}에서 나갈까요?`)) return
  run(async () => { await leaveBand(bandId); goHome() }, null, '밴드를 나가지 못했어요.')
}

const removeBand = () => {
  const typed = window.prompt(`밴드를 없애면 되돌릴 수 없어요. 확인을 위해 밴드 이름(${band.value.name})을 입력해 주세요.`)
  if (typed === null) return
  if (typed.trim() !== band.value.name) {
    say('밴드 이름이 달라서 없애지 않았어요.', true)
    return
  }
  run(async () => { await deleteBand(bandId); goHome() }, null, '밴드를 없애지 못했어요.')
}

const copied = ref(false)
let copiedTimer = null
const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(band.value.inviteCode)
    copied.value = true
    clearTimeout(copiedTimer)
    copiedTimer = setTimeout(() => { copied.value = false }, 1500)
  } catch {
    window.prompt('초대 코드를 복사해 주세요', band.value.inviteCode)
  }
}
onBeforeUnmount(() => clearTimeout(copiedTimer))

const joinedLabel = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
* { box-sizing: border-box; }
.state { color: #868e96; font-size: 0.9rem; }
.page-head h1 { margin: 0 0 4px; font-size: 1.4rem; font-weight: 900; }
.page-head p { margin: 0 0 20px; font-size: 0.85rem; color: #868e96; }

.notice {
  margin: 0 0 16px; padding: 12px 16px; border-radius: 12px; font-size: 0.85rem; font-weight: 700;
  background: #f0f4ff; color: #2d5bff;
}
.notice.bad { background: #fff5f5; color: #e03131; }

.card {
  background: #fff; border: 1px solid #e9ecef; border-radius: 24px; padding: 24px; margin-bottom: 16px;
}
.card h2 { margin: 0 0 14px; font-size: 0.95rem; font-weight: 900; display: flex; align-items: center; gap: 8px; }
.count { font-size: 0.75rem; color: #adb5bd; font-weight: 800; }
.desc { margin: 0 0 14px; font-size: 0.8rem; color: #868e96; line-height: 1.6; }

.field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.field span { font-size: 0.75rem; font-weight: 800; color: #495057; }
.field input, .field textarea {
  border: 1px solid #e9ecef; border-radius: 12px; padding: 11px 14px; font-family: inherit;
  font-size: 0.9rem; outline: none; background: #f8f9fa; resize: none;
}
.field input:focus, .field textarea:focus { border-color: #2d5bff; background: #fff; }

.info { display: grid; grid-template-columns: 56px 1fr; gap: 8px; margin: 0; font-size: 0.88rem; }
.info dt { color: #868e96; font-weight: 800; font-size: 0.78rem; }
.info dd { margin: 0; }

.row-end { display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer;
  font-weight: 800; font-size: 0.85rem; padding: 10px 16px; border-radius: 12px; font-family: inherit;
}
.btn:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #1f47e0; color: #fff; }
.btn.danger { border-color: #ffc9c9; color: #e03131; }
.btn.danger:hover:not(:disabled) { background: #fff5f5; border-color: #e03131; color: #e03131; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.code-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.code {
  font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 1.3rem; font-weight: 900; letter-spacing: 4px;
  background: #f1f3f5; padding: 8px 14px; border-radius: 12px; margin-right: 4px;
}

.members { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.member {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 16px; background: #f8f9fa;
  flex-wrap: wrap;
}
.member.me { background: #f0f4ff; }
.avatar { width: 48px; flex-shrink: 0; line-height: 0; }
.avatar :deep(.pixel-character) { width: 48px !important; height: auto !important; }
.who { flex: 1; min-width: 120px; display: flex; flex-direction: column; gap: 2px; }
.who strong { font-size: 0.9rem; font-weight: 800; }
.who em {
  font-style: normal; font-size: 0.65rem; font-weight: 800; color: #2d5bff; background: #fff;
  border: 1px solid #c5d3ff; padding: 1px 6px; border-radius: 6px; margin-left: 4px; vertical-align: 1px;
}
.who span { font-size: 0.72rem; color: #868e96; }
.part-select {
  border: 1px solid #e9ecef; background: #fff; border-radius: 10px; padding: 7px 10px; font-family: inherit;
  font-size: 0.8rem; font-weight: 700; color: #495057; cursor: pointer;
}
.part-select:disabled { cursor: default; opacity: 0.7; }
.member-actions { display: flex; gap: 6px; }
.mini {
  border: 1px solid #e9ecef; background: #fff; border-radius: 8px; padding: 6px 10px; cursor: pointer;
  font-family: inherit; font-size: 0.72rem; font-weight: 800; color: #495057;
}
.mini:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.mini.danger { color: #e03131; }
.mini.danger:hover:not(:disabled) { border-color: #e03131; color: #e03131; }
.mini:disabled { opacity: 0.5; cursor: not-allowed; }

.danger-zone h2 { color: #e03131; }
</style>
