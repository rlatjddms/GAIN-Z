import { defineStore } from 'pinia'

// 저장·오류 알림 (#267). 화면은 components/ui/AppToast.vue (화면 아래 가운데에 잠깐 떴다 사라진다)
// 언제 띄우는지:
// - 저장 버튼을 눌렀는데 화면이 그대로이거나 창이 닫히기만 하는 곳(설정·프로필·밴드 정보·메모·곡 저장) → toast.success('~했어요')
// - 결과가 바로 화면에 보이는 동작(댓글 달기·순서 바꾸기·삭제·즐겨찾기·스위치) → 띄우지 않는다
// - 자동 저장(악보 필기·연습 키) → 띄우지 않고 그 화면의 "저장됨 · 저장 중…" 표시로
// - 실패 → toast.error(문구). 쓰던 내용은 지우지 않는다
// 그대로 두는 것:
// - 입력 칸 옆 검사 오류(이름이 너무 김 등)는 입력 칸 옆
// - 팝업 안에서 한 일의 실패(비밀번호 바꾸기·잼 만들기 등)는 그 팝업 안 (고쳐서 다시 누를 수 있게)
// - 화면을 못 불러온 오류는 그 자리에 '다시 불러오기'와 함께
// - 로그인·가입·비밀번호 찾기 화면의 안내
// 문구는 "~했어요" / "~하지 못했어요. …" 말투로 맞춘다
const SUCCESS_MS = 2000
const ERROR_MS = 4500
// 긴 문구는 읽을 시간만큼 조금 더 (20자 넘는 글자마다 60ms, 6초까지)
const shownFor = (kind, text) => Math.min(6000, (kind === 'error' ? ERROR_MS : SUCCESS_MS) + Math.max(0, text.length - 20) * 60)

let seq = 0
let timer = null

export const useToastStore = defineStore('toast', {
  state: () => ({
    // 한 번에 하나만 { key, kind: 'success' | 'error', text }
    current: null,
  }),

  actions: {
    show(kind, text) {
      if (!text) return
      clearTimeout(timer)
      this.current = { key: ++seq, kind, text }
      const key = seq
      timer = setTimeout(() => { if (this.current?.key === key) this.current = null }, shownFor(kind, text))
    },
    success(text) { this.show('success', text) },
    error(text) { this.show('error', text) },
    dismiss() {
      clearTimeout(timer)
      this.current = null
    },
  },
})
