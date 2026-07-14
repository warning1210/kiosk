<template>
  <section class="order">
    <!-- CU-014: 홈버튼 - 클릭 시 초기 광고 화면으로 복귀 -->
    <button type="button" class="home-button" @click="goHome">🏠 처음으로</button>
    <p>단계: {{ orderFlow.stepLabel }}</p>

    <!-- 컴포넌트로 분리된 각 단계 렌더링 -->
    <OrderTypeStep v-if="orderFlow.step === 'orderType'" />
    <ProductStep v-else-if="orderFlow.step === 'product'" />
    <ContainerStep v-else-if="orderFlow.step === 'container'" />
    <FlavorStep v-else-if="orderFlow.step === 'flavor'" />
    <CartStep v-else-if="orderFlow.step === 'cart'" />
    <CustomerPaymentStep v-else-if="orderFlow.step === 'customer'" />
  </section>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderFlowStore } from '../../stores/orderFlow'
import { useCartStore } from '../../stores/cart'
import OrderTypeStep from './steps/OrderTypeStep.vue'
import ProductStep from './steps/ProductStep.vue'
import ContainerStep from './steps/ContainerStep.vue'
import FlavorStep from './steps/FlavorStep.vue'
import CartStep from './steps/CartStep.vue'
import CustomerPaymentStep from './steps/CustomerPaymentStep.vue'

const router = useRouter()
const orderFlow = useOrderFlowStore()
const cart = useCartStore()

onMounted(() => {
  orderFlow.init()
})

onUnmounted(() => {
  orderFlow.stopPolling()
})

// CU-014: 진행 중인 주문이 있으면 확인 후 초기 화면으로 복귀
function goHome() {
  if (cart.items.length > 0 && !confirm('진행 중인 주문을 취소하고 처음 화면으로 돌아가시겠습니까?')) return
  orderFlow.stopPolling()
  cart.clear()
  router.push('/')
}
</script>

<style scoped>
.order {
  padding: 2rem;
  padding-top: 4rem; /* 홈버튼 영역 확보 */
}

.home-button {
  position: fixed;
  top: 1rem;
  left: 1rem;
  z-index: 10;
  padding: 0.5rem 1rem;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.home-button:hover {
  background-color: #f5f5f5;
}
</style>