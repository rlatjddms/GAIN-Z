<template><template v-for="(part, i) in parts" :key="i"><span v-if="part.mention" class="mention">{{ part.text }}</span><template v-else>{{ part.text }}</template></template></template>
<script setup>
import { computed } from 'vue'
const props = defineProps({ text: { type: String, default: '' }, members: { type: Array, default: () => [] } })
const parts = computed(() => {
  const names = [...new Set(props.members.map(m => m.nickname))].sort((a, b) => b.length - a.length)
  const result = []; let plain = ''; let i = 0
  while (i < props.text.length) {
    const name = props.text[i] === '@' && (i === 0 || /\s/u.test(props.text[i - 1])) && names.find(n => props.text.startsWith(n, i + 1) && !/[\p{L}\p{N}_]/u.test(props.text[i + n.length + 1] || ''))
    if (name) { if (plain) result.push({ text: plain }); plain = ''; result.push({ text: '@' + name, mention: true }); i += name.length + 1 }
    else plain += props.text[i++]
  }
  if (plain) result.push({ text: plain })
  return result
})
</script>
<style scoped>.mention { color: #2d5bff; background: #edf2ff; border-radius: 4px; padding: 1px 3px; font-weight: 700; }</style>
