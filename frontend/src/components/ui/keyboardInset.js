// 휴대폰 키보드가 올라오면 아래 팝업(시트)도 키보드 위로 올린다 (#226)
// iOS 사파리·안드로이드 크롬은 키보드가 떠도 화면(레이아웃) 높이는 그대로라, 화면 아래에 붙은 시트가 키보드에 가려진다.
// 눈에 보이는 영역(visualViewport)으로 키보드 높이를 재서 <html>에 keyboard-open과 CSS 변수로 알려 준다.
// 실제 위치 조정은 styles/ui.css의 html.keyboard-open 규칙
//   --kb: 키보드 높이, --vv-top: 보이는 영역 위쪽이 밀린 만큼, --vvh: 보이는 영역 높이
const MIN_KEYBOARD_PX = 80 // 주소창이 줄었다 늘어나는 정도는 키보드로 보지 않는다

export function watchKeyboardInset() {
  const vv = typeof window !== 'undefined' ? window.visualViewport : null
  if (!vv) return
  const root = document.documentElement
  let open = false

  const update = () => {
    const kb = Math.max(0, Math.round(window.innerHeight - vv.height - vv.offsetTop))
    const nextOpen = kb > MIN_KEYBOARD_PX && isTyping()
    root.style.setProperty('--kb', `${nextOpen ? kb : 0}px`)
    root.style.setProperty('--vv-top', `${nextOpen ? Math.round(vv.offsetTop) : 0}px`)
    root.style.setProperty('--vvh', `${Math.round(vv.height)}px`)
    if (nextOpen !== open) {
      open = nextOpen
      root.classList.toggle('keyboard-open', open)
      if (open) keepFocusedVisible()
    }
  }

  vv.addEventListener('resize', update)
  vv.addEventListener('scroll', update)
  // 다른 입력칸으로 옮겨 가면 그 칸이 보이게
  document.addEventListener('focusin', () => { if (open) keepFocusedVisible() })
  update()
}

// 글자를 넣는 칸에 있을 때만 키보드로 본다 (화면 회전 등으로 높이가 바뀌는 경우 제외)
function isTyping() {
  const el = document.activeElement
  if (!el) return false
  if (el.isContentEditable || el.tagName === 'TEXTAREA') return true
  return el.tagName === 'INPUT' && !['checkbox', 'radio', 'range', 'button', 'submit', 'file', 'color'].includes(el.type)
}

// 시트가 올라간 뒤 입력칸이 시트 안에서 가려져 있으면 보이게 스크롤
function keepFocusedVisible() {
  requestAnimationFrame(() => {
    document.activeElement?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
  })
}
