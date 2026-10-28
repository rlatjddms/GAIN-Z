// 팝업을 열 때 글자 칸 자동 선택 규칙 (#313)
// - 컴퓨터(마우스): 지금처럼 첫 칸을 골라 바로 입력
// - 휴대폰(손가락): 글자 칸을 고르지 않는다. 고르면 키보드가 올라와 팝업 아래쪽을 가린다.
//   예외로 입력 칸 하나뿐인 짧은 팝업(이름 입력 등 AppDialog의 prompt)은 휴대폰에서도 바로 입력하게 둔다
// 예전에는 팝업마다 달라서(브라우저 <dialog>는 첫 칸을 저절로 고름) 어떤 팝업만 키보드가 올라왔다

export const canAutoFocusText = () => window.matchMedia?.('(hover: hover) and (pointer: fine)').matches ?? true

// 컴퓨터에서만 고른다
export function focusOnDesktop(el) {
  if (canAutoFocusText()) el?.focus()
}

const TEXT_FIELDS = 'input:not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit]), textarea, select, [contenteditable]'

// <dialog>.showModal()은 첫 칸을 저절로 고른다. 휴대폰에서는 열자마자(같은 순간, 키보드가 뜨기 전) 팝업 자체로 초점을 옮긴다.
// (<dialog autofocus>로 팝업 자체를 고르게 하는 방법은 크롬이 따르지 않았다)
export function watchDialogFocus() {
  const proto = window.HTMLDialogElement?.prototype
  if (!proto?.showModal || proto.showModal.patched) return
  const showModal = proto.showModal
  proto.showModal = function () {
    const result = showModal.call(this)
    const active = document.activeElement
    if (!canAutoFocusText() && active !== this && this.contains(active) && active.matches(TEXT_FIELDS)) {
      if (!this.hasAttribute('tabindex')) this.setAttribute('tabindex', '-1')
      this.focus({ preventScroll: true })
    }
    return result
  }
  proto.showModal.patched = true
}
