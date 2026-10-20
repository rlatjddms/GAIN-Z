import { defineStore } from 'pinia'
import { getAnnotations, saveAnnotation } from '../api/sheet'
import { apiErrorMessage } from '../api/practice'

// 악보 위 필기 (#167): 올린 장(사진·PDF 쪽) 위에 펜·형광펜으로 쓴 내 필기
// 원본 파일은 그대로 두고, 장(PDF는 쪽)마다 필기 JSON을 따로 저장한다
//
// 필기 한 면(surface) = { w: 1000, h, strokes: [{ t: 'pen' | 'hl', c: '#색', s: 굵기, p: [x, y, x, y, …] }] }
//   좌표는 면 너비를 1000으로 본 값이라, 확대·축소하거나 화면 크기가 달라도 같은 자리에 그려진다

export const SURFACE_WIDTH = 1000
export const TOOLS = {
  pen: { label: '펜', width: 3, colors: ['#e11d48', '#2563eb', '#111827'] },
  hl: { label: '형광펜', width: 18, colors: ['#facc15', '#4ade80', '#f472b6'] },
}
const MAX_HISTORY = 50
const SAVE_DELAY = 800
const RETRY_DELAY = 5000

export const surfaceKey = (pageId, pdfPage) => `${pageId}:${pdfPage}`

const parse = (data) => {
  try {
    const parsed = JSON.parse(data)
    return Array.isArray(parsed?.strokes) ? parsed.strokes : []
  } catch {
    return []
  }
}

let saveTimer = null

export const useAnnotationStore = defineStore('annotation', {
  state: () => ({
    sheetId: null,
    loaded: false,
    enabled: false,     // 필기 중이면 장 위에서 그리기 (아니면 스크롤)
    tool: 'pen',        // pen | hl | eraser
    colors: { pen: TOOLS.pen.colors[0], hl: TOOLS.hl.colors[0] },
    surfaces: {},       // surfaceKey → { pageId, pdfPage, h, strokes }
    history: [],        // 되돌리기: [[{ key, before }]] (한 번에 바뀐 면들)
    dirty: [],          // 아직 저장하지 않은 면
    status: 'idle',     // idle | saving | saved | error
    message: '',        // 서버가 저장을 거절한 까닭 (error일 때)
  }),

  getters: {
    color: (state) => state.colors[state.tool] || TOOLS.pen.colors[0],
    strokeCount: (state) => Object.values(state.surfaces).reduce((sum, s) => sum + s.strokes.length, 0),
    canUndo: (state) => state.history.length > 0,
  },

  actions: {
    async load(sheetId) {
      if (this.sheetId !== sheetId) {
        await this.flush()
        this.$reset()
        this.sheetId = sheetId
      }
      try {
        const list = await getAnnotations(sheetId)
        if (this.sheetId !== sheetId) return
        for (const a of list || []) {
          this.surfaces[surfaceKey(a.pageId, a.pdfPage)] = { pageId: a.pageId, pdfPage: a.pdfPage, h: null, strokes: parse(a.data) }
        }
        this.loaded = true
      } catch {
        // 필기를 못 불러와도 악보는 보여 준다. 그 상태에서 쓰면 덮어쓸 수 있어서 필기는 막는다
        this.loaded = false
      }
    },

    strokesOf(key) {
      return this.surfaces[key]?.strokes || []
    },

    setTool(tool) {
      this.tool = tool
    },
    setColor(color) {
      if (this.tool in this.colors) this.colors[this.tool] = color
    },

    // 한 면의 획을 바꾼다 (그리기·지우기). 되돌리기 기록을 남기고 잠시 뒤 저장
    change(changes) {
      const entry = []
      for (const { pageId, pdfPage, h, strokes } of changes) {
        const key = surfaceKey(pageId, pdfPage)
        const before = this.strokesOf(key)
        entry.push({ key, before })
        this.surfaces[key] = { pageId, pdfPage, h: h ?? this.surfaces[key]?.h ?? null, strokes }
        this.markDirty(key)
      }
      if (!entry.length) return
      this.history.push(entry)
      if (this.history.length > MAX_HISTORY) this.history.shift()
    },

    undo() {
      const entry = this.history.pop()
      if (!entry) return
      for (const { key, before } of entry) {
        this.surfaces[key].strokes = before
        this.markDirty(key)
      }
    },

    // 이 악보의 내 필기를 모두 지운다 (되돌리기로 살릴 수 있음)
    clearAll() {
      const changes = Object.values(this.surfaces)
        .filter((s) => s.strokes.length)
        .map((s) => ({ ...s, strokes: [] }))
      this.change(changes)
    },

    markDirty(key) {
      if (!this.dirty.includes(key)) this.dirty.push(key)
      this.status = 'saving'
      this.message = ''
      clearTimeout(saveTimer)
      saveTimer = setTimeout(() => this.flush(), SAVE_DELAY)
    },

    // 바뀐 면을 지금 저장 (화면을 떠날 때도 부름)
    async flush() {
      clearTimeout(saveTimer)
      saveTimer = null
      const keys = this.dirty.splice(0)
      if (!keys.length || !this.sheetId) return
      const sheetId = this.sheetId
      try {
        for (const key of keys) {
          const s = this.surfaces[key]
          const data = JSON.stringify({ w: SURFACE_WIDTH, h: s.h, strokes: s.strokes })
          await saveAnnotation(sheetId, s.pageId, s.pdfPage, data)
        }
        if (this.sheetId === sheetId && !this.dirty.length) this.status = 'saved'
      } catch (e) {
        if (this.sheetId !== sheetId) return
        for (const key of keys) if (!this.dirty.includes(key)) this.dirty.push(key)
        this.status = 'error'
        const code = e?.response?.status
        // 서버가 거절한 필기(너무 큼 등)는 다시 보내도 같아서, 다음에 고칠 때까지 기다린다. 그 밖엔 잠시 뒤 다시 저장
        this.message = code >= 400 && code < 500 && code !== 408 && code !== 429 ? apiErrorMessage(e) : ''
        if (!this.message) saveTimer = setTimeout(() => this.flush(), RETRY_DELAY)
      }
    },
  },
})
