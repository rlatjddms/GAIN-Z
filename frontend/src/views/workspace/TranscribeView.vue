<template>
  <SubPageLayout title="AI TRANSCRIBE" name="AI 악보 만들기" subtitle="음원을 올리면 내 악기에 맞는 악보를 만들어 줘요">
    <div class="page">

      <!-- 음원 업로드·분석: #52(기술 조사)·#53에서 연결 -->
      <WsCard title="음원 올리기" soon>
        <div class="dropzone">
          <div class="drop-icon"><PixelIcon name="music" :size="48" /></div>
          <strong>음원 파일을 끌어다 놓거나 눌러서 고르세요</strong>
          <p>MP3 · WAV · M4A</p>
          <button class="btn primary" disabled>파일 선택</button>
        </div>

        <ol class="steps">
          <li v-for="(s, i) in STEPS" :key="s.title">
            <span class="step-no">{{ i + 1 }}</span>
            <div>
              <strong>{{ s.title }}</strong>
              <p>{{ s.desc }}</p>
            </div>
          </li>
        </ol>
      </WsCard>

      <WsCard title="최근에 만든 악보" soon>
        <template #actions>
          <router-link to="/sheets" class="text-btn">보관함 보기 ›</router-link>
        </template>
        <p class="empty">아직 만든 악보가 없어요. 완성되면 알림으로 알려 드리고 보관함에 담아 둘게요.</p>
      </WsCard>
    </div>
  </SubPageLayout>
</template>

<script setup>
import PixelIcon from '../../components/PixelIcon.vue'
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'

const STEPS = [
  { title: '음원 올리기', desc: '연습하고 싶은 곡의 음원을 올려요.' },
  { title: 'AI 분석', desc: '악기별 소리를 나누고 음과 코드를 찾아요. 몇 분 걸릴 수 있어요.' },
  { title: '악보 완성', desc: '타브 · 코드 · 오선보로 만들어 보관함에 담아요.' },
]
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }

.dropzone {
  border: 2px dashed var(--c-primary-line); background: var(--c-primary-soft); border-radius: 16px; padding: 36px 20px;
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 6px;
}
.drop-icon { margin-bottom: 4px; color: var(--c-faint); }
.dropzone strong { font-size: 0.95rem; font-weight: 800; color: var(--c-text-2); }
.dropzone p { margin: 0 0 10px; font-size: 0.75rem; font-weight: 700; color: var(--c-faint); }


.steps { list-style: none; margin: 20px 0 0; padding: 0; display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.steps li { display: flex; gap: 10px; align-items: flex-start; }
.step-no {
  flex: none; width: 24px; height: 24px; border-radius: 8px; background: var(--c-primary-soft); color: var(--c-primary-text);
  font-size: 0.75rem; font-weight: 900; display: flex; align-items: center; justify-content: center;
}
.steps strong { display: block; font-size: 0.85rem; font-weight: 800; margin-bottom: 2px; }
.steps p { margin: 0; font-size: 0.75rem; color: var(--c-muted); line-height: 1.5; }

.text-btn { font-size: 0.75rem; font-weight: 800; color: var(--c-primary-text); text-decoration: none; }
.empty { margin: 0; padding: 20px 0; text-align: center; font-size: 0.8rem; color: var(--c-faint); }

@media (max-width: 760px) {
  .steps { grid-template-columns: 1fr; }
}
</style>
