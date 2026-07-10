<template>
  <section class="order">
    <h2>메뉴 선택</h2>
    <p v-if="loading">불러오는 중...</p>
    <p v-else-if="error">상품을 불러오지 못했습니다.</p>
    <ul v-else>
      <li v-for="product in products" :key="product.productId">
        {{ product.productName }} - {{ product.basePrice }}원
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../api/http'

const products = ref([])
const loading = ref(true)
const error = ref(false)

onMounted(async () => {
  try {
    const { data } = await http.get('/products')
    products.value = data
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
