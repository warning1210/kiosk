<template>
  <div class="shell"><BranchSidebar active="dashboard" /><main>
    <header>
      <div><RouterLink to="/branch/dashboard" class="back-link">← 대시보드로</RouterLink><h1>{{ notice ? notice.title : loading ? '불러오는 중...' : '내용을 찾을 수 없습니다' }}</h1></div>
    </header>

    <section v-if="notice" class="detail-card">
      <div class="detail-meta">
        <b :class="notice.noticeType === 'EVENT' ? 'tag-event' : 'tag-notice'">{{ notice.noticeType === 'EVENT' ? 'EVENT' : 'NOTICE' }}</b>
        <span v-if="notice.noticeType === 'EVENT'">행사 기간 {{ formatDate(notice.postedAt) }} ~ {{ formatDate(notice.endAt) }}</span>
        <span v-else>게시일 {{ formatDate(notice.postedAt) }}</span>
      </div>
      <img v-if="notice.imageUrl" :src="notice.imageUrl" alt="" class="detail-image">
      <p class="detail-content">{{ notice.content || '등록된 내용이 없습니다.' }}</p>
    </section>

    <section v-else-if="!loading" class="detail-card empty-card">
      <p>이미 종료됐거나 존재하지 않는 공지/이벤트입니다.</p>
      <RouterLink to="/branch/dashboard" class="back-link">대시보드로 돌아가기</RouterLink>
    </section>
  </main></div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/branch'
import BranchSidebar from '../../components/branch/BranchSidebar.vue'

const route = useRoute()
const notice = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await http.get('/notices')
    notice.value = data.find(
      (item) => item.noticeType === route.params.type && String(item.id) === String(route.params.id)
    ) || null
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function formatDate(value) {
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
</script>

<style scoped>
.shell{min-height:100vh;color:#1f2938;background:#f3f6fa}main{margin-left:238px;padding:32px 34px 55px}
header h1{margin:10px 0 0;font-size:24px}
.back-link{color:#5d62ef;font-size:11px;font-weight:800;text-decoration:none}
.detail-card{margin-top:24px;padding:28px 32px;background:#fff;border:1px solid #e5e9ef;border-radius:16px;box-shadow:0 3px 8px rgb(34 48 68/3%)}
.detail-meta{display:flex;align-items:center;gap:12px;margin-bottom:20px}
.detail-meta span{color:#8c95a2;font-size:11px}
.tag-event{padding:6px 10px;color:#fff;background:#f2a300;border-radius:6px;font-size:10px;letter-spacing:.08em}
.tag-notice{padding:6px 10px;color:#fff;background:#686bf0;border-radius:6px;font-size:10px;letter-spacing:.08em}
.detail-image{width:100%;max-width:480px;border-radius:12px;margin-bottom:20px}
.detail-content{white-space:pre-wrap;color:#374151;font-size:14px;line-height:1.8}
.empty-card{display:grid;gap:14px;justify-items:center;padding:60px;color:#8c95a2;font-size:12px;text-align:center}
@media(max-width:760px){main{margin-left:0;padding:20px}}
</style>
