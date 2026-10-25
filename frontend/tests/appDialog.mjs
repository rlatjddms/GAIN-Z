// 앱 확인창(components/ui/AppDialog.vue)에서 버튼을 누른다.
// 예전에는 브라우저 확인창(window.confirm)이라 page.on('dialog')로 받았지만, 지금은 화면 안의 <dialog role="alertdialog">다.
export async function confirmAppDialog(page, buttonName) {
  const dialog = page.getByRole('alertdialog')
  await dialog.getByRole('button', { name: buttonName, exact: true }).click()
  await dialog.waitFor({ state: 'hidden' })
}
