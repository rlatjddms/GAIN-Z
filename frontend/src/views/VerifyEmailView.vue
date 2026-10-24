<template>
  <!-- 인증 메일의 링크로 들어오는 화면 (#193): /verify-email?token=… -->
  <AuthCard subtitle="이메일 인증">
    <p v-if="state === 'checking'" class="auth-text">인증하는 중이에요…</p>

    <template v-else-if="state === 'done'">
      <h1 class="auth-title">이메일 인증을 마쳤어요</h1>
      <p class="auth-text">이제 로그인해서 GAIN-Z를 시작해 보세요.</p>
      <router-link to="/" class="auth-btn as-link">로그인하러 가기</router-link>
    </template>

    <template v-else>
      <h1 class="auth-title">링크를 쓸 수 없어요</h1>
      <p class="auth-text">{{ error }}<br>가입한 이메일을 넣으면 인증 메일을 다시 보내 드려요.</p>
      <form class="auth-form" @submit.prevent="resend">
        <input v-model="email" type="email" inputmode="email" autocomplete="email" class="auth-input" placeholder="가입한 이메일" aria-label="가입한 이메일" required>
        <p v-if="sentMessage" class="auth-msg success">{{ sentMessage }}</p>
        <button type="submit" class="auth-btn" :disabled="sending || cooldown > 0 || !email.trim()">
          {{ sending ? '보내는 중…' : cooldown > 0 ? `${cooldown}초 뒤 다시 보낼 수 있어요` : '인증 메일 다시 보내기' }}
        </button>
      </form>
      <p class="auth-footer"><router-link to="/">로그인으로 돌아가기</router-link></p>
    </template>
  </AuthCard>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AuthCard from '../components/ui/AuthCard.vue'
import { useCooldown } from '../components/ui/useCooldown'
import { confirmEmail, resendVerificationMail, MAIL_RESEND_SECONDS } from '../api/auth'

const route = useRoute()
const state = ref('checking')   // checking | done | failed
const error = ref('')
const email = ref('')
const sending = ref(false)
const sentMessage = ref('')
const { left: cooldown, start } = useCooldown()

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) {
    state.value = 'failed'
    error.value = '인증 링크가 올바르지 않아요.'
    return
  }
  try {
    await confirmEmail(token)
    state.value = 'done'
  } catch (e) {
    state.value = 'failed'
    error.value = e.response?.data?.message || '인증하지 못했어요.'
  }
})

const resend = async () => {
  sending.value = true
  sentMessage.value = ''
  try {
    await resendVerificationMail(email.value.trim())
    sentMessage.value = '인증 메일을 보냈어요. 메일이 안 보이면 스팸함도 확인해 주세요.'
    start(MAIL_RESEND_SECONDS)
  } catch (e) {
    sentMessage.value = ''
    error.value = e.response?.data?.message || '메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    sending.value = false
  }
}
</script>

<style scoped src="../components/ui/AuthField.css"></style>
<style scoped>
.as-link { display: block; text-align: center; text-decoration: none; box-sizing: border-box; }
</style>
