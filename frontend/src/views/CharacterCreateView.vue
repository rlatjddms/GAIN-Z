<template>
  <div class="character-create-root">
    <div class="login-background-deco"></div>

    <main class="create-card-container">
      <div class="create-grid">
        <!-- Left side: Visual Preview Box -->
        <section class="preview-panel">
          <div class="stage-spotlight">
            <div class="avatar-circle">
              <span class="preview-emoji">{{ selectedEmoji }}</span>
            </div>
            <div class="spotlight-shadow"></div>
          </div>

          <div class="preview-badge">
            PREVIEW
          </div>

          <div class="preview-info">
            <h3>{{ nickname || '닉네임' }}</h3>
          </div>
        </section>

        <!-- Right side: Creator Form -->
        <section class="form-panel">
          <div class="form-header">
            <div class="flex-between">
              <span class="step-badge">STEP 0{{ currentStep }} / 03</span>
              <span v-if="isEditMode" class="edit-badge">수정 모드</span>
            </div>
            <h2>{{ isEditMode ? '캐릭터 정보 수정' : '나만의 캐릭터 생성' }}</h2>
            <p>프로필과 시그니처 악기를 설정하세요.</p>
          </div>

          <!-- Step Indicator Bar -->
          <div class="step-indicator-bar">
            <div class="step-dot" :class="{ active: currentStep >= 1, complete: currentStep > 1 }">
              <span class="dot-inner">1</span>
              <span class="dot-label">프로필</span>
            </div>
            <div class="step-line" :class="{ fill: currentStep > 1 }"></div>
            <div class="step-dot" :class="{ active: currentStep >= 2, complete: currentStep > 2 }">
              <span class="dot-inner">2</span>
              <span class="dot-label">스타일</span>
            </div>
            <div class="step-line" :class="{ fill: currentStep > 2 }"></div>
            <div class="step-dot" :class="{ active: currentStep === 3 }">
              <span class="dot-inner">3</span>
              <span class="dot-label">악기</span>
            </div>
          </div>

          <!-- Form Steps (Vertically Centered) -->
          <div class="steps-container">
            <!-- STEP 1: Basic Profile -->
            <div v-show="currentStep === 1" class="step-content">
              <div class="input-group">
                <label for="nickname">닉네임</label>
                <input
                  id="nickname"
                  type="text"
                  v-model="nickname"
                  placeholder="2~10자의 한글, 영문, 숫자"
                  class="gz-input"
                  maxLength="10"
                  @input="validateNickname"
                />
                <p class="validation-tip" :class="{ error: !isNicknameValid && nickname.length > 0 }">
                  {{ nicknameFeedback }}
                </p>
              </div>

              <div class="input-group">
                <label>성별 선택</label>
                <div class="gender-selector">
                  <button
                    type="button"
                    class="gender-card"
                    :class="{ active: gender === 'MALE' }"
                    @click="gender = 'MALE'"
                  >
                    <span class="gender-icon">👨</span>
                    <span class="gender-label">남성</span>
                  </button>
                  <button
                    type="button"
                    class="gender-card"
                    :class="{ active: gender === 'FEMALE' }"
                    @click="gender = 'FEMALE'"
                  >
                    <span class="gender-icon">👩</span>
                    <span class="gender-label">여성</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- STEP 2: Hair Style & Hair Color -->
            <div v-show="currentStep === 2" class="step-content">
              <div class="input-group">
                <label>헤어 스타일</label>
                <div class="style-grid">
                  <button
                    v-for="style in hairStyleOptions"
                    :key="style.value"
                    type="button"
                    class="style-card"
                    :class="{ active: hairStyle === style.value }"
                    @click="hairStyle = style.value"
                  >
                    <span class="style-thumb">{{ style.emoji }}</span>
                    <span class="style-label">{{ style.label }}</span>
                  </button>
                </div>
              </div>

              <div class="input-group">
                <div class="flex-between">
                  <label>머리 색상</label>
                  <span class="color-hex">{{ hairColor }}</span>
                </div>
                <div class="color-row">
                  <button
                    v-for="color in hairColorPresets"
                    :key="color"
                    type="button"
                    class="color-preset"
                    :style="{ backgroundColor: color }"
                    :class="{ active: hairColor.toLowerCase() === color.toLowerCase() }"
                    @click="hairColor = color"
                  ></button>
                  <div class="custom-color-picker">
                    <input type="color" v-model="hairColor" class="picker-input" />
                    <span class="picker-icon">🎨</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- STEP 3: Instrument & Instrument Color -->
            <div v-show="currentStep === 3" class="step-content">
              <div class="input-group">
                <label>악기 종류</label>
                <div class="instrument-grid-compact">
                  <button
                    v-for="inst in instrumentOptions"
                    :key="inst.value"
                    type="button"
                    class="inst-card-grid"
                    :class="{ active: instrumentType === inst.value }"
                    @click="instrumentType = inst.value"
                  >
                    <span class="inst-grid-icon">{{ inst.emoji }}</span>
                    <span class="inst-grid-label">{{ inst.label }}</span>
                  </button>
                </div>
              </div>

              <div class="input-group">
                <div class="flex-between">
                  <label>악기 색상</label>
                  <span class="color-hex">{{ instrumentColor }}</span>
                </div>
                <div class="color-row">
                  <button
                    v-for="color in instColorPresets"
                    :key="color"
                    type="button"
                    class="color-preset"
                    :style="{ backgroundColor: color }"
                    :class="{ active: instrumentColor.toLowerCase() === color.toLowerCase() }"
                    @click="instrumentColor = color"
                  ></button>
                  <div class="custom-color-picker">
                    <input type="color" v-model="instrumentColor" class="picker-input" />
                    <span class="picker-icon">🎨</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Sticky Navigation buttons at bottom -->
          <div class="form-navigation">
            <button
              v-if="currentStep > 1"
              type="button"
              class="nav-btn-back"
              @click="prevStep"
              :disabled="isLoading"
            >
              이전 단계
            </button>
            <button
              v-else
              type="button"
              class="nav-btn-cancel"
              @click="handleCancelOrLogout"
              :disabled="isLoading"
            >
              {{ isEditMode ? '취소' : '로그아웃' }}
            </button>

            <button
              v-if="currentStep < 3"
              type="button"
              class="nav-btn-next"
              :disabled="!isStepValid"
              @click="nextStep"
            >
              다음 단계
            </button>
            <button
              v-else
              type="button"
              class="nav-btn-submit"
              :disabled="!isFormValid || isLoading"
              @click="handleSubmit"
            >
              <span v-if="isLoading" class="spinner"></span>
              {{ isLoading ? '처리 중...' : (isEditMode ? '수정 완료' : '생성 완료') }}
            </button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import axiosInstance from '../api/axios'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// Check if Edit mode
const isEditMode = computed(() => route.query.edit === 'true')

// Step state
const currentStep = ref(1)

// Form states
const nickname = ref('')
const gender = ref('MALE')
const hairStyle = ref('SHORT_CUT')
const hairColor = ref('#111827')
const instrumentType = ref('GUITAR')
const instrumentColor = ref('#B91C1C')

const isLoading = ref(false)
const isNicknameValid = ref(false)

onMounted(async () => {
  if (!authStore.isCharacterChecked) {
    await authStore.fetchCharacter()
  }

  const char = authStore.character
  if (char) {
    nickname.value = char.nickname || ''
    gender.value = char.gender || 'MALE'
    hairStyle.value = char.hairStyle || 'SHORT_CUT'
    hairColor.value = char.hairColor || '#111827'
    instrumentType.value = char.instrumentType || 'GUITAR'
    instrumentColor.value = char.instrumentColor || '#B91C1C'
    
    if (nickname.value) {
      validateNickname()
    }
  }
})

// Hair options details
const hairStyleOptions = [
  { value: 'BALD', label: '대머리', emoji: '🧑‍🦲' },
  { value: 'SHORT_CUT', label: '숏컷', emoji: '👦' },
  { value: 'MID_LENGTH_BOB', label: '보브컷', emoji: '👩' },
  { value: 'LONG_STRAIGHT', label: '생머리', emoji: '👱‍♀️' },
  { value: 'NATURAL_WAVE', label: '웨이브', emoji: '👩‍🦱' },
  { value: 'WOLF_CUT', label: '울프컷', emoji: '🐺' },
  { value: 'SIDE_PART_QUIFF', label: '포마드', emoji: '🧔' },
  { value: 'MESSY_ROUGH', label: '헝클어짐', emoji: '🦁' },
  { value: 'DREADLOCKS', label: '드레드락', emoji: '🦱' },
  { value: 'MOHAWK_SHARP', label: '모히칸', emoji: '🐔' }
]

const hairColorPresets = [
  '#111827', // Jet Black
  '#4B5563', // Ash Gray
  '#D4B275', // Honey Blonde
  '#B85C5C', // Auburn Red
  '#7C3AED', // Neon Purple
  '#EC4899', // Cotton Pink
  '#3B82F6', // Cobalt Blue
]

// Instrument options details
const instrumentOptions = [
  { value: 'GUITAR', label: '일렉기타', emoji: '🎸' },
  { value: 'BASS', label: '베이스', emoji: '🎸' },
  { value: 'DRUM', label: '드럼', emoji: '🥁' },
  { value: 'VOCAL', label: '보컬', emoji: '🎤' },
  { value: 'KEYBOARD', label: '건반', emoji: '🎹' }
]

const instColorPresets = [
  '#B91C1C', // Sunburst Cherry Red
  '#1D4ED8', // Oceanic Metallic Blue
  '#D97706', // Sunburst Gold Orange
  '#111827', // Matte Black
  '#F3F4F6', // Vintage Alpine White
  '#059669', // Emerald Green
  '#EC4899', // Punk Candy Pink
]

// Feedback messages
const nicknameFeedback = ref('')

const validateNickname = () => {
  const val = nickname.value.trim()
  if (!val) {
    nicknameFeedback.value = '닉네임은 필수 입력 항목입니다.'
    isNicknameValid.value = false
    return
  }

  if (val.length < 2 || val.length > 10) {
    nicknameFeedback.value = '닉네임은 2자 이상 10자 이하로 입력해주세요.'
    isNicknameValid.value = false
    return
  }

  const regExp = /^[a-zA-Z0-9가-힣]+$/
  if (!regExp.test(val)) {
    nicknameFeedback.value = '특수문자 및 공백은 포함될 수 없습니다.'
    isNicknameValid.value = false
    return
  }

  nicknameFeedback.value = '멋진 이름이네요!'
  isNicknameValid.value = true
}

// Map choices to single emoji
const selectedEmoji = computed(() => {
  if (instrumentType.value === 'DRUM') {
    return '🥁'
  }
  if (instrumentType.value === 'KEYBOARD') {
    return '🎹'
  }
  if (instrumentType.value === 'VOCAL') {
    return '🎤'
  }
  // For GUITAR & BASS (guitarists)
  return gender.value === 'MALE' ? '👨‍🎤' : '👩‍🎤'
})

// Validation for current step
const isStepValid = computed(() => {
  if (currentStep.value === 1) {
    return isNicknameValid.value && gender.value
  }
  if (currentStep.value === 2) {
    return hairStyle.value && hairColor.value
  }
  if (currentStep.value === 3) {
    return instrumentType.value && instrumentColor.value
  }
  return false
})

const isFormValid = computed(() => {
  return isNicknameValid.value && gender.value && hairStyle.value && hairColor.value && instrumentType.value && instrumentColor.value
})

const nextStep = () => {
  if (currentStep.value < 3 && isStepValid.value) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const handleCancelOrLogout = async () => {
  if (isEditMode.value) {
    // Go back to home
    router.push('/home')
  } else {
    // Log out and go back to login screen
    await authStore.logout()
    router.push('/')
  }
}

const handleSubmit = async () => {
  if (!isFormValid.value) return

  isLoading.value = true

  const payload = {
    nickname: nickname.value.trim(),
    instrumentType: instrumentType.value,
    instrumentColor: instrumentColor.value,
    gender: gender.value,
    hairStyle: hairStyle.value,
    hairColor: hairColor.value
  }

  if (isEditMode.value) {
    try {
      await axiosInstance.put('/api/v1/characters/me', payload)
      await authStore.fetchCharacter()
      alert('캐릭터 정보가 수정되었습니다')
      router.push('/home')
    } catch (error) {
      console.error('Character update failed:', error)
      if (error.response && error.response.data && error.response.data.message) {
        alert(`수정 에러: ${error.response.data.message}`)
      } else {
        alert('캐릭터 수정 중 오류가 발생했습니다. 다시 시도해 주세요.')
      }
    } finally {
      isLoading.value = false
    }
    return
  }

  try {
    await axiosInstance.post('/api/v1/characters', payload)
    await authStore.fetchCharacter()
    alert('캐릭터 생성 성공! 합주실로 이동합니다')
    router.push('/home')
  } catch (error) {
    console.error('Character creation failed:', error)
    if (error.response) {
      if (error.response.status === 409) {
        const responseData = error.response.data
        if (responseData && responseData.code === 'C001') {
          await authStore.fetchCharacter()
          alert('이미 보유하고 있는 캐릭터가 있습니다. 합주실로 이동합니다!')
          router.push('/home')
        } else if (responseData && responseData.message) {
          alert(`캐릭터 생성 에러: ${responseData.message}`)
        } else {
          alert('중복된 캐릭터 닉네임이거나 이미 캐릭터를 보유하고 있습니다.')
        }
      } else {
        alert('캐릭터를 생성하는 동안 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
      }
    } else {
      alert('서버 연결 실패. 네트워크 연결 상태를 확인하고 다시 시도해 주세요.')
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
  outline: none;
  -webkit-tap-highlight-color: transparent;
}

.character-create-root {
  width: 100vw;
  height: 100vh;
  background: #f8f9fb;
  color: #1a1a1a;
  font-family: 'Pretendard', sans-serif;
  position: fixed;
  top: 0;
  left: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  padding: 0 20px;
}

.login-background-deco {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 30% 80%, rgba(45,91,255,0.03) 0%, transparent 50%);
  z-index: 0;
}

.create-card-container {
  width: 100%;
  max-width: 980px;
  height: 560px;
  background: #ffffff;
  border: 1px solid #edf0f3;
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  z-index: 1;
}

.create-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  height: 100%;
}

/* ==========================================================================
   LEFT PANEL: VISUAL PREVIEW BOX
   ========================================================================== */
.preview-panel {
  background: #f1f3f5;
  border-right: 1px solid #e9ecef;
  padding: 40px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  gap: 20px;
  height: 100%;
}

.stage-spotlight {
  width: 220px;
  height: 220px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.avatar-circle {
  width: 180px;
  height: 180px;
  background: #ffffff;
  border: 2px solid #e9ecef;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
  z-index: 2;
  transition: transform 0.3s ease;
}

.avatar-circle:hover {
  transform: scale(1.05);
}

.preview-emoji {
  font-size: 6.5rem;
  line-height: 1;
  filter: drop-shadow(0 8px 10px rgba(0,0,0,0.08));
}

.spotlight-shadow {
  position: absolute;
  bottom: 5px;
  width: 130px;
  height: 12px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 50%;
  filter: blur(2px);
  z-index: 1;
}

.preview-badge {
  display: inline-block;
  background: #f0f4ff;
  border: 1px solid #cce0ff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.8px;
  color: #2d5bff;
}

.preview-info {
  text-align: center;
  width: 100%;
}

.preview-info h3 {
  font-size: 1.35rem;
  font-weight: 800;
  margin: 0;
  color: #1a1a1a;
  letter-spacing: -0.5px;
}

/* ==========================================================================
   RIGHT PANEL: FORM CREATOR
   ========================================================================== */
.form-panel {
  padding: 35px 45px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  overflow: hidden;
}

.form-header {
  margin-bottom: 10px;
  text-align: left;
}

.step-badge {
  font-size: 0.75rem;
  font-weight: 800;
  color: #2d5bff;
  letter-spacing: 0.8px;
}

.edit-badge {
  font-size: 0.72rem;
  font-weight: 800;
  background: #fff5f5;
  color: #fa5252;
  border: 1px solid #ffe3e3;
  padding: 3px 10px;
  border-radius: 20px;
}

.form-header h2 {
  font-size: 1.45rem;
  font-weight: 800;
  margin: 4px 0;
  color: #1a1a1a;
}

.form-header p {
  font-size: 0.82rem;
  color: #868e96;
  line-height: 1.4;
}

/* Progress Step Dots */
.step-indicator-bar {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
  padding: 0 10px;
}

.step-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  cursor: default;
}

.dot-inner {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f1f3f5;
  border: 1.5px solid #dee2e6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 800;
  color: #adb5bd;
  transition: all 0.3s ease;
}

.step-dot.active .dot-inner {
  background: #2d5bff;
  border-color: #2d5bff;
  color: #fff;
}

.step-dot.complete .dot-inner {
  background: #e7f5ff;
  border-color: #3b82f6;
  color: #2d5bff;
}

.dot-label {
  position: absolute;
  top: 30px;
  font-size: 0.7rem;
  font-weight: 700;
  white-space: nowrap;
  color: #adb5bd;
}

.step-dot.active .dot-label,
.step-dot.complete .dot-label {
  color: #1a1a1a;
}

.step-line {
  flex: 1;
  height: 2px;
  background: #f1f3f5;
  margin: 0 15px;
  position: relative;
  top: 0;
}

.step-line.fill {
  background: #2d5bff;
}

.steps-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin-bottom: 5px;
  overflow: hidden;
  padding: 6px;
}

.step-content {
  animation: fadeIn 0.3s ease-out;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  width: 100%;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

.input-group label {
  font-size: 0.82rem;
  font-weight: 800;
  color: #495057;
}

.gz-input {
  width: 100%;
  padding: 11px 15px;
  background: #ffffff;
  border: 1.5px solid #eceff2;
  border-radius: 12px;
  font-size: 0.88rem;
  color: #1a1a1a;
  transition: all 0.2s;
}

.gz-input:focus {
  border-color: #2d5bff;
  box-shadow: 0 0 0 4px rgba(45, 91, 255, 0.05);
}

.validation-tip {
  font-size: 0.75rem;
  color: #868e96;
  margin-top: 1px;
  padding-left: 2px;
}

.validation-tip.error {
  color: #fa5252;
  font-weight: 500;
}

.flex-between { display: flex; justify-content: space-between; align-items: center; }

/* Gender selector */
.gender-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.gender-card {
  background: #ffffff;
  border: 1.5px solid #eceff2;
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #868e96;
}

.gender-card:hover {
  border-color: #ced4da;
  color: #495057;
}

.gender-card.active {
  background: #f0f4ff;
  border-color: #2d5bff;
  color: #2d5bff;
}

.gender-icon {
  font-size: 1.4rem;
  line-height: 1;
}

.gender-label {
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.5px;
}

/* Hair style grid */
.style-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}

.style-card {
  background: #ffffff;
  border: 1.5px solid #eceff2;
  border-radius: 8px;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  transition: all 0.2s;
  color: #868e96;
}

.style-card:hover {
  border-color: #ced4da;
  color: #495057;
}

.style-card.active {
  background: #fdf2f8;
  border-color: #ec4899;
  color: #ec4899;
}

.style-thumb {
  font-size: 1.2rem;
}

.style-label {
  font-size: 0.68rem;
  font-weight: 700;
  text-align: center;
  white-space: nowrap;
}

/* Color Palette Selector */
.color-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 4px 0;
}

.color-preset {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.color-preset:hover {
  transform: scale(1.1);
}

.color-preset.active {
  border-color: #1a1a1a;
  transform: scale(1.05);
}

.custom-color-picker {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1.5px solid #eceff2;
  background: #f8f9fa;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.custom-color-picker:hover {
  background: #e9ecef;
}

.picker-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.picker-icon {
  font-size: 0.72rem;
  pointer-events: none;
}

.color-hex {
  font-size: 0.75rem;
  font-family: monospace;
  color: #adb5bd;
}

/* Instrument Grid Compact (Step 3) */
.instrument-grid-compact {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.inst-card-grid {
  background: #ffffff;
  border: 1.5px solid #eceff2;
  border-radius: 12px;
  padding: 12px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  color: #868e96;
  height: 80px;
}

.inst-card-grid:hover {
  border-color: #ced4da;
  color: #495057;
}

.inst-card-grid.active {
  background: #f0f4ff;
  border-color: #2d5bff;
  color: #2d5bff;
}

.inst-grid-icon {
  font-size: 1.6rem;
  line-height: 1;
}

.inst-grid-label {
  font-size: 0.72rem;
  font-weight: 800;
  text-align: center;
  white-space: nowrap;
}

/* ==========================================================================
   NAVIGATION / BOTTOM BUTTON BAR
   ========================================================================== */
.form-navigation {
  margin-top: 15px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.nav-btn-back,
.nav-btn-cancel {
  flex: 1;
  padding: 11px;
  background: transparent;
  border: 1.5px solid #eceff2;
  border-radius: 12px;
  font-size: 0.88rem;
  font-weight: 700;
  color: #495057;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.nav-btn-back:hover:not(:disabled),
.nav-btn-cancel:hover:not(:disabled) {
  background: #f8f9fa;
  color: #1a1a1a;
  border-color: #ced4da;
}

.nav-btn-cancel {
  border-color: #ffe3e3;
  color: #fa5252;
}

.nav-btn-cancel:hover:not(:disabled) {
  background: #fff5f5;
  color: #e03131;
  border-color: #ffa8a8;
}

.nav-btn-back:disabled,
.nav-btn-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nav-btn-next,
.nav-btn-submit {
  flex: 1;
  padding: 11px;
  background: #2d5bff;
  border: none;
  border-radius: 12px;
  font-size: 0.88rem;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.nav-btn-next:hover:not(:disabled),
.nav-btn-submit:hover:not(:disabled) {
  background: #1a4aff;
  box-shadow: 0 5px 15px rgba(45, 91, 255, 0.15);
}

.nav-btn-next:disabled,
.nav-btn-submit:disabled {
  background: #ced4da;
  color: #fff;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive adjustments */
@media (max-width: 860px) {
  .character-create-root {
    height: auto;
    overflow: visible;
    padding: 30px 20px;
    position: relative;
  }
  .create-card-container {
    height: auto;
  }
  .create-grid {
    grid-template-columns: 1fr;
  }
  .preview-panel {
    border-right: none;
    border-bottom: 1px solid #e9ecef;
    padding: 30px;
    height: auto;
  }
  .form-panel {
    padding: 30px;
    height: auto;
  }
  .steps-container {
    overflow: visible;
    margin-bottom: 0;
  }
}
</style>
