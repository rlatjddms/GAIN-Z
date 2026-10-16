<template>
  <!-- 밴드 커뮤니티: 맨 위 공지 한 줄, 아래 최근 글을 3열 카드로 -->
  <section class="community">
    <header class="head">
      <div class="title">
        COMMUNITY
        <span v-if="preview" class="preview-badge" title="게시판 API를 붙이기 전 미리보기 글이에요">미리보기</span>
      </div>
      <button class="write-btn" :disabled="preview" :title="preview ? '게시판을 연결한 뒤 쓸 수 있어요' : '글쓰기'">
        ✏️ 글쓰기
      </button>
    </header>

    <article v-if="notice" class="notice">
      <span class="notice-tag">공지</span>
      <div class="notice-body">
        <strong>{{ notice.title }}</strong>
        <span>{{ notice.content }}</span>
      </div>
      <span class="notice-meta">{{ authorName(notice) }} · {{ timeAgo(notice.minutesAgo) }}</span>
    </article>

    <div v-if="posts.length" class="grid">
      <article v-for="p in posts" :key="p.id" class="post">
        <div class="author">
          <div class="avatar">
            <PixelCharacter v-if="author(p)" :character="author(p).look"
                            :outfit="author(p).character?.currentOutfitSpriteKey || 'DEFAULT'"
                            :scale="2" :animate="false" />
          </div>
          <div class="who">
            <strong>{{ authorName(p) }}</strong>
            <span>{{ timeAgo(p.minutesAgo) }}</span>
          </div>
        </div>
        <h3>{{ p.title }}</h3>
        <p>{{ p.content }}</p>
        <footer>
          <span v-if="p.attachment" class="file" :title="p.attachment">📎 {{ p.attachment }}</span>
          <span class="comments">💬 {{ p.commentCount }}</span>
        </footer>
      </article>
    </div>
    <p v-else class="empty">아직 글이 없어요. 밴드원에게 첫 글을 남겨 보세요.</p>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import PixelCharacter from '../PixelCharacter.vue'

const props = defineProps({
  // 밴드 상세의 members (글쓴이 캐릭터)
  members: { type: Array, default: () => [] },
  notice: { type: Object, default: null },
  posts: { type: Array, default: () => [] },
  // 게시판 API 연결 전 미리보기 글인지
  preview: { type: Boolean, default: false },
})

// 글쓴이: 멤버 중 authorIndex번째 (악기는 밴드 파트로)
const looks = computed(() => props.members.map((m) => ({ ...m, look: { ...m.character, instrumentType: m.part } })))
const author = (post) => (looks.value.length ? looks.value[post.authorIndex % looks.value.length] : null)
const authorName = (post) => author(post)?.character?.nickname || '멤버'

const timeAgo = (minutes) => {
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${minutes}분 전`
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}시간 전`
  return `${Math.floor(minutes / (60 * 24))}일 전`
}
</script>

<style scoped>
.community { margin-top: 24px; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.title { font-weight: 800; font-size: 0.85rem; color: #495057; display: flex; align-items: center; gap: 8px; }
.preview-badge {
  font-size: 0.65rem; font-weight: 800; color: #868e96; background: #f1f3f5; padding: 2px 8px; border-radius: 6px;
}
.write-btn {
  border: 1px solid #2d5bff; background: #fff; color: #2d5bff; border-radius: 10px; padding: 7px 14px;
  font-family: inherit; font-size: 0.78rem; font-weight: 800; cursor: pointer; transition: 0.2s;
}
.write-btn:hover:not(:disabled) { background: #f0f4ff; }
.write-btn:disabled { border-color: #dee2e6; color: #adb5bd; cursor: not-allowed; }

.notice {
  display: flex; align-items: center; gap: 12px; background: #fff; border: 1px solid #e9ecef;
  border-left: 4px solid #2d5bff; border-radius: 16px; padding: 14px 18px; margin-bottom: 12px;
}
.notice-tag {
  flex-shrink: 0; font-size: 0.7rem; font-weight: 900; color: #fff; background: #2d5bff; padding: 3px 8px; border-radius: 6px;
}
.notice-body { flex: 1; min-width: 0; display: flex; gap: 10px; align-items: baseline; }
.notice-body strong { font-size: 0.9rem; font-weight: 800; white-space: nowrap; }
.notice-body span { font-size: 0.8rem; color: #868e96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-meta { flex-shrink: 0; font-size: 0.72rem; color: #adb5bd; font-weight: 600; }

.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.post {
  background: #fff; border: 1px solid #e9ecef; border-radius: 20px; padding: 16px;
  display: flex; flex-direction: column; gap: 8px; cursor: pointer;
  transition: 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.post:hover { border-color: #2d5bff; transform: translateY(-3px); box-shadow: 0 10px 20px rgba(45, 91, 255, 0.06); }
.author { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 36px; height: 36px; border-radius: 12px; background: #f1f5ff; overflow: hidden; flex-shrink: 0;
  display: flex; justify-content: center; align-items: flex-start;
}
/* 캐릭터 머리·상반신만 보이게 */
.avatar :deep(.pixel-character) { width: 48px !important; height: auto !important; margin-top: 1px; }
.who { display: flex; flex-direction: column; min-width: 0; }
.who strong { font-size: 0.8rem; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.who span { font-size: 0.7rem; color: #adb5bd; }
.post h3 { margin: 4px 0 0; font-size: 0.95rem; font-weight: 800; line-height: 1.35; }
.post p {
  margin: 0; font-size: 0.8rem; color: #868e96; line-height: 1.55;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.post footer {
  margin-top: auto; padding-top: 8px; display: flex; justify-content: space-between; align-items: center; gap: 8px;
  font-size: 0.72rem; color: #adb5bd; font-weight: 700;
}
.file { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #495057; background: #f8f9fa; padding: 3px 8px; border-radius: 6px; }
.comments { margin-left: auto; flex-shrink: 0; }
.empty { text-align: center; color: #adb5bd; font-size: 0.85rem; padding: 32px 0; }

@media (max-width: 900px) {
  .grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .grid { grid-template-columns: 1fr; }
  .notice { flex-wrap: wrap; }
  .notice-body { flex-direction: column; gap: 2px; flex-basis: 100%; order: 3; }
  .notice-body strong, .notice-body span { white-space: normal; }
}
</style>
