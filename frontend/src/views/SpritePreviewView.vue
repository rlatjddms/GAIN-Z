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
          <PixelCharacter :character="character" :scale="8" />
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
          <label>피부색 <input type="color" v-model="character.skinColor"></label>
          <label>머리색 <input type="color" v-model="character.hairColor"></label>
          <label>악기색 <input type="color" v-model="character.instrumentColor"></label>
          <label>의상색 <input type="color" v-model="character.outfitColor"></label>
        </div>
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
import { reactive } from 'vue'
import PixelCharacter from '../components/PixelCharacter.vue'
import CharacterStage from '../components/CharacterStage.vue'
import { listSprites } from '../pixel/sprites'

const hairStyles = listSprites('hair')
const instruments = listSprites('instrument')

const character = reactive({
  gender: 'FEMALE',
  hairStyle: 'LONG_STRAIGHT',
  instrumentType: 'GUITAR',
  skinColor: '#F8D7B5',
  hairColor: '#EC4899',
  instrumentColor: '#B91C1C',
  outfitColor: '#FFFFFF',
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
figcaption { font-size: 0.7rem; font-weight: 700; color: #495057; margin-top: 6px; }
</style>
