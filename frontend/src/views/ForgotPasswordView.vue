<template>
  <!-- 비밀번호 찾기 (#193): 이메일을 넣으면 재설정 링크를 메일로 보낸다 -->
  <AuthCard subtitle="비밀번호 찾기">
    <template v-if="!sent">
      <p class="auth-text">가입한 이메일을 넣으면 비밀번호를 다시 정하는 링크를 보내 드려요.</p>
      <form class="auth-form" @submit.prevent="send">
        <input v-model="email" type="email" inputmode="email" autocomplete="email" class="auth-input" placeholder="가입한 이메일" aria-label="가입한 이메일" required>
        <p v-if="error" class="auth-msg error">{{ error }}</p>
        <button type="submit" class="auth-btn" :disabled="sending || !email.trim()">{{ sending ? '보내는 중…' : '재설정 링크 받기' }}</button>
      </form>
    </template>

    <template v-else>
      <h1 class="auth-title">메일을 확인해 주세요</h1>
      <p class="auth-text">
        <strong>{{ sentTo }}</strong>(으)로 가입된 계정이 있다면 비밀번호 재설정 링크를 보냈어요.
        링크는 30분 동안만 쓸 수 있어요.
      </p>
      <p class="auth-note">메일이 안 보이면 스팸함도 확인해 주세요. 구글·카카오·네이버로만 가입했다면 그 계정으로 로그인해 주세요.</p>
      <button type="button" class="auth-btn ghost" :disabled="sending || cooldown > 0" @click="send">
        {{ sending ? '보내는 중…' : cooldown > 0 ? `${cooldown}초 뒤 다시 보낼 수 있어요` : '메일 다시 보내기' }}
      </button>
    </template>

    <p class="auth-footer"><router-link to="/">로그인으로 돌아가기</router-link></p>
  </AuthCard>
</template>

<script setup>
import { ref } from 'vue'
import AuthCard from '../components/ui/AuthCard.vue'
import { useCooldown } from '../components/ui/useCooldown'
import { requestPasswordReset, MAIL_RESEND_SECONDS } from '../api/auth'

const email = ref('')
const sentTo = ref('')
const sent = ref(false)
const sending = ref(false)
const error = ref('')
const { left: cooldown, start } = useCooldown()

const send = async () => {
  const target = sent.value ? sentTo.value : email.value.trim()
  if (!target) return
  sending.value = true
  error.value = ''
  try {
    await requestPasswordReset(target)
    sentTo.value = target
    sent.value = true
    start(MAIL_RESEND_SECONDS)
  } catch (e) {
    error.value = e.response?.data?.message || '메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    sending.value = false
  }
}
</script>

<style scoped src="../components/ui/AuthField.css"></style>
