<template>
  <div ref="root" class="reorder-list" :class="{ moving: drag, settling }" role="list">
    <div v-for="(item, index) in items" :key="item.id" :data-reorder-id="item.id" class="reorder-item" :class="{ held: drag?.id === item.id }" :style="itemStyle(item.id)" role="listitem">
      <slot :item="item" :index="index" :start="event => start(event, item.id)" :move="offset => move(index, offset)" />
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'
const props = defineProps({ items: { type: Array, required: true }, disabled: Boolean })
const emit = defineEmits(['reorder'])
const root = ref(null)
const drag = ref(null)
const settling = ref(false)
let timer
let frame
let pointerY = 0
let finish = false
function slotsFor(order, state) {
  let top = state.firstTop
  const slots = new Map()
  order.forEach(id => { slots.set(id, top); top += state.rects.get(id).height + state.gap })
  return slots
}
function start(event, id) {
  if (props.disabled || drag.value || event.button !== 0 || !event.isPrimary) return
  event.preventDefault()
  event.currentTarget.focus({ preventScroll: true })
  // DOM 속성은 문자열이라 숫자 id와 맞지 않으므로 목록 순서대로 원래 id에 붙인다.
  const ids = props.items.map(item => item.id)
  const rects = new Map([...root.value.children].map((el, index) => [ids[index], el.getBoundingClientRect()]))
  const gap = parseFloat(getComputedStyle(root.value).rowGap) || 0
  drag.value = { id, pointerId: event.pointerId, ids, order: [...ids], rects, gap, firstTop: rects.get(ids[0]).top, startX: event.clientX, startY: event.clientY, scrollY: window.scrollY, dx: 0, dy: 0 }
  pointerY = event.clientY
  finish = false
  window.addEventListener('pointermove', onMove, { passive: false })
  window.addEventListener('pointerup', end)
  window.addEventListener('pointercancel', cancel)
  window.addEventListener('keydown', onKey)
  window.addEventListener('blur', cancel)
  frame = requestAnimationFrame(autoScroll)
}
function updateOrder() {
  const state = drag.value
  if (!state || finish) return
  const rect = state.rects.get(state.id)
  const center = rect.top + rect.height / 2 + state.dy
  const others = state.ids.filter(id => id !== state.id)
  // 원래 자리의 중심을 기준으로 삼아 이동 중 순서가 반복해서 뒤집히지 않게 한다.
  const target = others.filter(id => center > state.rects.get(id).top + state.rects.get(id).height / 2).length
  others.splice(target, 0, state.id)
  state.order = others
}
function onMove(event) {
  if (!drag.value || finish || event.pointerId !== drag.value.pointerId) return
  event.preventDefault()
  pointerY = event.clientY
  drag.value.dx = event.clientX - drag.value.startX
  drag.value.dy = event.clientY - drag.value.startY + window.scrollY - drag.value.scrollY
  updateOrder()
}
function autoScroll() {
  if (!drag.value || finish) return
  const delta = pointerY < 100 ? -10 : pointerY > innerHeight - 80 ? 10 : 0
  if (delta) {
    window.scrollBy(0, delta)
    drag.value.dy = pointerY - drag.value.startY + window.scrollY - drag.value.scrollY
    updateOrder()
  }
  frame = requestAnimationFrame(autoScroll)
}
function itemStyle(id) {
  const state = drag.value
  if (!state) return null
  if (id === state.id && !finish) return { transform: `translate(${state.dx}px, ${state.dy}px)` }
  return { transform: `translateY(${slotsFor(state.order, state).get(id) - state.rects.get(id).top}px)` }
}
function detach() {
  cancelAnimationFrame(frame)
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', end)
  window.removeEventListener('pointercancel', cancel)
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('blur', cancel)
}
function end(event) {
  if (!drag.value || finish || event.pointerId !== drag.value.pointerId) return
  detach()
  finish = true
  settling.value = true
  // 놓을 때도 손잡이가 새 자리까지 이동한 뒤 실제 목록 순서를 바꾼다.
  drag.value = { ...drag.value }
  timer = setTimeout(async () => {
    const ids = drag.value.order
    emit('reorder', ids)
    drag.value = null
    await nextTick()
    settling.value = false
    finish = false
  }, matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 190)
}
function cancel() { detach(); clearTimeout(timer); drag.value = null; settling.value = false; finish = false }
function onKey(event) { if (event.key === 'Escape') { event.preventDefault(); cancel() } }
function move(index, offset) {
  if (props.disabled || drag.value) return
  const ids = props.items.map(item => item.id)
  if (index + offset < 0 || index + offset >= ids.length) return
  const [id] = ids.splice(index, 1)
  ids.splice(index + offset, 0, id)
  emit('reorder', ids)
}
onBeforeUnmount(cancel)
</script>

<style scoped>
.reorder-list { display:flex; flex-direction:column; gap:12px; min-width:0; }
.reorder-item { position:relative; min-width:0; }
.moving .reorder-item { transition:transform 190ms cubic-bezier(.2,.8,.2,1); }
.moving .held { z-index:5; transition:none; filter:drop-shadow(0 10px 14px #17243b22); }
.settling .held { transition:transform 180ms cubic-bezier(.2,.8,.2,1); }
@media(prefers-reduced-motion:reduce) { .moving .reorder-item,.settling .held { transition:none; } }
</style>
