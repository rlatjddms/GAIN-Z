// 메일 다시 보내기 같은 버튼을 몇 초 동안 쉬게 한다 (#193). left: 남은 초 (0이면 다시 누를 수 있음)
import { onBeforeUnmount, ref } from 'vue'

export function useCooldown() {
  const left = ref(0)
  let timer = null
  const stop = () => { clearInterval(timer); timer = null }
  const start = (seconds) => {
    stop()
    left.value = seconds
    timer = setInterval(() => {
      left.value -= 1
      if (left.value <= 0) { left.value = 0; stop() }
    }, 1000)
  }
  onBeforeUnmount(stop)
  return { left, start }
}
