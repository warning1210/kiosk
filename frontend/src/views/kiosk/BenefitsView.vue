<template>
  <main class="benefits-page">
    <header><span>STEP 4</span><h1>포인트 할인·적립</h1><p>해피포인트 회원이신가요?</p></header>
    <section class="benefit-grid">
      <button :class="{ active: cart.pointAction === 'EARN' }" @click="openPhone('EARN')"><b>HAPPY<br>POINT</b><strong>포인트 적립</strong><span>결제금액의 포인트 적립</span></button>
      <button :class="{ active: cart.pointAction === 'USE' }" @click="openPhone('USE')"><b>₩</b><strong>포인트 사용</strong><span>보유 포인트로 할인</span></button>
      <button :class="{ active: cart.pointAction === 'SKIP' }" @click="skip"><b>→</b><strong>건너뛰기</strong><span>할인·적립 없이 결제</span></button>
    </section>
    <section class="totals"><div><span>총 주문금액</span><b>{{ cart.totalPrice.toLocaleString() }}원</b></div><div><span>총 할인금액</span><b>-{{ cart.discountAmount.toLocaleString() }}원</b></div><div class="final"><span>최종 결제금액</span><strong>{{ cart.finalPrice.toLocaleString() }}원</strong></div></section>
    <footer><button @click="router.back()">이전</button><button class="primary" :disabled="!cart.pointAction" @click="next">다음 단계</button></footer>

    <div v-if="phoneModal" class="overlay"><div class="modal"><button class="close" @click="phoneModal=false">×</button><h2>휴대폰 번호를 입력해 주세요</h2><input v-model="phone" inputmode="numeric" maxlength="11" placeholder="01012345678"><p v-if="phoneError">휴대폰 번호 11자리를 입력해 주세요.</p><button class="primary wide" @click="confirmPhone">확인</button></div></div>
    <div v-if="pointModal" class="overlay"><div class="modal"><button class="close" @click="pointModal=false">×</button><h2>사용할 포인트를 입력해 주세요</h2><div class="available">보유 포인트 <b>4,500 P</b></div><input v-model.number="points" type="number" min="0" max="4500"><div class="quick"><button @click="addPoints(1000)">+1,000</button><button @click="addPoints(2000)">+2,000</button><button @click="points=4500">전액 사용</button></div><button class="primary wide" @click="usePoints">적용하기</button></div></div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
const route=useRoute(); const router=useRouter(); const cart=useCartStore(); const phoneModal=ref(false); const pointModal=ref(false); const intent=ref(''); const phone=ref(''); const phoneError=ref(false); const points=ref(0)
function openPhone(action){intent.value=action;phone.value='';phoneError.value=false;phoneModal.value=true}
function confirmPhone(){if(!/^01\d{8,9}$/.test(phone.value)){phoneError.value=true;return}phoneModal.value=false;if(intent.value==='USE'){pointModal.value=true}else{cart.setPointAction('EARN')}}
function addPoints(value){points.value=Math.min(4500,(Number(points.value)||0)+value)}
function usePoints(){cart.applyPoints(Math.min(4500,points.value));pointModal.value=false}
function skip(){cart.setPointAction('SKIP')}
function next(){router.push({name:'kiosk-payment',query:{orderType:route.query.orderType||'DINE_IN'}})}
</script>

<style scoped>
.benefits-page{min-height:100vh;padding:32px max(20px,calc((100vw - 900px)/2));color:#392f34;background:#fffafd}header{text-align:center}header>span{color:#ef3f91;font-weight:900}header h1{margin:7px}header p{color:#89737e}.benefit-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin:38px 0}.benefit-grid>button{display:grid;min-height:230px;padding:24px;place-content:center;justify-items:center;border:2px solid #eedde5;border-radius:24px;background:#fff;cursor:pointer}.benefit-grid>button.active{border-color:#ef3f91;box-shadow:0 10px 30px rgb(239 63 145/15%)}.benefit-grid b{display:grid;width:64px;height:64px;place-items:center;color:#ef3f91;background:#fff0f7;border-radius:18px}.benefit-grid strong{margin:17px 0 6px;font-size:20px}.benefit-grid span{color:#99828d;font-size:13px}.totals{padding:22px;background:#fff;border:1px solid #eee0e7;border-radius:20px}.totals div{display:flex;justify-content:space-between;padding:8px}.totals .final{margin-top:10px;padding-top:18px;border-top:1px solid #eadde3}.final strong{color:#ef3f91;font-size:22px}footer{display:grid;grid-template-columns:1fr 1.5fr;gap:12px;margin-top:25px}footer button,.wide{padding:18px;border:1px solid #ef3f91;border-radius:999px;color:#ef3f91;background:#fff;font-weight:800;cursor:pointer}.primary{color:#fff!important;background:#ef3f91!important}.primary:disabled{border-color:#ccc!important;background:#ccc!important}.overlay{position:fixed;inset:0;display:grid;place-items:center;padding:20px;background:rgb(40 32 36/55%)}.modal{position:relative;width:min(430px,100%);padding:34px;background:#fff;border-radius:24px}.close{position:absolute;top:14px;right:16px;border:0;background:#fce8f1;color:#ef3f91;border-radius:50%;font-size:22px}.modal input{width:100%;padding:16px;border:2px solid #efd8e3;border-radius:12px;font-size:20px}.modal p{color:#d73172}.available{display:flex;justify-content:space-between;margin:18px 0}.available b{color:#ef3f91}.quick{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin:12px 0 20px}.quick button{padding:10px;border:1px solid #efb6d0;color:#ef3f91;background:#fff;border-radius:8px}.wide{width:100%;border-radius:12px}@media(max-width:650px){.benefit-grid{grid-template-columns:1fr;}.benefit-grid>button{min-height:130px}}
</style>
