import axios from 'axios'
import { COMMUNITY_CACHE_KEY } from '../components/band/communityCache'

// 백엔드 주소. 배포 환경마다 다르므로 .env의 VITE_API_BASE_URL로 받는다 (#174).
// 빈 문자열이면 프론트와 같은 도메인(상대 경로)으로 부른다. 안 정하면 로컬 백엔드
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

// Create a main Axios instance
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Create a separate instance for reissuing tokens to avoid interceptor recursion
const reissueInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request Interceptor: Automatically inject the access token
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response Interceptor: Automatically handle 401 (Unauthorized) / Token Expiry
axiosInstance.interceptors.response.use(
  (response) => {
    return response
  },
  async (error) => {
    const originalRequest = error.config

    // If response status is 401 and we haven't already retried this request
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const isLocal = !!localStorage.getItem('refreshToken')
      const refreshToken = isLocal ? localStorage.getItem('refreshToken') : sessionStorage.getItem('refreshToken')

      if (refreshToken) {
        try {
          // Attempt to reissue tokens
          const response = await reissueInstance.post('/api/v1/auth/reissue', {
            refreshToken: refreshToken,
          })

          const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data.data
          const storage = isLocal ? localStorage : sessionStorage

          // Save new tokens
          storage.setItem('accessToken', newAccessToken)
          storage.setItem('refreshToken', newRefreshToken)

          // Retry the original request with the new access token
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
          return axiosInstance(originalRequest)
        } catch (reissueError) {
          // If token reissue fails, perform logout cleanup
          const cleanups = [localStorage, sessionStorage]
          cleanups.forEach((storage) => {
            storage.removeItem('accessToken')
            storage.removeItem('refreshToken')
            storage.removeItem('userEmail')
            storage.removeItem('userName')
            storage.removeItem(COMMUNITY_CACHE_KEY)
          })
          window.location.href = '/'
          return Promise.reject(reissueError)
        }
      } else {
        // No refresh token available, redirect to login
        const cleanups = [localStorage, sessionStorage]
        cleanups.forEach((storage) => {
          storage.removeItem('accessToken')
          storage.removeItem('refreshToken')
          storage.removeItem('userEmail')
          storage.removeItem('userName')
          storage.removeItem(COMMUNITY_CACHE_KEY)
        })
        window.location.href = '/'
      }
    }

    return Promise.reject(error)
  }
)

export default axiosInstance
