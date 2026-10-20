import { onBeforeUnmount, ref } from 'vue'

// 화면 너비 기준 (#124): CSS @media와 같은 값을 쓴다. 휴대폰 ≤ 640px, 태블릿 ≤ 860px
export const PHONE_QUERY = '(max-width: 640px)'
export const TABLET_QUERY = '(max-width: 860px)'

// 미디어 쿼리가 맞는지 반응형으로 알려 준다 (화면을 돌리거나 창 크기를 바꾸면 따라 바뀜)
export function useMediaQuery(query) {
  const list = window.matchMedia(query)
  const matches = ref(list.matches)
  const onChange = (e) => { matches.value = e.matches }
  list.addEventListener('change', onChange)
  onBeforeUnmount(() => list.removeEventListener('change', onChange))
  return matches
}
