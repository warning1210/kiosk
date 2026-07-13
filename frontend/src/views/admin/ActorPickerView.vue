<template>
  <section class="picker">
    <h2>관리자 선택</h2>
    <p class="hint">실제 로그인 기능이 붙기 전까지, 테스트할 관리자 계정을 선택하세요.</p>

    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <ul v-else class="admin-list">
      <li v-for="admin in admins" :key="admin.adminId">
        <button type="button" @click="selectAdmin(admin)">
          <strong>{{ admin.name }}</strong>
          <span class="role">{{ roleLabel(admin.role) }}</span>
          <span v-if="admin.branchName" class="branch">{{ admin.branchName }}</span>
        </button>
      </li>
    </ul>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdmins } from '../../api/admins'
import { useDevActorStore } from '../../stores/devActor'

const router = useRouter()
const devActor = useDevActorStore()

const admins = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    admins.value = await fetchAdmins()
  } catch (e) {
    error.value = '관리자 목록을 불러오지 못했습니다. 백엔드가 실행 중인지 확인해주세요.'
  } finally {
    loading.value = false
  }
})

function roleLabel(role) {
  if (role === 'SUPER_ADMIN') return '슈퍼관리자'
  if (role === 'HQ_ADMIN') return '본점 관리자'
  if (role === 'BRANCH_MANAGER') return '지점 관리자'
  return role
}

function selectAdmin(admin) {
  devActor.select(admin)
  if (admin.role === 'BRANCH_MANAGER') {
    router.push({ name: 'branch-inventory' })
  } else {
    router.push({ name: 'hq-stock-requests' })
  }
}
</script>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  gap: 0.5rem;
  padding: 1rem;
}

.hint {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.error {
  color: #dc2626;
}

.admin-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 320px;
}

.admin-list button {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.125rem;
  padding: 0.75rem 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  text-align: left;
}

.admin-list button:hover {
  border-color: #6366f1;
  background: #f5f5ff;
}

.role {
  font-size: 0.75rem;
  color: #6366f1;
}

.branch {
  font-size: 0.75rem;
  color: #6b7280;
}
</style>
