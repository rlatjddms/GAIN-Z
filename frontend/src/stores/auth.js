import { defineStore } from 'pinia'
import axiosInstance from '../api/axios'
import { getMyInfo } from '../api/user'
import { useBandStore } from './band'

// Helper to decode JWT payloads
function decodeJwt(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (error) {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken') || null,
    userEmail: localStorage.getItem('userEmail') || sessionStorage.getItem('userEmail') || null,
    userName: localStorage.getItem('userName') || sessionStorage.getItem('userName') || null,
    character: null,
    isCharacterChecked: false,
    // 내 계정 정보 (GET /api/v1/users/me). 이름은 여기서 받아 userName에도 넣는다
    profile: null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    displayName: (state) => {
      if (state.userName) return state.userName
      if (state.userEmail) {
        // Fallback: use email local part (e.g. test@example.com -> test)
        return state.userEmail.split('@')[0]
      }
      return '회원'
    },
  },

  actions: {
    // Save tokens and decode user payload
    setTokens(accessToken, refreshToken, keepLoggedIn = true) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken

      const storage = keepLoggedIn ? localStorage : sessionStorage
      const otherStorage = keepLoggedIn ? sessionStorage : localStorage

      // Clear other storage to avoid conflict
      otherStorage.removeItem('accessToken')
      otherStorage.removeItem('refreshToken')
      otherStorage.removeItem('userEmail')
      otherStorage.removeItem('userName')

      storage.setItem('accessToken', accessToken)
      storage.setItem('refreshToken', refreshToken)

      const decoded = decodeJwt(accessToken)
      if (decoded && decoded.sub) {
        this.userEmail = decoded.sub
        storage.setItem('userEmail', decoded.sub)

        // If we saved a name matching this email during signup, restore it
        const savedName = localStorage.getItem(`name_${decoded.sub}`) || sessionStorage.getItem(`name_${decoded.sub}`)
        if (savedName) {
          this.userName = savedName
          storage.setItem('userName', savedName)
        } else {
          // Clear current userName so we fall back to email prefix
          this.userName = null
          storage.removeItem('userName')
        }
      }
    },

    // Signup action
    // 이름은 받지 않는다 (#227): 화면에 보이는 이름은 캐릭터 닉네임
    async signup(email, password, emailVerificationToken) {
      const response = await axiosInstance.post('/api/v1/auth/signup', {
        email,
        password,
        emailVerificationToken,
      })
      return response.data
    },

    // Local login action
    async login(email, password, keepLoggedIn = true) {
      const response = await axiosInstance.post('/api/v1/auth/login', {
        email,
        password,
      })
      const { accessToken, refreshToken } = response.data.data
      this.setTokens(accessToken, refreshToken, keepLoggedIn)
      return response.data
    },

    // Logout action
    async logout() {
      try {
        // Only attempt backend logout if we are actually authenticated
        if (this.accessToken) {
          await axiosInstance.post('/api/v1/auth/logout')
        }
      } catch (error) {
        console.error('Backend logout request failed:', error)
      } finally {
        // Always clean up frontend state regardless of API success
        this.accessToken = null
        this.refreshToken = null
        const email = this.userEmail
        this.userEmail = null
        this.userName = null
        this.character = null
        this.isCharacterChecked = false
        this.profile = null
        useBandStore().reset()

        const cleanups = [localStorage, sessionStorage]
        cleanups.forEach((storage) => {
          storage.removeItem('accessToken')
          storage.removeItem('refreshToken')
          storage.removeItem('userEmail')
          storage.removeItem('userName')
          if (email) {
            storage.removeItem(`hasCharacter_${email}`)
            storage.removeItem(`userCharacter_${email}`)
          }
        })
      }
    },

    // 내 계정 정보 불러오기. 실패해도 화면은 저장된 이름으로 그대로 보여 준다
    async fetchProfile() {
      if (!this.accessToken) return null
      try {
        this.profile = await getMyInfo()
        this.setUserName(this.profile.name)
        return this.profile
      } catch (error) {
        console.error('Failed to fetch profile:', error)
        return null
      }
    },

    // 닉네임 바꾸기 반영 (헤더 표시용으로 토큰과 같은 저장소에 둔다)
    setUserName(name) {
      this.userName = name
      if (this.profile) this.profile.name = name
      const storage = localStorage.getItem('accessToken') ? localStorage : sessionStorage
      storage.setItem('userName', name)
      if (this.userEmail) localStorage.setItem(`name_${this.userEmail}`, name)
    },

    // 회원 탈퇴 뒤: 서버 로그아웃 없이 이 기기의 로그인 정보만 지운다
    async clearAfterWithdraw() {
      const email = this.userEmail
      this.accessToken = null
      await this.logout()
      if (email) localStorage.removeItem(`name_${email}`)
    },

    // Fetch character details from backend
    async fetchCharacter() {
      if (!this.accessToken) {
        this.character = null
        this.isCharacterChecked = false
        return null
      }
      try {
        const response = await axiosInstance.get('/api/v1/characters/me')
        this.character = response.data.data
        this.isCharacterChecked = true
        return this.character
      } catch (error) {
        console.error('Failed to fetch character:', error)
        if (error.response && error.response.status === 404) {
          this.character = null
          this.isCharacterChecked = true
        } else {
          this.character = null
        }
        return null
      }
    },
  },
})
