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

      <div class="layout" :class="{ wide: tab === 'stage' }">
        <!-- 미리보기: 바꾼 모습이 바로 보인다 -->
        <div class="preview">
          <!-- 무대 가운데(캐릭터 쪽)만 카메라처럼 잘라 보여 준다. 배율은 무대 전체와 같다 -->
          <!-- 배경 탭에서는 무대 전체를 보여 주고, 올린 소품을 끌어 옮길 수 있다 -->
          <CharacterStage v-if="preview" :character="preview" bare :view-width="tab === 'stage' ? 208 : PREVIEW_VIEW_WIDTH"
                          :decor-editable="tab === 'stage'" @decor-move="moveDecor" />
          <p class="preview-caption">{{ STAGE_NAMES[draft.stageBackground] }} 무대 · {{ clothes.find((o) => o.id === draft.outfitId)?.name || '기본 의상' }} · {{ BOTTOMS.find((b) => b.key === draft.bottom)?.name || '청바지' }}</p>
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
                <button v-for="h in HAIR_STYLE_OPTIONS" :key="h.value" class="opt"
                        :class="{ on: draft.hairStyle === h.value, locked: !unlocked(h.level) }"
                        :disabled="!unlocked(h.level)" :title="unlocked(h.level) ? '' : `Lv.${h.level}에 해금`"
                        @click="draft.hairStyle = h.value">
                  <span class="opt-thumb">
                    <PixelSprite :parts="[['body', draft.gender === 'MALE' ? 'MALE' : 'FEMALE'], ['hair', h.value]]" :colors="looks" :region="HEAD_REGION" :box="48" :label="h.label" />
                    <i v-if="!unlocked(h.level)">🔒</i>
                  </span>{{ h.label }}
                  <small v-if="!unlocked(h.level)" class="lv">Lv.{{ h.level }}</small>
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
                  <span class="opt-thumb">
                    <PixelSprite :parts="[['instrument', i.value]]" :colors="looks" :box="34 * (i.thumb || 1)" exact :label="i.label" />
                  </span>{{ i.label }}
                </button>
              </div>
              <!-- 악기 종류(밴드 포지션)는 그대로, 그림에서만 내려놓기 -->
              <label class="toggle">
                <input type="checkbox" :checked="!draft.instrumentHidden" @change="draft.instrumentHidden = !$event.target.checked">
                <span>악기 표시</span>
                <small>끄면 옷이 잘 보여요 (맡은 악기는 그대로)</small>
              </label>
            </div>

            <div class="field">
              <span class="label">악기 색상</span>
              <ColorSwatches v-model="draft.instrumentColor" :presets="INSTRUMENT_COLOR_PRESETS" label="악기 색상" />
            </div>
          </div>

          <!-- 의상: 상의(성별마다 다른 그림)와 하의를 따로 고른다. 썸네일은 옷만 -->
          <div v-else-if="tab === 'outfit'" class="tab-body">
            <p class="hint">5레벨마다 상의와 하의를 한 벌씩 얻어요. 크롭티는 Lv.7, 치마는 Lv.8이에요.</p>
            <p v-if="wardrobeError" class="warn">{{ wardrobeError }}</p>
            <div class="field">
              <span class="label">상의</span>
              <div class="items small">
                <button
                  v-for="o in clothes" :key="o.id" class="item"
                  :class="{ on: draft.outfitId === o.id, locked: !o.owned }"
                  :disabled="!o.owned"
                  @click="draft.outfitId = o.id"
                >
                  <span class="item-thumb sprite">
                    <PixelSprite :parts="[['outfit', outfitSpriteName(o.spriteKey, draft.gender)]]" :item-colors="draft.itemColors" :box="44" :label="o.name" />
                    <i v-if="!o.owned">🔒</i>
                  </span>
                  <strong>{{ o.name }}</strong>
                  <small>{{ o.owned ? (draft.outfitId === o.id ? '입는 중' : '입기') : `Lv.${o.requiredLevel}에 해금` }}</small>
                </button>
              </div>
              <div v-if="isColorable(wornTop.key)" class="item-color">
                <span>{{ wornTop.name }} 색</span>
                <ColorSwatches :model-value="itemColorOf(wornTop.key, draft.itemColors)" :presets="ITEM_COLORS[wornTop.key].presets"
                               :label="`${wornTop.name} 색`" @update:model-value="setItemColor(wornTop.key, $event)" />
              </div>
            </div>
            <div class="field">
              <span class="label">하의</span>
              <div class="items small">
                <button
                  v-for="b in BOTTOMS" :key="b.key" class="item"
                  :class="{ on: draft.bottom === b.key, locked: !unlocked(b.level) }"
                  :disabled="!unlocked(b.level)"
                  @click="draft.bottom = b.key"
                >
                  <span class="item-thumb sprite">
                    <PixelSprite :parts="[['bottom', b.key]]" :item-colors="draft.itemColors" :box="44" :label="b.name" />
                    <i v-if="!unlocked(b.level)">🔒</i>
                  </span>
                  <strong>{{ b.name }}</strong>
                  <small>{{ unlocked(b.level) ? (draft.bottom === b.key ? '입는 중' : '입기') : `Lv.${b.level}에 해금` }}</small>
                </button>
              </div>
              <div v-if="isColorable(draft.bottom)" class="item-color">
                <span>{{ nameOf(draft.bottom) }} 색</span>
                <ColorSwatches :model-value="itemColorOf(draft.bottom, draft.itemColors)" :presets="ITEM_COLORS[draft.bottom].presets"
                               :label="`${nameOf(draft.bottom)} 색`" @update:model-value="setItemColor(draft.bottom, $event)" />
              </div>
            </div>
          </div>

          <!-- 소품: 자리마다 하나씩. 신발처럼 늘 입는 자리는 '없음'이 없다 -->
          <div v-else-if="tab === 'accessory'" class="tab-body">
            <p class="hint">레벨이 오를 때마다 소품이나 헤어스타일을 하나씩 얻어요.</p>
            <div v-for="slot in ACCESSORY_SLOTS" :key="slot.key" class="field">
              <span class="label">{{ slot.label }}</span>
              <div class="items small">
                <button v-if="!slot.required" class="item" :class="{ on: !draft[slot.field] }" @click="draft[slot.field] = null">
                  <span class="item-thumb none">없음</span>
                  <small>{{ !draft[slot.field] ? '쓰는 중' : '벗기' }}</small>
                </button>
                <button
                  v-for="a in accessoriesOf(slot.key)" :key="a.key" class="item"
                  :class="{ on: draft[slot.field] === a.key, locked: !unlocked(a.level) }"
                  :disabled="!unlocked(a.level)"
                  @click="draft[slot.field] = a.key"
                >
                  <span class="item-thumb sprite">
                    <PixelSprite :parts="[[slot.folder, a.key]]" :item-colors="draft.itemColors" :box="44" :label="a.name" />
                    <i v-if="!unlocked(a.level)">🔒</i>
                  </span>
                  <strong>{{ a.name }}</strong>
                  <small>{{ unlocked(a.level) ? (draft[slot.field] === a.key ? '쓰는 중' : '쓰기') : `Lv.${a.level}에 해금` }}</small>
                </button>
              </div>
              <div v-if="isColorable(draft[slot.field])" class="item-color">
                <span>{{ nameOf(draft[slot.field]) }} 색</span>
                <ColorSwatches :model-value="itemColorOf(draft[slot.field], draft.itemColors)" :presets="ITEM_COLORS[draft[slot.field]].presets"
                               :label="`${nameOf(draft[slot.field])} 색`" @update:model-value="setItemColor(draft[slot.field], $event)" />
              </div>
            </div>
          </div>

          <!-- 배경: 레벨로 얻은 무대는 계속 간직하고 그중 하나를 고른다 -->
          <div v-else class="tab-body">
            <p class="hint">Lv.10·20·30에 새 무대를 얻고, 레코드를 모두 모으면 리스닝 룸을 얻어요. 얻은 무대는 언제든 다시 고를 수 있어요.</p>
            <p v-if="wardrobeError" class="warn">{{ wardrobeError }}</p>
            <div class="stages">
              <button
                v-for="b in backgrounds" :key="b.key" class="stage"
                :class="{ on: draft.stageBackground === b.key, locked: !b.unlocked }"
                :disabled="!b.unlocked"
                @click="draft.stageBackground = b.key"
              >
                <span class="thumb">
                  <StageScene :character="preview" :stage-key="b.key" :effects="[]" :decor="[]" :animate="false" />
                  <i v-if="!b.unlocked">🔒</i>
                </span>
                <strong>{{ STAGE_NAMES[b.key] }}</strong>
                <small>{{ b.unlocked ? (draft.stageBackground === b.key ? '사용 중' : '고르기') : stageLockText(b) }}</small>
              </button>
            </div>

            <div class="sub-section">
              <span class="label">방 꾸미기</span>
              <p class="hint">레코드를 종류별로 모두 모으면 소품을 얻어요. 올린 소품은 위 미리보기에서 끌어서 어디든 옮길 수 있어요. 캐릭터 발보다 아래에 두면 캐릭터 앞에 보여요.</p>
              <div class="items small">
                <button
                  v-for="d in ROOM_DECOR_ITEMS" :key="d.key" class="item"
                  :class="{ on: isPlaced(d.key), locked: !decorUnlocked(d.set) }"
                  :disabled="!decorUnlocked(d.set)"
                  @click="toggleDecor(d)"
                >
                  <span class="item-thumb sprite">
                    <img class="decor-thumb" :src="decorUrl(d.key)" :alt="d.name">
                    <i v-if="!decorUnlocked(d.set)">🔒</i>
                  </span>
                  <strong>{{ d.name }}</strong>
                  <small>{{ decorUnlocked(d.set) ? (isPlaced(d.key) ? '올려 둠' : '올리기') : decorLockText(d.set) }}</small>
                </button>
              </div>
            </div>

            <div class="sub-section">
              <span class="label">무대 효과</span>
              <p class="hint">5레벨마다 의상과 함께 무대 배경이나 효과를 얻어요. 여러 개를 같이 켤 수 있어요.</p>
              <div class="stages">
                <button class="stage" :class="{ on: !draft.stageEffects.length }" @click="draft.stageEffects = []">
                  <span class="thumb"><StageScene :character="preview" :stage-key="draft.stageBackground" :effects="[]" :decor="[]" :animate="false" /></span>
                  <strong>효과 없음</strong>
                  <small>{{ !draft.stageEffects.length ? '사용 중' : '모두 끄기' }}</small>
                </button>
                <button
                  v-for="e in STAGE_EFFECTS" :key="e.key" class="stage"
                  :class="{ on: draft.stageEffects.includes(e.key), locked: !unlocked(e.level) }"
                  :disabled="!unlocked(e.level)"
                  @click="toggleEffect(e.key)"
                >
                  <span class="thumb">
                    <StageScene :character="preview" :stage-key="draft.stageBackground" :effects="[e.key]" :decor="[]" :animate="false" />
                    <i v-if="!unlocked(e.level)">🔒</i>
                  </span>
                  <strong>{{ e.icon }} {{ e.name }}</strong>
                  <small>{{ unlocked(e.level) ? (draft.stageEffects.includes(e.key) ? '켜짐' : '켜기') : `Lv.${e.level}에 해금` }}</small>
                </button>
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
import PixelSprite from '../components/PixelSprite.vue'
import StageScene from '../components/StageScene.vue'
import { useAuthStore } from '../stores/auth'
import { getWardrobe, updateWardrobe, updateAppearance } from '../api/character'
import { apiErrorMessage } from '../api/practice'
import { getLevelTier } from '../constants/levelTier'
import {
  GENDER_OPTIONS, HAIR_STYLE_OPTIONS, SKIN_COLOR_PRESETS, HAIR_COLOR_PRESETS,
  INSTRUMENT_OPTIONS, INSTRUMENT_COLOR_PRESETS, nicknameProblem,
} from '../constants/characterOptions'
import { DEFAULT_COLORS, HEAD_REGION } from '../pixel/palette'
import { outfitSpriteName } from '../pixel/sprites'
import { ACCESSORY_SLOTS, ACCESSORIES, BOTTOMS, BOTTOM_SLOT, STAGE_EFFECTS, wornIn } from '../constants/rewards'
import { ITEM_COLORS, isColorable, itemColorOf } from '../constants/itemColors'
import { ROOM_DECOR_SETS, ROOM_DECOR_ITEMS, decorUrl } from '../constants/recordRewards'
import { rarityOf } from '../constants/records'

const TABS = [
  { key: 'look', label: '외형' },
  { key: 'outfit', label: '의상' },
  { key: 'accessory', label: '소품' },
  { key: 'stage', label: '배경' },
]
// 미리보기는 무대(208칸) 중 가운데 96칸만 보여 준다 → 왼쪽 칸에 크게, 한 화면에 들어가게
const PREVIEW_VIEW_WIDTH = 96
const STAGE_NAMES = { ROOKIE: '방구석', RISING: '동네 클럽', PRO: '투어 공연장', LEGEND: '전설의 무대', LISTENING_ROOM: '리스닝 룸' }
// 레코드 수집 보상: 무대 잠금 안내
const stageLockText = (b) => (b.key === 'LISTENING_ROOM' ? '레코드 12개를 모두 모으면' : `Lv.${b.requiredLevel}에 해금`)
const decorLockText = (setKey) => `${rarityOf(ROOM_DECOR_SETS.find((s) => s.key === setKey).rarity).label}를 모두 모으면`

const authStore = useAuthStore()
const tab = ref('look')

// 저장된 상태와 지금 고르는 중인 상태
const APPEARANCE_KEYS = ['nickname', 'gender', 'skinColor', 'hairStyle', 'hairColor', 'instrumentType', 'instrumentColor']
const original = ref(null)
const draft = reactive({
  nickname: '', gender: 'MALE', skinColor: DEFAULT_COLORS.skinColor, hairStyle: 'SHORT_CUT', hairColor: '#111827',
  instrumentType: 'GUITAR', instrumentColor: '#B91C1C', outfitId: null, stageBackground: null,
  itemColors: {}, instrumentHidden: false, stageEffects: [], decorLayout: [], bottom: 'JEANS', hat: null, glasses: null, earrings: null, neck: null, waist: null, shoes: 'BASIC_SHOES',
})
const WEAR_SLOTS = [BOTTOM_SLOT, ...ACCESSORY_SLOTS]
const ACCESSORY_FIELDS = WEAR_SLOTS.map((s) => s.field)
const WARDROBE_KEYS = ['outfitId', 'stageBackground', 'instrumentHidden', ...ACCESSORY_FIELDS]
// 목록·표라서 내용으로 비교하는 값
const LIST_KEYS = ['itemColors', 'stageEffects', 'decorLayout']

const outfits = ref([])
const backgrounds = ref([])
// 방 꾸미기 세트 잠금 여부 [{ key, unlocked }] (레코드 수집 보상, 서버가 판단)
const decorSets = ref([])
const decorUnlocked = (key) => decorSets.value.some((s) => s.key === key && s.unlocked)

// 방 꾸미기: 소품을 올리거나 내리고, 미리보기에서 끈 자리로 옮긴다 (목록은 새로 만들어 넣어 되돌리기가 섞이지 않게)
const isPlaced = (key) => draft.decorLayout.some((d) => d.key === key)
const toggleDecor = (item) => {
  draft.decorLayout = isPlaced(item.key)
    ? draft.decorLayout.filter((d) => d.key !== item.key)
    : [...draft.decorLayout, { key: item.key, x: item.defaultX }]
}
const moveDecor = (key, x, y) => {
  draft.decorLayout = draft.decorLayout.map((d) => (d.key === key ? { ...d, x, y } : d))
}
// 무대 효과: 여러 개를 켜고 끈다 (표 순서대로)
const toggleEffect = (key) => {
  const on = new Set(draft.stageEffects)
  if (on.has(key)) on.delete(key)
  else on.add(key)
  draft.stageEffects = STAGE_EFFECTS.map((e) => e.key).filter((k) => on.has(k))
}
const wardrobeLoaded = ref(false)
const wardrobeError = ref('')

const snapshot = (c, wardrobe) => ({
  nickname: c.nickname || '', gender: c.gender || 'MALE', skinColor: c.skinColor || DEFAULT_COLORS.skinColor,
  hairStyle: c.hairStyle || 'SHORT_CUT', hairColor: c.hairColor || '#111827',
  instrumentType: c.instrumentType || 'GUITAR', instrumentColor: c.instrumentColor || '#B91C1C',
  outfitId: wardrobe?.outfits?.find((o) => o.equipped)?.id ?? c.currentOutfitId ?? null,
  // 고른 적 없으면 레벨로 얻은 가장 큰 무대 (서버도 같은 규칙으로 돌려준다)
  stageBackground: wardrobe?.stageBackground ?? c.stageBackground ?? getLevelTier(c.level ?? 1).key,
  stageEffects: [...(c.stageEffects || [])],
  decorLayout: (c.decorLayout || []).map((d) => ({ ...d })),
  instrumentHidden: !!c.instrumentHidden,
  itemColors: { ...(c.itemColors || {}) },
  ...Object.fromEntries(WEAR_SLOTS.map((s) => [s.field, wornIn(s, c)])),
})

onMounted(async () => {
  if (!authStore.character) await authStore.fetchCharacter()
  let wardrobe = null
  try {
    wardrobe = await getWardrobe()
    outfits.value = wardrobe.outfits || []
    backgrounds.value = wardrobe.backgrounds || []
    decorSets.value = wardrobe.decorSets || []
    wardrobeLoaded.value = true
  } catch (e) {
    wardrobeError.value = `의상·배경 목록을 불러오지 못했어요. ${apiErrorMessage(e)}`
  }
  original.value = snapshot(authStore.character || {}, wardrobe)
  Object.assign(draft, original.value)
})

const appearanceDirty = computed(() => !!original.value && APPEARANCE_KEYS.some((k) => draft[k] !== original.value[k]))
const wardrobeDirty = computed(() => !!original.value && wardrobeLoaded.value
  && (WARDROBE_KEYS.some((k) => draft[k] !== original.value[k])
    || LIST_KEYS.some((k) => JSON.stringify(draft[k]) !== JSON.stringify(original.value[k]))))
const dirty = computed(() => appearanceDirty.value || wardrobeDirty.value)
const nicknameError = computed(() => (appearanceDirty.value ? nicknameProblem(draft.nickname) : ''))

const clothes = computed(() => outfits.value.filter((o) => o.category === 'CLOTHES'))

// 레벨 보상: 레벨이 닿으면 가진 것 (레벨은 내려가지 않는다)
const level = computed(() => authStore.character?.level ?? 1)
const unlocked = (required) => !required || level.value >= required
const accessoriesOf = (slot) => ACCESSORIES.filter((a) => a.slot === slot)
// 아이템 색: 입고 있는 아이템마다 추천 색이나 직접 고른 색 (기본 색이면 저장하지 않음)
const wornTop = computed(() => {
  const o = clothes.value.find((c) => c.id === draft.outfitId)
  return { key: o?.spriteKey || 'DEFAULT', name: o?.name || '기본 의상' }
})
const nameOf = (key) => [...BOTTOMS, ...ACCESSORIES].find((i) => i.key === key)?.name || ''
const setItemColor = (key, color) => {
  const next = { ...draft.itemColors }
  if (color.toLowerCase() === ITEM_COLORS[key].presets[0].toLowerCase()) delete next[key]
  else next[key] = color
  draft.itemColors = next
}

// 썸네일 색: 지금 고르는 피부·머리·악기 색
const looks = computed(() => ({ skinColor: draft.skinColor, hairColor: draft.hairColor, instrumentColor: draft.instrumentColor }))

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
      await updateWardrobe(Object.fromEntries([...WARDROBE_KEYS, ...LIST_KEYS].map((k) => [k, draft[k]])))
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

.layout { display: grid; grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr); gap: 16px; align-items: start; }
.preview { display: flex; flex-direction: column; gap: 8px; }
.preview-caption { margin: 0; text-align: center; font-size: 0.78rem; font-weight: 800; color: #868e96; }

/* 넓은 화면: 옷장 전체가 한 화면에 들어오고, 긴 목록은 오른쪽 칸 안에서만 스크롤 */
@media (min-width: 861px) {
  .layout { height: calc(100vh - 250px); min-height: 440px; }
  .panel { height: 100%; display: flex; flex-direction: column; }
  .tab-body { flex: 1; min-height: 0; overflow-y: auto; }
}
@media (max-width: 860px) {
  .layout { grid-template-columns: minmax(0, 1fr); }
}
/* 배경 탭: 무대를 크게 보도록 미리보기를 위에 꽉 차게, 목록은 그 아래 */
.layout.wide { grid-template-columns: minmax(0, 1fr); height: auto; }
.layout.wide .panel { height: auto; }
.layout.wide .tab-body { overflow: visible; }

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
.opt-thumb {
  position: relative; width: 52px; height: 40px; border-radius: 10px; background: #f8f9fa;
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.opt.on .opt-thumb { background: #fff; }
.opt-thumb i, .item-thumb.sprite i { font-style: normal; }
.opt-thumb i { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(248, 249, 250, 0.75); font-size: 0.9rem; }
.item-thumb.sprite { align-items: center; }
.toggle {
  display: grid; grid-template-columns: auto 1fr; align-items: center; column-gap: 8px; row-gap: 2px; cursor: pointer;
  padding: 10px 12px; background: #f8f9fa; border-radius: 12px;
}
.toggle input { width: 16px; height: 16px; accent-color: #2d5bff; }
.toggle span { font-size: 0.82rem; font-weight: 800; color: #495057; }
.toggle small { grid-column: 2; font-size: 0.7rem; font-weight: 700; color: #adb5bd; }
.decor-thumb { width: 40px; image-rendering: pixelated; }
.item-color { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 10px 12px; background: #f8f9fa; border-radius: 12px; }
.item-color > span { font-size: 0.75rem; font-weight: 800; color: #868e96; }

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
.item.locked, .stage.locked, .opt.locked { cursor: not-allowed; background: #f8f9fa; }
.opt.locked { color: #adb5bd; }
.lv { font-size: 0.65rem; font-weight: 800; color: #adb5bd; }
.items.small { grid-template-columns: repeat(auto-fill, minmax(96px, 1fr)); gap: 8px; }
.items.small .item { padding: 10px 6px; }
.items.small .item strong { font-size: 0.78rem; }
.item-thumb.none { align-items: center; font-size: 0.75rem; font-weight: 800; color: #adb5bd; }

.item.locked strong, .stage.locked strong { color: #adb5bd; }

.stages { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.stage { display: flex; flex-direction: column; gap: 6px; padding: 8px 8px 10px; border-radius: 14px; text-align: left; }
.thumb { position: relative; display: block; border-radius: 9px; overflow: hidden; line-height: 0; }
.thumb i { position: absolute; inset: 0; z-index: 5; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.55); font-style: normal; line-height: 1; }
.stage strong { font-size: 0.82rem; font-weight: 800; }
.stage small { font-size: 0.7rem; font-weight: 700; color: #adb5bd; }

</style>
