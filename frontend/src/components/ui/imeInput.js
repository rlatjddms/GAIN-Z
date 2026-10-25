// 한글 조합 중에도 v-model 값이 바로 바뀌게 (#263)
// Vue의 v-model은 글자를 조합하는 동안(compositionstart ~ compositionend) 입력 칸에 composing 표시를 해 두고 값을 넘기지 않는다.
// 그래서 한글 첫 글자 "가"를 치는 동안에는 값이 빈 칸이라, 입력할 때마다 하는 검사나 버튼 켜기가 한 박자 늦었다.
// 입력 칸의 input 이벤트가 v-model에 닿기 전에(캡처 단계) 표시를 지워서, 조합 중인 글자까지 바로 값으로 넘기게 한다.
// 값은 입력 칸에 이미 있는 그대로라 Vue가 칸을 다시 쓰지 않아 조합이 깨지지 않는다
export function watchImeInput(target = document) {
  const onInput = (event) => {
    if (event.isComposing && event.target && 'composing' in event.target) event.target.composing = false
  }
  target.addEventListener('input', onInput, true)
  return () => target.removeEventListener('input', onInput, true)
}
