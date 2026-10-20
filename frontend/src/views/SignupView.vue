<template>
  <div class="gain-z-signup-root">
    <div class="login-background-deco"></div>

    <main class="signup-card-container">
      <div class="brand-header">
        <div class="brand-logo"><AppLogo :pixel="5" /></div>
        <p class="brand-sub">회원 정보를 입력해주세요</p>
      </div>

      <form class="signup-form" @submit.prevent="handleSignup">
        <div class="input-grid">
          <div class="input-wrapper">
            <input
              v-model="formData.name"
              type="text"
              placeholder="이름"
              class="gz-input"
              required
            />
          </div>

          <div class="input-wrapper">
            <div class="input-with-btn">
              <input
                v-model="formData.email"
                type="email"
                placeholder="이메일 주소"
                class="gz-input"
                @input="resetEmailCheck"
                required
              />
              <button type="button" class="check-btn" @click="checkEmail" :disabled="isCheckingEmail">
                {{ isCheckingEmail ? '...' : '중복확인' }}
              </button>
            </div>
            <p v-if="emailMessage" :class="['status-msg', isEmailChecked ? 'success' : 'error']">
              {{ emailMessage }}
            </p>
          </div>

          <div class="input-wrapper">
            <input
              v-model="formData.password"
              type="password"
              placeholder="비밀번호 (8~16자 영문, 숫자, 특수문자)"
              class="gz-input"
              @input="validatePassword"
              required
            />
            <p v-if="pwValidationMessage" :class="['status-msg', isPwValidPattern ? 'success' : 'error']">
              {{ pwValidationMessage }}
            </p>
          </div>

          <div class="input-wrapper">
            <input
              v-model="formData.passwordConfirm"
              type="password"
              placeholder="비밀번호 확인"
              class="gz-input"
              :disabled="!isPwValidPattern"
              required
            />
            <p v-if="formData.passwordConfirm && pwConfirmMessage" :class="['status-msg', isPwMatched ? 'success' : 'error']">
              {{ pwConfirmMessage }}
            </p>
          </div>
        </div>

        <button type="submit" class="signup-btn-main" :disabled="!isFormValid || isLoading">
          {{ isLoading ? '가입 중...' : '가입하기' }}
        </button>
      </form>

      <div class="signup-footer">
        이미 회원이신가요? <router-link to="/">로그인으로 돌아가기</router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDialogStore } from '../stores/dialog'
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

.gain-z-signup-root {
  width: 100%; min-height: 100vh;
  display: flex; justify-content: center; align-items: center;
  background: #f8f9fb; font-family: 'Pretendard', sans-serif; position: relative;
}

.login-background-deco {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: radial-gradient(circle at 30% 80%, color-mix(in srgb, var(--c-primary) 3%, transparent) 0%, transparent 50%);
}

.signup-card-container {
  width: 100%; max-width: 480px;
  padding: 60px 50px; background: #ffffff; border-radius: 24px;
  border: 1px solid #edf0f3; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
  z-index: 1; text-align: center;
}

.brand-header { margin-bottom: 45px; }
.brand-logo { display: flex; justify-content: center; margin-bottom: 10px; }
.brand-sub { font-size: 0.95rem; color: #a0a8b1; letter-spacing: -0.3px; }

.input-grid { display: flex; flex-direction: column; gap: 14px; }
.input-wrapper { position: relative; text-align: left; }

.input-with-btn { display: flex; gap: 8px; }

.gz-input {
  width: 100%; padding: 18px 20px; border: 1.5px solid #eceff2; border-radius: 12px;
  font-size: 0.95rem; background: #fff; transition: all 0.2s; color: var(--c-text); outline: none;
}
.gz-input:focus { border-color: var(--c-primary); box-shadow: 0 0 0 4px color-mix(in srgb, var(--c-primary) 5%, transparent); }
.gz-input:disabled { background: #f8f9fb; color: var(--c-faint); cursor: not-allowed; }

.check-btn {
  padding: 0 16px; background: var(--c-line-soft); border: 1.5px solid #eceff2; border-radius: 12px;
  font-size: 0.85rem; font-weight: 600; cursor: pointer; white-space: nowrap; color: var(--c-text-2);
  transition: background 0.2s;
}
.check-btn:hover:not(:disabled) { background: var(--c-line); }
.check-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.status-msg { font-size: 0.78rem; margin: 6px 0 0 4px; line-height: 1.3; }
.status-msg.error { color: #fa5252; font-weight: 500; }
.status-msg.success { color: #37b24d; font-weight: 600; }

.signup-btn-main {
  width: 100%; padding: 18px; background: var(--c-primary); color: #fff; border: none;
  border-radius: 12px; font-size: 1rem; font-weight: 700; cursor: pointer;
  transition: 0.25s ease; margin-top: 35px;
}
.signup-btn-main:disabled { background: #ced4da; cursor: not-allowed; box-shadow: none; }
.signup-btn-main:hover:not(:disabled) { background: var(--c-primary-strong); box-shadow: 0 5px 15px color-mix(in srgb, var(--c-primary) 20%, transparent); }

.signup-footer { margin-top: 45px; font-size: 0.9rem; color: #a0a8b1; }
.signup-footer a { color: var(--c-primary-text); font-weight: 700; text-decoration: none; margin-left: 8px; }
</style>