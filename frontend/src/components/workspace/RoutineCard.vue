<template>
  <WsCard :title="isToday ? '오늘의 루틴' : `${dateLabel(date)} 루틴`">
    <template #actions>
      <button v-if="isToday" class="text-btn" @click="openForm(null)">+ 추가</button>
    </template>

    <p v-if="routines.length" class="summary" :class="{ done: allDone }">
      {{ allDone ? '모두 완료했어요' : `${doneCount} / ${routines.length} 완료` }}
    </p>

    <p v-if="!editable && routines.length" class="readonly">7일이 지난 날은 볼 수만 있어요.</p>

    <ul class="routine-list" :class="{ loading, readonly: !editable }">
      <li v-for="r in routines" :key="r.id" :class="{ completed: r.completed }">
        <button
          class="check" :class="{ on: r.completed }"
          :disabled="!editable"
          :aria-label="r.completed ? '완료 취소' : '완료로 표시'"
          @click="setCount(r, r.completed ? 0 : r.targetCount)"
        ><Check v-if="r.completed" :size="14" :stroke-width="3" /></button>
        <div class="body" @dblclick="isToday && openForm(r)">
          <span class="content">{{ r.content }}</span>
          <span class="schedule">{{ scheduleLabel(r) }}</span>
          <span class="bar"><i :style="{ width: `${Math.min(100, (r.count / r.targetCount) * 100)}%` }"></i></span>
        </div>
        <div class="counter">
          <button class="step" :disabled="!editable || r.count <= 0" aria-label="한 번 빼기" @click="setCount(r, r.count - 1)">−</button>
          <span class="count">{{ r.count }} / {{ r.targetCount }}</span>
          <button class="step" :disabled="!editable" aria-label="한 번 더하기" @click="setCount(r, r.count + 1)">+</button>
        </div>
        <button v-if="isToday" class="more" aria-label="루틴 수정" @click="openForm(r)">⋯</button>
      </li>
    </ul>

    <div v-if="!loading && !routines.length" class="empty">
      <p>{{ isToday ? '스케일 10회처럼 매일 또는 정한 요일마다 할 연습을 정해 두고 체크해요.' : '이날은 일정에 있는 루틴이 없어요.' }}</p>
      <button v-if="isToday" class="small primary" @click="openForm(null)">루틴 추가하기</button>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <RoutineFormModal :open="formOpen" :routine="formRoutine" @close="formOpen = false" @saved="load" />
  </WsCard>
</template>

<script setup>
import { Check } from 'lucide-vue-next'
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import WsCard from './WsCard.vue'
import RoutineFormModal from './RoutineFormModal.vue'
import { getRoutines, checkRoutine } from '../../api/routine'
import { apiErrorMessage } from '../../api/practice'
import { dateKey, dateLabel, isEditable } from '../../constants/practice'
import { scheduleLabel } from '../../constants/routine'

// date: 보고 있는 날짜 ('YYYY-MM-DD'). 그날 일정인 루틴만 나온다. 체크는 최근 7일만, 루틴 추가·수정은 오늘 화면에서만
const props = defineProps({ date: { type: String, required: true } })
const date = computed(() => props.date)
const isToday = computed(() => props.date === dateKey())
const editable = computed(() => isEditable(props.date))

const routines = ref([])
const loading = ref(false)
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
  formOpen.value = false
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

// 추가·수정 팝업 (routine이 null이면 새로 추가)
const formOpen = ref(false)
const formRoutine = ref(null)
const openForm = (r) => {
  formRoutine.value = r
  formOpen.value = true
}
</script>

<style scoped>
.text-btn {
  border: none; background: none; padding: 0; font: inherit; font-size: 0.75rem; font-weight: 800;
  color: var(--c-primary-text); cursor: pointer;
}

.summary { margin: -6px 0 12px; font-size: 0.75rem; font-weight: 800; color: var(--c-muted); }
.summary.done { color: #2b8a3e; }

.routine-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; transition: opacity 0.2s; }
.routine-list.loading { opacity: 0.4; }
.readonly { margin: -6px 0 10px; font-size: 0.75rem; font-weight: 700; color: var(--c-faint); }
.check:disabled { cursor: default; }
.routine-list li {
  display: flex; align-items: center; gap: 10px;
  background: var(--c-bg); border-radius: 12px; padding: 10px 12px; transition: background 0.2s;
}
.routine-list li.completed { background: #ebfbee; }

.check {
  flex: none; width: 22px; height: 22px; border: 2px solid #ced4da; border-radius: 7px; background: #fff;
  color: #fff; font-size: 0.8rem; font-weight: 900; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.check.on { background: #2b8a3e; border-color: #2b8a3e; }

.body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.content { font-size: 0.85rem; font-weight: 800; color: #343a40; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.schedule { margin-top: -4px; font-size: 0.68rem; font-weight: 700; color: var(--c-faint); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.completed .content { color: #2b8a3e; }
.bar { height: 6px; background: var(--c-line); border-radius: 4px; overflow: hidden; }
.bar i { display: block; height: 100%; background: color-mix(in srgb, var(--c-primary) 78%, #fff); border-radius: 4px; transition: width 0.2s; }
.completed .bar i { background: #40c057; }

.counter { flex: none; display: flex; align-items: center; gap: 4px; }
.step {
  width: 26px; height: 26px; border: 1px solid var(--c-line); border-radius: 8px; background: #fff; color: var(--c-text-2);
  font-size: 0.95rem; font-weight: 900; cursor: pointer; line-height: 1;
}
.step:hover:not(:disabled) { border-color: var(--c-primary); color: var(--c-primary-text); }
.step:disabled { opacity: 0.35; cursor: default; }
.count { min-width: 44px; text-align: center; font-size: 0.75rem; font-weight: 800; color: var(--c-text-2); font-variant-numeric: tabular-nums; }

.more {
  flex: none; width: 24px; height: 24px; border: none; background: none; border-radius: 7px;
  color: var(--c-faint); font-weight: 900; cursor: pointer;
}
.more:hover { background: var(--c-line); color: var(--c-text-2); }

.small {
  border: 1px solid var(--c-line); background: #fff; color: var(--c-text-2); cursor: pointer; font-family: inherit;
  font-size: 0.75rem; font-weight: 800; padding: 7px 12px; border-radius: 9px;
}
.small.primary { background: var(--c-primary); border-color: var(--c-primary); color: #fff; }
.small:disabled { opacity: 0.45; cursor: not-allowed; }

.empty { text-align: center; padding: 12px 0 4px; }
.empty p { margin: 0 0 12px; font-size: 0.8rem; color: var(--c-faint); }
.error { margin: 10px 0 0; font-size: 0.75rem; font-weight: 700; color: #e03131; }
</style>
