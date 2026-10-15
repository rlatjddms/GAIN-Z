<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="result" class="backdrop" @click.self="emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-label="레벨 업">
          <div class="badge">LEVEL UP!</div>
          <div class="level">Lv.{{ result.previousLevel }} → <strong>Lv.{{ result.currentLevel }}</strong></div>
          <p v-if="newTier" class="tier">{{ newTier.name }} 무대로 올라갔어요!</p>
          <p class="exp">이번 연습으로 +{{ result.expGained }} EXP</p>

          <div v-if="result.unlockedOutfits?.length" class="unlocked">
            <strong>새로 해금된 의상</strong>
            <ul><li v-for="o in result.unlockedOutfits" :key="o.id">{{ o.name }}</li></ul>
          </div>

          <button class="btn" @click="emit('close')">확인</button>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 레벨업했거나 의상이 해금됐을 때만 띄운다
// result: { previousLevel, currentLevel, expGained, unlockedOutfits }
import { computed } from 'vue'
import { getLevelTier } from '../../constants/levelTier'

const props = defineProps({ result: { type: Object, default: null } })
const emit = defineEmits(['close'])

const newTier = computed(() => {
  if (!props.result) return null
  const before = getLevelTier(props.result.previousLevel), after = getLevelTier(props.result.currentLevel)
  return before.key !== after.key ? after : null
})
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
.unlocked { margin-top: 16px; background: #fff9db; border-radius: 12px; padding: 12px; font-size: 0.8rem; }
.unlocked strong { display: block; margin-bottom: 4px; color: #e67700; }
.unlocked ul { margin: 0; padding: 0; list-style: none; color: #495057; font-weight: 700; }
.btn {
  margin-top: 20px; width: 100%; border: none; background: #2d5bff; color: #fff; cursor: pointer;
  font-weight: 800; font-size: 0.9rem; padding: 12px; border-radius: 12px; font-family: inherit;
}
@keyframes pop { 0% { transform: scale(0.6); opacity: 0; } 70% { transform: scale(1.08); } 100% { transform: scale(1); opacity: 1; } }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
