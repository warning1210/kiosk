<template>
  <router-view v-slot="{ Component, route }">
    <!-- 키오스크 주문·결제 화면은 실제 키오스크처럼 세로로 긴(9:16) 프레임 안에 담아 보여줍니다. -->
    <div v-if="isKiosk(route)" class="kiosk-stage">
      <div class="kiosk-frame">
        <component :is="Component" />
      </div>
    </div>
    <!-- 광고, 본점(/admin), 분점(/branch) 화면은 원래 100% 크기로 표시합니다. -->
    <component :is="Component" v-else />
  </router-view>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

// 세로형 키오스크 프레임의 논리 해상도(디자인 기준). 내용은 1024px 폭 기준으로 만들어져 있어
// 그 폭을 유지한 채 9:16 세로 비율(1024 x 1820)로 감싸고, 화면 크기에 맞춰 통째로 축소한다.
const FRAME_W = 1024
const FRAME_H = 1820

function isKiosk(route) {
  return (
    route.path.startsWith('/kiosk/') ||
    route.path.startsWith('/pay/') ||
    route.path.startsWith('/payment/')
  )
}

// 브라우저 창 크기에 맞춰 세로형 프레임이 항상 화면 안에 꽉 차도록 축소/확대 배율을 계산한다.
function fitKioskFrame() {
  const scale = Math.min(window.innerWidth / FRAME_W, window.innerHeight / FRAME_H)
  document.documentElement.style.setProperty('--kiosk-scale', String(scale))
}

onMounted(() => {
  fitKioskFrame()
  window.addEventListener('resize', fitKioskFrame)
})

onUnmounted(() => {
  window.removeEventListener('resize', fitKioskFrame)
})
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* 키오스크 화면을 감싸는 무대 - 세로 프레임 양옆의 남는 공간은 어둡게 처리해 실제 키오스크 느낌을 준다. */
.kiosk-stage {
  position: fixed;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background: #f7f0ed;
  overflow: hidden;
}

/* 세로로 긴 키오스크 프레임.
   transform이 걸려 있어 내부의 position:fixed 요소(하단 결제 바, 팝업 등)도 이 프레임 기준으로 자리잡는다. */
.kiosk-frame {
  flex: 0 0 auto;
  width: 1024px;
  height: 1820px;
  background: #fff;
  overflow: hidden;
  transform: scale(var(--kiosk-scale, 0.5));
  transform-origin: top center;
  display: flex;
  flex-direction: column;
}

/* 프레임 안의 화면(OrderView의 .order 래퍼, 각 화면의 .page)이 세로 공간을 꽉 채우도록 - 내용이 위쪽에만 몰리지 않게 한다. */
.kiosk-frame > *,
.kiosk-frame .order {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
}

.kiosk-frame .page {
  flex: 1 1 auto;
  min-height: 0;
}
</style>
