import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import HomeView from '../views/HomeView.vue'
import CharacterCreateView from '../views/CharacterCreateView.vue'
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
      path: '/character-create',
      name: 'character-create',
      component: CharacterCreateView
    },
    {
      path: '/oauth2/redirect',
      name: 'oauth2-redirect',
      component: OAuth2RedirectView
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (to.path === '/home') {
    if (!authStore.isAuthenticated) {
      // Direct unauthorized access to login
      next('/')
    } else {
      if (!authStore.isCharacterChecked) {
        await authStore.fetchCharacter()
      }
      if (!authStore.character) {
        next('/character-create')
      } else {
        next()
      }
    }
  } else if (to.path === '/character-create') {
    if (!authStore.isAuthenticated) {
      next('/')
    } else {
      if (!authStore.isCharacterChecked) {
        await authStore.fetchCharacter()
      }
      if (to.query.edit !== 'true' && authStore.character) {
        next('/home')
      } else {
        next()
      }
    }
  } else if ((to.path === '/' || to.path === '/signup') && authStore.isAuthenticated) {
    if (!authStore.isCharacterChecked) {
      await authStore.fetchCharacter()
    }
    if (!authStore.character) {
      next('/character-create')
    } else {
      next('/home')
    }
  } else {
    next()
  }
})

export default router

