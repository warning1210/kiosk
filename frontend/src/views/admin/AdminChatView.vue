<template>
  <main class="page">
    <AdminSidebar active="chat" />

    <section class="content">
      <AdminPageHeader title="채팅 상담" subtitle="지점의 상담 요청에 실시간으로 응대하세요." />

      <div class="chat-shell">
        <aside class="room-list">
          <div v-if="!rooms.length" class="empty">진행 중인 상담이 없습니다.</div>
          <button
            v-for="room in rooms"
            :key="room.chatRoomId"
            type="button"
            class="room"
            :class="{ active: selectedRoomId === room.chatRoomId }"
            @click="selectRoom(room.chatRoomId)"
          >
            <strong>{{ room.branchName }}</strong>
            <span :class="['status', room.consultationStatus === 'OPEN' ? 'approved' : 'pending']">{{ room.consultationStatus === 'OPEN' ? '진행중' : '종료' }}</span>
            <p>{{ room.lastMessagePreview || '메시지가 없습니다.' }}</p>
          </button>
        </aside>

        <section class="thread">
          <div v-if="!selectedRoomId" class="empty thread-empty">왼쪽에서 지점을 선택하세요.</div>
          <template v-else>
            <div class="messages" ref="messagesEl">
              <div v-for="message in messages" :key="message.chatMessageId" :class="['bubble', message.fromHq ? 'mine' : 'theirs']">
                <span class="sender">{{ message.senderName }}</span>
                <p>{{ message.messageContent }}</p>
                <small>{{ formatTime(message.createdAt) }}</small>
              </div>
            </div>
            <form class="composer" @submit.prevent="send">
              <input v-model.trim="draft" placeholder="메시지를 입력하세요" :disabled="sending">
              <button :disabled="sending || !draft" type="submit">전송</button>
            </form>
          </template>
        </section>
      </div>
    </section>
  </main>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'

const rooms = ref([])
const messages = ref([])
const selectedRoomId = ref(null)
const draft = ref('')
const sending = ref(false)
const messagesEl = ref(null)
let timer = null

onMounted(() => {
  loadRooms()
  timer = setInterval(() => {
    loadRooms()
    if (selectedRoomId.value) loadMessages(selectedRoomId.value)
  }, 3000)
})
onBeforeUnmount(() => clearInterval(timer))

async function loadRooms() {
  try {
    rooms.value = (await http.get('/hq/chat/rooms')).data
  } catch (e) {
    console.error(e)
  }
}

function selectRoom(roomId) {
  selectedRoomId.value = roomId
  loadMessages(roomId)
}

async function loadMessages(roomId) {
  try {
    const { data } = await http.get(`/hq/chat/rooms/${roomId}/messages`)
    messages.value = data
    await nextTick()
    if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  } catch (e) {
    console.error(e)
  }
}

async function send() {
  if (!draft.value || !selectedRoomId.value) return
  sending.value = true
  try {
    await http.post(`/hq/chat/rooms/${selectedRoomId.value}/messages`, { content: draft.value })
    draft.value = ''
    await loadMessages(selectedRoomId.value)
    await loadRooms()
  } catch (e) {
    console.error(e)
  } finally {
    sending.value = false
  }
}

function formatTime(value) {
  return new Intl.DateTimeFormat('ko-KR', { timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.chat-shell{display:grid;grid-template-columns:280px 1fr;gap:16px;margin-top:20px;height:calc(100vh - 220px)}
.room-list{overflow-y:auto;background:#fff;border:1px solid #e4e8ef;border-radius:16px;padding:10px}
.room{display:block;width:100%;padding:14px;margin-bottom:6px;text-align:left;border:1px solid transparent;border-radius:10px;background:transparent;cursor:pointer}
.room:hover{background:#f8fafc}.room.active{border-color:#cfd3fa;background:#f5f6ff}
.room strong{font-size:12px}.room p{overflow:hidden;margin:6px 0 0;color:#8c95a2;font-size:10px;text-overflow:ellipsis;white-space:nowrap}
.status{float:right;padding:3px 7px;border-radius:6px;font-size:9px;font-weight:800}.status.pending{color:#d57d00;background:#fff3d6}.status.approved{color:#0b9654;background:#e2f8ec}
.thread{display:flex;flex-direction:column;background:#fff;border:1px solid #e4e8ef;border-radius:16px;overflow:hidden}
.thread-empty{display:flex;align-items:center;justify-content:center;height:100%}
.messages{flex:1;overflow-y:auto;padding:20px;display:flex;flex-direction:column;gap:10px}
.bubble{max-width:60%;padding:10px 13px;border-radius:12px;font-size:12px}
.bubble.theirs{align-self:flex-start;background:#f1f3f7}
.bubble.mine{align-self:flex-end;background:#6266ef;color:#fff}
.bubble .sender{display:block;margin-bottom:4px;font-size:9px;font-weight:800;opacity:.7}
.bubble p{margin:0;white-space:pre-wrap}
.bubble small{display:block;margin-top:4px;font-size:8px;opacity:.6}
.composer{display:flex;gap:8px;padding:14px;border-top:1px solid #e9edf2}
.composer input{flex:1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}
.composer button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}
.composer button:disabled{opacity:.55}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.chat-shell{grid-template-columns:1fr;height:auto}.room-list{max-height:200px}.thread{height:60vh}}
</style>
