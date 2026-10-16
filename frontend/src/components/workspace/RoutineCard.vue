<template>
  <WsCard :title="isToday ? '오늘의 루틴' : `${dateLabel(date)} 루틴`">
    <template #actions>
      <button v-if="isToday && !adding" class="text-btn" @click="startAdd">+ 추가</button>
    </template>

    <p v-if="routines.length" class="summary" :class="{ done: allDone }">
      {{ allDone ? '모두 완료했어요 🎉' : `${doneCount} / ${routines.length} 완료` }}
    </p>

    <p v-if="!editable && routines.length" class="readonly">7일이 지난 날은 볼 수만 있어요.</p>

    <ul class="routine-list" :class="{ loading, readonly: !editable }">
      <li v-for="r in routines" :key="r.id" :class="{ completed: r.completed }">
        <!-- 수정 모드 -->
        <form v-if="editingId === r.id" class="edit-form" @submit.prevent="saveEdit(r)">
          <input v-model="form.content" class="content-input" maxlength="50" placeholder="루틴 이름" aria-label="루틴 이름">
          <label class="target-input">
            <input v-model.number="form.targetCount" type="number" min="1" max="99" aria-label="목표 횟수">회
          </label>
          <div class="form-btns">
            <button type="button" class="small danger" @click="remove(r)">삭제</button>
            <button type="button" class="small" @click="editingId = null">취소</button>
            <button type="submit" class="small primary" :disabled="!formValid">저장</button>
          </div>
        </form>

        <!-- 보기 모드 -->
        <template v-else>
          <button
            class="check" :class="{ on: r.completed }"
            :disabled="!editable"
            :aria-label="r.completed ? '완료 취소' : '완료로 표시'"
            @click="setCount(r, r.completed ? 0 : r.targetCount)"
          >{{ r.completed ? '✓' : '' }}</button>
          <div class="body" @dblclick="isToday && startEdit(r)">
            <span class="content">{{ r.content }}</span>
            <span class="bar"><i :style="{ width: `${Math.min(100, (r.count / r.targetCount) * 100)}%` }"></i></span>
          </div>
          <div class="counter">
            <button class="step" :disabled="!editable || r.count <= 0" aria-label="한 번 빼기" @click="setCount(r, r.count - 1)">−</button>
            <span class="count">{{ r.count }} / {{ r.targetCount }}</span>
            <button class="step" :disabled="!editable" aria-label="한 번 더하기" @click="setCount(r, r.count + 1)">+</button>
          </div>
          <button v-if="isToday" class="more" aria-label="루틴 수정" @click="startEdit(r)">⋯</button>
        </template>
      </li>
    </ul>

    <!-- 추가 -->
    <form v-if="adding" class="add-form" @submit.prevent="add">
      <input ref="addInput" v-model="form.content" class="content-input" maxlength="50" placeholder="예: 스케일 연습" aria-label="루틴 이름">
      <label class="target-input">
        <input v-model.number="form.targetCount" type="number" min="1" max="99" aria-label="목표 횟수">회
      </label>
      <div class="form-btns">
        <button type="button" class="small" @click="adding = false">취소</button>
        <button type="submit" class="small primary" :disabled="!formValid || saving">추가</button>
      </div>
    </form>

    <div v-if="!loading && !routines.length && !adding" class="empty">
      <p>스케일 10회처럼 매일 할 연습을 정해 두고 체크해요.</p>
      <button v-if="isToday" class="small primary" @click="startAdd">첫 루틴 추가하기</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
  </WsCard>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import WsCard from './WsCard.vue'
import { getRoutines, createRoutine, updateRoutine, deleteRoutine, checkRoutine } from '../../api/routine'
import { apiErrorMessage } from '../../api/practice'
import { dateKey, dateLabel, isEditable } from '../../constants/practice'

// date: 보고 있는 날짜 ('YYYY-MM-DD'). 체크는 최근 7일만, 루틴 추가·수정은 오늘 화면에서만
const props = defineProps({ date: { type: String, required: true } })
const date = computed(() => props.date)
const isToday = computed(() => props.date === dateKey())
const editable = computed(() => isEditable(props.date))

const routines = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const doneCount = computed(() => routines.value.filter((r) => r.completed).length)
const allDone = computed(() => routines.value.length > 0 && doneCount.value === routines.value.length)

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    routines.value = await getRoutines(date.value)
  } catch (e) {
    error.value = `루틴을 불러오지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    loading.value = false
  }
}
onMounted(load)

// 날짜가 바뀌면 보내지 않은 체크를 먼저 저장하고 그 날짜로 다시 불러온다
watch(() => props.date, async () => {
  await flushChecks()
  editingId.value = null
  adding.value = false
  load()
})

// 체크: 화면은 바로 바꾸고, 빠르게 여러 번 눌러도 마지막 횟수만 잠시 뒤 한 번 보낸다
const timers = new Map()
const setCount = (r, count) => {
  const next = Math.max(0, Math.min(999, count))
  r.count = next
  r.completed = next >= r.targetCount
  clearTimeout(timers.get(r.id)?.timer)
  const day = date.value
  timers.set(r.id, { timer: setTimeout(() => sendCheck(r, day), 400), routine: r, date: day })
}
const sendCheck = async (r, day) => {
  timers.delete(r.id)
  try {
    const saved = await checkRoutine(r.id, day, r.count)
    if (day === date.value) Object.assign(r, { count: saved.count, completed: saved.completed })
  } catch (e) {
    error.value = `체크를 저장하지 못했어요. ${apiErrorMessage(e)}`
    load()
  }
}
// 날짜를 바꾸거나 화면을 떠나기 전에 보내지 않은 체크를 바로 보낸다
const flushChecks = () => Promise.all([...timers.values()].map(({ timer, routine, date: day }) => {
  clearTimeout(timer)
  return sendCheck(routine, day)
}))
onBeforeUnmount(flushChecks)

// 추가·수정 폼
const form = reactive({ content: '', targetCount: 10 })
const formValid = computed(() => form.content.trim().length > 0 && form.targetCount >= 1 && form.targetCount <= 99)
const adding = ref(false)
const addInput = ref(null)
const editingId = ref(null)

const startAdd = async () => {
  editingId.value = null
  Object.assign(form, { content: '', targetCount: 10 })
  adding.value = true
  await nextTick()
  addInput.value?.focus()
}
const add = async () => {
  saving.value = true
  error.value = ''
  try {
    await createRoutine({ content: form.content.trim(), targetCount: form.targetCount })
    adding.value = false
    await load()
  } catch (e) {
    error.value = `루틴을 추가하지 못했어요. ${apiErrorMessage(e)}`
  } finally {
    saving.value = false
  }
}

const startEdit = (r) => {
  adding.value = false
  Object.assign(form, { content: r.content, targetCount: r.targetCount })
  editingId.value = r.id
}
const saveEdit = async (r) => {
  if (!formValid.value) return
  error.value = ''
  try {
    const saved = await updateRoutine(r.id, { content: form.content.trim(), targetCount: form.targetCount })
    Object.assign(r, { content: saved.content, targetCount: saved.targetCount })
    r.completed = r.count >= r.targetCount
    editingId.value = null
  } catch (e) {
    error.value = `루틴을 수정하지 못했어요. ${apiErrorMessage(e)}`
  }
}
const remove = async (r) => {
  if (!window.confirm(`'${r.content}' 루틴을 삭제할까요?\n지난 체크 기록은 남아요.`)) return
  error.value = ''
  try {
    await deleteRoutine(r.id)
    routines.value = routines.value.filter((x) => x.id !== r.id)
    editingId.value = null
  } catch (e) {
    error.value = `루틴을 삭제하지 못했어요. ${apiErrorMessage(e)}`
  }
}
</script>

<style scoped>
.text-btn {
  border: none; background: none; padding: 0; font: inherit; font-size: 0.75rem; font-weight: 800;
  color: #2d5bff; cursor: pointer;
}

.summary { margin: -6px 0 12px; font-size: 0.75rem; font-weight: 800; color: #868e96; }
.summary.done { color: #2b8a3e; }

.routine-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; transition: opacity 0.2s; }
.routine-list.loading { opacity: 0.4; }
.readonly { margin: -6px 0 10px; font-size: 0.75rem; font-weight: 700; color: #adb5bd; }
.check:disabled { cursor: default; }
.routine-list li {
  display: flex; align-items: center; gap: 10px;
  background: #f8f9fa; border-radius: 12px; padding: 10px 12px; transition: background 0.2s;
}
.routine-list li.completed { background: #ebfbee; }

.check {
  flex: none; width: 22px; height: 22px; border: 2px solid #ced4da; border-radius: 7px; background: #fff;
  color: #fff; font-size: 0.8rem; font-weight: 900; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.check.on { background: #2b8a3e; border-color: #2b8a3e; }

.body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.content { font-size: 0.85rem; font-weight: 800; color: #343a40; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.completed .content { color: #2b8a3e; }
.bar { height: 6px; background: #e9ecef; border-radius: 4px; overflow: hidden; }
.bar i { display: block; height: 100%; background: #5a7dff; border-radius: 4px; transition: width 0.2s; }
.completed .bar i { background: #40c057; }

.counter { flex: none; display: flex; align-items: center; gap: 4px; }
.step {
  width: 26px; height: 26px; border: 1px solid #e9ecef; border-radius: 8px; background: #fff; color: #495057;
  font-size: 0.95rem; font-weight: 900; cursor: pointer; line-height: 1;
}
.step:hover:not(:disabled) { border-color: #2d5bff; color: #2d5bff; }
.step:disabled { opacity: 0.35; cursor: default; }
.count { min-width: 44px; text-align: center; font-size: 0.75rem; font-weight: 800; color: #495057; font-variant-numeric: tabular-nums; }

.more {
  flex: none; width: 24px; height: 24px; border: none; background: none; border-radius: 7px;
  color: #adb5bd; font-weight: 900; cursor: pointer;
}
.more:hover { background: #e9ecef; color: #495057; }

.edit-form, .add-form { flex: 1; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.add-form { margin-top: 8px; background: #f8f9fa; border-radius: 12px; padding: 10px 12px; }
.content-input {
  flex: 1; min-width: 120px; border: 1px solid #dee2e6; border-radius: 9px; padding: 8px 10px;
  font: inherit; font-size: 0.85rem; font-weight: 700; outline: none;
}
.content-input:focus { border-color: #2d5bff; }
.target-input { display: flex; align-items: center; gap: 4px; font-size: 0.8rem; font-weight: 700; color: #868e96; }
.target-input input {
  width: 52px; border: 1px solid #dee2e6; border-radius: 9px; padding: 8px 6px; text-align: right;
  font: inherit; font-size: 0.85rem; font-weight: 800; outline: none;
}
.form-btns { display: flex; gap: 6px; margin-left: auto; }
.small {
  border: 1px solid #e9ecef; background: #fff; color: #495057; cursor: pointer; font-family: inherit;
  font-size: 0.75rem; font-weight: 800; padding: 7px 12px; border-radius: 9px;
}
.small.primary { background: #2d5bff; border-color: #2d5bff; color: #fff; }
.small.danger { color: #e03131; border-color: #ffc9c9; }
.small:disabled { opacity: 0.45; cursor: not-allowed; }

.empty { text-align: center; padding: 12px 0 4px; }
.empty p { margin: 0 0 12px; font-size: 0.8rem; color: #adb5bd; }
.error { margin: 10px 0 0; font-size: 0.75rem; font-weight: 700; color: #e03131; }
</style>
