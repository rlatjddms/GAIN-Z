import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import HomeView from '../views/HomeView.vue'
import CharacterCreateView from '../views/CharacterCreateView.vue'
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
    // 홈 카드에서 들어가는 SOLO 상세 화면
    {
      path: '/practice',
      name: 'practice',
      component: () => import('../views/workspace/PracticeView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/sheets',
      name: 'sheets',
      component: () => import('../views/workspace/SheetsView.vue'),
      meta: { requiresCharacter: true }
    },
    // 악보 올리기 / 보관함 악보 보기(:id = 보관함 항목) / 내가 올린 악보 고치기
    {
      path: '/sheets/new',
      name: 'sheet-new',
      component: () => import('../views/workspace/SheetUploadView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/sheets/:id(\\d+)',
      name: 'sheet-detail',
      component: () => import('../views/workspace/SheetDetailView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/sheets/:id(\\d+)/edit',
      name: 'sheet-edit',
      component: () => import('../views/workspace/SheetUploadView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/wardrobe',
      name: 'wardrobe',
      component: () => import('../views/WardrobeView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/mypage',
      name: 'mypage',
      component: () => import('../views/MyPageView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('../views/NotificationsView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/friends',
      name: 'friends',
      component: () => import('../views/FriendsView.vue'),
      meta: { requiresCharacter: true }
    },
    // 홈 BAND 탭에서 들어가는 밴드 화면
    {
      path: '/bands/:id(\\d+)/setlists',
      name: 'band-setlists',
      component: () => import('../views/band/BandSetlistsView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/bands/:id(\\d+)/jams',
      name: 'band-jams',
      component: () => import('../views/band/BandJamsView.vue'),
      meta: { requiresCharacter: true }
    },
    {
      path: '/bands/:id(\\d+)/community',
      name: 'band-community',
      redirect: to => ({ path: '/home', query: { band: to.params.id, ...(to.query.post ? { post: to.query.post } : {}), ...(to.query.new ? { compose: '1' } : {}), ...(to.query.edit ? { edit: '1' } : {}) } }),
      meta: { requiresCharacter: true }
    },
    {
      path: '/bands/:id(\\d+)/settings',
      name: 'band-settings',
      component: () => import('../views/band/BandSettingsView.vue'),
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
  // 마디 악보(타브·코드) 렌더러 미리보기
  routes.push({
    path: '/dev/tabs',
    name: 'dev-tabs',
    component: () => import('../views/TabPreviewView.vue')
  })
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 브라우저 탭 제목: '연습 기록장 · GAIN-Z'
const TITLES = {
  login: '로그인', signup: '회원가입', home: '홈', practice: '연습 기록장', sheets: '악보 보관함',
  'sheet-new': '악보 만들기', 'sheet-detail': '악보', 'sheet-edit': '악보 고치기',
  wardrobe: '옷장', mypage: '마이페이지', notifications: '알림', friends: '친구',
  'band-setlists': '셋리스트', 'band-jams': '레이어 잼', 'band-settings': '밴드 설정', 'character-create': '캐릭터 만들기',
}
router.afterEach((to) => {
  const title = TITLES[to.name]
  document.title = title ? `${title} · GAIN-Z` : 'GAIN-Z'
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 로그인 + 캐릭터가 있어야 들어갈 수 있는 화면 (홈과 SOLO 상세 화면)
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
      // 캐릭터가 있으면 만들기 화면 대신 홈으로. 예전 "캐릭터 수정"(?edit=true)은 옷장으로 옮겼다
      if (authStore.character) {
        next(to.query.edit === 'true' ? '/wardrobe' : '/home')
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

