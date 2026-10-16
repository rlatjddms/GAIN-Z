<template>
  <SubPageLayout>
    <div class="page">
      <div class="page-head">
        <div>
          <h2>옷장</h2>
          <p>바꾼 모습은 저장해야 반영돼요.</p>
        </div>
        <div class="head-actions">
          <span v-if="savedMessage" class="saved">{{ savedMessage }}</span>
          <button class="btn" :disabled="!dirty || saving" @click="reset">되돌리기</button>
          <button class="btn primary" :disabled="!dirty || saving || !!nicknameError" @click="save">{{ saving ? '저장 중…' : '저장' }}</button>
        </div>
      </div>
      <p v-if="error" class="error">{{ error }}</p>

      <div class="layout">
        <!-- 미리보기: 바꾼 모습이 바로 보인다 -->
        <div class="preview">
          <CharacterStage v-if="preview" :character="preview" bare />
        </div>

        <section class="panel">
          <nav class="tabs" role="tablist">
            <button
              v-for="t in TABS" :key="t.key" role="tab"
              :aria-selected="tab === t.key" :class="{ on: tab === t.key }" @click="tab = t.key"
            >{{ t.label }}</button>
          </nav>

          <!-- 외형 -->
          <div v-if="tab === 'look'" class="tab-body">
            <label class="field">
              <span class="label">닉네임</span>
              <input v-model="draft.nickname" class="input" maxlength="10">
              <span v-if="nicknameError" class="warn">{{ nicknameError }}</span>
            </label>

            <div class="field">
              <span class="label">성별</span>
              <div class="chips">
                <button v-for="g in GENDER_OPTIONS" :key="g.value" class="chip" :class="{ on: draft.gender === g.value }" @click="draft.gender = g.value">{{ g.label }}</button>
              </div>
            </div>

            <div class="field">
              <span class="label">피부 색상</span>
              <ColorSwatches v-model="draft.skinColor" :presets="SKIN_COLOR_PRESETS" label="피부 색상" />
            </div>

            <div class="field">
              <span class="label">헤어 스타일</span>
              <div class="grid">
                <button v-for="h in HAIR_STYLE_OPTIONS" :key="h.value" class="opt" :class="{ on: draft.hairStyle === h.value }" @click="draft.hairStyle = h.value">
                  <span class="emoji">{{ h.emoji }}</span>{{ h.label }}
                </button>
              </div>
            </div>

            <div class="field">
              <span class="label">머리 색상</span>
              <ColorSwatches v-model="draft.hairColor" :presets="HAIR_COLOR_PRESETS" label="머리 색상" />
            </div>

            <div class="field">
              <span class="label">악기</span>
              <div class="grid">
                <button v-for="i in INSTRUMENT_OPTIONS" :key="i.value" class="opt" :class="{ on: draft.instrumentType === i.value }" @click="draft.instrumentType = i.value">
                  <span class="emoji">{{ i.emoji }}</span>{{ i.label }}
                </button>
              </div>
            </div>

            <div class="field">
              <span class="label">악기 색상</span>
              <ColorSwatches v-model="draft.instrumentColor" :presets="INSTRUMENT_COLOR_PRESETS" label="악기 색상" />
            </div>
          </div>

          <!-- 의상 (상의는 성별마다 다른 그림) -->
          <div v-else-if="tab === 'outfit'" class="tab-body">
            <p class="hint">레벨이 오르거나 업적을 달성하면 새 옷이 여기에 생겨요.</p>
            <p v-if="wardrobeError" class="warn">{{ wardrobeError }}</p>
            <div class="items">
              <button
                v-for="o in clothes" :key="o.id" class="item"
                :class="{ on: draft.outfitId === o.id, locked: !o.owned }"
                :disabled="!o.owned"
                @click="draft.outfitId = o.id"
              >
                <span class="item-thumb">
                  <PixelCharacter :character="{ ...preview, currentOutfitSpriteKey: o.spriteKey }" :outfit="o.spriteKey || 'DEFAULT'" :scale="2" :animate="false" />
                  <i v-if="!o.owned">🔒</i>
                </span>
                <strong>{{ o.name }}</strong>
                <small>{{ o.owned ? (draft.outfitId === o.id ? '입는 중' : '입기') : `Lv.${o.requiredLevel}에 해금` }}</small>
              </button>
            </div>
          </div>

          <!-- 악세서리 (추가 예정) -->
          <div v-else-if="tab === 'accessory'" class="tab-body">
            <div v-if="!accessories.length" class="empty">
              <span class="empty-icon">🧢</span>
              <strong>아직 악세서리가 없어요</strong>
              <p>모자·안경 같은 악세서리가 곧 추가될 예정이에요.</p>
            </div>
            <div v-else class="items">
              <div v-for="o in accessories" :key="o.id" class="item" :class="{ locked: !o.owned }">
                <span class="item-thumb">{{ o.owned ? '🧢' : '🔒' }}</span>
                <strong>{{ o.name }}</strong>
                <small>{{ o.owned ? '곧 착용할 수 있어요' : `Lv.${o.requiredLevel}에 해금` }}</small>
              </div>
            </div>
          </div>

          <!-- 배경: 레벨로 얻은 무대는 계속 간직하고 그중 하나를 고른다 -->
          <div v-else class="tab-body">
            <p class="hint">레벨이 오르면 새 무대를 얻어요. 얻은 무대는 언제든 다시 고를 수 있어요.</p>
            <p v-if="wardrobeError" class="warn">{{ wardrobeError }}</p>
            <div class="stages">
              <button
                v-for="b in backgrounds" :key="b.key" class="stage"
                :class="{ on: draft.stageBackground === b.key, locked: !b.unlocked }"
                :disabled="!b.unlocked"
                @click="draft.stageBackground = b.key"
              >
                <span class="thumb" :style="{ backgroundImage: `url(${tierOf(b.key).background})` }">
                  <i v-if="!b.unlocked">🔒</i>
                </span>
                <strong>{{ STAGE_NAMES[b.key] }}</strong>
                <small>{{ b.unlocked ? (draft.stageBackground === b.key ? '사용 중' : '고르기') : `Lv.${b.requiredLevel}에 해금` }}</small>
              </button>
            </div>

            <div class="sub-section">
              <span class="label">꾸미기 효과</span>
              <div class="empty small">
                <strong>✨ 반짝이 같은 무대 효과가 곧 추가될 예정이에요</strong>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </SubPageLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import SubPageLayout from '../components/workspace/SubPageLayout.vue'
import CharacterStage from '../components/CharacterStage.vue'
import ColorSwatches from '../components/ColorSwatches.vue'
import PixelCharacter from '../components/PixelCharacter.vue'
import { useAuthStore } from '../stores/auth'
import { getWardrobe, updateWardrobe, updateAppearance } from '../api/character'
import { apiErrorMessage } from '../api/practice'
import { LEVEL_TIERS, getLevelTier } from '../constants/levelTier'
import {
  GENDER_OPTIONS, HAIR_STYLE_OPTIONS, SKIN_COLOR_PRESETS, HAIR_COLOR_PRESETS,
  INSTRUMENT_OPTIONS, INSTRUMENT_COLOR_PRESETS, nicknameProblem,
} from '../constants/characterOptions'
import { DEFAULT_COLORS } from '../pixel/palette'

const TABS = [
  { key: 'look', label: '외형' },
  { key: 'outfit', label: '의상' },
  { key: 'accessory', label: '악세서리' },
  { key: 'stage', label: '배경' },
]
const STAGE_NAMES = { ROOKIE: '방구석', RISING: '동네 클럽', PRO: '투어 공연장', LEGEND: '전설의 무대' }
const tierOf = (key) => LEVEL_TIERS.find((t) => t.key === key)

const authStore = useAuthStore()
const tab = ref('look')

// 저장된 상태와 지금 고르는 중인 상태
const APPEARANCE_KEYS = ['nickname', 'gender', 'skinColor', 'hairStyle', 'hairColor', 'instrumentType', 'instrumentColor']
const original = ref(null)
const draft = reactive({
  nickname: '', gender: 'MALE', skinColor: DEFAULT_COLORS.skinColor, hairStyle: 'SHORT_CUT', hairColor: '#111827',
  instrumentType: 'GUITAR', instrumentColor: '#B91C1C', outfitId: null, stageBackground: null,
})

const outfits = ref([])
const backgrounds = ref([])
const wardrobeLoaded = ref(false)
const wardrobeError = ref('')

const snapshot = (c, wardrobe) => ({
  nickname: c.nickname || '', gender: c.gender || 'MALE', skinColor: c.skinColor || DEFAULT_COLORS.skinColor,
  hairStyle: c.hairStyle || 'SHORT_CUT', hairColor: c.hairColor || '#111827',
  instrumentType: c.instrumentType || 'GUITAR', instrumentColor: c.instrumentColor || '#B91C1C',
  outfitId: wardrobe?.outfits?.find((o) => o.equipped)?.id ?? c.currentOutfitId ?? null,
  // 고른 적 없으면 레벨로 얻은 가장 큰 무대 (서버도 같은 규칙으로 돌려준다)
  stageBackground: wardrobe?.stageBackground ?? c.stageBackground ?? getLevelTier(c.level ?? 1).key,
})

onMounted(async () => {
  if (!authStore.character) await authStore.fetchCharacter()
  let wardrobe = null
  try {
    wardrobe = await getWardrobe()
    outfits.value = wardrobe.outfits || []
    backgrounds.value = wardrobe.backgrounds || []
    wardrobeLoaded.value = true
  } catch (e) {
    wardrobeError.value = `의상·배경 목록을 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
  original.value = snapshot(authStore.character || {}, wardrobe)
  Object.assign(draft, original.value)
})

const appearanceDirty = computed(() => !!original.value && APPEARANCE_KEYS.some((k) => draft[k] !== original.value[k]))
const wardrobeDirty = computed(() => !!original.value && wardrobeLoaded.value
  && (draft.outfitId !== original.value.outfitId || draft.stageBackground !== original.value.stageBackground))
const dirty = computed(() => appearanceDirty.value || wardrobeDirty.value)
const nicknameError = computed(() => (appearanceDirty.value ? nicknameProblem(draft.nickname) : ''))

const clothes = computed(() => outfits.value.filter((o) => o.category === 'CLOTHES'))
const accessories = computed(() => outfits.value.filter((o) => o.category === 'ACCESSORY'))

// 미리보기 캐릭터 = 저장된 캐릭터 + 고르는 중인 값 (입을 옷의 그림 포함)
const preview = computed(() => {
  if (!authStore.character) return null
  const outfit = outfits.value.find((o) => o.id === draft.outfitId)
  return { ...authStore.character, ...draft, currentOutfitSpriteKey: outfit?.spriteKey ?? authStore.character.currentOutfitSpriteKey }
})

const reset = () => {
  if (original.value) Object.assign(draft, original.value)
}

const saving = ref(false)
const error = ref('')
const savedMessage = ref('')
const save = async () => {
  saving.value = true
  error.value = ''
  savedMessage.value = ''
  try {
    if (appearanceDirty.value) {
      const body = Object.fromEntries(APPEARANCE_KEYS.map((k) => [k, draft[k]]))
      await updateAppearance({ ...body, nickname: draft.nickname.trim() })
    }
    if (wardrobeDirty.value) {
      await updateWardrobe({ outfitId: draft.outfitId, stageBackground: draft.stageBackground })
    }
    await authStore.fetchCharacter()
    original.value = { ...draft, nickname: draft.nickname.trim() }
    draft.nickname = original.value.nickname
    savedMessage.value = '저장했어요'
    setTimeout(() => { savedMessage.value = '' }, 3000)
  } catch (e) {
    error.value = `저장하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

// 저장하지 않고 나가려 하면 한 번 묻는다
onBeforeRouteLeave(() => (dirty.value ? window.confirm('저장하지 않은 변경이 있어요. 나갈까요?') : true))
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.page-head h2 { margin: 0 0 4px; font-size: 1.3rem; font-weight: 900; }
.page-head p { margin: 0; font-size: 0.8rem; font-weight: 700; color: #adb5bd; }
.head-actions { display: flex; align-items: center; gap: 8px; }
.saved { font-size: 0.8rem; font-weight: 800; color: #2b8a3e; }
.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer; font-family: inherit;
  font-weight: 800; font-size: 0.85rem; padding: 10px 18px; border-radius: 12px;
}
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }
.error { margin: 0; padding: 10px 14px; border-radius: 12px; background: #fff5f5; color: #e03131; font-size: 0.8rem; font-weight: 700; }

.layout { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.preview { position: sticky; top: 88px; }

.panel { background: #fff; border: 1px solid #e9ecef; border-radius: 20px; overflow: hidden; }
.tabs { display: flex; border-bottom: 1px solid #f1f3f5; }
.tabs button {
  flex: 1; border: none; background: none; padding: 14px 0; cursor: pointer; font-family: inherit;
  font-size: 0.9rem; font-weight: 800; color: #adb5bd; border-bottom: 2px solid transparent;
}
.tabs button.on { color: #2d5bff; border-bottom-color: #2d5bff; }
.tab-body { padding: 20px 22px; display: flex; flex-direction: column; gap: 18px; }

.field { display: flex; flex-direction: column; gap: 8px; }
.label { font-size: 0.75rem; font-weight: 800; color: #868e96; }
.input {
  border: 1px solid #dee2e6; border-radius: 10px; padding: 10px 12px; font: inherit; font-size: 0.9rem; font-weight: 700; outline: none;
}
.input:focus { border-color: #2d5bff; }
.warn { font-size: 0.72rem; font-weight: 700; color: #f08c00; margin: 0; }
.hint { margin: 0; font-size: 0.78rem; color: #adb5bd; }

.chips { display: flex; gap: 8px; }
.chip, .opt, .item, .stage {
  border: 1px solid #e9ecef; background: #fff; cursor: pointer; font-family: inherit; color: #495057;
  transition: border-color 0.15s, background 0.15s;
}
.chip { padding: 8px 18px; border-radius: 10px; font-size: 0.85rem; font-weight: 800; }
.chip.on, .opt.on, .item.on, .stage.on { border-color: #2d5bff; background: #f0f4ff; color: #2d5bff; }

.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(84px, 1fr)); gap: 8px; }
.opt {
  display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 10px 4px; border-radius: 12px;
  font-size: 0.75rem; font-weight: 800;
}
.emoji { font-size: 1.3rem; }

.items { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.item {
  display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 16px 8px; border-radius: 14px;
}
.item-thumb {
  position: relative; display: flex; align-items: flex-end; justify-content: center;
  width: 64px; height: 72px; border-radius: 12px; background: #f1f3f5; overflow: hidden; font-size: 1.6rem;
}
.item-thumb i { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(248, 249, 250, 0.7); font-style: normal; }
.empty { display: flex; flex-direction: column; align-items: center; gap: 6px; text-align: center; padding: 28px 12px; border: 2px dashed #e9ecef; border-radius: 14px; }
.empty.small { padding: 14px 12px; }
.empty-icon { font-size: 1.8rem; }
.empty strong { font-size: 0.85rem; font-weight: 800; color: #868e96; }
.empty p { margin: 0; font-size: 0.75rem; color: #adb5bd; }
.sub-section { display: flex; flex-direction: column; gap: 8px; }
.item strong { font-size: 0.85rem; font-weight: 800; }
.item small { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }
.item.on small { color: #2d5bff; }
.item.locked, .stage.locked { cursor: not-allowed; background: #f8f9fa; }
.item.locked strong, .stage.locked strong { color: #adb5bd; }

.stages { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.stage { display: flex; flex-direction: column; gap: 6px; padding: 8px 8px 10px; border-radius: 14px; text-align: left; }
.thumb {
  position: relative; display: block; aspect-ratio: 208 / 72; border-radius: 9px; overflow: hidden;
  background-size: cover; background-position: center; image-rendering: pixelated;
}
.thumb i { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.55); font-style: normal; }
.stage strong { font-size: 0.82rem; font-weight: 800; }
.stage small { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }

@media (max-width: 860px) {
  .layout { grid-template-columns: minmax(0, 1fr); }
  .preview { position: static; }
}
</style>
