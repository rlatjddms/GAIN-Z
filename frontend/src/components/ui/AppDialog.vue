<template>
  <!-- 브라우저 dialog(showModal)로 띄워서, 다른 팝업(글쓰기 창 등)이 열려 있어도 그 위에 뜬다 -->
  <dialog ref="el" class="app-dialog" role="alertdialog" aria-labelledby="app-dialog-title"
          @cancel.prevent="cancel" @click="onBackdrop">
    <form v-if="d" class="body" @submit.prevent="ok">
      <h2 id="app-dialog-title">{{ d.title }}</h2>
      <p v-if="d.text" class="text">{{ d.text }}</p>
      <input v-if="d.kind === 'prompt'" ref="input" v-model="value" class="input" :readonly="d.readonly"
             :placeholder="d.placeholder || ''" :aria-label="d.title" @focus="d.readonly && $event.target.select()">
      <div class="dialog-actions">
        <button v-if="d.kind !== 'alert' && !d.readonly" ref="cancelBtn" type="button" class="btn" @click="cancel">{{ d.cancelText || '취소' }}</button>
        <button ref="okBtn" type="submit" class="btn" :class="d.danger ? 'danger solid' : 'primary'" :disabled="!canOk">{{ d.okText || '확인' }}</button>
      </div>
    </form>
  </dialog>
</template>

<script setup>
// 앱 안 확인창 (#134). stores/dialog.js의 alert·confirm·prompt가 띄운다. App.vue에 하나만 둔다
import { computed, nextTick, ref, watch } from 'vue'
import { useDialogStore } from '../../stores/dialog'

const store = useDialogStore()
const d = computed(() => store.current)
const value = ref('')
const el = ref(null), input = ref(null), okBtn = ref(null), cancelBtn = ref(null)

// match: 이 글자를 똑같이 입력해야 확인을 누를 수 있다 (밴드 없애기)
const canOk = computed(() => d.value?.kind !== 'prompt' || !d.value.match || value.value.trim() === d.value.match)

watch(d, async (cur) => {
  if (!cur) { if (el.value?.open) el.value.close(); return }
  value.value = cur.value || ''
  // 이미 열려 있으면 닫았다가 다시 열어 가장 위로 올린다
  if (el.value.open) el.value.close()
  el.value.showModal()
  await nextTick()
  // 위험한 확인은 취소에, 입력창이 있으면 입력창에 먼저 초점
  ;(input.value || (cur.danger && cancelBtn.value) || okBtn.value)?.focus()
})

function ok() {
  if (!canOk.value) return
  store.close(d.value.kind === 'prompt' ? value.value : true)
}
function cancel() {
  if (!d.value) return
  store.close(d.value.kind === 'prompt' ? null : d.value.kind === 'alert')
}
// 바깥(어두운 곳)을 누르면 취소
function onBackdrop(event) {
  if (event.target !== el.value) return
  const r = el.value.getBoundingClientRect()
  if (event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom) cancel()
}
</script>

<style scoped>
.app-dialog {
  width: calc(100% - 32px); max-width: 360px; box-sizing: border-box; padding: 0; border: 0;
  background: var(--c-surface); border-radius: var(--radius-xl); box-shadow: var(--shadow-lg);
  font-family: var(--font-sans); color: var(--c-text);
}
.app-dialog::backdrop { background: rgba(15, 23, 42, 0.45); }
.body { padding: 24px; display: flex; flex-direction: column; gap: 12px; }
/* 확인창은 .body의 gap이 이미 있어서 공용 버튼 줄 위 여백을 줄인다 */
.body .dialog-actions { margin-top: 4px; }
h2 { margin: 0; font-size: 1.05rem; font-weight: 900; line-height: 1.4; }
.text { margin: 0; font-size: 0.85rem; font-weight: 600; line-height: 1.6; color: var(--c-muted); white-space: pre-line; }
.input {
  border: 1px solid var(--c-line); border-radius: 10px; padding: 10px 12px; font: inherit; font-size: 0.9rem; font-weight: 700;
  outline: none; min-width: 0; box-sizing: border-box; color: var(--c-text);
}
.input:focus { border-color: var(--c-primary); }
</style>
