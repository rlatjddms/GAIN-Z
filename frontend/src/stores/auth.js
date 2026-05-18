import { defineStore } from 'pinia'
import axiosInstance from '../api/axios'

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
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    displayName: (state) => {
      if (state.userName) return state.userName
      if (state.userEmail) {
        // Fallback: use email local part (e.g. test@example.com -> test)
        return state.userEmail.split('@')[0]
      }
      return '성은' // Final fallback from default mockup
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

    // Check if email already exists
    async checkEmail(email) {
      try {
        const response = await axiosInstance.get('/api/v1/auth/check-email', {
          params: { email },
        })
        // The API returns isDuplicate: true (exists) or false (available)
        return response.data.data
      } catch (error) {
        console.error('Email check failed:', error)
        throw error
      }
    },

    // Signup action
    async signup(name, email, password) {
      try {
        const response = await axiosInstance.post('/api/v1/auth/signup', {
          name,
          email,
          password,
        })

        // On success, save the name locally for this email to enhance UX later
        localStorage.setItem(`name_${email}`, name)
        return response.data
      } catch (error) {
        console.error('Signup failed:', error)
        throw error
      }
    },

    // Local login action
    async login(email, password, keepLoggedIn = true) {
      try {
        const response = await axiosInstance.post('/api/v1/auth/login', {
          email,
          password,
        })

        const { accessToken, refreshToken } = response.data.data
        this.setTokens(accessToken, refreshToken, keepLoggedIn)
        return response.data
      } catch (error) {
        console.error('Login failed:', error)
        throw error
      }
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
        this.userEmail = null
        this.userName = null

        const cleanups = [localStorage, sessionStorage]
        cleanups.forEach((storage) => {
          storage.removeItem('accessToken')
          storage.removeItem('refreshToken')
          storage.removeItem('userEmail')
          storage.removeItem('userName')
        })
      }
    },
  },
})
