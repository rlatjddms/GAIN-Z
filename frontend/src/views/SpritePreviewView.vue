<template>
  <div class="sprite-preview">
    <header class="head">
      <h1>픽셀 스프라이트 미리보기</h1>
      <p>개발용 페이지. 스프라이트 PNG(<code>src/assets/sprites/</code>)를 바꾸면 바로 반영됩니다.</p>
    </header>

    <section class="panel">
      <h2>조합 미리보기</h2>
      <div class="playground">
        <div class="stage">
          <PixelCharacter :character="character" :outfit="outfit" :scale="8" />
        </div>
        <div class="controls">
          <label>성별
            <select v-model="character.gender">
              <option value="FEMALE">FEMALE</option>
              <option value="MALE">MALE</option>
            </select>
          </label>
          <label>머리
            <select v-model="character.hairStyle">
              <option v-for="h in hairStyles" :key="h" :value="h">{{ h }}</option>
            </select>
          </label>
          <label>악기
            <select v-model="character.instrumentType">
              <option v-for="i in instruments" :key="i" :value="i">{{ i }}</option>
            </select>
          </label>
          <label>의상
            <select v-model="outfit">
              <option v-for="o in outfits" :key="o" :value="o">{{ o }}</option>
            </select>
          </label>
          <label v-for="slot in ACCESSORY_SLOTS" :key="slot.key">{{ slot.label }}
            <select v-model="character[slot.field]">
              <option :value="null">없음</option>
              <option v-for="a in listSprites(slot.folder)" :key="a" :value="a">{{ a }}</option>
            </select>
          </label>
          <label>피부색 <input type="color" v-model="character.skinColor"></label>
          <label>머리색 <input type="color" v-model="character.hairColor"></label>
          <label>악기색 <input type="color" v-model="character.instrumentColor"></label>
          <label>의상색 <input type="color" v-model="character.outfitColor"></label>
        </div>
      </div>
    </section>

    <section class="panel">
      <h2>썸네일 (옷장·캐릭터 만들기)</h2>
      <div class="grid thumbs">
        <figure v-for="h in hairStyles" :key="`h-${h}`">
          <PixelSprite :parts="[['body', character.gender], ['hair', h]]" :colors="character" :region="HEAD_REGION" :box="48" />
          <figcaption>{{ h }}</figcaption>
        </figure>
        <figure v-for="i in instruments" :key="`i-${i}`">
          <PixelSprite :parts="[['instrument', i]]" :colors="character" :box="36" exact />
          <figcaption>{{ i }}</figcaption>
        </figure>
        <template v-for="slot in ACCESSORY_SLOTS" :key="`s-${slot.key}`">
          <figure v-for="a in listSprites(slot.folder)" :key="`a-${a}`">
            <PixelSprite :parts="[[slot.folder, a]]" :box="44" />
            <figcaption>{{ a }}</figcaption>
          </figure>
        </template>
      </div>
    </section>

    <section class="panel">
      <h2>아이템 추천 색 {{ colorItems.length }}종</h2>
      <div class="color-items">
        <div v-for="it in colorItems" :key="it.key" class="color-item">
          <figure v-for="c in it.presets" :key="c">
            <PixelSprite :parts="[[it.layer, it.file]]" :item-colors="{ [it.key]: c }" :box="44" />
            <figcaption :style="{ color: c }">●</figcaption>
          </figure>
          <span>{{ it.key }}</span>
        </div>
      </div>
    </section>

    <section class="panel">
      <h2>무대 효과 {{ STAGE_EFFECTS.length }}종</h2>
      <div class="fx-grid">
        <figure v-for="e in STAGE_EFFECTS" :key="e.key">
          <StageScene :character="character" stage-key="RISING" :effect="e.key" />
          <figcaption>{{ e.icon }} {{ e.key }}</figcaption>
        </figure>
      </div>
    </section>

    <section class="panel">
      <h2>레벨 구간별 무대</h2>
      <CharacterStage v-for="lv in [3, 14, 35, 50]" :key="lv" :character="{ ...character, nickname: '미리보기', level: lv, exp: 40 }" />
    </section>

    <section class="panel">
      <h2>머리 스타일 {{ hairStyles.length }}종</h2>
      <div class="grid">
        <figure v-for="h in hairStyles" :key="h">
          <PixelCharacter :character="{ ...character, hairStyle: h }" :scale="4" :animate="false" />
          <figcaption>{{ h }}</figcaption>
        </figure>
      </div>
    </section>

    <section class="panel">
      <h2>의상 {{ outfits.length }}종</h2>
      <div class="grid">
        <figure v-for="o in outfits" :key="o">
          <PixelCharacter :character="character" :outfit="o" :scale="4" :animate="false" />
          <figcaption>{{ o }}</figcaption>
        </figure>
      </div>
    </section>

    <section v-for="slot in ACCESSORY_SLOTS" :key="slot.key" class="panel">
      <h2>소품 · {{ slot.label }} {{ listSprites(slot.folder).length }}종</h2>
      <div class="grid">
        <figure v-for="a in listSprites(slot.folder)" :key="a">
          <PixelCharacter :character="{ ...character, [slot.field]: a }" :outfit="outfit" :scale="4" :animate="false" />
          <figcaption>{{ a }}</figcaption>
        </figure>
      </div>
    </section>

    <section class="panel">
      <h2>악기 {{ instruments.length }}종</h2>
      <div class="grid">
        <figure v-for="i in instruments" :key="i">
          <PixelCharacter :character="{ ...character, instrumentType: i }" :scale="4" :animate="false" />
          <figcaption>{{ i }}</figcaption>
        </figure>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import PixelCharacter from '../components/PixelCharacter.vue'
import CharacterStage from '../components/CharacterStage.vue'
import PixelSprite from '../components/PixelSprite.vue'
import StageScene from '../components/StageScene.vue'
import { HEAD_REGION } from '../pixel/palette'
import { listSprites } from '../pixel/sprites'
import { ACCESSORY_SLOTS as WEAR_SLOTS, BOTTOM_SLOT, STAGE_EFFECTS, ACCESSORIES, BOTTOMS, slotOf } from '../constants/rewards'
import { ITEM_COLORS } from '../constants/itemColors'
import { outfitSpriteName } from '../pixel/sprites'

// 하의도 소품처럼 자리 하나로 보여 준다
const ACCESSORY_SLOTS = [BOTTOM_SLOT, ...WEAR_SLOTS]

const hairStyles = listSprites('hair')
const instruments = listSprites('instrument')
// 의상 파일은 <이름>_<성별>.png → 이름만
const outfits = [...new Set(listSprites('outfit').map((n) => n.replace(/_(MALE|FEMALE)$/, '')))]
const outfit = ref('DEFAULT')

// 색을 바꿀 수 있는 아이템과 추천 색 (그림 폴더·파일 찾기)
const colorItems = Object.entries(ITEM_COLORS).map(([key, spec]) => {
  const item = [...ACCESSORIES, ...BOTTOMS].find((i) => i.key === key)
  return item
    ? { key, presets: spec.presets, layer: slotOf(item.slot).folder, file: key }
    : { key, presets: spec.presets, layer: 'outfit', file: outfitSpriteName(key, 'MALE') }
})

const character = reactive({
  gender: 'FEMALE',
  hairStyle: 'LONG_STRAIGHT',
  instrumentType: 'GUITAR',
  skinColor: '#F8D7B5',
  hairColor: '#EC4899',
  instrumentColor: '#B91C1C',
  outfitColor: '#FFFFFF',
  bottom: null, hat: null, glasses: null, earrings: null, neck: null, waist: null, shoes: null,
})
</script>

<style scoped>
.sprite-preview { max-width: 1040px; margin: 0 auto; padding: 32px 20px 60px; font-family: 'Pretendard', sans-serif; color: #1a1a1a; }
.head h1 { font-size: 1.4rem; margin: 0 0 6px; }
.head p { margin: 0 0 24px; color: #666; font-size: 0.9rem; }
code { background: #f1f3f5; padding: 1px 6px; border-radius: 6px; }
.panel { background: #fff; border: 1px solid #e9ecef; border-radius: 20px; padding: 20px 24px; margin-bottom: 20px; }
.panel h2 { font-size: 1rem; margin: 0 0 16px; }
.playground { display: flex; gap: 28px; align-items: flex-start; flex-wrap: wrap; }
.stage { background: linear-gradient(#dcebc9, #b9d39a); border-radius: 14px; padding: 20px 40px; }
.controls { display: grid; grid-template-columns: repeat(3, auto); gap: 12px 18px; font-size: 0.85rem; font-weight: 600; }
.controls label { display: flex; flex-direction: column; gap: 6px; }
select, input[type=color] { font: inherit; padding: 6px 8px; border: 1px solid #dee2e6; border-radius: 8px; background: #fff; }
input[type=color] { width: 60px; height: 34px; padding: 2px; }
.grid { display: flex; flex-wrap: wrap; gap: 14px; }
figure { margin: 0; background: #f8f9fa; border-radius: 12px; padding: 12px 10px 8px; text-align: center; }
.color-items { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 10px; }
.color-item { display: flex; align-items: center; gap: 6px; background: #f8f9fa; border-radius: 12px; padding: 8px; }
.color-item figure { padding: 4px; background: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 52px; min-height: 52px; }
.color-item figcaption { margin: 0; font-size: 0.8rem; }
.color-item > span { font-size: 0.62rem; font-weight: 800; color: #868e96; word-break: break-all; }
.fx-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.fx-grid figure { padding: 0 0 8px; overflow: hidden; }
.thumbs figure { min-width: 64px; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; }
figcaption { font-size: 0.7rem; font-weight: 700; color: #495057; margin-top: 6px; }
</style>
