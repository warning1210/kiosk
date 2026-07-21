<template>
  <section class="login">
    <h2>본점 관리자 로그인</h2>
    <form @submit.prevent="onSubmit">
      <input v-model="loginId" type="text" placeholder="아이디" />
      <input v-model="password" type="password" placeholder="비밀번호" />
      <button type="submit" :disabled="loading">{{ loading ? '로그인 중...' : '로그인' }}</button>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'

const router = useRouter()
const loginId = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.post('/hq-auth/login', { loginId: loginId.value, password: password.value })
    localStorage.setItem('hq-session', JSON.stringify(data))
    router.push('/admin/dashboard')
  } catch (e) {
    error.value = e.response?.data?.message || '로그인할 수 없습니다.'
  } finally {
    loading.value = false
  }
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

.error {
  color: #d33;
  font-size: 0.85rem;
}
</style>
