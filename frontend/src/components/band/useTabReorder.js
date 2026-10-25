import { ref, onBeforeUnmount } from 'vue'

// 가로로 늘어선 탭을 끌어서 순서 바꾸기 (홈 밴드 탭, #266)
// - 마우스: 누른 채 조금(MOUSE_SLOP) 움직이면 끌기 시작. 그냥 누르면 지금처럼 탭 고르기
// - 손가락: 꾹 누르고(HOLD_MS) 있으면 끌기 시작. 바로 밀면 지금처럼 탭 줄 가로 스크롤
// 끄는 동안 DOM 순서는 그대로 두고 transform으로만 움직이고, 놓으면 onReorder(새 순서 id들)를 부른다
const MOUSE_SLOP = 6
const TOUCH_SLOP = 8
const HOLD_MS = 350

export function useTabReorder({ container, ids, canReorder, onReorder }) {
  // { id, ids: 원래 순서, rects: 원래 자리 { left, width }, gap, startX, dx, order: 미리 보기 순서 }
  const drag = ref(null)
  // 놓은 직후 한 번은 애니메이션 없이 새 순서로 (transform을 풀 때 튀지 않게)
  const settling = ref(false)
  let pending = null      // 누른 뒤 아직 끌기 전 { id, pointerId, x, y, touch, timer }
  let justDragged = false // 끌고 놓은 직후의 click은 탭 고르기로 치지 않는다

  const begin = () => {
    const p = pending
    if (!p) return
    const rects = new Map()
    container.value?.querySelectorAll('[data-tab-id]').forEach((el) => {
      const r = el.getBoundingClientRect()
      rects.set(Number(el.dataset.tabId), { left: r.left, width: r.width })
    })
    const order = ids()
    if (order.some((id) => !rects.has(id))) { cancel(); return }
    const gap = parseFloat(getComputedStyle(container.value).columnGap) || 0
    drag.value = { id: p.id, ids: order, rects, gap, startX: p.x, dx: 0, order }
    navigator.vibrate?.(10)
  }

  const onMove = (e) => {
    const p = pending
    if (!p || e.pointerId !== p.pointerId) return
    if (!drag.value) {
      const moved = Math.hypot(e.clientX - p.x, e.clientY - p.y)
      if (p.touch) { if (moved > TOUCH_SLOP) stop(); return } // 꾹 누르기 전에 움직이면 스크롤
      if (moved < MOUSE_SLOP) return
      begin()
    }
    const d = drag.value
    d.dx = e.clientX - d.startX
    // 잡은 탭 가운데가 넘어간 다른 탭 가운데 수만큼 앞에 둔다
    const own = d.rects.get(d.id)
    const center = own.left + own.width / 2 + d.dx
    const next = d.ids.filter((id) => id !== d.id)
    const target = next.filter((id) => center > d.rects.get(id).left + d.rects.get(id).width / 2).length
    next.splice(target, 0, d.id)
    if (next.some((id, i) => id !== d.order[i])) d.order = next
  }

  // 끄는 동안 손가락이 화면을 스크롤하지 않게
  const blockScroll = (e) => { if (drag.value) e.preventDefault() }

  const stop = () => {
    clearTimeout(pending?.timer)
    pending = null
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', end)
    window.removeEventListener('pointercancel', cancel)
    window.removeEventListener('touchmove', blockScroll)
  }

  const end = (e) => {
    if (pending && e.pointerId !== pending.pointerId) return
    const d = drag.value
    stop()
    if (!d) return
    justDragged = true
    setTimeout(() => { justDragged = false }, 0)
    settling.value = true
    drag.value = null
    if (d.order.some((id, i) => id !== d.ids[i])) onReorder(d.order)
    requestAnimationFrame(() => requestAnimationFrame(() => { settling.value = false }))
  }

  const cancel = () => {
    stop()
    drag.value = null
  }

  const press = (e, id) => {
    if (!canReorder() || pending || drag.value || e.button > 0 || !e.isPrimary) return
    const touch = e.pointerType !== 'mouse'
    pending = { id, pointerId: e.pointerId, x: e.clientX, y: e.clientY, touch, timer: null }
    if (touch) pending.timer = setTimeout(begin, HOLD_MS)
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', end)
    window.addEventListener('pointercancel', cancel)
    window.addEventListener('touchmove', blockScroll, { passive: false })
  }

  const tabStyle = (id) => {
    const d = drag.value
    if (!d) return null
    if (id === d.id) return { transform: `translateX(${d.dx}px)`, zIndex: 12 }
    // 미리 보기 순서대로 왼쪽부터 실제 너비만큼 다시 늘어놓은 자리
    let left = d.rects.get(d.ids[0]).left
    for (const other of d.order) {
      if (other === id) break
      left += d.rects.get(other).width + d.gap
    }
    return { transform: `translateX(${left - d.rects.get(id).left}px)` }
  }

  // 끌고 놓은 직후 click이면 true (탭 고르기를 건너뛴다)
  const wasDragged = () => justDragged

  onBeforeUnmount(stop)
  return { drag, settling, press, tabStyle, wasDragged }
}
