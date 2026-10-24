<template>
  <!-- 밴드 커뮤니티: 맨 위 공지 한 줄, 아래 최근 글을 3열 카드로 -->
  <section class="community">
    <header class="head">
      <div class="title">
        COMMUNITY
        <span class="post-total">{{ total }}</span>
      </div>
      <div class="head-actions">
        <button class="search-btn" :class="{ on: searching }" :aria-pressed="searchOpen" aria-label="게시글 검색" @click="$emit('search-toggle')"><Search :size="15" :stroke-width="2.4" /></button>
        <button class="mine-btn" :aria-pressed="mine" @click="$emit('mine')">내가 쓴 글</button>
        <button class="write-btn" @click="$emit('write')"><BoardIcon name="edit" :size="14" />글쓰기</button>
      </div>
    </header>

    <!-- 검색 막대 (#188) -->
    <slot name="search" />

    <article v-for="notice in notices" :key="notice.id" class="notice" role="link" tabindex="0" @click="$emit('open', notice)" @keydown.enter.self="$emit('open', notice)">
      <span class="notice-tag">공지</span>
      <div class="notice-body">
        <strong>{{ notice.title }}</strong>
        <span>{{ notice.content }}</span>
      </div>
      <span class="notice-meta">{{ authorName(notice) }} · {{ timeAgo(notice.createdAt) }}<small v-if="notice.noticeExpiresAt">{{ postDate(notice.noticeExpiresAt) }}까지</small></span>
      <BandPostMenu v-if="notice.mine" :title="notice.title" @edit="$emit('edit', notice)" @delete="$emit('delete', notice)" />
    </article>

    <div v-if="loading" class="grid" role="status" aria-label="게시글 불러오는 중"><div v-for="i in 3" :key="i" class="post skeleton" /></div>
    <div v-else-if="posts.length" class="grid">
      <article v-for="p in posts" :key="p.id" class="post" role="link" tabindex="0" @click="$emit('open', p)" @keydown.enter.self="$emit('open', p)">
        <div class="author">
          <ProfileAvatar :src="p.author?.profileImageUrl" :size="36" :alt="`${authorName(p)} 프로필 사진`" />
          <div class="who">
            <strong>{{ authorName(p) }}</strong>
            <span>{{ timeAgo(p.createdAt) }}</span>
          </div>
          <BandPostMenu v-if="p.mine" :title="p.title" @edit="$emit('edit', p)" @delete="$emit('delete', p)" />
        </div>
        <h3><span v-if="p.notice" class="small-notice">공지</span>{{ p.title }}</h3>
        <div class="post-body">
          <p>{{ p.content }}</p>
          <div v-if="firstVisual(p)" class="thumbnail-wrap"><BandPostAttachment :attachment="firstVisual(p)" compact /><span v-if="attachmentsOf(p).length > 1" class="attachment-count">+{{ attachmentsOf(p).length - 1 }}</span></div>
        </div>
        <footer>
          <span v-if="attachmentsOf(p).length" class="file"><BoardIcon name="attach" :size="12" />첨부 {{ attachmentsOf(p).length }}</span>
          <span class="comments"><BoardIcon name="comment" :size="15" />댓글 {{ p.commentCount }}</span>
        </footer>
      </article>
    </div>
    <p v-else-if="!notices.length" class="empty">{{ searching ? '조건에 맞는 글이 없어요.' : mine ? '아직 내가 쓴 글이 없어요.' : '아직 글이 없어요. 밴드원에게 첫 글을 남겨 보세요.' }}</p>
  </section>
</template>

<script setup>
import { Search } from 'lucide-vue-next'
import ProfileAvatar from '../ProfileAvatar.vue'
import BandPostMenu from './BandPostMenu.vue'
import BandPostAttachment from './BandPostAttachment.vue'
import BoardIcon from './BoardIcon.vue'
import { attachmentsOf, postTimeAgo, postDate } from '../../constants/bandBoard'

defineProps({
  notices: { type: Array, default: () => [] },
  posts: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  loading: Boolean,
  mine: Boolean,
  // 검색 조건이 걸려 있는지, 검색 줄이 펼쳐져 있는지 (#188)
  searching: Boolean,
  searchOpen: Boolean,
})
defineEmits(['open', 'write', 'mine', 'edit', 'delete', 'search-toggle'])
const firstVisual = post => attachmentsOf(post).find(file => /^(image|video)\//.test(file.type))

const authorName = (post) => post.author?.nickname || '멤버'

const timeAgo = postTimeAgo
</script>

<style scoped>
.community { margin-top: 24px; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.head-actions { display: flex; gap: 8px; }
.search-btn { display: flex; align-items: center; justify-content: center; width: 32px; border: 1px solid #dce2ed; background: white; color: #53627a; border-radius: 10px; cursor: pointer; }
.search-btn[aria-pressed=true], .search-btn.on { background: var(--c-primary-soft); border-color: color-mix(in srgb, var(--c-primary) 55%, #fff); color: var(--c-primary-text); }
.mine-btn { border: 1px solid #dce2ed; background: white; color: #53627a; border-radius: 10px; padding: 7px 12px; font: inherit; font-size: 12px; font-weight: 700; cursor: pointer; }
.mine-btn[aria-pressed=true] { background: var(--c-primary-soft); border-color: color-mix(in srgb, var(--c-primary) 55%, #fff); color: var(--c-primary-text); }
.title { font-weight: 800; font-size: 0.85rem; color: var(--c-text-2); display: flex; align-items: center; gap: 8px; }
.write-btn {
  display: flex; align-items: center; gap: 7px;
  border: 1px solid var(--c-primary); background: #fff; color: var(--c-primary-text); border-radius: 10px; padding: 7px 14px;
  font-family: inherit; font-size: 0.78rem; font-weight: 800; cursor: pointer; transition: 0.2s;
}
.write-btn:hover:not(:disabled) { background: var(--c-primary-soft); }

.notice {
  cursor: pointer;
  display: flex; align-items: center; gap: 12px; background: #fff; border: 1px solid var(--c-line);
  border-radius: 16px; padding: 14px 18px; margin-bottom: 12px;
}
.notice-tag {
  flex-shrink: 0; font-size: 0.7rem; font-weight: 900; color: #fff; background: var(--c-primary); padding: 3px 8px; border-radius: 6px;
}
.notice-body { flex: 1; min-width: 0; display: flex; gap: 10px; align-items: baseline; }
.notice-body strong { min-width: 0; overflow: hidden; text-overflow: ellipsis; font-size: 0.9rem; font-weight: 800; white-space: nowrap; }
.notice-body span { font-size: 0.8rem; color: var(--c-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-meta small { display: block; margin-top: 4px; color: #858e9b; font-size: 10px; font-weight: 400; }
.notice-meta { flex-shrink: 0; font-size: 0.72rem; color: var(--c-faint); font-weight: 600; }

.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.post {
  background: #fff; border: 1px solid var(--c-line); border-radius: 20px; padding: 16px;
  display: flex; flex-direction: column; gap: 8px; cursor: pointer;
  transition: 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.post:hover { border-color: var(--c-primary); transform: translateY(-3px); box-shadow: 0 10px 20px color-mix(in srgb, var(--c-primary) 6%, transparent); }
.author { display: flex; align-items: center; gap: 10px; }
.post-body { display: flex; gap: 12px; min-height: 84px; align-items: flex-start; }
.post-body p { flex: 1; min-width: 0; }
.who { display: flex; flex-direction: column; min-width: 0; }
.who strong { font-size: 0.8rem; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.who span { font-size: 0.7rem; color: var(--c-faint); }
.post h3 {
  margin: 4px 0 0; font-size: 0.95rem; font-weight: 800; line-height: 1.35;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  overflow-wrap: anywhere;
}
.post p {
  margin: 0; font-size: 0.8rem; color: var(--c-muted); line-height: 1.55; overflow-wrap: anywhere;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.post footer {
  margin-top: auto; padding-top: 8px; display: flex; justify-content: space-between; align-items: center; gap: 8px;
  font-size: 0.72rem; color: var(--c-faint); font-weight: 700;
}
.file { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--c-text-2); background: var(--c-bg); padding: 3px 8px; border-radius: 6px; }
.comments { margin-left: auto; flex-shrink: 0; display: flex; align-items: center; gap: 5px; color: #52627a; }
.file { display: flex; align-items: center; gap: 4px; }
.post-total { color: var(--c-faint); font-size: 11px; font-weight: 600; }
.thumbnail-wrap { position: relative; flex: none; }
.attachment-count { position: absolute; right: 5px; bottom: 5px; color: white; background: #17243bbb; padding: 3px 5px; border-radius: 5px; font-size: 10px; font-weight: 700; }
.small-notice { color: var(--c-primary-text); font-size: 10px; margin-right: 6px; }
.skeleton { min-height: 180px; background: linear-gradient(110deg, #fff, #f5f7fa, #fff); }
.empty { text-align: center; color: var(--c-faint); font-size: 0.85rem; padding: 32px 0; }

@media (max-width: 860px) {
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
/* 휴대폰: 공지는 넓은 화면처럼 한 줄(제목만, 내용 줄임), 글은 두 줄로 나란히 작은 카드 */
@media (max-width: 640px) {
  .notice { gap: 8px; padding: 10px 12px; border-radius: 14px; margin-bottom: 10px; }
  .notice-tag { font-size: 0.65rem; padding: 2px 6px; }
  .notice-body span { display: none; }
  .notice-body strong { font-size: 0.82rem; }
  .notice-meta { font-size: 0.66rem; }
  .notice-meta small { display: none; }

  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  .post { padding: 12px; border-radius: 16px; gap: 6px; }
  .post:hover { transform: none; }
  .author { gap: 6px; }
  .author :deep(.profile-avatar) { width: 26px !important; height: 26px !important; }
  .who strong { font-size: 0.72rem; }
  .who span { font-size: 0.62rem; }
  .post h3 { margin-top: 2px; font-size: 0.82rem; }
  .post-body { min-height: 0; gap: 6px; flex-direction: column; }
  .post p { font-size: 0.72rem; -webkit-line-clamp: 2; }
  .thumbnail-wrap { align-self: stretch; }
  .post footer { padding-top: 4px; font-size: 0.64rem; }
  .file { padding: 2px 6px; }
  .skeleton { min-height: 140px; }
}
</style>
