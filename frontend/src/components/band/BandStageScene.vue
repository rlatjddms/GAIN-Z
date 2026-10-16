<template>
  <!-- 합주실 무대: 배경(208×72) 위에 멤버 캐릭터를 나란히 세운다. 이름·파트 표는 무대 아래 HTML로 -->
  <div class="scene" :style="{ aspectRatio: `${STAGE_WIDTH} / ${STAGE_HEIGHT}` }">
    <div class="world" :style="{ backgroundImage: `url(${stage.background})` }">
      <div
        v-for="(m, i) in actors"
        :key="m.memberId"
        class="actor"
        :class="{ me: m.me }"
        :style="actorStyle(i)"
        :title="`${m.character?.nickname || '멤버'} · ${partLabel(m.part)}`"
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
import { computed } from 'vue'
import PixelCharacter from '../PixelCharacter.vue'
import { getLevelTier } from '../../constants/levelTier'
import { INSTRUMENT_OPTIONS } from '../../constants/characterOptions'
import { SPRITE_WIDTH, SPRITE_HEIGHT, STAGE_WIDTH, STAGE_HEIGHT } from '../../pixel/palette'

const props = defineProps({
  // 밴드 상세의 members (리더 먼저)
  members: { type: Array, default: () => [] },
})

// 무대는 멤버 평균 레벨에 맞는 등급
const stage = computed(() => {
  const levels = props.members.map((m) => m.character?.level || 1)
  const avg = levels.length ? Math.round(levels.reduce((a, b) => a + b, 0) / levels.length) : 1
  return getLevelTier(avg)
})

// 악기는 캐릭터 기본 악기 대신 이 밴드에서 맡은 파트로 (멤버가 바뀔 때만 새로 만든다)
const actors = computed(() => props.members.map((m) => ({ ...m, look: { ...m.character, instrumentType: m.part } })))

const pct = (v, total) => `${(v / total) * 100}%`

// 멤버 수만큼 무대 가로를 나눠 가운데에 세운다. 8명이면 거의 붙어 선다 (24px × 8 = 192px)
const actorStyle = (i) => {
  const n = props.members.length
  const gap = Math.min(40, (STAGE_WIDTH - 16) / Math.max(n, 1))
  const center = STAGE_WIDTH / 2 + (i - (n - 1) / 2) * gap
  return {
    left: pct(center - SPRITE_WIDTH / 2, STAGE_WIDTH),
    top: pct(stage.value.floorY - SPRITE_HEIGHT, STAGE_HEIGHT),
    width: pct(SPRITE_WIDTH, STAGE_WIDTH),
  }
}

const partLabel = (part) => INSTRUMENT_OPTIONS.find((o) => o.value === part)?.label || part
</script>

<style scoped>
.scene { position: relative; width: 100%; overflow: hidden; }
.world {
  position: absolute; inset: 0;
  background-size: 100% 100%; background-repeat: no-repeat;
  image-rendering: pixelated; image-rendering: crisp-edges;
}
.actor { position: absolute; line-height: 0; }
.actor :deep(.pixel-character) {
  width: 100% !important; height: auto !important;
  image-rendering: pixelated; image-rendering: crisp-edges;
}
</style>
