<template>
  <main class="home">
    <div class="brand">baskin robbins</div>
    <p>달콤한 순간을 골라보세요</p>
    <RouterLink to="/kiosk/type">주문 시작하기</RouterLink>
  </main>
</template>

<style scoped>
.home { display: grid; min-height: 100vh; padding: 24px; place-content: center; justify-items: center; color: #3a3035; background: radial-gradient(circle at 50% 35%, #fff 0, #fff5fa 55%, #fce4f0 100%); }
.brand { color: #ef3f91; font-size: clamp(42px, 8vw, 76px); font-weight: 900; letter-spacing: -.06em; }
p { margin: 12px 0 40px; color: #7e6571; font-size: 18px; }
a { padding: 20px 54px; color: #fff; background: #ef3f91; border-radius: 18px; box-shadow: 0 14px 30px rgb(239 63 145 / 28%); font-size: 22px; font-weight: 800; text-decoration: none; }
</style>
