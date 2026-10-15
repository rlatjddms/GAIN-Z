import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import HomeView from '../views/HomeView.vue'
import CharacterCreateView from '../views/CharacterCreateView.vue'
import WorkspaceView from '../views/WorkspaceView.vue'
import OAuth2RedirectView from '../views/OAuth2RedirectView.vue'
import { useAuthStore } from '../stores/auth'

const routes = [
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
      component: HomeView,
      meta: { requiresCharacter: true }
    },
    {
      path: '/workspace',
      name: 'workspace',
      component: WorkspaceView,
      meta: { requiresCharacter: true }
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

// 개발 모드 전용: 픽셀 스프라이트 미리보기 (/dev/sprites)
if (import.meta.env.DEV) {
  routes.push({
    path: '/dev/sprites',
    name: 'dev-sprites',
    component: () => import('../views/SpritePreviewView.vue')
  })
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 로그인 + 캐릭터가 있어야 들어갈 수 있는 화면 (홈, 워크스페이스)
  if (to.meta.requiresCharacter) {
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

