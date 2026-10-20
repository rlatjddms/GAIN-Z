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

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  const accessToken = route.query.accessToken
  const refreshToken = route.query.refreshToken

  if (accessToken && refreshToken) {
    // Read the temporarily saved keepLoggedIn preference
    const keepPref = sessionStorage.getItem('keepLoggedInPreference')
    const keepLoggedIn = keepPref !== 'false' // default to true if null/not set
    sessionStorage.removeItem('keepLoggedInPreference') // clean up

    // Save tokens and update auth state with the correct storage persistence preference
    authStore.setTokens(accessToken, refreshToken, keepLoggedIn)
    
    // Redirect to home
    router.push('/home')
  } else {
    // If tokens are missing, redirect back to login
    router.push('/')
  }
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
  color: #495057;
  font-weight: 600;
  letter-spacing: -0.3px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
