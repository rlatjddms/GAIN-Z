<template>
  <div class="gain-z-login-root">
    <div class="login-background-deco"></div>

    <main class="login-card-container">
      <div class="brand-header">
        <div class="brand-logo"><AppLogo :pixel="5" /></div>
        <p class="brand-sub">당신의 합주가 시작되는 곳</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="input-grid">
          <div class="input-wrapper">
            <input
              v-model="email"
              type="email"
              placeholder="이메일"
              class="gz-input"
              required
            />
          </div>

          <div class="input-wrapper pw-group">
            <input
              v-model="password"
              :type="isPwVisible ? 'text' : 'password'"
              placeholder="비밀번호"
              class="gz-input"
              required
            />
            <button type="button" class="pw-eye-btn" @click="isPwVisible = !isPwVisible">
              <svg v-if="!isPwVisible" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
            </button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-msg">
          {{ errorMessage }}
        </div>

        <div class="form-options">
          <label class="check-container">
            <input type="checkbox" v-model="keepLoggedIn">
            <span class="checkmark"></span>
            로그인 상태 유지
          </label>
          <a href="#" class="forgot-pw">비밀번호 찾기</a>
        </div>

        <button type="submit" class="login-btn-main" :disabled="isLoading">
          {{ isLoading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <div class="social-divider">
        <span>간편 로그인</span>
      </div>

      <div class="social-row">
        <button class="social-btn kakao" title="카카오 로그인" @click="handleSocialLogin('kakao')">
          <span class="s-icon"></span>
        </button>
        <button class="social-btn naver" title="네이버 로그인" @click="handleSocialLogin('naver')">
          <span class="s-icon">N</span>
        </button>
        <button class="social-btn google" title="구글 로그인" @click="handleSocialLogin('google')">
          <svg viewBox="0 0 24 24" width="20" height="20"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>
        </button>
      </div>

      <div class="login-footer">
        아직 계정이 없으신가요? <router-link to="/signup">회원가입</router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLogo from '../components/AppLogo.vue'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const keepLoggedIn = ref(true)
const isPwVisible = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!email.value || !password.value) return

  isLoading.value = true
  errorMessage.value = ''

  try {
    await authStore.login(email.value, password.value, keepLoggedIn.value)

    const firstLoginKey = `firstLogin_${email.value.trim()}`
    const isFirstLogin = localStorage.getItem(firstLoginKey) === 'true'
    if (isFirstLogin) {
      localStorage.removeItem(firstLoginKey) // Clear the flag
      router.push('/character-create')
    } else {
      router.push('/home')
    }
  } catch (error) {
    console.error(error)
    if (error.response && error.response.data && error.response.data.message) {
      errorMessage.value = error.response.data.message
    } else {
      errorMessage.value = '이메일 또는 비밀번호가 올바르지 않습니다.'
    }
  } finally {
    isLoading.value = false
  }
}

const handleSocialLogin = (provider) => {
  // Save keepLoggedIn checkbox state in sessionStorage temporarily
  sessionStorage.setItem('keepLoggedInPreference', keepLoggedIn.value.toString())
  // Redirect to backend OAuth2 authorization URL
  window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`
}
</script>

<style scoped>
* { box-sizing: border-box; }

.gain-z-login-root {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f8f9fb;
  font-family: 'Pretendard', sans-serif;
  position: relative;
}

.login-background-deco {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 70% 20%, color-mix(in srgb, var(--c-primary) 3%, transparent) 0%, transparent 50%);
}

.login-card-container {
  width: 100%;
  max-width: 480px;
  padding: 60px 50px;
  background: #ffffff;
  border-radius: 24px;
  border: 1px solid #edf0f3;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
  z-index: 1;
  text-align: center;
}

.brand-header {
  margin-bottom: 45px;
}

.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.brand-sub {
  font-size: 0.95rem;
  color: #a0a8b1;
  letter-spacing: -0.3px;
}

.input-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-wrapper {
  position: relative;
}

.gz-input {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid #eceff2;
  border-radius: 12px;
  font-size: 0.95rem;
  background: #fff;
  transition: all 0.2s;
  color: var(--c-text);
  outline: none;
}

.gz-input:focus {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--c-primary) 5%, transparent);
}

.pw-eye-btn {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #c1c8d0;
  cursor: pointer;
  display: flex;
}

.error-msg {
  color: #fa5252;
  font-size: 0.82rem;
  margin-top: 10px;
  font-weight: 500;
  text-align: left;
  padding-left: 4px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 2px 35px;
  font-size: 0.85rem;
}

.check-container {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64707d;
  cursor: pointer;
}

.forgot-pw {
  color: #a0a8b1;
  text-decoration: none;
}

.login-btn-main {
  width: 100%;
  padding: 14px;
  background: var(--c-primary);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: 0.25s ease;
}

.login-btn-main:hover:not(:disabled) {
  background: var(--c-primary-strong);
  box-shadow: 0 5px 15px color-mix(in srgb, var(--c-primary) 20%, transparent);
}

.login-btn-main:disabled {
  background: #ced4da;
  cursor: not-allowed;
}

.social-divider {
  margin: 45px 0 25px;
  position: relative;
}

.social-divider::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 1px;
  background: var(--c-line-soft);
}

.social-divider span {
  position: relative;
  background: #fff;
  padding: 0 15px;
  font-size: 0.8rem;
  color: var(--c-faint);
}

.social-row {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.social-btn {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, filter 0.2s;
}

.social-btn:hover {
  transform: translateY(-3px);
  filter: brightness(0.98);
}

.kakao { background: #fee500; }
.kakao .s-icon {
  width: 18px; height: 17px; background: #3c1e1e; border-radius: 45%; position: relative;
}
.kakao .s-icon::after {
  content: ""; position: absolute; left: 3px; bottom: -2px; width: 0; height: 0;
  border-left: 4px solid transparent; border-right: 4px solid transparent; border-top: 5px solid #3c1e1e;
}

.naver { background: #03c75a; color: #fff; font-weight: 900; font-size: 1.1rem; }

.google { background: #fff; border: 1px solid var(--c-line) !important; }

.login-footer {
  margin-top: 45px;
  font-size: 0.9rem;
  color: #a0a8b1;
}

.login-footer a {
  color: var(--c-primary-text);
  font-weight: 700;
  text-decoration: none;
  margin-left: 8px;
}
/* 휴대폰: 카드가 화면 끝에 붙지 않게 바깥 여백, 안쪽 여백은 줄인다 (#124) */
@media (max-width: 640px) {
  .gain-z-login-root { height: auto; min-height: 100dvh; padding: 24px 16px; }
  .login-card-container { padding: 40px 22px; }
}
@media (max-width: 640px) {
  .gz-input { padding: 12px 14px; }
  .login-btn-main { padding: 12px; }
  .social-btn { width: 44px; height: 44px; }
}
</style>