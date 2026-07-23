<template>
  <router-view v-slot="{ Component, route }">
    <!-- 키오스크 주문·결제 화면만 80% 배율을 사용합니다. -->
    <div
      v-if="route.path.startsWith('/kiosk/') || route.path.startsWith('/pay/') || route.path.startsWith('/payment/')"
      class="app-scale"
    >
      <component :is="Component" />
    </div>
    <!-- 광고, 본점(/admin), 분점(/branch) 화면은 원래 100% 크기로 표시합니다. -->
    <component :is="Component" v-else />
  </router-view>
</template>

<script setup>
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
}

.app-scale {
  width: 125%;
  min-height: 125vh;
  transform: scale(0.8);
  transform-origin: top left;
}
</style>
