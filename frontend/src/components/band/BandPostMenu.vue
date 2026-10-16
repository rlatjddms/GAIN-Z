<template>
  <details ref="menu" class="card-menu" @click.stop @keydown.stop @keydown.esc.prevent="close">
    <summary :aria-label="`${title} 글 관리`"><BoardIcon name="more" :size="20" /></summary>
    <div><button @click="select('edit')"><BoardIcon name="edit" :size="15" />글 수정</button><button class="danger" @click="select('delete')"><BoardIcon name="trash" :size="15" />글 삭제</button></div>
  </details>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import BoardIcon from './BoardIcon.vue'
defineProps({ title: String })
const emit = defineEmits(['edit', 'delete'])
const menu = ref(null)
function close() { if (menu.value) menu.value.open = false }
function outside(event) { if (!menu.value?.contains(event.target)) close() }
function select(action) { close(); emit(action) }
onMounted(() => document.addEventListener('click', outside, true))
onBeforeUnmount(() => document.removeEventListener('click', outside, true))
</script>
<style scoped>
.card-menu { position: relative; margin-left: auto; flex: none; }
summary { display: grid; place-items: center; list-style: none; width: 32px; height: 32px; border: 0; border-radius: 9px; color: #52627a; background: transparent; cursor: pointer; }
summary::-webkit-details-marker { display: none; }
.card-menu > div { position: absolute; top: 38px; right: 0; z-index: 15; min-width: 125px; padding: 5px; background: white; border: 1px solid #e2e7ef; border-radius: 11px; box-shadow: 0 8px 25px #17243b20; }
button { display: flex; align-items: center; gap: 7px; border: 0; background: white; color: #405372; width: 100%; padding: 10px; border-radius: 6px; font: inherit; font-size: 12px; cursor: pointer; }
button:hover { background: #f1f4fa; }.danger { color: #d94841; }
</style>
