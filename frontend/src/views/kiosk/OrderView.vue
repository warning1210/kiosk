<template>
  <section class="order">
    <p>단계: {{ orderFlow.stepLabel }}</p>

    <OrderTypeStep v-if="orderFlow.step === 'orderType'" />
    <ProductStep v-else-if="orderFlow.step === 'product'" />
    <ContainerStep v-else-if="orderFlow.step === 'container'" />
    <FlavorStep v-else-if="orderFlow.step === 'flavor'" />
    <CartStep v-else-if="orderFlow.step === 'cart'" />
    <CustomerPaymentStep v-else-if="orderFlow.step === 'customer'" />
    <ReceiptStep v-else-if="orderFlow.step === 'receipt'" />
  </section>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useOrderFlowStore } from '../../stores/orderFlow'
import OrderTypeStep from './steps/OrderTypeStep.vue'
import ProductStep from './steps/ProductStep.vue'
import ContainerStep from './steps/ContainerStep.vue'
import FlavorStep from './steps/FlavorStep.vue'
import CartStep from './steps/CartStep.vue'
import CustomerPaymentStep from './steps/CustomerPaymentStep.vue'
import ReceiptStep from './steps/ReceiptStep.vue'

const orderFlow = useOrderFlowStore()

onMounted(() => {
  orderFlow.init()
})

onUnmounted(() => {
  orderFlow.stopPolling()
})
</script>
