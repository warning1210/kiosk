<template>
  <main class="home" aria-label="키오스크 광고" @click="startOrder">
    <Transition name="ad-fade" mode="out-in">
      <img
        :key="currentAdIndex"
        class="advertisement"
        :src="advertisements[currentAdIndex].src"
        :alt="advertisements[currentAdIndex].alt"
      />
    </Transition>

    <div class="pagination" aria-hidden="true">
      <span
        v-for="(_, index) in advertisements"
        :key="index"
        :class="{ active: index === currentAdIndex }"
      />
    </div>

    <button type="button" class="branch-switch" @click.stop="goBranchLogin">
      지점 관리자로 전환
    </button>
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import ad1 from '../assets/kiosk/ads/ad-1.png'
import ad2 from '../assets/kiosk/ads/ad-2.png'
import ad3 from '../assets/kiosk/ads/ad-3.png'
import ad4 from '../assets/kiosk/ads/ad-4.png'

const ROTATION_INTERVAL_MS = 10_000
const advertisements = [
  { src: ad1, alt: '기다림 없이 바로 주문' },
  { src: ad2, alt: '봄날의 달콤함을 좋아하세요?' },
  { src: ad3, alt: '대한민국을 응원합니다. Go Korea!' },
  { src: ad4, alt: '함께 즐기는 패밀리팩' }
]

const router = useRouter()
const cart = useCartStore()
const currentAdIndex = ref(0)
let rotationTimer

onMounted(() => {
  rotationTimer = window.setInterval(() => {
    currentAdIndex.value = (currentAdIndex.value + 1) % advertisements.length
  }, ROTATION_INTERVAL_MS)
})

onUnmounted(() => {
  window.clearInterval(rotationTimer)
})

function startOrder() {
  cart.clear()
  router.push('/kiosk/order')
}

function goBranchLogin() {
  router.push('/branch/login')
}
</script>

<style scoped>
.home {
  position: relative;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  background: #f7f0ed;
  cursor: pointer;
}

.advertisement {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.ad-fade-enter-active,
.ad-fade-leave-active {
  transition: opacity 0.6s ease;
}

.ad-fade-enter-from,
.ad-fade-leave-to {
  opacity: 0;
}

.pagination {
  position: absolute;
  bottom: 82px;
  left: 50%;
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgb(0 0 0 / 20%);
  transform: translateX(-50%);
}

.pagination span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgb(255 255 255 / 55%);
  transition: width 0.25s ease, border-radius 0.25s ease, background 0.25s ease;
}

.pagination span.active {
  width: 24px;
  border-radius: 999px;
  background: #fff;
}

.branch-switch {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 9px 14px;
  color: rgb(255 255 255 / 85%);
  border: 1px solid rgb(255 255 255 / 35%);
  background: rgb(0 0 0 / 25%);
  border-radius: 999px;
  backdrop-filter: blur(8px);
  font-size: 12px;
  cursor: pointer;
}

@media (prefers-reduced-motion: reduce) {
  .ad-fade-enter-active,
  .ad-fade-leave-active {
    transition: none;
  }
}
</style>
