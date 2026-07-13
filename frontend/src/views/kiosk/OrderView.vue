<template>
  <section class="order">
    <h2>메뉴 선택</h2>
    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error">상품을 불러오지 못했습니다.</p>
    <div v-else>
      <div v-for="category in categories" :key="category.categoryId" class="category-section">
        <h3>{{ category.categoryName }}</h3>
        <ul>
          <li v-for="product in category.products" :key="product.productId">
            {{ product.productName }} - {{ product.basePrice }}원
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../api/http'

const categories = ref([])
const loading = ref(true)
const error = ref(false)

onMounted(async () => {
  try {
    // 임시로 지점 ID 1번의 메뉴를 가져오도록 설정
    const { data } = await http.get('/kiosk/1/menus')
    categories.value = data
  } catch (e) {
    error.value = true
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.order {
  padding: 2rem;
}
</style>
