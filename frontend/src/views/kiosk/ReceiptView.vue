<template>
  <main class="receipt-page">
    <section class="complete screen-only">
      <span class="check">✓</span>
      <p>{{ creating ? '주문을 접수하고 있습니다' : error ? '주문 접수에 실패했습니다' : '결제가 완료되었습니다' }}</p>
      <h1>대기번호</h1><strong>{{ creating ? '···' : waitingNumber }}</strong>
      <small>{{ error || '상품이 준비되면 번호를 불러드릴게요.' }}</small>
      <button v-if="error" class="retry" @click="createOrder">다시 접수</button>
    </section>

    <section class="print-panel screen-only">
      <div><h2>영수증을 출력하시겠어요?</h2><p>C:\ReceiptService 감열 프린터 또는 PDF로 출력할 수 있습니다.</p></div>
      <div class="print-actions">
        <button type="button" :disabled="creating || !!error || thermalPrinting" @click="printThermal">
          <b>▤</b><span>{{ thermalPrinting ? '전송 중...' : '감열 프린터 출력' }}</span><small>localhost:8888</small>
        </button>
        <button type="button" :disabled="creating || !!error" @click="printPdf">
          <b>PDF</b><span>PDF / 인쇄 미리보기</span><small>브라우저 인쇄 기능</small>
        </button>
      </div>
      <p v-if="printMessage" class="print-message" :class="{ fail: printFailed }">{{ printMessage }}</p>
      <button class="finish" type="button" @click="finish">완료하고 처음으로</button>
    </section>

    <section class="paper" aria-label="주문 영수증">
      <header class="receipt-brand"><strong>baskin<span>BR</span>robbins</strong><small>www.baskinrobbins.co.kr</small></header>
      <div class="waiting"><span>주문번호(일반주문)</span><b>{{ waitingNumber }}</b></div>
      <div class="meta">[정상] {{ receiptDate }}　POS:08(강남점)</div>
      <div class="rule"></div>
      <div class="columns"><b>제품명<br><small>(할인내역)</small></b><b>수량</b><b>금액<br><small>(할인금액)</small></b></div>
      <div class="rule thin"></div>
      <div v-for="(item,index) in cart.items" :key="item.cartItemId" class="receipt-item">
        <span class="number">{{ String(index + 1).padStart(3,'0') }}</span>
        <div><strong>(컵) {{ item.productName }}</strong><small v-for="flavor in item.flavors" :key="flavor.flavorId">&gt; {{ flavor.flavorName }}</small></div>
        <span>1</span><b>{{ item.basePrice.toLocaleString() }}</b>
      </div>
      <div v-if="cart.discountAmount" class="discount"><span>할인</span><b>-{{ cart.discountAmount.toLocaleString() }}</b></div>
      <div class="rule"></div>
      <div class="total"><span>결제금액</span><strong>{{ cart.finalPrice.toLocaleString() }}원</strong></div>
      <footer>We make people happy</footer>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import http from '../../api/http'

const route=useRoute(),router=useRouter(),cart=useCartStore()
const waitingNumber=ref('-'),orderNumber=ref(''),creating=ref(true),error=ref('')
const thermalPrinting=ref(false),printMessage=ref(''),printFailed=ref(false)
const receiptDate=new Date().toLocaleString('sv-SE').replace('T',' ')
let created=false

async function createOrder(){
  if(created)return;creating.value=true;error.value=''
  try{
    const{data}=await http.post('/kiosk/orders',{branchId:null,kioskCode:'KIOSK-01',orderType:route.query.orderType==='TAKEOUT'?'TAKEOUT':'DINE_IN',paymentMethod:route.query.method||'CARD',discountAmount:cart.discountAmount,items:cart.items.map(item=>({productId:item.productId,quantity:1,flavorIds:item.flavors.map(f=>f.flavorId)}))})
    waitingNumber.value=data.waitingNumber;orderNumber.value=data.orderNumber;created=true
  }catch(e){console.error(e);error.value=e.response?.data?.message||'서버 연결을 확인한 뒤 다시 시도해 주세요.'}
  finally{creating.value=false}
}

async function printThermal(){
  thermalPrinting.value=true;printMessage.value='';printFailed.value=false
  const orderItem=cart.items.map((item,index)=>`${String(index+1).padStart(3,'0')} (컵)${item.productName} 1 ${item.basePrice.toLocaleString()}원\n${item.flavors.map(f=>` >${f.flavorName}`).join(', ')}`).join('\n')
  try{
    await fetch('http://localhost:8888/receipt',{method:'POST',mode:'no-cors',headers:{'Content-Type':'text/plain;charset=UTF-8'},body:JSON.stringify({orderNo:String(waitingNumber.value),orderItem,price:`${cart.finalPrice.toLocaleString()}원`,orderDate:receiptDate})})
    printMessage.value='감열 프린터로 영수증을 전송했습니다.'
  }catch(e){console.error(e);printFailed.value=true;printMessage.value='출력 서비스를 찾을 수 없습니다. C:\\ReceiptService\\install.bat을 관리자 권한으로 실행해 주세요.'}
  finally{thermalPrinting.value=false}
}
function printPdf(){window.print()}
function finish(){cart.clear();router.replace('/')}
onMounted(createOrder)
</script>

<style scoped>
.receipt-page{display:grid;min-height:100vh;padding:36px 20px 60px;grid-template-columns:minmax(280px,420px) 300px;gap:28px;place-content:center;align-items:center;color:#342e31;background:linear-gradient(145deg,#fff 0,#fff2f8 56%,#f4f3ff 100%)}
.complete{text-align:center}.check{display:grid;width:68px;height:68px;margin:auto;place-items:center;color:#fff;background:#ef3f91;border-radius:50%;font-size:38px}.complete p{margin:13px 0 0;color:#8c7580}.complete h1{margin:20px 0 0;font-size:18px}.complete>strong{display:block;color:#ef3f91;font-size:88px;line-height:1.05}.complete small{color:#8e7a83}.retry{margin-top:16px;padding:10px 18px;color:#fff;border:0;background:#ef3f91;border-radius:10px;font-weight:800}
.print-panel{grid-column:1;padding:25px;background:#fff;border:1px solid #f0dce6;border-radius:22px;box-shadow:0 15px 40px rgb(95 48 70/10%)}.print-panel h2{margin:0;font-size:20px}.print-panel>div>p{margin:7px 0 20px;color:#8f7c85;font-size:12px}.print-actions{display:grid;grid-template-columns:1fr 1fr;gap:10px}.print-actions button{display:grid;min-height:105px;padding:14px;place-content:center;justify-items:center;color:#5d5056;border:1px solid #ead7e0;background:#fff;border-radius:14px}.print-actions button:first-child{color:#fff;border-color:#ef3f91;background:#ef3f91}.print-actions button:disabled{opacity:.5}.print-actions b{font-size:18px}.print-actions span{margin:7px 0 3px;font-size:12px;font-weight:900}.print-actions small{font-size:9px;opacity:.72}.print-message{padding:10px;color:#187d4a;background:#e9f8f0;border-radius:9px;font-size:10px}.print-message.fail{color:#b83243;background:#ffeaed}.finish{width:100%;margin-top:11px;padding:12px;color:#83717a;border:1px solid #e5d7de;background:#fff;border-radius:10px;font-weight:800}
.paper{grid-column:2;grid-row:1/span 2;width:80mm;min-height:150mm;padding:9mm 6mm 8mm;color:#171717;background:#fff;box-shadow:0 18px 55px rgb(54 35 44/20%);font-family:'Malgun Gothic',sans-serif;font-size:10px}.receipt-brand{text-align:center}.receipt-brand strong{display:block;font-family:Arial,sans-serif;font-size:22px;letter-spacing:-2px}.receipt-brand strong span{font-size:29px;letter-spacing:-4px}.receipt-brand small{font-size:7px}.waiting{display:grid;margin:5mm 0 3mm;text-align:center}.waiting span{font-size:15px}.waiting b{font-size:35px;line-height:1}.meta{padding:3mm 0;font-size:9px}.rule{height:1px;margin:1.5mm 0;background:#222}.rule.thin{background:#777}.columns{display:grid;grid-template-columns:1fr 24px 54px;gap:4px;align-items:end}.columns b:nth-child(n+2){text-align:right}.columns small{font-weight:400}.receipt-item{display:grid;grid-template-columns:22px 1fr 18px 49px;gap:3px;padding:2mm 0}.receipt-item>div strong,.receipt-item>div small{display:block}.receipt-item>div small{margin-top:1mm;font-size:8px}.receipt-item>span:nth-last-child(2),.receipt-item>b{text-align:right}.discount{display:flex;justify-content:space-between;padding:1mm 0}.total{display:flex;justify-content:space-between;padding:2mm 0;font-size:12px}.barcode{height:14mm;margin:4mm 8mm 0;background:repeating-linear-gradient(90deg,#111 0 1px,transparent 1px 3px,#111 3px 5px,transparent 5px 6px,#111 6px 7px,transparent 7px 10px)}.barcode-number{text-align:center;letter-spacing:3px;font-size:8px}.paper footer{margin-top:2mm;text-align:center;font-family:Georgia,serif;font-size:17px;font-weight:800;font-style:italic}
@media(max-width:760px){.receipt-page{grid-template-columns:1fr}.paper{grid-column:1;grid-row:auto;margin:auto}.print-panel{grid-column:1}}
@media print{.screen-only{display:none!important}:global(body){margin:0;background:#fff}.receipt-page{display:block;min-height:0;padding:0;background:#fff}.paper{width:72mm;min-height:0;margin:0;padding:4mm;box-shadow:none}@page{size:80mm auto;margin:0}}
</style>
