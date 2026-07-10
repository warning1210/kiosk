# ⚡ 5분 안에 이해하는 QR 결제 시스템

## 🎯 한 문장 요약
"사용자가 주문 완료하면, QR 코드가 나타나고, 휴대폰으로 스캔하면 결제 페이지로 이동"

---

## 🔄 4단계 프로세스

### 1️⃣ **사용자가 주문 (프론트엔드)**
```
아이스크림 선택 → 맛 선택 → 장바구니 담기 → "결제 완료" 버튼 클릭
```

### 2️⃣ **주문 저장 (백엔드)**
```
주문 정보를 데이터베이스에 저장
┌─────────────┐
│ orders      │  ← 주문 정보
│ order_items │  ← 주문한 상품들
│ flavors     │  ← 맛 정보
│ payments    │  ← 결제 정보
└─────────────┘
```

### 3️⃣ **QR 코드 생성 (백엔드 + 프론트엔드)**
```
백엔드: 토스페이먼츠 결제 URL 생성
       ↓
프론트엔드: 그 URL로 QR 이미지 생성
       ↓
화면에 QR 이미지 표시
```

### 4️⃣ **사용자가 QR 스캔 (휴대폰)**
```
휴대폰 카메라로 QR 스캔 → 토스페이먼츠 결제 페이지 자동 열기 → 결제 완료
```

---

## 📂 추가된 파일 5개

| # | 파일 이름 | 역할 | 위치 |
|---|---------|------|------|
| 1 | `PaymentService.java` | QR 정보 생성 | backend/service/ |
| 2 | `PaymentController.java` | API 엔드포인트 | backend/controller/ |
| 3 | `PaymentQrRequest.java` | 요청 데이터 형식 | backend/dto/ |
| 4 | `PaymentQrResponse.java` | 응답 데이터 형식 | backend/dto/ |
| 5 | `App.vue 수정` | QR 모달 추가 | frontend/src/ |

---

## 🔗 중요한 API 2개

### API 1: QR 코드 생성
```
요청:  POST /api/payments/qr
입력:  { "orderId": 1, "amount": 25000 }
출력:  {
  "qrCode": "base64 인코딩된 이미지",
  "checkoutUrl": "https://...결제링크...",
  "amount": 25000,
  "expiresAt": "2026-07-10T10:35:00"
}
```

### API 2: 결제 확인
```
요청:  GET /api/payments/confirm?paymentKey=...&orderId=...&amount=...
출력:  { "status": "DONE" }  // 결제 완료
```

---

## 💡 프론트엔드 변경사항

### 추가된 함수
```javascript
// 1. 이 함수가 자동으로 호출됨 (checkout 후)
async function generatePaymentQr(orderId, amount) {
  // a. 백엔드 API 호출
  const qrData = await fetch('/api/payments/qr', {...});
  
  // b. 받은 URL로 QR 이미지 생성
  const qrImage = await QRCode.toDataURL(qrData.checkoutUrl);
  
  // c. 화면에 표시
  showPaymentModal.value = true;
}
```

### 추가된 HTML (모달)
```html
<!-- QR 코드를 보여주는 팝업 -->
<div v-if="showPaymentModal" class="modal">
  <img :src="paymentQr.qrCode" />  <!-- QR 이미지 -->
  <p>₩{{ paymentQr.amount }}</p>   <!-- 결제 금액 -->
  <a :href="paymentQr.checkoutUrl">
    토스페이먼츠로 이동
  </a>
</div>
```

---

## 🖥️ 백엔드 변경사항

### PaymentController.java
```java
@PostMapping("/qr")
public ResponseEntity<PaymentQrResponse> generateQrCode(
        @RequestBody PaymentQrRequest request) {
    
    // 백엔드 → 프론트엔드 반환
    return ResponseEntity.ok(
        paymentService.generateQrCode(request.orderId(), request.amount())
    );
}
```

### PaymentService.java
```java
public PaymentQrResponse generateQrCode(Long orderId, Integer amount) {
    // 1. 고유한 주문 ID 생성
    String orderId_str = "ORD-" + orderId + "-" + System.currentTimeMillis();
    
    // 2. 결제 URL 생성 (토스페이먼츠)
    String checkoutUrl = String.format(
        "https://sandbox-payment.tosspayments.com/login?orderId=%s&amount=%d",
        orderId_str, amount
    );
    
    // 3. 정보 반환
    return new PaymentQrResponse(..., checkoutUrl, ...);
}
```

---

## 📊 데이터 흐름

```
사용자 액션                    프론트엔드              백엔드
   │                           │                      │
   ├─ 상품 선택                 │                      │
   ├─ 맛 선택                   │                      │
   ├─ 장바구니 담기             │                      │
   │                           │                      │
   └─ "결제 완료" 클릭          │                      │
                               │                      │
                               ├─ POST /orders/checkout ──→
                               │                      │
                               │              DB에 저장 (4개 테이블)
                               │                      │
                               │                      ├─ orders
                               │                      ├─ order_items
                               │                      ├─ flavors
                               │                      └─ payments
                               │                      │
                               │      ← 주문 정보 반환  │
                               │                      │
                               ├─ POST /payments/qr ──→
                               │                      │
                               │              토스 URL 생성
                               │                      │
                               │      ← QR 정보 반환   │
                               │                      │
                               ├─ QR 이미지 생성
                               │  (프론트엔드에서)
                               │
                               └─ QR 모달 표시
                                  
   └─ QR 스캔                   
      (휴대폰)  ─────────────→ 토스페이먼츠 결제 페이지
```

---

## 🧪 테스트하는 방법

### 1. 백엔드 실행 확인
```
http://localhost:18080/api/orders
→ 주문 목록이 나오면 ✅
```

### 2. 프론트엔드 실행 확인
```
http://localhost:5174/
→ 쿠키 주문 페이지가 나오면 ✅
```

### 3. 결제 테스트
1. 상품 선택
2. "결제 완료" 버튼 클릭
3. QR 코드가 나타나면 ✅
4. "토스페이먼츠로 이동" 클릭
5. 결제 페이지로 이동하면 ✅

---

## ⚙️ 설정값 (백엔드)

`application.properties` 파일에서:

```properties
# 토스페이먼츠 설정
toss.payments.api.url=https://api.tosspayments.com

# 테스트 환경 (실제 돈 안 나감)
toss.payments.client.key=test_ck_integration
toss.payments.secret.key=test_sk_integration_dummy_key...

# QR 코드 유효 시간 (5분 = 300초)
toss.payments.qr.timeout=300
```

---

## 🎓 초보자 팁

### 왜 두 곳에서 코드를 수정해야 할까?

**프론트엔드** (Frontend): 사용자가 보는 화면
- HTML, CSS, JavaScript
- 버튼, 입력창, 이미지 등

**백엔드** (Backend): 서버에서 데이터 처리
- Java, Spring Boot
- 데이터베이스 저장, API 제공

**둘이 협력해야 함**:
```
프론트: "결제해줘!" 
        ↓
백엔드: "알겠어. 주문 저장하고 QR 만들어줄게"
        ↓
프론트: "QR 이미지 생성해서 화면에 띄울게"
```

---

## 📚 새로운 단어들

| 단어 | 의미 |
|------|------|
| **API** | 두 프로그램이 대화하는 방법 |
| **POST** | 서버에 데이터 보내기 |
| **GET** | 서버에서 데이터 받기 |
| **JSON** | 데이터를 텍스트로 표현하는 형식 |
| **QR 코드** | 카메라로 스캔하는 이미지 |
| **Base64** | 이미지를 텍스트로 변환하는 방식 |

---

## ✅ 완성된 기능

- [x] 상품 주문
- [x] 장바구니
- [x] 주문 저장 (DB)
- [x] **QR 코드 생성** ← 새로 추가!
- [x] **QR 모달 표시** ← 새로 추가!
- [x] **토스페이먼츠 연동** ← 새로 추가!

---

**Tip**: 완전한 설명은 `GUIDE_QR_PAYMENT.md` 파일을 읽으세요!
