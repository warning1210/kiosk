<template>
  <main class="page">
    <AdminSidebar active="notices" />

    <section class="content">
      <AdminPageHeader title="공지사항 관리" subtitle="본사 공지사항을 작성하고 지점 노출 여부를 관리할 수 있습니다." />

      <div class="summary">
        <AdminStatCard icon="▨" label="전체" :value="`${notices.length}개`" />
        <AdminStatCard icon="✓" label="게시중" :value="`${statusCount('PUBLISHED')}개`" tone="green" />
        <AdminStatCard icon="✎" label="임시저장" :value="`${statusCount('DRAFT')}개`" tone="orange" />
        <AdminStatCard icon="✕" label="숨김" :value="`${statusCount('HIDDEN')}개`" tone="pink" />
      </div>

      <!-- 공지 작성/수정 폼 -->
      <section class="form-card">
        <h2>{{ editingId ? '공지사항 수정' : '새 공지사항 작성' }}</h2>
        <form class="entity-form" @submit.prevent="submitNotice">
          <div class="image-field">
            <label class="image-upload">
              <img v-if="form.imageUrl" :src="form.imageUrl" alt="">
              <span v-else>이미지를 선택해주세요<br>PNG, JPG, JPEG (최대 5MB)</span>
              <input type="file" accept="image/png,image/jpeg" @change="handleImageUpload">
            </label>
            <span v-if="uploading" class="uploading">업로드 중...</span>
          </div>
          <div class="fields">
            <input v-model.trim="form.title" required placeholder="제목">
            <select v-model="form.status">
              <option value="DRAFT">임시저장</option>
              <option value="PUBLISHED">게시</option>
              <option value="HIDDEN">숨김</option>
            </select>
            <textarea v-model.trim="form.content" required placeholder="내용" rows="4"></textarea>
            <div class="form-actions">
              <button v-if="editingId" type="button" class="cancel" @click="cancelEdit">취소</button>
              <button :disabled="saving" type="submit">{{ saving ? '저장 중' : editingId ? '수정 완료' : '공지 등록' }}</button>
            </div>
          </div>
        </form>
        <p v-if="formError" class="alert">{{ formError }}</p>
      </section>

      <!-- 목록 -->
      <section class="list-card">
        <div class="list-head">
          <div>
            <h2>공지사항 목록</h2>
            <span>{{ loading ? '불러오는 중' : `${filteredNotices.length}개` }}</span>
          </div>
          <label class="search"><span>⌕</span><input v-model="keyword" placeholder="제목 검색"></label>
        </div>

        <div class="tabs">
          <button v-for="tab in statusTabs" :key="tab.value" :class="{ active: statusFilter === tab.value }" type="button" @click="statusFilter = tab.value">
            {{ tab.label }} <span>{{ tab.value === '' ? notices.length : statusCount(tab.value) }}</span>
          </button>
        </div>

        <div v-if="!loading && !pagedNotices.length" class="empty">등록된 공지사항이 없습니다.</div>
        <table v-else>
          <thead><tr><th>공지</th><th>상태</th><th>작성자</th><th>작성일</th><th>처리</th></tr></thead>
          <tbody>
            <tr v-for="notice in pagedNotices" :key="notice.noticeId">
              <td>
                <div class="entity-cell">
                  <img v-if="notice.imageUrl" :src="notice.imageUrl" alt="">
                  <span v-else class="thumb-placeholder">{{ notice.title.slice(0, 1) }}</span>
                  <strong>{{ notice.title }}</strong>
                </div>
              </td>
              <td><span class="status" :class="notice.status">{{ statusLabel(notice.status) }}</span></td>
              <td>{{ notice.authorName || '-' }}</td>
              <td>{{ formatDate(notice.createdAt) }}</td>
              <td><button class="edit" type="button" @click="startEdit(notice)">수정</button></td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-foot">
          <span>전체 {{ filteredNotices.length }}개 중 {{ pageStart }}-{{ pageEnd }} 표시</span>
          <AdminPagination v-model="page" :total="filteredNotices.length" :page-size="pageSize" />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../../api/hq'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminPagination from '../../components/admin/AdminPagination.vue'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024

const notices = ref([])
const loading = ref(true)

const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 6

const statusTabs = [
  { label: '전체', value: '' }, { label: '게시중', value: 'PUBLISHED' },
  { label: '임시저장', value: 'DRAFT' }, { label: '숨김', value: 'HIDDEN' }
]

function emptyForm() {
  return { title: '', content: '', imageUrl: '', status: 'DRAFT' }
}

const form = reactive(emptyForm())
const editingId = ref(null)
const saving = ref(false)
const formError = ref('')
const uploading = ref(false)

onMounted(loadNotices)

async function loadNotices() {
  loading.value = true
  try {
    notices.value = (await http.get('/hq/notices')).data
  } catch (e) {
    formError.value = e.response?.data?.message || '공지사항 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function handleImageUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (file.size > MAX_IMAGE_BYTES) {
    formError.value = '이미지는 5MB 이하만 업로드할 수 있습니다.'
    return
  }

  uploading.value = true
  try {
    const body = new FormData()
    body.append('file', file)
    const { data } = await http.post('/hq/uploads', body)
    form.imageUrl = data.url
  } catch (e) {
    formError.value = e.response?.data?.message || '이미지 업로드에 실패했습니다.'
  } finally {
    uploading.value = false
  }
}

async function submitNotice() {
  saving.value = true
  formError.value = ''
  try {
    if (editingId.value) {
      const { data } = await http.put(`/hq/notices/${editingId.value}`, form)
      const index = notices.value.findIndex(n => n.noticeId === editingId.value)
      if (index !== -1) notices.value[index] = data
    } else {
      const { data } = await http.post('/hq/notices', form)
      notices.value.unshift(data)
    }
    cancelEdit()
  } catch (e) {
    formError.value = e.response?.data?.message || '공지사항을 저장하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

function startEdit(notice) {
  editingId.value = notice.noticeId
  Object.assign(form, {
    title: notice.title, content: notice.content,
    imageUrl: notice.imageUrl || '', status: notice.status
  })
  formError.value = ''
}
function cancelEdit() {
  editingId.value = null
  Object.assign(form, emptyForm())
}

function statusCount(status) {
  return notices.value.filter(n => n.status === status).length
}
function statusLabel(status) {
  return { DRAFT: '임시저장', PUBLISHED: '게시중', HIDDEN: '숨김' }[status] || status
}
function formatDate(value) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

const filteredNotices = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return notices.value.filter(n =>
    (!statusFilter.value || n.status === statusFilter.value) &&
    (!word || n.title.toLowerCase().includes(word))
  )
})
const pageStart = computed(() => filteredNotices.value.length ? (page.value - 1) * pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(page.value * pageSize, filteredNotices.value.length))
const pagedNotices = computed(() => filteredNotices.value.slice((page.value - 1) * pageSize, page.value * pageSize))
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.summary{display:grid;grid-template-columns:repeat(4,minmax(140px,1fr));gap:14px;margin:22px 0 20px}
.form-card{margin-bottom:20px;padding:20px 22px;background:#fff;border:1px solid #e4e8ef;border-radius:14px}
.form-card h2{margin:0 0 12px;font-size:14px}
.entity-form{display:flex;gap:18px}
.image-field{display:grid;gap:8px;flex:0 0 180px}
.image-upload{position:relative;display:grid;place-items:center;width:180px;height:150px;overflow:hidden;padding:10px;color:#9aa2ad;border:1px dashed #d7dbe3;border-radius:12px;background:#fafbfd;font-size:10px;text-align:center;cursor:pointer}
.image-upload img{width:100%;height:100%;object-fit:cover;border-radius:9px}
.image-upload input{position:absolute;inset:0;opacity:0;cursor:pointer}
.uploading{color:#5960e9;font-size:10px;font-weight:700}
.fields{display:grid;flex:1;grid-template-columns:1fr 1fr;gap:8px}
.fields textarea{grid-column:1/-1;padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px;font-family:inherit;resize:vertical}
.fields input,.fields select{padding:11px 13px;border:1px solid #dfe3e9;border-radius:8px;font-size:12px}
.form-actions{display:flex;justify-content:flex-end;gap:8px;grid-column:1/-1}
.form-actions button{padding:11px 18px;color:#fff;border:0;background:#6266ef;border-radius:8px;font-weight:800;font-size:11px;cursor:pointer}
.form-actions button:disabled{opacity:.55}
.form-actions .cancel{color:#697487;background:#eef0f3}
.alert{margin-top:10px;padding:13px;color:#b52c48;background:#fff0f3;border:1px solid #ffd7df;border-radius:9px;font-size:11px}
.list-card{overflow:hidden;background:#fff;border:1px solid #e4e8ef;border-radius:16px}
.list-head{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;padding:19px 22px;border-bottom:1px solid #e9edf2}
.list-head h2{margin:0;font-size:15px}.list-head span{color:#8c95a2;font-size:10px}
.search{display:flex;align-items:center;gap:7px;width:200px;padding:0 10px;border:1px solid #dfe3e9;border-radius:8px}
.search input{width:100%;padding:9px 0;border:0;outline:0;font-size:11px}
.tabs{display:flex;gap:3px;padding:12px 22px 0}
.tabs button{padding:8px 11px;color:#697487;border:0;background:transparent;border-radius:8px;font-size:12px;font-weight:700;cursor:pointer}
.tabs button span{padding:2px 5px;background:#eef0f3;border-radius:6px;font-size:9px}
.tabs button.active{color:#5f63ee;background:#eef0ff}.tabs button.active span{background:#dbdefc}
table{width:100%;border-collapse:collapse;font-size:11px;margin-top:8px}
th{padding:12px 16px;color:#8c95a2;text-align:left;font-weight:800;border-bottom:1px solid #e9edf2}
td{padding:12px 16px;border-bottom:1px solid #f1f3f7;vertical-align:middle}
.entity-cell{display:flex;align-items:center;gap:10px}
.entity-cell img,.thumb-placeholder{width:36px;height:36px;border-radius:9px;object-fit:cover}
.thumb-placeholder{display:grid;place-items:center;color:#5f63ee;background:#eef0ff;font-weight:900}
.status{display:inline-block;padding:5px 8px;border-radius:6px;font-size:9px;font-weight:800}
.status.PUBLISHED{color:#0b9654;background:#e2f8ec}.status.DRAFT{color:#d57d00;background:#fff3d6}.status.HIDDEN{color:#c63750;background:#ffe8ed}
.edit{padding:7px 10px;color:#5960e9;border:1px solid #d9deea;background:#fff;border-radius:7px;font-size:9px;font-weight:800;cursor:pointer}
.empty{padding:50px;color:#929ba7;text-align:center;font-size:11px}
.pagination-foot{display:flex;align-items:center;justify-content:space-between;padding:8px 22px;border-top:1px solid #e9edf2}
.pagination-foot>span{color:#8c95a2;font-size:10px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}.summary{grid-template-columns:1fr 1fr}.entity-form{flex-direction:column}.image-field{flex:none;width:100%}.image-upload{width:100%}.fields{grid-template-columns:1fr}table{display:block;overflow-x:auto}}
</style>
