<template>
  <!-- 친구 줄·친구 프로필의 ⋯ 메뉴 (#229): 밴드에 초대, 친구 삭제. 모양은 게시판 카드 메뉴(BandPostMenu)와 같다 -->
  <details ref="menu" class="friend-menu" @click.stop @keydown.stop @keydown.esc.prevent="close">
    <summary :aria-label="`${name} 메뉴`" :aria-disabled="disabled || null"><BoardIcon name="more" :size="20" /></summary>
    <div>
      <button v-if="canInvite" :disabled="disabled" @click="select('invite')"><UserPlus :size="15" :stroke-width="2.2" />밴드에 초대</button>
      <button class="danger" :disabled="disabled" @click="select('remove')"><BoardIcon name="trash" :size="15" />친구 삭제</button>
    </div>
  </details>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { UserPlus } from 'lucide-vue-next'
import BoardIcon from '../band/BoardIcon.vue'

defineProps({
  name: { type: String, default: '친구' },
  canInvite: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['invite', 'remove'])
const menu = ref(null)
function close() { if (menu.value) menu.value.open = false }
function outside(event) { if (!menu.value?.contains(event.target)) close() }
function select(action) { close(); emit(action) }
onMounted(() => document.addEventListener('click', outside, true))
onBeforeUnmount(() => document.removeEventListener('click', outside, true))
</script>

<style scoped>
.friend-menu { position: relative; flex: none; }
summary {
  display: grid; place-items: center; list-style: none; width: 36px; height: 36px; border-radius: 10px;
  color: #52627a; cursor: pointer; -webkit-tap-highlight-color: transparent;
}
summary:hover { color: #343a40; background: var(--c-line-soft); }
summary:focus-visible { outline: 2px solid var(--c-primary-line); outline-offset: 1px; }
summary::-webkit-details-marker { display: none; }
.friend-menu > div {
  position: absolute; top: 40px; right: 0; z-index: 15; min-width: 140px; padding: 5px;
  background: white; border: 1px solid #e2e7ef; border-radius: 11px; box-shadow: 0 8px 25px #17243b20;
}
button {
  display: flex; align-items: center; gap: 7px; width: 100%; padding: 10px; border: 0; border-radius: 6px;
  background: white; color: #405372; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; white-space: nowrap;
}
button:hover:not(:disabled) { background: #f1f4fa; }
button:disabled { opacity: 0.5; cursor: default; }
.danger { color: #d94841; }
</style>
