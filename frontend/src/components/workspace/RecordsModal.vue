<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="open" class="backdrop" @click.self="close">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="records-title">
          <header class="modal-head">
            <div>
              <h2 id="records-title">MY RECORDS <span class="count">{{ store.ownedCount }} / {{ store.total || '—' }}</span></h2>
              <p>업적을 달성하면 음반이 하나씩 쌓여요</p>
            </div>
            <button class="close" aria-label="닫기" @click="close"><X :size="18" :stroke-width="2.4" /></button>
          </header>

          <div class="legend">
            <span v-for="r in RECORD_RARITIES" :key="r.key" class="legend-item">
              <RecordIcon :type="r.type" :color="LEGEND_COLORS[r.key]" :size="20" />
              {{ r.label }} <small>{{ r.rarity }}</small>
            </span>
          </div>

          <p v-if="error" class="error">{{ error }}</p>
          <p v-else-if="!store.loaded" class="loading">불러오는 중…</p>

          <template v-else>
            <!-- 고른 레코드 자세히 -->
            <section v-if="selected" class="detail" :style="{ '--c': rarityOf(selected.rarity).border }">
              <RecordIcon :type="rarityOf(selected.rarity).type" :color="selected.color" :locked="!selected.owned" :size="72" />
              <div class="detail-text">
                <span class="detail-rarity">{{ rarityOf(selected.rarity).label }} · {{ rarityOf(selected.rarity).rarity }}</span>
                <strong>{{ selected.owned ? selected.name : '???' }}</strong>
                <p v-if="selected.owned">{{ selected.description }}</p>
                <p class="condition">{{ selected.condition }}</p>
                <span v-if="selected.owned" class="acquired">{{ dateText(selected.acquiredAt) }} 획득</span>
                <div v-else class="progress">
                  <div class="bar"><div class="fill" :style="{ width: `${progressRatio(selected.progress) * 100}%` }"></div></div>
                  <span>{{ progressText(selected.progress) }}</span>
                </div>
              </div>
              <button class="detail-close" aria-label="자세히 닫기" @click="selectedKey = null"><X :size="14" :stroke-width="2.4" /></button>
            </section>

            <!-- 도감: 얻은 것은 재킷 색, 못 얻은 것은 실루엣과 진행도 -->
            <div class="record-grid">
              <button v-for="r in store.records" :key="r.key" class="slot" :class="{ owned: r.owned, on: selectedKey === r.key }"
                      :style="{ '--c': rarityOf(r.rarity).border }" :title="r.owned ? r.name : r.condition"
                      @click="selectedKey = selectedKey === r.key ? null : r.key">
                <RecordIcon :type="rarityOf(r.rarity).type" :color="r.color" :locked="!r.owned" :size="44" />
                <span class="slot-name">{{ r.owned ? r.name : '???' }}</span>
                <span v-if="!r.owned" class="mini-bar"><span :style="{ width: `${progressRatio(r.progress) * 100}%` }"></span></span>
              </button>
            </div>

            <!-- 수집 보상: 종류별로 다 모으면 방 꾸미기 세트, 전부 모으면 리스닝 룸 -->
            <section class="rewards">
              <h3>수집 보상</h3>
              <ul>
                <li v-for="s in setProgress" :key="s.key" :class="{ done: s.done }">
                  <span class="reward-icons">
                    <img :src="decorUrl(s.player.key)" alt=""><img :src="decorUrl(s.rack.key)" alt="">
                  </span>
                  <span class="reward-text">
                    <b>{{ s.label }} {{ s.owned }} / {{ s.total }}</b>
                    <small>{{ s.player.name }} + {{ s.rack.name }}</small>
                  </span>
                  <span class="reward-state">{{ s.done ? '받음' : '모으는 중' }}</span>
                </li>
                <li :class="{ done: allDone }">
                  <span class="reward-icons"><img :src="listeningThumb" alt="" class="room"></span>
                  <span class="reward-text">
                    <b>전부 {{ store.ownedCount }} / {{ store.total || 12 }}</b>
                    <small>리스닝 룸 무대</small>
                  </span>
                  <span class="reward-state">{{ allDone ? '받음' : '모으는 중' }}</span>
                </li>
              </ul>
              <p class="rewards-hint">받은 보상은 옷장 배경 탭에서 고를 수 있어요.</p>
            </section>
          </template>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { X } from 'lucide-vue-next'
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import RecordIcon from './RecordIcon.vue'
import { useRecordsStore } from '../../stores/records'
import { apiErrorMessage } from '../../api/practice'
import { RECORD_RARITIES, rarityOf, progressText, progressRatio } from '../../constants/records'
import { ROOM_DECOR_SETS, decorUrl } from '../../constants/recordRewards'
import listeningThumb from '../../assets/stages/listening.png'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 열 때 바로 자세히 볼 레코드 (획득 연출에서 '도감에서 보기')
  focusKey: { type: String, default: null },
})
const emit = defineEmits(['close'])
const close = () => emit('close')

const LEGEND_COLORS = { COMMON: 'color-mix(in srgb, var(--c-primary) 78%, #fff)', RARE: '#e8590c', LEGENDARY: '#7c3aed' }

const store = useRecordsStore()
const error = ref('')
const selectedKey = ref(null)
const selected = computed(() => store.records.find((r) => r.key === selectedKey.value) || null)

const dateText = (at) => {
  const d = new Date(at)
  return Number.isNaN(d.getTime()) ? '' : `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
}

// 열 때마다 최신 진행도로
watch(() => props.open, async (open) => {
  if (!open) return
  selectedKey.value = props.focusKey
  error.value = ''
  try {
    await store.load()
  } catch (e) {
    error.value = apiErrorMessage(e, '레코드를 불러오지 못했어요.')
  }
})

// 열려 있는 동안 Esc로 닫기
const onKey = (e) => { if (e.key === 'Escape') close() }
watch(() => props.open, (v) => {
  if (v) window.addEventListener('keydown', onKey)
  else window.removeEventListener('keydown', onKey)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

// 종류별 수집 현황 → 방 꾸미기 세트
const setProgress = computed(() => ROOM_DECOR_SETS.map((set) => {
  const ofRarity = store.records.filter((r) => r.rarity === set.rarity)
  const owned = ofRarity.filter((r) => r.owned).length
  return { ...set, label: rarityOf(set.rarity).label, owned, total: ofRarity.length, done: ofRarity.length > 0 && owned === ofRarity.length }
}))
const allDone = computed(() => store.records.length > 0 && store.records.every((r) => r.owned))
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 560px; max-height: calc(100vh - 32px); overflow-y: auto;
  background: #fff; border-radius: 24px; padding: 24px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  font-family: 'Pretendard', sans-serif; color: #1a1a1a; box-sizing: border-box;
}
.modal-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.modal-head h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; display: flex; align-items: center; gap: 8px; }
.modal-head p { margin: 0; font-size: 0.8rem; color: #868e96; }
.count { font-size: 0.75rem; font-weight: 800; color: var(--c-primary-text); background: var(--c-primary-soft); padding: 3px 8px; border-radius: 6px; }
.close {
  flex: none; width: 32px; height: 32px; border: none; border-radius: 10px; background: #f1f3f5;
  color: #868e96; font-size: 0.9rem; cursor: pointer;
}
.close:hover { background: #e9ecef; color: #1a1a1a; }

.legend { display: flex; gap: 16px; margin-bottom: 14px; flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 0.75rem; font-weight: 800; color: #495057; }
.legend-item small { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }

.detail {
  position: relative; display: flex; align-items: center; gap: 16px; margin-bottom: 14px; padding: 16px;
  border-radius: 16px; background: #f8f9fa; border: 2px solid color-mix(in srgb, var(--c) 55%, #fff);
}
.detail-text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.detail-rarity { font-size: 0.68rem; font-weight: 800; color: #868e96; }
.detail-text strong { font-size: 1rem; font-weight: 900; }
.detail-text p { margin: 0; font-size: 0.8rem; font-weight: 700; color: #495057; line-height: 1.5; }
.detail-text .condition { color: #868e96; }
.acquired { font-size: 0.72rem; font-weight: 800; color: #2f9e44; }
.progress { display: flex; align-items: center; gap: 8px; margin-top: 2px; }
.progress span { font-size: 0.72rem; font-weight: 800; color: #495057; white-space: nowrap; }
.bar { flex: 1; height: 8px; border-radius: 999px; background: #e9ecef; overflow: hidden; }
.fill { height: 100%; border-radius: 999px; background: var(--c-primary); transition: width 0.3s; }
.detail-close {
  position: absolute; top: 8px; right: 8px; width: 24px; height: 24px; border: none; background: none;
  border-radius: 6px; cursor: pointer; color: #adb5bd; font-size: 0.8rem;
}
.detail-close:hover { background: #e9ecef; color: #495057; }

.record-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.slot {
  border-radius: 14px; background: #f8f9fa; padding: 14px 6px 10px; cursor: pointer; font-family: inherit;
  border: 2px solid color-mix(in srgb, var(--c) 55%, #fff);
  display: flex; flex-direction: column; align-items: center; gap: 6px; transition: transform 0.12s, box-shadow 0.12s;
}
.slot:hover { transform: translateY(-2px); box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08); }
.slot.owned { background: #fff; }
.slot.on { box-shadow: 0 0 0 3px color-mix(in srgb, var(--c) 45%, #fff); }
.slot-name {
  max-width: 100%; font-size: 0.7rem; font-weight: 800; color: #ced4da; letter-spacing: 1px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.slot.owned .slot-name { color: #343a40; letter-spacing: 0; }
.mini-bar { width: 70%; height: 4px; border-radius: 999px; background: #e9ecef; overflow: hidden; }
.mini-bar span { display: block; height: 100%; background: #adb5bd; }

.error { margin: 0; font-size: 0.8rem; color: #e03131; font-weight: 700; }
.loading { margin: 0; padding: 24px 0; text-align: center; font-size: 0.85rem; color: #adb5bd; font-weight: 700; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-active .modal, .modal-leave-active .modal { transition: transform 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal, .modal-leave-to .modal { transform: translateY(12px) scale(0.98); }

@media (max-width: 480px) {
  .record-grid { grid-template-columns: repeat(3, 1fr); }
}
.rewards { margin-top: 16px; padding-top: 14px; border-top: 1px solid #f1f3f5; }
.rewards h3 { margin: 0 0 8px; font-size: 0.8rem; font-weight: 900; color: #495057; }
.rewards ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: 6px; }
.rewards li { display: flex; align-items: center; gap: 10px; padding: 6px 10px; border-radius: 12px; background: #f8f9fa; opacity: 0.75; }
.rewards li.done { background: #fff9db; opacity: 1; }
.reward-icons { display: flex; align-items: flex-end; gap: 2px; width: 64px; flex-shrink: 0; }
.reward-icons img { width: 30px; image-rendering: pixelated; }
.reward-icons img.room { width: 64px; border-radius: 4px; }
.rewards li:not(.done) .reward-icons img { filter: grayscale(1); opacity: 0.6; }
.reward-text { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.reward-text b { font-size: 0.8rem; font-weight: 900; color: #343a40; }
.reward-text small { font-size: 0.7rem; font-weight: 700; color: #868e96; }
.reward-state { font-size: 0.7rem; font-weight: 800; color: #adb5bd; }
.rewards li.done .reward-state { color: #e67700; }
.rewards-hint { margin: 8px 0 0; font-size: 0.7rem; color: #adb5bd; }
</style>
