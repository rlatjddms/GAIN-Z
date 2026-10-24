<template>
  <!-- 회원가입 (#190): 로그인과 같은 톤. 휴대폰·앱에서는 카드 없이 화면 전체 -->
  <div class="signup-root">
    <main class="signup-panel">
      <header class="top">
        <router-link to="/" class="back" aria-label="로그인으로 돌아가기"><ChevronLeft :size="24" :stroke-width="2.2" /></router-link>
        <AppLogo class="top-logo" :pixel="4" />
        <h1 class="title">회원가입</h1>
        <p class="sub">이메일로 GAIN-Z를 시작해요</p>
      </header>

      <form class="signup-form" @submit.prevent="handleSignup">
        <div class="field">
          <input
            v-model="formData.name"
            type="text"
            autocomplete="name"
            enterkeyhint="next"
            placeholder="이름"
            aria-label="이름"
            class="gz-input"
            required
          />
        </div>

        <div class="field">
          <div class="input-with-btn">
            <input
              v-model="formData.email"
              type="email"
              inputmode="email"
              autocomplete="email"
              enterkeyhint="next"
              placeholder="이메일 주소"
              aria-label="이메일 주소"
              class="gz-input"
              @input="resetEmailCheck"
              required
            />
            <button type="button" class="check-btn" @click="checkEmail" :disabled="isCheckingEmail">
              {{ isCheckingEmail ? '확인 중' : '중복 확인' }}
            </button>
          </div>
          <p v-if="emailMessage" :class="['status-msg', isEmailChecked ? 'success' : 'error']">
            {{ emailMessage }}
          </p>
        </div>

        <div class="field">
          <input
            v-model="formData.password"
            type="password"
            autocomplete="new-password"
            enterkeyhint="next"
            placeholder="비밀번호 (8~16자 영문, 숫자, 특수문자)"
            aria-label="비밀번호"
            class="gz-input"
            @input="validatePassword"
            required
          />
          <p v-if="pwValidationMessage" :class="['status-msg', isPwValidPattern ? 'success' : 'error']">
            {{ pwValidationMessage }}
          </p>
        </div>

        <div class="field">
          <input
            v-model="formData.passwordConfirm"
            type="password"
            autocomplete="new-password"
            enterkeyhint="done"
            placeholder="비밀번호 확인"
            aria-label="비밀번호 확인"
            class="gz-input"
            :disabled="!isPwValidPattern"
            required
          />
          <p v-if="formData.passwordConfirm && pwConfirmMessage" :class="['status-msg', isPwMatched ? 'success' : 'error']">
            {{ pwConfirmMessage }}
          </p>
        </div>

        <button type="submit" class="signup-btn" :disabled="!isFormValid || isLoading">
          {{ isLoading ? '가입 중…' : '가입하기' }}
        </button>
      </form>

      <p class="footer">
        이미 회원이신가요? <router-link to="/">로그인</router-link>
      </p>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDialogStore } from '../stores/dialog'
import { ChevronLeft } from 'lucide-vue-next'
import AppLogo from '../components/AppLogo.vue'

const router = useRouter()
const dialog = useDialogStore()
const authStore = useAuthStore()

const isCheckingEmail = ref(false)
const isEmailChecked = ref(false)
const emailMessage = ref('')

const isPwValidPattern = ref(false)
const pwValidationMessage = ref('')

const isLoading = ref(false)

const formData = ref({
  name: '',
  email: '',
  password: '',
  passwordConfirm: ''
})

// 이메일 입력 시 중복확인 상태 초기화
const resetEmailCheck = () => {
  isEmailChecked.value = false
  emailMessage.value = ''
}

// 이메일 중복확인
const checkEmail = async () => {
  if (!formData.value.email) {
    emailMessage.value = '이메일을 입력해주세요.'
    return
  }

  // Basic email validation regex
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(formData.value.email)) {
    emailMessage.value = '올바른 이메일 형식이 아닙니다.'
    return
  }

  isCheckingEmail.value = true
  emailMessage.value = ''

  try {
    const isDuplicate = await authStore.checkEmail(formData.value.email)
    if (isDuplicate) {
      emailMessage.value = '이미 가입된 이메일 주소입니다.'
      isEmailChecked.value = false
    } else {
      emailMessage.value = '사용 가능한 이메일입니다.'
      isEmailChecked.value = true
    }
  } catch (error) {
    console.error(error)
    emailMessage.value = '이메일 중복 확인에 실패했습니다. 다시 시도해주세요.'
    isEmailChecked.value = false
  } finally {
    isCheckingEmail.value = false
  }
}

// 비밀번호 정규식 유효성 검사 (8~16자 영문, 숫자, 특수문자 조합)
const validatePassword = () => {
  const pw = formData.value.password
  // Matches backend pattern: (?=.*[0-9])(?=.*[a-zA-Z])(?=.*\W)(?=\S+$).{8,16}
  const regExp = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[\W_])(?=\S+$).{8,16}$/

  if (!pw) {
    pwValidationMessage.value = ''
    isPwValidPattern.value = false
    return
  }

  if (regExp.test(pw)) {
    pwValidationMessage.value = '사용 가능한 비밀번호입니다.'
    isPwValidPattern.value = true
  } else {
    pwValidationMessage.value = '8~16자의 영문, 숫자, 특수문자를 모두 포함해야 하며 공백은 허용되지 않습니다.'
    isPwValidPattern.value = false
  }
}

// 비밀번호 일치 여부 검사
const isPwMatched = computed(() => {
  return formData.value.password === formData.value.passwordConfirm
})

// 비밀번호 확인 칸 메시지
const pwConfirmMessage = computed(() => {
  if (!formData.value.passwordConfirm) return ''
  return isPwMatched.value ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'
})

// 전체 폼 유효성 체크
const isFormValid = computed(() => {
  return (
    formData.value.name &&
    isEmailChecked.value &&
    isPwValidPattern.value &&
    isPwMatched.value
  )
})

// 회원가입 전송
const handleSignup = async () => {
  if (!isFormValid.value) return

  isLoading.value = true

  try {
    await authStore.signup(
      formData.value.name,
      formData.value.email,
      formData.value.password
    )
    localStorage.setItem(`firstLogin_${formData.value.email.trim()}`, 'true')
    await dialog.alert({ title: 'GAIN-Z 가입을 축하해요!', text: '로그인하고 시작해 보세요.' })
    router.push('/')
  } catch (error) {
    console.error(error)
    if (error.response && error.response.data && error.response.data.message) {
      await dialog.alert({ title: '가입하지 못했어요', text: error.response.data.message })
    } else {
      await dialog.alert({ title: '가입하지 못했어요', text: '잠시 후 다시 시도해 주세요.' })
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
* { box-sizing: border-box; }

.signup-root {
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
.signup-panel {
  position: relative;
  width: 100%;
  max-width: 440px;
  padding: 40px 40px 32px;
  background: var(--c-surface);
  border: 1px solid var(--c-line);
  border-radius: 24px;
}

.top { display: flex; flex-direction: column; align-items: center; gap: 6px; margin-bottom: 28px; text-align: center; }
.back {
  position: absolute;
  top: 16px;
  left: 12px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: var(--c-text-2);
}
.back:hover { background: var(--c-line-soft); }
.top-logo { margin-bottom: 8px; }
.title { margin: 0; font-size: 1.35rem; font-weight: 800; color: var(--c-text); }
.sub { margin: 0; font-size: 0.92rem; color: var(--c-muted); }

.signup-form { display: flex; flex-direction: column; gap: 12px; }
.field { text-align: left; }
.input-with-btn { display: flex; gap: 8px; }

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
.gz-input:focus { border-color: var(--c-primary); box-shadow: 0 0 0 4px color-mix(in srgb, var(--c-primary) 10%, transparent); }
.gz-input:disabled { background: var(--c-bg); color: var(--c-faint); cursor: not-allowed; }

.check-btn {
  flex: none;
  height: 50px;
  padding: 0 16px;
  background: var(--c-line-soft);
  border: 1.5px solid var(--c-line);
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--c-text-2);
  white-space: nowrap;
  cursor: pointer;
}
.check-btn:hover:not(:disabled) { background: var(--c-line); }
.check-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.status-msg { margin: 6px 4px 0; font-size: 0.8rem; line-height: 1.35; }
.status-msg.error { color: var(--c-danger); font-weight: 500; }
.status-msg.success { color: var(--c-success); font-weight: 600; }

.signup-btn {
  width: 100%;
  height: 52px;
  margin-top: 12px;
  background: var(--c-primary);
  color: var(--c-on-primary);
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}
.signup-btn:hover:not(:disabled) { background: var(--c-primary-strong); }
.signup-btn:disabled { background: var(--c-faint); cursor: not-allowed; }

.footer { margin: 24px 0 0; text-align: center; font-size: 0.9rem; color: var(--c-muted); }
.footer a { margin-left: 6px; color: var(--c-primary-text); font-weight: 700; text-decoration: none; }

/* 휴대폰·앱: 카드 없이 화면 전체, 가입 버튼은 아래쪽 */
@media (max-width: 640px) {
  .signup-root {
    align-items: stretch;
    padding:
      calc(12px + env(safe-area-inset-top))
      calc(20px + env(safe-area-inset-right))
      calc(20px + env(safe-area-inset-bottom))
      calc(20px + env(safe-area-inset-left));
    background: var(--c-surface);
  }
  .signup-panel {
    max-width: none;
    display: flex;
    flex-direction: column;
    padding: 56px 0 0;
    border: none;
    border-radius: 0;
  }
  .back { top: 0; left: -8px; }
  .top { align-items: flex-start; text-align: left; }
  .top-logo { display: none; }
  .signup-form { flex: 1; }
  .signup-btn { margin-top: auto; }
}
</style>
