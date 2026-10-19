<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="result" class="backdrop" @click.self="emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-label="레벨 업">
          <div class="badge">LEVEL UP!</div>
          <div class="level">Lv.{{ result.previousLevel }} → <strong>Lv.{{ result.currentLevel }}</strong></div>
          <p v-if="newTier" class="tier">{{ newTier.name }} 무대로 올라갔어요!</p>
          <p class="exp">이번 연습으로 +{{ result.expGained }} EXP</p>

          <!-- 레벨마다 받는 보상 -->
          <div v-if="rewards.length" class="rewards">
            <strong>새로 받은 보상</strong>
            <ul>
              <li v-for="r in rewards" :key="`${r.type}-${r.key}`">
                <span class="thumb">
                  <PixelSprite v-if="r.type === 'OUTFIT'" :parts="[['outfit', outfitSpriteName(r.key, me?.gender)]]" :box="40" />
                  <PixelSprite v-else-if="r.type === 'HAIR'" :parts="[['body', me?.gender === 'MALE' ? 'MALE' : 'FEMALE'], ['hair', r.key]]" :colors="me || {}" :region="HEAD_REGION" :box="48" />
                  <PixelSprite v-else-if="r.type === 'ACCESSORY' || r.type === 'BOTTOM'" :parts="[[slotOf(r.slot).folder, r.key]]" :box="40" />
                  <span v-else class="stage-icon">🎪</span>
                </span>
                <span class="what">
                  <small>Lv.{{ r.level }} · {{ rewardLabel(r) }}</small>
                  <b>{{ r.name }}</b>
                </span>
              </li>
            </ul>
          </div>
          <p v-if="next" class="next">다음 보상: Lv.{{ next.level }} {{ next.rewards.map((r) => r.name).join(' · ') }}</p>

          <div class="actions">
            <button v-if="rewards.length" class="btn ghost" @click="goWardrobe">옷장에서 입어 보기</button>
            <button class="btn" @click="emit('close')">확인</button>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 레벨업했을 때 띄운다. 받은 보상은 레벨 보상 표(constants/rewards.js)에서 찾는다
// result: { previousLevel, currentLevel, expGained }
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import PixelSprite from '../PixelSprite.vue'
import { HEAD_REGION } from '../../pixel/palette'
import { outfitSpriteName } from '../../pixel/sprites'
import { useAuthStore } from '../../stores/auth'
import { getLevelTier } from '../../constants/levelTier'
import { rewardsBetween, nextReward, rewardLabel, slotOf } from '../../constants/rewards'

const props = defineProps({ result: { type: Object, default: null } })
const emit = defineEmits(['close'])

const newTier = computed(() => {
  if (!props.result) return null
  const before = getLevelTier(props.result.previousLevel), after = getLevelTier(props.result.currentLevel)
  return before.key !== after.key ? after : null
})

const authStore = useAuthStore()
const router = useRouter()
const me = computed(() => authStore.character)
const rewards = computed(() => (props.result ? rewardsBetween(props.result.previousLevel, props.result.currentLevel) : []))
const next = computed(() => (props.result ? nextReward(props.result.currentLevel) : null))

const goWardrobe = () => {
  emit('close')
  router.push('/wardrobe')
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 360px; box-sizing: border-box; text-align: center;
  background: #fff; border-radius: 24px; padding: 28px 24px 22px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a;
}
.badge {
  display: inline-block; font-size: 0.8rem; font-weight: 900; letter-spacing: 1px; color: #fff;
  background: linear-gradient(90deg, #ff922b, #f59f00); padding: 6px 14px; border-radius: 8px;
  animation: pop 0.5s ease;
}
.level { margin-top: 14px; font-size: 1.5rem; font-weight: 800; color: #868e96; }
.level strong { color: #f08c00; font-weight: 900; }
.tier { margin: 8px 0 0; font-size: 0.9rem; font-weight: 800; color: #495057; }
.exp { margin: 6px 0 0; font-size: 0.8rem; font-weight: 700; color: #adb5bd; }
.rewards { margin-top: 16px; background: #fff9db; border-radius: 14px; padding: 12px; text-align: left; }
.rewards > strong { display: block; margin-bottom: 8px; font-size: 0.78rem; font-weight: 900; color: #e67700; text-align: center; }
.rewards ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 6px; max-height: 220px; overflow-y: auto; }
.rewards li { display: flex; align-items: center; gap: 10px; background: #fff; border-radius: 10px; padding: 4px 10px 4px 4px; }
.thumb { width: 48px; height: 56px; border-radius: 8px; background: #f8f9fa; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; }
.stage-icon { font-size: 1.5rem; }
.what { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.what small { font-size: 0.68rem; font-weight: 800; color: #adb5bd; }
.what b { font-size: 0.88rem; font-weight: 900; color: #343a40; }
.next { margin: 10px 0 0; font-size: 0.72rem; font-weight: 700; color: #adb5bd; }
.actions { margin-top: 18px; display: flex; gap: 8px; }
.btn {
  flex: 1; border: none; background: #2d5bff; color: #fff; cursor: pointer;
  font-weight: 800; font-size: 0.88rem; padding: 12px; border-radius: 12px; font-family: inherit;
}
.btn.ghost { background: #f1f3f5; color: #495057; }
@keyframes pop { 0% { transform: scale(0.6); opacity: 0; } 70% { transform: scale(1.08); } 100% { transform: scale(1); opacity: 1; } }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
