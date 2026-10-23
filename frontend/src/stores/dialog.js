import { defineStore } from 'pinia'

// 앱 안 확인창 (#134): 브라우저 기본 alert·confirm·prompt 대신 쓴다. 화면은 components/ui/AppDialog.vue
// 사용: await dialog.alert('저장했어요') / if (await dialog.confirm({ title, text, okText, danger })) … / const v = await dialog.prompt({ … })
export const useDialogStore = defineStore('dialog', {
  state: () => ({ current: null }),
  actions: {
    open(kind, options) {
      const opts = typeof options === 'string' ? { title: options } : options
      // 이미 떠 있는 창은 취소로 닫고 새 창을 띄운다
      if (this.current) this.current.resolve(kind === 'prompt' ? null : false)
      return new Promise((resolve) => { this.current = { kind, ...opts, resolve } })
    },
    alert(options) { return this.open('alert', options) },
    confirm(options) { return this.open('confirm', options) },
    // value: 처음 들어 있는 글자, readonly: 복사용으로 보여 주기만
    prompt(options) { return this.open('prompt', options) },
    close(result) {
      const cur = this.current
      this.current = null
      cur?.resolve(result)
    },
  },
})
