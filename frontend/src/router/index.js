import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import HomeView from '../views/HomeView.vue'
import OAuth2RedirectView from '../views/OAuth2RedirectView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView
    },
    {
      path: '/signup',
      name: 'signup',
      component: SignupView
    },
    {
      path: '/home',
      name: 'home',
      component: HomeView
    },
    {
      path: '/oauth2/redirect',
      name: 'oauth2-redirect',
      component: OAuth2RedirectView
    }
  ]
})

// Global Navigation Guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.path === '/home') {
    if (!authStore.isAuthenticated) {
      // Direct unauthorized access to login
      next('/')
    } else {
      next()
    }
  } else if ((to.path === '/' || to.path === '/signup') && authStore.isAuthenticated) {
    // Redirect authenticated users to home instead of login/signup
    next('/home')
  } else {
    next()
  }
})

export default router
