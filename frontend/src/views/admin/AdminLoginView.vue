<template>
  <section class="login">
    <h2>관리자 로그인</h2>
    <form @submit.prevent="onSubmit">
      <input v-model="loginId" type="text" placeholder="아이디" />
      <input v-model="password" type="password" placeholder="비밀번호" />
      <button type="submit">로그인</button>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginId = ref('')
const password = ref('')

function onSubmit() {
  // TODO: call POST /api/admin/login - 백엔드 인증 붙기 전까지 1번 지점 대시보드로 임시 이동
  router.push('/branch/1/dashboard')
}
</script>

<style scoped>
.login {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 0.5rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 240px;
}
</style>
