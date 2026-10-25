<template>
  <Teleport to="body">
    <transition name="modal">
      <div v-if="friend" class="backdrop" @click.self="!sending && emit('close')">
        <section class="modal" role="dialog" aria-modal="true" aria-labelledby="band-invite-title">
          <h2 id="band-invite-title">{{ friend.name }} 님을 밴드에 초대</h2>
          <p class="sub">초대할 밴드를 골라 주세요. {{ friend.name }} 님은 알림을 눌러 파트를 고르고 들어와요.</p>

          <div v-if="loading" class="muted">밴드를 불러오는 중…</div>
          <p v-else-if="!bandStore.bands.length" class="muted">아직 가입한 밴드가 없어요. 홈 BAND 탭에서 밴드를 만들어 보세요.</p>
          <ul v-else class="bands" role="radiogroup" aria-label="초대할 밴드">
            <li v-for="b in bandStore.bands" :key="b.id">
              <button type="button" role="radio" class="band" :class="{ on: pickedId === b.id }" :aria-checked="pickedId === b.id"
                      :disabled="!!blockedReason(b) || sending" @click="pickedId = b.id">
                <span class="b-text"><strong>{{ b.name }}</strong><small>{{ blockedReason(b) || `멤버 ${b.members.length}명` }}</small></span>
                <Check v-if="pickedId === b.id" :size="16" :stroke-width="3" aria-hidden="true" />
              </button>
            </li>
          </ul>
          <p v-if="error" class="error">{{ error }}</p>

          <div class="actions">
            <button type="button" class="btn" :disabled="sending" @click="emit('close')">취소</button>
            <button type="button" class="btn primary" :disabled="!pickedId || sending" @click="send">{{ sending ? '보내는 중…' : '초대 보내기' }}</button>
          </div>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
// 친구에게 밴드 초대 보내기 (#229). 이미 그 밴드에 있거나 인원이 찬 밴드는 고를 수 없다
import { ref, watch } from 'vue'
import { Check } from 'lucide-vue-next'
import { useBandStore } from '../../stores/band'
import { inviteFriendToBand, MAX_BAND_MEMBERS } from '../../api/band'
import { apiErrorMessage } from '../../api/practice'

const props = defineProps({
  // { userId, name } 초대할 친구. null이면 닫힘
  friend: { type: Object, default: null },
})
// sent(friend, band): 보냈을 때 (알림 문구는 부르는 쪽에서)
const emit = defineEmits(['close', 'sent'])

const bandStore = useBandStore()
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const pickedId = ref(null)

const blockedReason = (band) => {
  if (band.members.some((m) => m.userId === props.friend?.userId)) return '이미 함께 있는 밴드예요'
  if (band.members.length >= MAX_BAND_MEMBERS) return `인원이 다 찼어요 (${MAX_BAND_MEMBERS}명)`
  return ''
}

watch(() => props.friend, async (friend) => {
  if (!friend) return
  error.value = ''
  pickedId.value = null
  loading.value = true
  await bandStore.fetchMyBands()
  loading.value = false
  // 고를 수 있는 밴드가 하나면 미리 골라 둔다
  const open = bandStore.bands.filter((b) => !blockedReason(b))
  if (open.length === 1) pickedId.value = open[0].id
})

const send = async () => {
  const band = bandStore.bands.find((b) => b.id === pickedId.value)
  if (!band || sending.value) return
  sending.value = true
  error.value = ''
  try {
    await inviteFriendToBand(band.id, props.friend.userId)
    emit('sent', props.friend, band)
  } catch (e) {
    error.value = apiErrorMessage(e, '초대를 보내지 못했어요.')
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 2000; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; padding: 16px;
}
.modal {
  width: 100%; max-width: 400px; max-height: calc(100dvh - 32px); overflow-y: auto; box-sizing: border-box;
  background: var(--c-surface); border-radius: var(--radius-xl); padding: 24px; box-shadow: var(--shadow-lg);
  font-family: 'Pretendard', sans-serif; color: var(--c-text);
}
h2 { margin: 0 0 4px; font-size: 1.05rem; font-weight: 900; overflow-wrap: anywhere; }
.sub { margin: 0 0 16px; font-size: 0.8rem; line-height: 1.5; color: var(--c-muted); }
.muted { margin: 0; padding: 16px 0; font-size: 0.82rem; color: var(--c-muted); text-align: center; }
.bands { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.band {
  width: 100%; display: flex; align-items: center; gap: 10px; padding: 12px 14px; box-sizing: border-box;
  border: 1px solid var(--c-line); border-radius: 12px; background: var(--c-surface);
  font: inherit; text-align: left; color: var(--c-text); cursor: pointer;
}
.band:hover:not(:disabled) { border-color: var(--c-primary-line); }
.band.on { border-color: var(--c-primary); background: var(--c-primary-soft); color: var(--c-primary-text); }
.band:disabled { cursor: default; background: var(--c-bg); }
.band:disabled .b-text strong { color: var(--c-faint); }
.b-text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.b-text strong { font-size: 0.88rem; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.b-text small { font-size: 0.72rem; color: var(--c-muted); }
.error { margin: 12px 0 0; font-size: 0.8rem; font-weight: 700; color: var(--c-danger); }
.actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
/* 휴대폰: 아래에서 올라오는 시트로 (#124) */
@media (max-width: 640px) {
  .backdrop { align-items: flex-end; padding: 0; }
  .modal {
    width: 100%; max-width: none; max-height: 92dvh;
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    padding-bottom: calc(24px + env(safe-area-inset-bottom));
  }
}
</style>
