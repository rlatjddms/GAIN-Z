<template>
  <!-- 비밀번호 재설정 메일의 링크로 들어오는 화면 (#193): /reset-password?token=… -->
  <AuthCard subtitle="새 비밀번호 정하기">
    <template v-if="!done">
      <form class="auth-form" @submit.prevent="save">
        <input
          v-model="password"
          type="password"
          autocomplete="new-password"
          class="auth-input"
          placeholder="새 비밀번호 (8~16자 영문, 숫자, 특수문자)"
          aria-label="새 비밀번호"
          required
        >
        <p v-if="password && !patternOk" class="auth-msg error">{{ PASSWORD_RULE }}</p>
        <input
          v-model="confirm"
          type="password"
          autocomplete="new-password"
          class="auth-input"
          placeholder="새 비밀번호 확인"
          aria-label="새 비밀번호 확인"
          :disabled="!patternOk"
          required
        >
        <p v-if="confirm && !matched" class="auth-msg error">비밀번호가 일치하지 않습니다.</p>
        <p v-if="error" class="auth-msg error">{{ error }}</p>
        <button type="submit" class="auth-btn" :disabled="saving || !patternOk || !matched || !token">{{ saving ? '바꾸는 중…' : '비밀번호 바꾸기' }}</button>
      </form>
      <p v-if="!token" class="auth-note">재설정 링크가 올바르지 않아요. <router-link to="/forgot-password">링크 다시 받기</router-link></p>
      <p v-else-if="expired" class="auth-note"><router-link to="/forgot-password">재설정 링크 다시 받기</router-link></p>
    </template>

    <template v-else>
      <h1 class="auth-title">비밀번호를 바꿨어요</h1>
      <p class="auth-text">새 비밀번호로 로그인해 주세요. 다른 기기에 로그인돼 있었다면 다시 로그인해야 해요.</p>
      <router-link to="/" class="auth-btn as-link">로그인하러 가기</router-link>
    </template>

    <p v-if="!done" class="auth-footer"><router-link to="/">로그인으로 돌아가기</router-link></p>
  </AuthCard>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import AuthCard from '../components/ui/AuthCard.vue'
import { resetPassword } from '../api/auth'
import { PASSWORD_PATTERN, PASSWORD_RULE } from '../constants/auth'

const route = useRoute()
const token = typeof route.query.token === 'string' ? route.query.token : ''
const password = ref('')
const confirm = ref('')
const saving = ref(false)
const error = ref('')
const expired = ref(false)
const done = ref(false)
const patternOk = computed(() => PASSWORD_PATTERN.test(password.value))
const matched = computed(() => !!confirm.value && password.value === confirm.value)

const save = async () => {
  if (!patternOk.value || !matched.value || !token) return
  saving.value = true
  error.value = ''
  try {
    await resetPassword(token, password.value)
    done.value = true
  } catch (e) {
    error.value = e.response?.data?.message || '비밀번호를 바꾸지 못했어요. 잠시 후 다시 시도해 주세요.'
    expired.value = e.response?.status === 400
  } finally {
    saving.value = false
  }
}
</script>

<style scoped src="../components/ui/AuthField.css"></style>
<style scoped>
.as-link { display: block; text-align: center; text-decoration: none; box-sizing: border-box; }
.auth-note a { color: var(--c-primary-text); font-weight: 700; }
</style>
