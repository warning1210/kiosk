<template>
  <main class="qr-page">
    <header><b>toss</b><h1>QR 코드를 스캔해 주세요</h1><p>토스 앱에서 스캔하면 결제가 진행됩니다.</p></header>
    <section class="qr-card">
      <div class="qr" aria-label="데모용 QR 코드"><i v-for="(cell,index) in qrCells" :key="index" :class="{on:cell}"></i></div>
      <strong>{{ cart.finalPrice.toLocaleString() }}원</strong>
      <div class="timer" :class="{urgent:remaining<=10}"><span :style="{width:`${remaining/60*100}%`}"></span></div>
      <p>남은 시간 <b>{{ remaining }}초</b></p>
      <small>현재 화면은 개발용 결제 시뮬레이션입니다.</small>
    </section>
    <div class="actions"><button @click="cancel">취소</button><button class="success" @click="success">결제 완료 시뮬레이션</button></div>
  </main>
</template>
<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
const route=useRoute();const router=useRouter();const cart=useCartStore();const remaining=ref(60)
const qrCells=Array.from({length:441},(_,i)=>{const x=i%21,y=Math.floor(i/21);const finder=(x<7&&y<7)||(x>13&&y<7)||(x<7&&y>13);return finder?((x%6===0)||(y%6===0)||(x%6>=2&&x%6<=4&&y%6>=2&&y%6<=4)):((x*3+y*5+x*y)%7<3)})
const timer=setInterval(()=>{remaining.value-=1;if(remaining.value<=0){clearInterval(timer);router.replace({name:'kiosk-payment',query:{timeout:'1'}})}},1000)
onBeforeUnmount(()=>clearInterval(timer))
function cancel(){router.replace({name:'kiosk-payment'})}
function success(){router.replace({name:'kiosk-receipt',query:{method:'TOSS',orderType:route.query.orderType||'DINE_IN'}})}
</script>
<style scoped>
.qr-page{display:grid;min-height:100vh;padding:36px 20px;place-content:center;justify-items:center;color:#342e32;background:#f7f9ff}header{text-align:center}header>b{color:#1261ff;font-size:32px;font-style:italic}h1{margin:10px}header p{color:#7d7b85}.qr-card{width:min(390px,92vw);padding:30px;text-align:center;background:#fff;border-radius:28px;box-shadow:0 18px 50px rgb(42 66 120/15%)}.qr{display:grid;width:230px;height:230px;margin:auto;padding:12px;grid-template-columns:repeat(21,1fr);grid-template-rows:repeat(21,1fr);background:#fff;border:1px solid #ddd}.qr i{background:#fff}.qr i.on{background:#111}.qr-card>strong{display:block;margin:22px;color:#1261ff;font-size:25px}.timer{height:8px;overflow:hidden;background:#e9eefa;border-radius:99px}.timer span{display:block;height:100%;background:#1261ff;transition:width 1s linear}.timer.urgent span{background:#ef3f91}.qr-card p{margin-bottom:5px}.qr-card small{color:#a2a0a7}.actions{display:grid;width:min(390px,92vw);grid-template-columns:1fr 1.5fr;gap:10px;margin-top:18px}.actions button{padding:16px;border:1px solid #1261ff;color:#1261ff;background:#fff;border-radius:14px;font-weight:800}.actions .success{color:#fff;background:#1261ff}
</style>
