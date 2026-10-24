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

          <!-- 이메일 인증 (#193): 인증하기 → 메일로 받은 6자리 코드 → 맞으면 비밀번호 칸이 열린다 -->
          <div class="input-wrapper">
            <div class="input-with-btn">
              <input
                v-model="formData.email"
                type="email"
                placeholder="이메일 주소"
                class="gz-input"
                :disabled="emailVerified"
                @input="resetEmailVerify"
                required
              />
              <button v-if="emailVerified" type="button" class="check-btn" @click="resetEmailVerify">변경</button>
              <button v-else type="button" class="check-btn" @click="sendCode" :disabled="sendingCode || cooldown > 0">
                {{ sendingCode ? '...' : cooldown > 0 ? `${cooldown}초` : codeSent ? '다시 보내기' : '인증하기' }}
              </button>
            </div>
            <p v-if="emailMessage" :class="['status-msg', emailOk ? 'success' : 'error']">
              {{ emailMessage }}
            </p>
          </div>

          <div v-if="codeSent && !emailVerified" class="input-wrapper">
            <div class="input-with-btn">
              <input
                ref="codeInput"
                v-model="code"
                type="text"
                inputmode="numeric"
                autocomplete="one-time-code"
                maxlength="6"
                placeholder="인증 코드 6자리"
                class="gz-input code-input"
                @input="onCodeInput"
                @keydown.enter.prevent="verifyCode"
              />
              <button type="button" class="check-btn" @click="verifyCode" :disabled="verifying || code.length !== 6">
                {{ verifying ? '...' : '확인' }}
              </button>
            </div>
            <p v-if="codeError" class="status-msg error">{{ codeError }}</p>
            <p v-else class="status-msg hint">메일로 받은 코드를 넣어 주세요. 10분 동안 쓸 수 있고, 안 보이면 스팸함도 확인해 주세요.</p>
          </div>

          <div class="input-wrapper">
            <input
              v-model="formData.password"
              type="password"
              :placeholder="emailVerified ? '비밀번호 (8~16자 영문, 숫자, 특수문자)' : '이메일 인증 후 입력할 수 있어요'"
              class="gz-input"
              :disabled="!emailVerified"
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
              :disabled="!emailVerified || !isPwValidPattern"
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
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDialogStore } from '../stores/dialog'
import AppLogo from '../components/AppLogo.vue'
import { useCooldown } from '../components/ui/useCooldown'
import { sendSignupCode, verifySignupCode, retryAfterOf, DUPLICATE_EMAIL, EMAIL_NOT_VERIFIED, MAIL_RESEND_SECONDS } from '../api/auth'

const router = useRouter()
const dialog = useDialogStore()
const authStore = useAuthStore()

// 이메일 인증 (#193)
const sendingCode = ref(false)
const codeSent = ref(false)
const emailMessage = ref('')
const emailOk = ref(false)
const code = ref('')
const codeInput = ref(null)
const codeError = ref('')
const verifying = ref(false)
const emailVerificationToken = ref('')
const emailVerified = computed(() => !!emailVerificationToken.value)
const { left: cooldown, start } = useCooldown()

const isPwValidPattern = ref(false)
const pwValidationMessage = ref('')

const isLoading = ref(false)

const formData = ref({
  name: '',
  email: '',
  password: '',
  passwordConfirm: ''
})

// 이메일을 고치면 인증을 처음부터 (보낸 코드도 버린다)
const resetEmailVerify = () => {
  emailVerificationToken.value = ''
  codeSent.value = false
  code.value = ''
  codeError.value = ''
  emailMessage.value = ''
  emailOk.value = false
}

const errorMessageOf = (error, fallback) => error.response?.data?.message || fallback

// 인증하기: 이미 가입된 이메일이면 빨간 글씨, 아니면 코드 메일
const sendCode = async () => {
  const email = formData.value.email.trim()
  emailOk.value = false
  if (!email) {
    emailMessage.value = '이메일을 입력해주세요.'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    emailMessage.value = '올바른 이메일 형식이 아닙니다.'
    return
  }

  sendingCode.value = true
  emailMessage.value = ''
  codeError.value = ''
  try {
    await sendSignupCode(email)
    codeSent.value = true
    code.value = ''
    emailOk.value = true
    emailMessage.value = `${email}(으)로 인증 코드를 보냈어요.`
    start(MAIL_RESEND_SECONDS)
    await nextTick()
    codeInput.value?.focus()
  } catch (error) {
    // 1분 안에 보낸 코드가 아직 쓸 수 있으니 코드 칸을 열어 준다
    const retryAfter = retryAfterOf(error)
    if (retryAfter) {
      codeSent.value = true
      emailOk.value = true
      emailMessage.value = '1분 안에 보낸 코드가 있어서 새로 보내지 않았어요. 받은 메일의 코드를 넣어 주세요.'
      start(retryAfter)
      await nextTick()
      codeInput.value?.focus()
      return
    }
    emailMessage.value = error.response?.data?.code === DUPLICATE_EMAIL
      ? '이미 가입된 이메일이에요.'
      : errorMessageOf(error, '인증 코드를 보내지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    sendingCode.value = false
  }
}

// 숫자만 남긴다
const onCodeInput = () => {
  code.value = code.value.replace(/\D/g, '').slice(0, 6)
  codeError.value = ''
}

const verifyCode = async () => {
  if (code.value.length !== 6 || verifying.value) return
  verifying.value = true
  codeError.value = ''
  try {
    emailVerificationToken.value = await verifySignupCode(formData.value.email.trim(), code.value)
    emailOk.value = true
    emailMessage.value = '이메일 인증을 마쳤어요.'
  } catch (error) {
    codeError.value = errorMessageOf(error, '확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    verifying.value = false
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
    emailVerified.value &&
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
      formData.value.email.trim(),
      formData.value.password,
      emailVerificationToken.value
    )
    localStorage.setItem(`firstLogin_${formData.value.email.trim()}`, 'true')
    await dialog.alert({ title: 'GAIN-Z 가입을 축하해요!', text: '로그인하고 시작해 보세요.' })
    router.push('/')
  } catch (error) {
    console.error(error)
    // 인증한 지 30분이 지났으면 다시 인증
    if (error.response?.data?.code === EMAIL_NOT_VERIFIED) {
      resetEmailVerify()
      emailMessage.value = error.response.data.message
      return
    }
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
  width: 100%; padding: 14px 18px; border: 1.5px solid #eceff2; border-radius: 12px;
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
.status-msg.hint { color: var(--c-muted); }
.code-input { letter-spacing: 4px; font-variant-numeric: tabular-nums; }
.code-input::placeholder { letter-spacing: normal; }

.signup-btn-main {
  width: 100%; padding: 14px; background: var(--c-primary); color: #fff; border: none;
  border-radius: 12px; font-size: 1rem; font-weight: 700; cursor: pointer;
  transition: 0.25s ease; margin-top: 35px;
}
.signup-btn-main:disabled { background: #ced4da; cursor: not-allowed; box-shadow: none; }
.signup-btn-main:hover:not(:disabled) { background: var(--c-primary-strong); box-shadow: 0 5px 15px color-mix(in srgb, var(--c-primary) 20%, transparent); }

.signup-footer { margin-top: 45px; font-size: 0.9rem; color: #a0a8b1; }
.signup-footer a { color: var(--c-primary-text); font-weight: 700; text-decoration: none; margin-left: 8px; }
/* 휴대폰·앱 (#190): 카드 없이 화면 전체를 쓴다. 노치·홈 표시줄 자리는 비워 둔다 */
@media (max-width: 640px) {
  .gain-z-signup-root {
    height: auto;
    min-height: 100dvh;
    padding:
      calc(24px + env(safe-area-inset-top))
      calc(32px + env(safe-area-inset-right))
      calc(24px + env(safe-area-inset-bottom))
      calc(32px + env(safe-area-inset-left));
    background: #ffffff;
  }
  .login-background-deco { display: none; }
  .signup-card-container { max-width: 360px; padding: 0; border: none; border-radius: 0; box-shadow: none; }
}
@media (max-width: 640px) {
  .gz-input { padding: 12px 14px; }
  .signup-btn-main { padding: 12px; margin-top: 24px; }
}
</style>