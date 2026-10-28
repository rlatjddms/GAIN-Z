import { useRouter } from 'vue-router'

// 화면 위 "뒤로" 버튼 (#314). 바로 전 기록이 돌아갈 화면이면 기록을 한 칸 되돌리고(router.back),
// 아니면(주소로 바로 들어온 경우 등) 그 화면으로 바꾼다(replace).
// 예전에는 뒤로 버튼이 돌아갈 화면을 새로 쌓아서(push), 아이폰에서 밀어서 뒤로가기를 하면
// 홈 → 셋리스트 → 홈 → 잼 → 홈…을 하나씩 다 거꾸로 지나갔다
export function useGoBack() {
  const router = useRouter()
  return (to) => {
    const back = window.history.state?.back
    if (back && router.resolve(back).path === router.resolve(to).path) router.back()
    else router.replace(to)
  }
}
