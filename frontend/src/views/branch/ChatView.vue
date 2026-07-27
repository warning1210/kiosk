<template>
  <div class="shell"><BranchSidebar active="chat" /><main>
    <header><div><h1>본점 채팅</h1><p>본점과 실시간으로 상담하세요.</p></div></header>

    <section class="thread">
      <div class="messages" ref="messagesEl">
        <div v-if="!messages.length" class="empty">아직 메시지가 없습니다. 아래에서 본점에 문의해보세요.</div>
        <div v-for="message in messages" :key="message.chatMessageId" :class="['bubble', message.fromHq ? 'theirs' : 'mine']">
          <span class="sender">{{ message.senderName }}</span>
          <p>{{ message.messageContent }}</p>
          <small>{{ formatTime(message.createdAt) }}</small>
        </div>
      </div>
      <form class="composer" @submit.prevent="send">
        <input v-model.trim="draft" placeholder="메시지를 입력하세요" :disabled="sending">
        <button :disabled="sending || !draft" type="submit">전송</button>
      </form>
    </section>
  </main></div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import http from '../../api/branch'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

const messages = ref([])
const draft = ref('')
const sending = ref(false)
const messagesEl = ref(null)
let timer = null

onMounted(() => {
  load()
  timer = setInterval(load, 3000)
})
onBeforeUnmount(() => clearInterval(timer))

async function load() {
  try {
    const { data } = await http.get('/chat/messages')
    messages.value = data
    await nextTick()
    if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  } catch (e) {
    console.error(e)
  }
}

async function send() {
  if (!draft.value) return
  sending.value = true
  try {
    await http.post('/chat/messages', { content: draft.value })
    draft.value = ''
    await load()
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
.shell{min-height:100vh;color:#1f2938;background:#f3f6fa}main{margin-left:238px;padding:32px 34px 55px}header h1{margin:0;font-size:27px}header p{margin:7px 0 0;color:#7f8997;font-size:11px}
.thread{display:flex;flex-direction:column;margin-top:24px;height:calc(100vh - 220px);background:#fff;border:1px solid #e5e9ef;border-radius:16px;overflow:hidden}
.messages{flex:1;overflow-y:auto;padding:20px;display:flex;flex-direction:column;gap:10px}
.bubble{max-width:60%;padding:10px 13px;border-radius:12px;font-size:12px}
.bubble.theirs{align-self:flex-start;background:#f1f3f7}
.bubble.mine{align-self:flex-end;background:#5f63ee;color:#fff}
.bubble .sender{display:block;margin-bottom:4px;font-size:9px;font-weight:800;opacity:.7}
.bubble p{margin:0;white-space:pre-wrap}
.bubble small{display:block;margin-top:4px;font-size:8px;opacity:.6}
.composer{display:flex;gap:8px;padding:14px;border-top:1px solid #e9edf2}
.composer input{flex:1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}
.composer button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}
.composer button:disabled{opacity:.55}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
@media(max-width:760px){main{margin-left:0;padding:20px}.thread{height:70vh}}
</style>
