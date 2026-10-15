<template>
  <SubPageLayout>
  <div class="page">
    <div class="page-head">
      <h2>악보 보관함</h2>
      <div class="head-actions">
        <router-link to="/transcribe" class="btn">✨ AI로 악보 만들기</router-link>
        <button class="btn primary" disabled>+ 악보 담기</button>
      </div>
    </div>

    <!-- 보관함 목록: #49(API)·#50에서 연결 -->
    <WsCard title="내 악보" soon>
      <template #actions>
        <div class="filters">
          <button class="chip on" disabled>전체</button>
          <button class="chip" disabled>★ 즐겨찾기</button>
        </div>
      </template>
      <div class="empty">
        <div class="empty-icon">📂</div>
        <strong>아직 담아 둔 악보가 없어요</strong>
        <p>악보를 담으면 여기서 메모를 남기고, 내 키로 바꿔서 볼 수 있어요.<br>AI로 만든 악보도 여기에 모여요.</p>
      </div>
    </WsCard>
  </div>
  </SubPageLayout>
</template>

<script setup>
import SubPageLayout from '../../components/workspace/SubPageLayout.vue'
import WsCard from '../../components/workspace/WsCard.vue'
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 4px; }
.page-head h2 { margin: 0; font-size: 1.3rem; font-weight: 900; }
.head-actions { display: flex; gap: 8px; }

.btn {
  border: 1px solid #e9ecef; background: #fff; color: #495057; font-family: inherit;
  font-weight: 800; font-size: 0.8rem; padding: 10px 14px; border-radius: 12px; text-decoration: none;
}
a.btn:hover { border-color: #2d5bff; color: #2d5bff; }
.btn.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.btn:disabled { opacity: 0.4; cursor: not-allowed; }

.filters { display: flex; gap: 6px; }
.chip {
  border: 1px solid #e9ecef; background: #fff; color: #868e96; font-family: inherit;
  font-size: 0.7rem; font-weight: 800; padding: 5px 10px; border-radius: 8px;
}
.chip.on { background: #f0f4ff; border-color: #c9d6ff; color: #2d5bff; }
.chip:disabled { cursor: not-allowed; }

.empty {
  border: 2px dashed #e9ecef; border-radius: 16px; padding: 48px 20px;
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px;
}
.empty-icon { font-size: 2rem; margin-bottom: 4px; }
.empty strong { font-size: 0.95rem; font-weight: 800; color: #495057; }
.empty p { margin: 0; font-size: 0.8rem; color: #adb5bd; line-height: 1.6; }
</style>
