<template>
  <!-- 30초 미리듣기 재생·정지 버튼 (#187). 줄 전체가 눌리는 목록 안에서도 줄은 열지 않는다 -->
  <button
    type="button"
    class="preview-btn"
    :class="{ playing, small }"
    :aria-label="playing ? '미리듣기 멈추기' : '30초 미리듣기'"
    :title="playing ? '멈추기' : '30초 미리듣기'"
    @click.stop.prevent="toggle(url)"
  >
    <Pause v-if="playing" :size="small ? 11 : 14" :stroke-width="2.6" />
    <Play v-else :size="small ? 11 : 14" :stroke-width="2.6" />
  </button>
</template>

<script setup>
import { computed, onBeforeUnmount } from 'vue'
import { Pause, Play } from 'lucide-vue-next'
import { usePreviewPlayer } from '../music/previewPlayer'

const props = defineProps({
  url: { type: String, required: true },
  small: { type: Boolean, default: false },
})
const { playingUrl, toggle, stop } = usePreviewPlayer()
const playing = computed(() => playingUrl.value === props.url)
// 이 버튼이 사라지면(모달 닫기 등) 소리도 멈춘다
onBeforeUnmount(() => { if (playing.value) stop() })
</script>

<style scoped>
.preview-btn {
  flex: none; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--c-primary-line); border-radius: 50%; background: var(--c-primary-soft); color: var(--c-primary-text);
  cursor: pointer; transition: background 0.15s, color 0.15s;
}
.preview-btn:hover, .preview-btn.playing { background: var(--c-primary); border-color: var(--c-primary); color: var(--c-on-primary); }
.preview-btn.small { width: 22px; height: 22px; }
</style>
