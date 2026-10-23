<template>
  <!-- 합주실 무대: 배경(208×72) 위에 멤버 캐릭터를 나란히 세운다. 이름·파트 표는 무대 아래 HTML로
       reorderable이면 캐릭터를 좌우로 끌어서 자리를 바꾼다 (놓으면 reorder로 새 멤버 순서를 알린다) -->
  <div ref="sceneRef" class="scene" :class="{ reorderable, dragging: !!drag }"
       :style="{ aspectRatio: `${STAGE_WIDTH} / ${STAGE_HEIGHT}` }">
    <div class="world" :style="{ backgroundImage: `url(${stage.background})` }">
      <div
        v-for="m in actors"
        :key="m.memberId"
        class="actor"
        :class="{ me: m.me, held: drag?.id === m.memberId }"
        :style="actorStyle(m.memberId)"
        :title="`${m.character?.nickname || '멤버'} · ${partLabel(m.part)}`"
        @pointerdown="startDrag($event, m.memberId)"
      >
        <PixelCharacter
          :character="m.look"
          :outfit="m.character?.currentOutfitSpriteKey || 'DEFAULT'"
          :scale="1"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import PixelCharacter from '../PixelCharacter.vue'
import { getBandStage } from '../../constants/bandStage'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { SPRITE_WIDTH, SPRITE_HEIGHT, STAGE_WIDTH, STAGE_HEIGHT } from '../../pixel/palette'

const props = defineProps({
  // 밴드 상세의 members (무대 배치 순서)
  members: { type: Array, default: () => [] },
  // 합주실 배경 키 (constants/bandStage.js)
  stageKey: { type: String, default: 'STUDIO' },
  // 끌어서 자리 바꾸기 (리더만)
  reorderable: { type: Boolean, default: false },
})
// reorder(memberIds): 바뀐 무대 순서
const emit = defineEmits(['reorder'])

const stage = computed(() => getBandStage(props.stageKey))

// 악기는 캐릭터 기본 악기 대신 이 밴드에서 맡은 파트로 (멤버가 바뀔 때만 새로 만든다)
const actors = computed(() => props.members.map((m) => ({ ...m, look: { ...m.character, instrumentType: m.part } })))

const pct = (v, total) => `${(v / total) * 100}%`

// 멤버 수만큼 무대 가로를 나눠 가운데에 세운다. 8명이면 거의 붙어 선다 (24px × 8 = 192px)
const gap = computed(() => Math.min(40, (STAGE_WIDTH - 16) / Math.max(props.members.length, 1)))
const slotCenter = (i, n) => STAGE_WIDTH / 2 + (i - (n - 1) / 2) * gap.value

/* ---------- 끌어서 자리 바꾸기 ---------- */

const sceneRef = ref(null)
// { id, from: 원래 칸, pointerX: 누른 화면 x, x: 지금 무대 x(도트), moved }
const drag = ref(null)

const ids = computed(() => props.members.map((m) => m.memberId))

// 끄는 동안 보여 줄 순서: 잡은 캐릭터를 가장 가까운 칸에 끼워 넣는다
const previewOrder = computed(() => {
  if (!drag.value) return ids.value
  const n = ids.value.length
  let target = 0
  let best = Infinity
  for (let i = 0; i < n; i++) {
    const d = Math.abs(slotCenter(i, n) - drag.value.x)
    if (d < best) { best = d; target = i }
  }
  const rest = ids.value.filter((id) => id !== drag.value.id)
  rest.splice(target, 0, drag.value.id)
  return rest
})

const actorStyle = (id) => {
  const n = ids.value.length
  const held = drag.value?.id === id
  const center = held ? drag.value.x : slotCenter(previewOrder.value.indexOf(id), n)
  return {
    left: pct(center - SPRITE_WIDTH / 2, STAGE_WIDTH),
    top: pct(stage.value.floorY - SPRITE_HEIGHT - (held && drag.value.moved ? 2 : 0), STAGE_HEIGHT),
    width: pct(SPRITE_WIDTH, STAGE_WIDTH),
  }
}

const dotsPerPx = () => STAGE_WIDTH / (sceneRef.value?.getBoundingClientRect().width || STAGE_WIDTH)

const startDrag = (e, id) => {
  if (!props.reorderable || ids.value.length < 2 || e.button > 0) return
  e.preventDefault()
  const from = ids.value.indexOf(id)
  drag.value = { id, from, pointerX: e.clientX, x: slotCenter(from, ids.value.length), moved: false }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', endDrag)
  window.addEventListener('pointercancel', cancelDrag)
}

const onMove = (e) => {
  if (!drag.value) return
  const dx = (e.clientX - drag.value.pointerX) * dotsPerPx()
  const n = ids.value.length
  const min = slotCenter(0, n) - gap.value / 2
  const max = slotCenter(n - 1, n) + gap.value / 2
  drag.value.x = Math.min(max, Math.max(min, slotCenter(drag.value.from, n) + dx))
  if (Math.abs(dx) > 1) drag.value.moved = true
}

const stopListening = () => {
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', endDrag)
  window.removeEventListener('pointercancel', cancelDrag)
}

const endDrag = () => {
  const order = previewOrder.value
  const changed = order.some((id, i) => id !== ids.value[i])
  stopListening()
  drag.value = null
  if (changed) emit('reorder', order)
}

const cancelDrag = () => {
  stopListening()
  drag.value = null
}

onBeforeUnmount(stopListening)

const partLabel = (part) => INSTRUMENT_OPTIONS.find((o) => o.value === part)?.label || part
</script>

<style scoped>
.scene { position: relative; width: 100%; overflow: hidden; user-select: none; }
.world {
  position: absolute; inset: 0;
  background-size: 100% 100%; background-repeat: no-repeat;
  image-rendering: pixelated; image-rendering: crisp-edges;
}
.actor { position: absolute; line-height: 0; transition: left 0.18s ease, top 0.12s ease; }
.actor :deep(.pixel-character) {
  width: 100% !important; height: auto !important;
  image-rendering: pixelated; image-rendering: crisp-edges;
  pointer-events: none;
}
.reorderable .actor { cursor: grab; touch-action: none; }
.dragging, .dragging .actor { cursor: grabbing; }
/* 잡은 캐릭터는 손을 바로 따라오고 맨 앞에 */
.actor.held { transition: top 0.12s ease; z-index: 5; filter: drop-shadow(0 2px 0 rgba(0, 0, 0, 0.25)); }
</style>
