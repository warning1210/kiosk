<template>
  <main class="page">
    <AdminSidebar :active="route.meta.active" />

    <section class="content">
      <AdminPageHeader :title="route.meta.title" :subtitle="route.meta.subtitle" />

      <section class="coming-soon">
        <span class="badge">🚧</span>
        <h2>준비 중입니다</h2>
        <p>{{ route.meta.title }} 화면은 다음 단계에서 구현될 예정입니다.</p>
      </section>
    </section>
  </main>
</template>

<script setup>
import { useRoute } from 'vue-router'
import AdminSidebar from '../../components/admin/AdminSidebar.vue'
import AdminPageHeader from '../../components/admin/AdminPageHeader.vue'

const route = useRoute()
</script>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;color:#202938;background:#f3f6fa}.content{margin-left:238px;padding:38px 42px}
.coming-soon{display:grid;justify-items:center;gap:10px;padding:90px 20px;margin-top:24px;background:#fff;border:1px solid #e4e8ef;border-radius:16px;text-align:center}
.badge{display:grid;width:56px;height:56px;place-items:center;background:#eef0ff;border-radius:16px;font-size:24px}
.coming-soon h2{margin:6px 0 0;font-size:17px}
.coming-soon p{margin:0;color:#8c95a2;font-size:12px}
@media(max-width:980px){.content{margin-left:0;padding:25px 16px}}
</style>
