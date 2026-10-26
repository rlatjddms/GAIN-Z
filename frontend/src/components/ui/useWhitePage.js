import { onMounted, onBeforeUnmount } from 'vue'

// 휴대폰에서 흰 화면을 쓰는 페이지(로그인·회원가입 등, #226): 그동안 페이지 바탕(html·body)도 흰색으로.
// 바탕은 다른 화면에 맞춘 연한 회색이라, 앱에서 상태바·홈 표시줄 자리나 위아래로 당겼을 때 회색 띠가 보였다.
// 색은 App.vue의 html.white-page 규칙
export function useWhitePage() {
  onMounted(() => document.documentElement.classList.add('white-page'))
  onBeforeUnmount(() => document.documentElement.classList.remove('white-page'))
}
