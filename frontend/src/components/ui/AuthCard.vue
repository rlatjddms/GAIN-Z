<template>
  <!-- 로그인 밖 화면(이메일 인증·비밀번호 찾기)의 틀 (#193). 로그인·회원가입과 같은 모양:
       넓은 화면은 가운데 카드, 휴대폰·앱은 카드 없이 화면 전체 -->
  <div class="auth-root">
    <main class="auth-card">
      <div class="brand">
        <router-link to="/" aria-label="로그인으로"><AppLogo :pixel="5" /></router-link>
        <p v-if="subtitle" class="sub">{{ subtitle }}</p>
      </div>
      <slot />
    </main>
  </div>
</template>

<script setup>
import AppLogo from '../AppLogo.vue'
import { useWhitePage } from './useWhitePage'

useWhitePage()

defineProps({
  subtitle: { type: String, default: '' },
})
</script>

<style scoped>
* { box-sizing: border-box; }
.auth-root {
  width: 100%; min-height: 100vh; min-height: 100dvh;
  display: flex; justify-content: center; align-items: center;
  background: #f8f9fb; font-family: 'Pretendard', sans-serif;
}
.auth-card {
  width: 100%; max-width: 480px; padding: 60px 50px;
  background: #fff; border-radius: 24px; border: 1px solid #edf0f3; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
  text-align: center;
}
.brand { display: flex; flex-direction: column; align-items: center; gap: 10px; margin-bottom: 36px; }
.sub { margin: 0; font-size: 0.95rem; color: #a0a8b1; letter-spacing: -0.3px; }

/* 휴대폰·앱: 카드 없이 화면 전체 */
@media (max-width: 640px) {
  .auth-root {
    padding:
      calc(24px + env(safe-area-inset-top))
      calc(32px + env(safe-area-inset-right))
      calc(24px + env(safe-area-inset-bottom))
      calc(32px + env(safe-area-inset-left));
    background: #fff;
  }
  .auth-card { max-width: 360px; padding: 0; border: none; border-radius: 0; box-shadow: none; }
  /* 로그인 화면과 같은 너비·로고·제목 크기 (#226) */
  .auth-card { max-width: 280px; }
  .brand { margin-bottom: 28px; }
  .brand :deep(svg) { width: auto; height: 28px; }
  .sub { font-size: 0.85rem; }
}
</style>
