<template>
  <router-view v-slot="{ Component, route }">
    <component :is="Component" v-if="route.path === '/'" />
    <div v-else class="app-scale">
      <component :is="Component" />
    </div>
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
