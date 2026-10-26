import { ref, onMounted, onBeforeUnmount } from 'vue'

// 셋리스트·레이어 잼 목록 접기 (#254). 한 줄 화면(휴대폰, 860px 이하)에서는 목록이 길면 아래 내용까지 한참 내려가야 해서
// 목록을 접고 펼 수 있게 한다. 목록에서 하나를 고르면 접어서 아래 내용이 바로 보이게 한다. 넓은 화면에서는 늘 펼친다
const NARROW = '(max-width: 860px)'

export function useCollapsibleList() {
  const narrow = ref(false)
  const collapsed = ref(false)
  let media = null

  const sync = () => {
    narrow.value = media.matches
    if (!narrow.value) collapsed.value = false
  }
  onMounted(() => {
    media = window.matchMedia(NARROW)
    sync()
    media.addEventListener('change', sync)
  })
  onBeforeUnmount(() => media?.removeEventListener('change', sync))

  return {
    narrow,
    collapsed,
    toggle: () => { collapsed.value = !collapsed.value },
    // 목록에서 골랐을 때
    collapseIfNarrow: () => { if (narrow.value) collapsed.value = true },
  }
}
