<template>
  <!-- 4단계: 컵/콘 선택 (컵/콘 둘 다 가능한 상품만 이 화면을 거침) -->
  <div>
    <h2>{{ orderFlow.selectedProduct.productName }} - 용기 선택</h2>
    <button type="button" @click="orderFlow.containerType = 'CUP'">
      컵{{ orderFlow.containerType === 'CUP' ? ' ✓' : '' }}
    </button>
    <button type="button" @click="orderFlow.containerType = 'CONE'">
      콘{{ orderFlow.containerType === 'CONE' ? ' ✓' : '' }}
    </button>

    <button type="button" @click="orderFlow.step = 'product'">뒤로</button>
    <button type="button" @click="orderFlow.proceedPastContainer">다음</button>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'

const orderFlow = useOrderFlowStore()
</script>
