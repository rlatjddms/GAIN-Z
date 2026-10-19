<template>
  <SubPageLayout pill="FRIENDS">
    <div class="page">
      <router-link to="/friends" class="to-list">‹ 친구 목록</router-link>

      <p v-if="loadError" class="error">{{ loadError }}</p>
      <div v-else-if="!profile" class="loading">불러오는 중…</div>

      <template v-else>
        <!-- 프로필 머리: 사진·이름·닉네임 -->
        <section class="head">
          <ProfileAvatar :src="profile.profileImageUrl" :size="72" :alt="`${profile.name} 프로필 사진`" />
          <div class="who">
            <h2>{{ profile.name }}</h2>
            <p v-if="profile.character">{{ profile.character.nickname }} · Lv.{{ profile.character.level ?? 1 }}</p>
            <p v-else>아직 캐릭터가 없어요</p>
            <span v-if="sinceText" class="since">{{ sinceText }}부터 친구</span>
          </div>
          <button class="unfriend" :disabled="removing" @click="unfriend">{{ removing ? '끊는 중…' : '친구 끊기' }}</button>
        </section>
        <p v-if="actionError" class="error">{{ actionError }}</p>

        <!-- 꾸민 캐릭터와 무대 배경 그대로 -->
        <CharacterStage v-if="profile.character" :character="profile.character" />

        <!-- 연습 통계 -->
        <WsCard title="연습 통계">
          <div class="stats">
            <div class="stat">
              <span>총 연습 시간</span>
              <strong>{{ totalText }}</strong>
            </div>
            <div class="stat streak">
              <span>지금 연속</span>
              <strong>{{ profile.character?.currentStreak ?? 0 }}일 🔥</strong>
            </div>
            <div class="stat">
              <span>최고 연속</span>
              <strong>{{ profile.character?.maxStreak ?? 0 }}일</strong>
            </div>
            <div class="stat">
              <span>주 악기</span>
              <strong>{{ instrumentLabel || '—' }}</strong>
            </div>
          </div>
          <p v-if="joinedText" class="joined">{{ joinedText }} GAIN-Z 가입</p>
        </WsCard>

        <!-- 깃허브 잔디처럼 최근 연습 -->
        <PracticeLogCard :daily="profile.dailyPractice" />

        <!-- 함께하는 밴드 -->
        <WsCard :title="`함께하는 밴드 ${profile.bands.length}`">
          <p v-if="!profile.bands.length" class="empty">아직 들어간 밴드가 없어요</p>
          <ul v-else class="bands">
            <li v-for="b in profile.bands" :key="b.bandId" class="band" :class="{ together: b.together }">
              <span class="band-name">
                <span v-if="b.position === 'LEADER'" title="리더">👑 </span>{{ b.name }}
              </span>
              <span class="part">{{ partLabel(b.part) }}</span>
              <span v-if="b.together" class="badge">같은 밴드</span>
            </li>
          </ul>
        </WsCard>
      </template>
    </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SubPageLayout from '../components/workspace/SubPageLayout.vue'
import WsCard from '../components/workspace/WsCard.vue'
import ProfileAvatar from '../components/ProfileAvatar.vue'
import CharacterStage from '../components/CharacterStage.vue'
import PracticeLogCard from '../components/workspace/PracticeLogCard.vue'
import { getFriendProfile, removeFriend } from '../api/friend'
import { apiErrorMessage } from '../api/practice'
import { INSTRUMENT_OPTIONS } from '../constants/characterOptions'
import { durationText } from '../constants/practice'

const route = useRoute()
const router = useRouter()

const profile = ref(null)
const loadError = ref('')
const actionError = ref('')

const load = async (userId) => {
  profile.value = null
  loadError.value = ''
  try {
    profile.value = await getFriendProfile(userId)
  } catch (e) {
    loadError.value = apiErrorMessage(e, '친구 프로필을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

watch(() => route.params.id, (id) => { if (id) load(id) }, { immediate: true })

const dayText = (at) => {
  if (!at) return ''
  const d = new Date(at)
  return Number.isNaN(d.getTime()) ? '' : `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
}
const sinceText = computed(() => dayText(profile.value?.since))
const joinedText = computed(() => dayText(profile.value?.joinedAt))

// 캐릭터의 총 연습 시간은 분 단위
const totalText = computed(() => durationText((profile.value?.character?.totalPracticeTime ?? 0) * 60))

const partLabel = (type) => {
  const option = INSTRUMENT_OPTIONS.find((o) => o.value === type)
  return option ? `${option.emoji} ${option.label}` : type
}
const instrumentLabel = computed(() => {
  const type = profile.value?.character?.instrumentType
  return type ? partLabel(type) : ''
})

/* ---------- 친구 끊기 ---------- */
const removing = ref(false)
const unfriend = async () => {
  if (removing.value) return
  if (!window.confirm(`${profile.value.name} 님과 친구를 끊을까요?`)) return
  removing.value = true
  actionError.value = ''
  try {
    await removeFriend(profile.value.userId)
    router.replace('/friends')
  } catch (e) {
    actionError.value = apiErrorMessage(e, '친구를 끊지 못했어요.')
  } finally {
    removing.value = false
  }
}
</script>

<style scoped>
.page { max-width: 720px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; }
.to-list { align-self: flex-start; font-size: 0.8rem; font-weight: 800; color: #2d5bff; text-decoration: none; }
.to-list:hover { text-decoration: underline; }

.head {
  display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid #e9ecef;
  border-radius: 20px; padding: 20px 22px;
}
.who { flex: 1; min-width: 0; }
.who h2 { margin: 0; font-size: 1.25rem; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.who p { margin: 3px 0 0; font-size: 0.85rem; font-weight: 700; color: #495057; }
.since { display: inline-block; margin-top: 6px; font-size: 0.72rem; font-weight: 800; color: #2d5bff; background: #f0f4ff; padding: 3px 9px; border-radius: 999px; }
.unfriend {
  align-self: flex-start; border: none; background: none; padding: 0; cursor: pointer;
  font-family: inherit; font-size: 0.72rem; font-weight: 700; color: #adb5bd; text-decoration: underline;
}
.unfriend:hover:not(:disabled) { color: #e03131; }
.unfriend:disabled { cursor: not-allowed; }

.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.stat {
  background: #f8f9fa; border: 1px solid #f1f3f5; border-radius: 12px; padding: 10px 12px;
  display: flex; flex-direction: column; gap: 4px; min-width: 0;
}
.stat span { font-size: 0.7rem; font-weight: 700; color: #868e96; white-space: nowrap; }
.stat strong { font-size: 1rem; font-weight: 900; color: #1a1a1a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat.streak { background: #fff4e6; border-color: #ffe8cc; }
.stat.streak strong { color: #ff922b; }
.joined { margin: 12px 0 0; font-size: 0.72rem; font-weight: 700; color: #adb5bd; }

.bands { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; }
.band { display: flex; align-items: center; gap: 10px; padding: 10px 2px; border-top: 1px solid #f1f3f5; }
.band:first-child { border-top: none; padding-top: 0; }
.band-name { flex: 1; min-width: 0; font-size: 0.88rem; font-weight: 900; color: #343a40; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.part { font-size: 0.75rem; font-weight: 700; color: #868e96; }
.badge { font-size: 0.68rem; font-weight: 800; color: #2f9e44; background: #ebfbee; padding: 3px 8px; border-radius: 999px; }

.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading, .empty { margin: 0; padding: 8px 0; font-size: 0.85rem; color: #adb5bd; font-weight: 700; }

@media (max-width: 640px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .head { flex-wrap: wrap; }
}
</style>
