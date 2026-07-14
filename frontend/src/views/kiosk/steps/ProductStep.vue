<template>
  <!-- 2단계: 카테고리 탭 + 상품 목록 (탭 클릭 시 메인 목록만 바뀜) -->
  <div>
    <!-- 상단 토글 탭 -->
    <nav class="category-tabs">
      <button
        v-for="category in orderFlow.categories"
        :key="category.categoryId"
        type="button"
        :class="{ active: orderFlow.selectedCategory?.categoryId === category.categoryId }"
        @click="orderFlow.selectedCategory = category"
      >
        {{ category.categoryName }}
      </button>
    </nav>

    <h2>{{ orderFlow.selectedCategory?.categoryName }} 메뉴 선택</h2>  
    <p v-if="orderFlow.loading">불러오는 중...</p>
    <p v-else-if="orderFlow.loadError">상품을 불러오지 못했습니다.</p>
    <ul v-else>
      <li v-for="product in orderFlow.visibleProducts" :key="product.productId">
        <button type="button" @click="orderFlow.selectProduct(product)">
          {{ product.productName }} - {{ product.basePrice.toLocaleString() }}원
        </button>
      </li>
    </ul>
    <button v-if="cart.items.length" type="button" @click="orderFlow.step = 'cart'">
      장바구니 확인 ({{ cart.totalCount }})
    </button>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
</script>

<style scoped>
.category-tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}

button.active {
  background: #333;
  color: #fff;
}
</style>
