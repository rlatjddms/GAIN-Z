<template>
  <div class="mention-input">
    <textarea ref="input" v-bind="$attrs" :value="modelValue" @input="update" @click="locate" @keyup="locate" @compositionstart="composing = true" @compositionend="finishComposition" @keydown="navigate" />
    <div v-if="suggestions.length" class="suggestions" role="listbox" aria-label="멘션할 멤버">
      <button v-for="(member, index) in suggestions" :key="member.userId" type="button" role="option" :aria-selected="index === selected" @mousedown.prevent @click="choose(member)"><span class="at">@</span><strong>{{ member.nickname }}</strong><small>멤버 태그</small></button>
    </div>
  </div>
</template>
<script setup>
import { computed, ref, nextTick } from 'vue'
defineOptions({ inheritAttrs: false })
const props = defineProps({ modelValue: { type: String, default: '' }, members: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const input = ref(null)
const composing = ref(false)
const query = ref(null)
const start = ref(0)
const selected = ref(0)
const suggestions = computed(() => query.value === null ? [] : props.members.filter(m => m.nickname.toLocaleLowerCase().includes(query.value.toLocaleLowerCase())).slice(0, 8))
function locate(event) {
  if (composing.value || ['ArrowUp', 'ArrowDown', 'Escape', 'Enter'].includes(event?.key)) return
  const before = input.value.value.slice(0, input.value.selectionStart)
  const match = before.match(/(?:^|\s)@([^@\s]*)$/u)
  query.value = match ? match[1] : null
  if (match) start.value = before.length - match[1].length - 1
  selected.value = 0
}
// 조합 중인 한글도 즉시 부모 상태에 반영해 첫 글자부터 등록할 수 있다.
function update(event) { emit('update:modelValue', event.target.value); locate() }
function finishComposition(event) { composing.value = false; update(event) }
async function choose(member) {
  const end = input.value.selectionStart
  const prefix = props.modelValue.slice(0, start.value) + '@' + member.nickname + ' '
  const next = prefix + props.modelValue.slice(end)
  if (input.value.maxLength >= 0 && next.length > input.value.maxLength) return
  emit('update:modelValue', next)
  query.value = null
  await nextTick()
  input.value.focus(); input.value.setSelectionRange(prefix.length, prefix.length)
}
function navigate(event) {
  if (event.isComposing || composing.value || !suggestions.value.length) return
  if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); query.value = null }
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') { event.preventDefault(); selected.value = (selected.value + (event.key === 'ArrowDown' ? 1 : -1) + suggestions.value.length) % suggestions.value.length }
  if (event.key === 'Enter') { event.preventDefault(); choose(suggestions.value[selected.value]) }
}
defineExpose({ focus: () => input.value?.focus() })
</script>
<style scoped>
.mention-input { position: relative; min-width: 0; }
textarea { box-sizing: border-box; width: 100%; font: inherit; color: inherit; }
.suggestions { position: relative; z-index: 12; margin: 5px 0; max-height: 190px; overflow: auto; border: 1px solid #dce3f0; background: white; border-radius: 12px; padding: 5px; box-shadow: 0 6px 20px #17243b12; }
.suggestions button { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px; border: 0; border-radius: 7px; background: white; color: #344054; font: inherit; font-size: 12px; text-align: left; cursor: pointer; }
.suggestions button[aria-selected=true], .suggestions button:hover { background: var(--c-primary-soft); }
.at { color: var(--c-primary-text); font-weight: 800; }
small { margin-left: auto; color: #7b8495; }
</style>
