<template>
  <!-- 밴드 커뮤니티: 맨 위 공지 한 줄, 아래 최근 글을 3열 카드로 -->
  <section class="community">
    <header class="head">
      <div class="title">
        COMMUNITY
      </div>
      <div class="head-actions">
        <button class="write-btn" @click="$emit('list')">전체 보기</button>
        <button class="write-btn" @click="$emit('write')">✏️ 글쓰기</button>
      </div>
    </header>

    <article v-if="notice" class="notice" role="link" tabindex="0" @click="$emit('open', notice)" @keydown.enter="$emit('open', notice)">
      <span class="notice-tag">공지</span>
      <div class="notice-body">
        <strong>{{ notice.title }}</strong>
        <span>{{ notice.content }}</span>
      </div>
      <span class="notice-meta">{{ authorName(notice) }} · {{ timeAgo(notice.createdAt) }}</span>
    </article>

    <div v-if="posts.length" class="grid">
      <article v-for="p in posts" :key="p.id" class="post" role="link" tabindex="0" @click="$emit('open', p)" @keydown.enter.self="$emit('open', p)">
        <div class="author">
          <ProfileAvatar :src="p.author?.profileImageUrl" :size="36" :alt="`${authorName(p)} 프로필 사진`" />
          <div class="who">
            <strong>{{ authorName(p) }}</strong>
            <span>{{ timeAgo(p.createdAt) }}</span>
          </div>
        </div>
        <h3>{{ p.title }}</h3>
        <div class="post-body">
          <p>{{ p.content }}</p>
          <BandPostAttachment v-if="p.attachment && /^(image|video)\//.test(p.attachment.type)" :attachment="p.attachment" compact />
        </div>
        <footer>
          <span v-if="p.attachment" class="file" :title="p.attachment.name">📎 {{ p.attachment.name }}</span>
          <span class="comments">💬 {{ p.commentCount }}</span>
        </footer>
      </article>
    </div>
    <p v-else class="empty">아직 글이 없어요. 밴드원에게 첫 글을 남겨 보세요.</p>
  </section>
</template>

<script setup>
import ProfileAvatar from '../ProfileAvatar.vue'
import BandPostAttachment from './BandPostAttachment.vue'

defineProps({
  notice: { type: Object, default: null },
  posts: { type: Array, default: () => [] },
})
defineEmits(['open', 'write', 'list'])

const authorName = (post) => post.author?.nickname || '멤버'

const timeAgo = (date) => {
  const minutes = Math.max(0, (Date.now() - new Date(date).getTime()) / 60000)
  if (minutes < 1) return '방금'
  if (minutes < 60) return `${Math.floor(minutes)}분 전`
  if (minutes < 60 * 24) return `${Math.floor(minutes / 60)}시간 전`
  return `${Math.floor(minutes / (60 * 24))}일 전`
}
</script>

<style scoped>
.community { margin-top: 24px; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.head-actions { display: flex; gap: 8px; }
.title { font-weight: 800; font-size: 0.85rem; color: #495057; display: flex; align-items: center; gap: 8px; }
.write-btn {
  border: 1px solid #2d5bff; background: #fff; color: #2d5bff; border-radius: 10px; padding: 7px 14px;
  font-family: inherit; font-size: 0.78rem; font-weight: 800; cursor: pointer; transition: 0.2s;
}
.write-btn:hover:not(:disabled) { background: #f0f4ff; }

.notice {
  cursor: pointer;
  display: flex; align-items: center; gap: 12px; background: #fff; border: 1px solid #e9ecef;
  border-radius: 16px; padding: 14px 18px; margin-bottom: 12px;
}
.notice-tag {
  flex-shrink: 0; font-size: 0.7rem; font-weight: 900; color: #fff; background: #2d5bff; padding: 3px 8px; border-radius: 6px;
}
.notice-body { flex: 1; min-width: 0; display: flex; gap: 10px; align-items: baseline; }
.notice-body strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; font-size: 0.9rem; font-weight: 800; white-space: nowrap; }
.notice-body span { font-size: 0.8rem; color: #868e96; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-meta { flex-shrink: 0; font-size: 0.72rem; color: #adb5bd; font-weight: 600; }

.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.post {
  background: #fff; border: 1px solid #e9ecef; border-radius: 20px; padding: 16px;
  display: flex; flex-direction: column; gap: 8px; cursor: pointer;
  transition: 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.post:hover { border-color: #2d5bff; transform: translateY(-3px); box-shadow: 0 10px 20px rgba(45, 91, 255, 0.06); }
.author { display: flex; align-items: center; gap: 10px; }
.post-body { display: flex; gap: 12px; min-height: 84px; align-items: flex-start; }
.post-body p { flex: 1; min-width: 0; }
.who { display: flex; flex-direction: column; min-width: 0; }
.who strong { font-size: 0.8rem; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.who span { font-size: 0.7rem; color: #adb5bd; }
.post h3 {
  margin: 4px 0 0; font-size: 0.95rem; font-weight: 800; line-height: 1.35;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  overflow-wrap: anywhere;
}
.post p {
  margin: 0; font-size: 0.8rem; color: #868e96; line-height: 1.55; overflow-wrap: anywhere;
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
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .grid { grid-template-columns: 1fr; }
  .notice { flex-wrap: wrap; }
  .notice-body { flex-direction: column; gap: 2px; flex-basis: 100%; order: 3; }
  .notice-body strong, .notice-body span { white-space: normal; }
}
</style>
