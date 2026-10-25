<template>
  <div class="redirect-container">
    <div class="loader-box">
      <div class="spinner"></div>
      <p class="redirect-text">로그인 처리 중입니다...</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { exchangeOAuthCode } from '../api/auth'
import { apiErrorMessage } from '../api/practice'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()

// 소셜 로그인을 마치고 돌아온 곳. 서버는 토큰 대신 1분짜리 한 번용 코드(?code=)를 주고, 여기서 토큰으로 바꾼다 (#281).
// 토큰이 주소(방문 기록·서버 기록)에 남지 않게. 이 화면은 기록에 남기지 않고(replace) 바로 다음 화면으로 간다
onMounted(async () => {
  const { code, linked } = route.query
  // 예전 서버(?accessToken=&refreshToken=)도 잠깐 받아 준다 (프론트·백엔드 배포 시점이 달라도 로그인되게)
  let tokens = route.query.accessToken && route.query.refreshToken
    ? { accessToken: route.query.accessToken, refreshToken: route.query.refreshToken }
    : null
  if (!tokens && code) {
    try {
      tokens = await exchangeOAuthCode(code)
    } catch (e) {
      toast.error(apiErrorMessage(e, '로그인하지 못했어요. 다시 시도해 주세요.'))
    }
  }
  if (!tokens) {
    router.replace('/')
    return
  }

  // 로그인 화면에서 고른 "로그인 유지"를 쓰고 지운다 (없으면 유지)
  const keepLoggedIn = sessionStorage.getItem('keepLoggedInPreference') !== 'false'
  sessionStorage.removeItem('keepLoggedInPreference')
  authStore.setTokens(tokens.accessToken, tokens.refreshToken, keepLoggedIn)

  // 마이페이지에서 소셜을 연결하고 돌아왔으면 마이페이지로 (#189)
  if (linked) router.replace({ path: '/mypage', query: { linked } })
  else router.replace('/home')
})
</script>

<style scoped>
.redirect-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f8f9fb;
  font-family: 'Pretendard', sans-serif;
}

.loader-box {
  text-align: center;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #eceff2;
  border-top-color: var(--c-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

.redirect-text {
  font-size: 1rem;
  color: var(--c-text-2);
  font-weight: 600;
  letter-spacing: -0.3px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
