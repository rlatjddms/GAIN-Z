<template>
  <!-- 로그인 (#190): 휴대폰·앱(홈 화면 실행)에서는 카드 없이 화면 전체를 쓰고, 넓은 화면에서는 가운데 카드 -->
  <div class="login-root">
    <main class="login-panel">
      <header class="hero">
        <PixelCharacter class="hero-character" :scale="4" />
        <AppLogo class="hero-logo" :pixel="5" />
        <p class="hero-sub">당신의 합주가 시작되는 곳</p>
      </header>

      <section class="actions">
        <div class="social-list">
          <button type="button" class="social-btn kakao" @click="handleSocialLogin('kakao')">
            <span class="kakao-mark" aria-hidden="true"></span>
            카카오로 시작하기
          </button>
          <button type="button" class="social-btn naver" @click="handleSocialLogin('naver')">
            <span class="naver-mark" aria-hidden="true">N</span>
            네이버로 시작하기
          </button>
          <button type="button" class="social-btn google" @click="handleSocialLogin('google')">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>
            Google로 시작하기
          </button>
        </div>

        <div class="divider"><span>또는 이메일로 로그인</span></div>

        <form class="login-form" @submit.prevent="handleLogin">
          <input
            v-model="email"
            type="email"
            inputmode="email"
            autocomplete="email"
            enterkeyhint="next"
            placeholder="이메일"
            aria-label="이메일"
            class="gz-input"
            required
          />
          <div class="pw-group">
            <input
              v-model="password"
              :type="isPwVisible ? 'text' : 'password'"
              autocomplete="current-password"
              enterkeyhint="go"
              placeholder="비밀번호"
              aria-label="비밀번호"
              class="gz-input"
              required
            />
            <button
              type="button"
              class="pw-eye-btn"
              :aria-label="isPwVisible ? '비밀번호 숨기기' : '비밀번호 보기'"
              @click="isPwVisible = !isPwVisible"
            >
              <EyeOff v-if="isPwVisible" :size="18" :stroke-width="2" />
              <Eye v-else :size="18" :stroke-width="2" />
            </button>
          </div>

          <p v-if="errorMessage" class="error-msg" role="alert">{{ errorMessage }}</p>

          <label class="keep">
            <input v-model="keepLoggedIn" type="checkbox">
            로그인 상태 유지
          </label>

          <button type="submit" class="login-btn" :disabled="isLoading">
            {{ isLoading ? '로그인 중…' : '로그인' }}
          </button>
        </form>

        <p class="footer">
          아직 계정이 없으신가요? <router-link to="/signup">회원가입</router-link>
        </p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import AppLogo from '../components/AppLogo.vue'
import PixelCharacter from '../components/PixelCharacter.vue'
import { API_BASE_URL } from '../api/axios'

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
  window.location.href = `${API_BASE_URL}/oauth2/authorization/${provider}`
}
</script>

<style scoped>
* { box-sizing: border-box; }

.login-root {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px 16px;
  background: var(--c-bg);
  font-family: 'Pretendard', sans-serif;
}

/* 넓은 화면: 가운데 카드 */
.login-panel {
  width: 100%;
  max-width: 440px;
  padding: 44px 40px 36px;
  background: var(--c-surface);
  border: 1px solid var(--c-line);
  border-radius: 24px;
  text-align: center;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
}
.hero-character { margin-bottom: 4px; }
.hero-sub { margin: 0; font-size: 0.95rem; color: var(--c-muted); letter-spacing: -0.3px; }

/* 소셜 로그인: 가로로 꽉 찬 큰 버튼 */
.social-list { display: flex; flex-direction: column; gap: 10px; }
.social-btn {
  width: 100%;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  border-radius: 12px;
  font-size: 0.97rem;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.15s, transform 0.1s;
}
.social-btn:hover { filter: brightness(0.97); }
.social-btn:active { transform: scale(0.99); }
.kakao { background: #fee500; color: rgba(0, 0, 0, 0.85); }
.kakao-mark {
  width: 18px; height: 16px; background: #191600; border-radius: 50%; position: relative;
}
.kakao-mark::after {
  content: ""; position: absolute; left: 3px; bottom: -3px;
  border-left: 4px solid transparent; border-right: 4px solid transparent; border-top: 5px solid #191600;
}
.naver { background: #03c75a; color: #fff; }
.naver-mark { font-weight: 900; font-size: 1.05rem; line-height: 1; }
.google { background: #fff; color: var(--c-text); border: 1px solid var(--c-line); }

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0 16px;
  font-size: 0.8rem;
  color: var(--c-faint);
}
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: var(--c-line); }

.login-form { display: flex; flex-direction: column; gap: 10px; text-align: left; }
.gz-input {
  width: 100%;
  height: 50px;
  padding: 0 16px;
  border: 1.5px solid var(--c-line);
  border-radius: 12px;
  /* 16px 아래면 아이폰이 누를 때 화면을 확대한다 */
  font-size: 16px;
  background: var(--c-surface);
  color: var(--c-text);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.gz-input:focus {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--c-primary) 10%, transparent);
}
.pw-group { position: relative; }
.pw-group .gz-input { padding-right: 48px; }
.pw-eye-btn {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--c-faint);
  cursor: pointer;
}

.error-msg { margin: 2px 4px 0; font-size: 0.85rem; font-weight: 500; color: var(--c-danger); }

.keep {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 2px;
  font-size: 0.88rem;
  color: var(--c-text-2);
  cursor: pointer;
}
.keep input { width: 18px; height: 18px; accent-color: var(--c-primary); }

.login-btn {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  background: var(--c-primary);
  color: var(--c-on-primary);
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}
.login-btn:hover:not(:disabled) { background: var(--c-primary-strong); }
.login-btn:disabled { background: var(--c-faint); cursor: not-allowed; }

.footer { margin: 24px 0 0; font-size: 0.9rem; color: var(--c-muted); }
.footer a { margin-left: 6px; color: var(--c-primary-text); font-weight: 700; text-decoration: none; }

/* 휴대폰·앱: 카드 없이 화면 전체. 위는 캐릭터·로고, 아래는 엄지가 닿는 버튼 */
@media (max-width: 640px) {
  .login-root {
    align-items: stretch;
    padding:
      calc(24px + env(safe-area-inset-top))
      calc(20px + env(safe-area-inset-right))
      calc(20px + env(safe-area-inset-bottom))
      calc(20px + env(safe-area-inset-left));
    background: var(--c-surface);
  }
  .login-panel {
    max-width: none;
    display: flex;
    flex-direction: column;
    padding: 0;
    border: none;
    border-radius: 0;
  }
  .hero { flex: 1; justify-content: center; min-height: 200px; margin-bottom: 24px; }
  .footer { margin-top: 20px; }
}
</style>
