<template>
  <!-- 5단계: 맛 선택 (CU-006) -->
  <div>
    <h2>{{ orderFlow.selectedProduct.productName }}</h2>

    <div v-if="orderFlow.selectedProduct.requiresFlavorSelection">
      <!-- CU-006-2: 선택 진행률 표시 -->
      <p>
        맛 선택 ({{ orderFlow.selectedFlavorIds.length }} / {{ orderFlow.selectedProduct.selectableFlavorCount }})
        <span>- 같은 맛을 여러 번 선택할 수 있어요</span>
      </p>
      <ul class="flavor-grid">
        <li v-for="flavor in orderFlow.flavors" :key="flavor.flavorId">
          <button
            type="button"
            :disabled="!orderFlow.canPickMoreFlavor()"
            @click="orderFlow.toggleFlavor(flavor.flavorId)"
          >
            {{ flavor.flavorName }}
            <span v-if="orderFlow.flavorSelectedCount(flavor.flavorId) > 0">
              (x{{ orderFlow.flavorSelectedCount(flavor.flavorId) }})
            </span>
          </button>
        </li>
      </ul>
    </div>

    <div v-if="orderFlow.selectedProduct.isLarge">
      <label>
        숟가락 개수
        <input v-model.number="orderFlow.spoonCount" type="number" min="0" />
      </label>
      <label>
        드라이아이스 시간(분)
        <select v-model.number="orderFlow.dryIceMinutes">
          <option :value="null">사용 안함</option>
          <option :value="10">10분</option>
          <option :value="20">20분</option>
          <option :value="30">30분</option>
        </select>
      </label>
    </div>

    <button type="button" @click="orderFlow.step = 'product'">뒤로</button>
    <!-- CU-006-1: 조건 충족 전까지 담기 버튼 비활성화 -->
    <button type="button" :disabled="!orderFlow.canConfirmFlavor" @click="orderFlow.confirmAddToCart">
      {{ orderFlow.editingItemId ? '수정 완료' : '장바구니 담기' }}
    </button>

    <!-- 담은 맛을 화면 하단에 실시간 표시 -->
    <footer v-if="orderFlow.selectedFlavorSummary.length" class="flavor-summary-bar">
      <p>선택한 맛</p>
      <ul>
        <li v-for="entry in orderFlow.selectedFlavorSummary" :key="entry.flavorId">
          {{ entry.flavorName }} x{{ entry.count }}
          <button type="button" @click="orderFlow.removeOneFlavor(entry.flavorId)">−</button>
        </li>
      </ul>
    </footer>
  </div>
</template>

<script setup>
import { useOrderFlowStore } from '../../../stores/orderFlow'

const orderFlow = useOrderFlowStore()
</script>

<style scoped>
.flavor-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.5rem;
  list-style: none;
  margin: 0 0 1rem 0;
  padding: 0;
}

.flavor-grid button {
  width: 100%;
}

.flavor-summary-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fafafa;
  border-top: 1px solid #ddd;
  padding: 0.75rem 1.5rem;
}

.flavor-summary-bar ul {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.flavor-summary-bar li {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
}
</style>
