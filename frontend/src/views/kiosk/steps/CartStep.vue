<template>
  <!-- 6단계: 장바구니 확인 (CU-007) -->
  <div>
    <h2>장바구니 확인</h2>
    <p>
      {{ cart.orderType === 'DINE_IN' ? '매장' : '포장' }} · {{ cart.totalCount }}개 ·
      {{ cart.amountBeforeDiscount.toLocaleString() }}원
    </p>
    <ul>
      <li v-for="item in cart.items" :key="item.id">
        {{ item.productName }}
        <span v-if="item.flavors.length">({{ item.flavors.map((f) => f.flavorName).join(', ') }})</span>
        {{ (item.unitPrice * item.quantity).toLocaleString() }}원
        <button type="button" @click="orderFlow.editItem(item)">수정</button>
        <button type="button" @click="orderFlow.removeFromCart(item.id)">삭제</button>
      </li>
    </ul>
    <button type="button" @click="orderFlow.step = 'product'">메뉴 더 담기</button>
    <button type="button" @click="orderFlow.step = 'customer'">결제</button>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'
import { useCartStore } from '../../../stores/cart'

const orderFlow = useOrderFlowStore()
const cart = useCartStore()
</script>
