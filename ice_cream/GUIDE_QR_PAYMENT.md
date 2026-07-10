# 🍪 쿠키 주문 키오스크 - 토스페이먼츠 QR 결제 시스템 설명서

이 문서는 초보 개발자도 이해할 수 있도록 우리 프로젝트의 QR 결제 기능을 설명합니다.

---

## 📋 목차
1. [시스템 전체 구조](#시스템-전체-구조)
2. [각 파일의 역할](#각-파일의-역할)
3. [동작 흐름](#동작-흐름)
4. [코드 설명](#코드-설명)
5. [토스페이먼츠란?](#토스페이먼츠란)

---

## 🏗️ 시스템 전체 구조

```
사용자가 브라우저에서 주문
        ↓
프론트엔드 (Vue.js) - 상품 선택, 장바구니
        ↓
"결제 완료" 버튼 클릭
        ↓
백엔드 (Spring Boot) - 주문 저장, QR 생성 API
        ↓
프론트엔드 - QR 코드 이미지 표시
        ↓
사용자 휴대폰으로 QR 스캔
        ↓
토스페이먼츠 결제 페이지 열기
        ↓
결제 완료!
```

---

## 📁 각 파일의 역할

### 🎨 프론트엔드 (Frontend) - 사용자가 보는 화면

#### 1. `frontend/src/App.vue`
- **역할**: 전체 UI 화면 구성
- **포함된 기능**:
  - 상품 선택 화면
  - 맛 선택 화면
  - 장바구니 화면
  - **새로 추가: QR 코드 결제 모달**

**수정한 부분**:
```javascript
// 1. QR 코드 상태 추가
const paymentQr = ref(null);           // QR 코드 데이터 저장
const showPaymentModal = ref(false);   // QR 모달 보이기/숨기기

// 2. checkout() 함수에서 주문 후 QR 생성
async function checkout() {
  // ... 주문 저장 ...
  await generatePaymentQr(lastOrder.value.id, lastOrder.value.finalAmount);
}

// 3. 새로운 함수: QR 코드 생성
async function generatePaymentQr(orderId, amount) {
  // 백엔드에 QR 생성 요청
  // 받은 URL로 QR 이미지 생성
  // 모달에 표시
}
```

#### 2. `frontend/src/styles.css`
- **역할**: 화면 디자인 (CSS)
- **수정한 부분**: QR 모달 스타일 추가
  - `.modal-overlay` - 어두운 배경
  - `.modal-content` - 흰색 박스
  - `.qr-code` - QR 이미지 스타일
  - `.payment-link-btn` - 결제 버튼

#### 3. `frontend/package.json`
- **역할**: 사용하는 라이브러리 목록
- **수정한 부분**: `"qrcode": "^1.5.3"` 추가
  - QR 코드 이미지를 생성하는 라이브러리

---

### 🖥️ 백엔드 (Backend) - 데이터 처리 및 API

#### 1. `backend/src/main/java/com/example/kiosksim/service/PaymentService.java`
- **역할**: 결제 관련 로직 처리
- **포함된 함수**:

```java
// 함수 1: QR 코드 생성
public PaymentQrResponse generateQrCode(Long orderId, Integer amount)
- 입력: 주문번호, 금액
- 출력: QR 코드 정보 (URL, 만료시간 등)
- 역할: 토스페이먼츠 결제 URL 생성

// 함수 2: 결제 확인
public PaymentQrResponse confirmPayment(String paymentKey, String orderId, String amount)
- 입력: 결제키, 주문번호, 금액
- 출력: 결제 상태
- 역할: 토스페이먼츠에 결제 완료 확인
```

#### 2. `backend/src/main/java/com/example/kiosksim/controller/PaymentController.java`
- **역할**: API 엔드포인트 (URL 경로)
- **API 목록**:

```
POST /api/payments/qr
- 요청: orderId, amount
- 응답: QR 코드 정보

GET /api/payments/confirm
- 요청: paymentKey, orderId, amount
- 응답: 결제 완료 여부
```

#### 3. `backend/pom.xml`
- **역할**: 사용하는 라이브러리 목록 (Maven)
- **추가한 라이브러리**:
  - `gson` - JSON 데이터 처리
  - `httpclient5` - 외부 API 호출
  - `commons-codec` - Base64 인코딩

#### 4. `backend/src/main/resources/application.properties`
- **역할**: 설정 파일
- **추가한 설정**:
```properties
toss.payments.api.url=https://api.tosspayments.com
toss.payments.client.key=test_ck_integration
toss.payments.secret.key=test_sk_integration_dummy_key_do_not_use_in_production
toss.payments.qr.timeout=300
```

#### 5. `backend/src/main/java/com/example/kiosksim/dto/PaymentQrRequest.java`
- **역할**: QR 생성 요청 데이터 형식 정의
```java
public record PaymentQrRequest(
    Long orderId,      // 주문번호
    Integer amount     // 결제 금액
)
```

#### 6. `backend/src/main/java/com/example/kiosksim/dto/PaymentQrResponse.java`
- **역할**: QR 생성 응답 데이터 형식 정의
```java
public record PaymentQrResponse(
    String qrCode,        // QR 코드 (Base64)
    Long orderId,         // 주문번호
    Integer amount,       // 금액
    String status,        // 상태
    String expiresAt,     // 만료 시간
    String checkoutUrl    // 결제 URL
)
```

---

## 🔄 동작 흐름 (자세히)

### 1단계: 사용자가 "결제 완료" 버튼 클릭

```vue
<!-- App.vue에서 -->
<button @click="checkout">결제 완료 처리 후 DB 저장</button>
```

### 2단계: 백엔드에 주문 저장

```javascript
// 프론트엔드 App.vue
async function checkout() {
  // 1. 백엔드에 POST 요청으로 주문 저장
  const response = await fetch(`${API_BASE}/orders/checkout`, {
    method: "POST",
    body: JSON.stringify(draft.value)  // 장바구니 데이터
  });
  
  lastOrder.value = await response.json();  // 저장된 주문 정보 받기
  
  // 2. 주문 저장 성공하면 QR 생성 함수 호출
  await generatePaymentQr(lastOrder.value.id, lastOrder.value.finalAmount);
}
```

**백엔드에서 하는 일 (OrderController.java)**:
```java
@PostMapping("/checkout")
public OrderResponse checkout(@RequestBody OrderDraftRequest request) {
    // 1. 주문 정보 저장
    // 2. 주문 상세 저장
    // 3. 맛 정보 저장
    // 4. 결제 정보 저장
    return orderResponse;
}
```

### 3단계: QR 코드 생성 요청

```javascript
// 프론트엔드 App.vue
async function generatePaymentQr(orderId, amount) {
  // 1. 백엔드에 POST 요청
  const response = await fetch(`${API_BASE}/payments/qr`, {
    method: "POST",
    body: JSON.stringify({
      orderId: orderId,    // 예: 1
      amount: amount       // 예: 25000
    })
  });
  
  const qrData = await response.json();
  
  // 2. 받은 URL로 QR 코드 이미지 생성 (프론트엔드에서)
  const qrCodeImage = await QRCode.toDataURL(qrData.checkoutUrl);
  
  // 3. 모달 창에 QR 이미지 표시
  paymentQr.value = qrCodeImage;
  showPaymentModal.value = true;
}
```

**백엔드에서 하는 일 (PaymentController.java)**:
```java
@PostMapping("/qr")
public ResponseEntity<PaymentQrResponse> generateQrCode(
    @RequestBody PaymentQrRequest request) {
    
    // PaymentService 호출
    PaymentQrResponse response = paymentService.generateQrCode(
        request.orderId(),
        request.amount()
    );
    
    return ResponseEntity.ok(response);
}
```

**PaymentService.java에서 하는 일**:
```java
public PaymentQrResponse generateQrCode(Long orderId, Integer amount) {
    // 1. 주문 ID와 타임스탬프로 고유한 orderId 생성
    String orderId_str = "ORD-" + orderId + "-" + System.currentTimeMillis();
    
    // 2. 결제 URL 생성 (토스페이먼츠)
    String checkoutUrl = String.format(
        "https://sandbox-payment.tosspayments.com/login?orderId=%s&amount=%d",
        orderId_str, amount
    );
    
    // 3. QR 코드 정보 반환
    return new PaymentQrResponse(
        "",                    // 프론트에서 QR 생성하므로 빈 값
        orderId,              // 원래 주문번호
        amount,               // 결제 금액
        "PENDING",            // 상태
        expiresAt,            // 만료 시간
        checkoutUrl           // 결제 URL (프론트에서 QR 생성할 때 사용)
    );
}
```

### 4단계: 사용자가 QR 스캔

```
휴대폰 카메라로 QR 코드 스캔
        ↓
토스페이먼츠 결제 페이지 자동 열기
```

### 5단계: 토스페이먼츠 결제 완료

사용자가 카드, 계좌이체 등으로 결제 완료

---

## 💻 코드 설명 (초보자용)

### 프론트엔드 (Frontend)

#### App.vue에서 QR 관련 코드

```vue
<script setup>
import QRCode from "qrcode";  // QR 라이브러리 임포트

// QR 관련 상태 변수
const paymentQr = ref(null);           // QR 코드 데이터 저장 변수
const showPaymentModal = ref(false);   // 모달 보이기 여부

// 결제 함수
async function checkout() {
  try {
    // 1. 주문 저장
    const response = await fetch(`${API_BASE}/orders/checkout`, {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(draft.value)
    });
    
    lastOrder.value = await response.json();
    
    // 2. QR 코드 생성
    await generatePaymentQr(lastOrder.value.id, lastOrder.value.finalAmount);
    
  } catch (error) {
    console.error("결제 실패:", error);
  }
}

// QR 코드 생성 함수
async function generatePaymentQr(orderId, amount) {
  try {
    // 1. 백엔드에서 결제 URL 받기
    const response = await fetch(`${API_BASE}/payments/qr`, {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ orderId, amount })
    });
    
    const qrData = await response.json();
    
    // 2. 결제 URL로 QR 이미지 생성
    const qrCodeDataUrl = await QRCode.toDataURL(qrData.checkoutUrl, {
      errorCorrectionLevel: 'H',
      type: 'image/png',
      width: 300
    });
    
    // 3. Base64로 변환해서 저장
    const base64QrCode = qrCodeDataUrl.replace('data:image/png;base64,', '');
    
    paymentQr.value = {
      ...qrData,
      qrCode: base64QrCode
    };
    
    // 4. 모달 보이기
    showPaymentModal.value = true;
    
  } catch (error) {
    console.error("QR 코드 생성 실패:", error);
  }
}
</script>

<!-- HTML에서 QR 모달 표시 -->
<template>
  <div v-if="showPaymentModal" class="modal-overlay">
    <div class="modal-content">
      <h2>QR 결제</h2>
      
      <!-- QR 코드 이미지 표시 -->
      <img :src="`data:image/png;base64,${paymentQr.qrCode}`" 
           alt="QR Code" 
           class="qr-code" />
      
      <!-- 결제 금액 표시 -->
      <p>결제 금액: ₩{{ paymentQr.amount.toLocaleString() }}</p>
      
      <!-- 결제 링크 -->
      <a :href="paymentQr.checkoutUrl" target="_blank">
        토스페이먼츠로 이동
      </a>
    </div>
  </div>
</template>
```

---

### 백엔드 (Backend)

#### PaymentController.java

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    // 생성자 (dependency injection)
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    // API 1: QR 코드 생성
    @PostMapping("/qr")  // POST /api/payments/qr
    public ResponseEntity<PaymentQrResponse> generateQrCode(
            @Valid @RequestBody PaymentQrRequest request) {
        
        // 요청 받은 데이터
        // request.orderId() → 주문번호
        // request.amount() → 결제 금액
        
        // PaymentService에서 QR 생성
        PaymentQrResponse response = paymentService.generateQrCode(
            request.orderId(), 
            request.amount()
        );
        
        // 응답 반환
        return ResponseEntity.ok(response);
    }
    
    // API 2: 결제 확인
    @GetMapping("/confirm")  // GET /api/payments/confirm?paymentKey=...&orderId=...&amount=...
    public ResponseEntity<PaymentQrResponse> confirmPayment(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam String amount) {
        
        PaymentQrResponse response = paymentService.confirmPayment(
            paymentKey, orderId, amount
        );
        
        return ResponseEntity.ok(response);
    }
}
```

**@PostMapping과 @GetMapping 설명**:
- `@PostMapping` = POST 요청 받는 곳 (데이터 생성/저장)
- `@GetMapping` = GET 요청 받는 곳 (데이터 조회)
- `@RequestBody` = 요청 본문에서 데이터 받기
- `@RequestParam` = URL 파라미터에서 데이터 받기

#### PaymentService.java

```java
@Service  // 이것이 Service 클래스임을 표시
@Slf4j    // 로깅 라이브러리
public class PaymentService {
    
    // 설정값 주입 (application.properties에서)
    @Value("${toss.payments.api.url}")
    private String apiUrl;
    
    @Value("${toss.payments.client.key}")
    private String clientKey;
    
    // QR 코드 생성 메서드
    public PaymentQrResponse generateQrCode(Long orderId, Integer amount) {
        try {
            // 1. 고유한 주문 ID 생성
            String orderId_str = "ORD-" + orderId + "-" + System.currentTimeMillis();
            //    "ORD-123-1720656000000"
            
            // 2. 만료 시간 계산 (현재 + 5분)
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(300);
            
            // 3. 토스페이먼츠 결제 URL 생성
            String checkoutUrl = String.format(
                "https://sandbox-payment.tosspayments.com/login?orderId=%s&amount=%d",
                orderId_str,  // 고유한 주문번호
                amount        // 결제 금액 (예: 25000)
            );
            
            // 4. 응답 생성
            return new PaymentQrResponse(
                "",                    // QR 코드 (프론트에서 생성)
                orderId,               // 원래 주문번호
                amount,                // 결제 금액
                "PENDING",             // 상태: 대기중
                expiresAt.format(...), // 만료 시간 문자열
                checkoutUrl            // 결제 URL
            );
            
        } catch (Exception e) {
            // 에러 발생 시
            log.error("QR 코드 생성 실패", e);
            throw new RuntimeException("QR 코드 생성 실패: " + e.getMessage());
        }
    }
    
    // 결제 확인 메서드
    public PaymentQrResponse confirmPayment(
            String paymentKey, 
            String orderId, 
            String amount) {
        try {
            // 1. 토스페이먼츠 API에 결제 확인 요청
            String requestBody = createConfirmRequestBody(paymentKey, orderId, amount);
            String response = callTossPaymentsApi("/v1/payments/confirm", requestBody);
            
            // 2. 응답을 JSON으로 파싱
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String status = jsonResponse.get("status").getAsString();
            
            // 3. 결과 반환
            return new PaymentQrResponse(
                paymentKey,
                Long.valueOf(orderId.split("-")[1]),
                Integer.valueOf(amount),
                status,  // "DONE", "CANCELED" 등
                LocalDateTime.now().format(...),
                ""
            );
            
        } catch (Exception e) {
            log.error("결제 확인 실패", e);
            throw new RuntimeException("결제 승인 실패: " + e.getMessage());
        }
    }
}
```

---

## 🏦 토스페이먼츠란?

**토스페이먼츠**는 한국의 온라인 결제 회사입니다.

### 주요 기능:
1. **QR 코드 결제**: 카메라로 QR 스캔하면 결제 진행
2. **카드 결제**: 신용카드로 결제
3. **계좌이체**: 은행 계좌로 결제
4. **테스트 환경**: 실제 돈 안 내고 테스트 가능

### 우리 프로젝트에서 사용하는 방식:

```
1. 우리 키오스크 → 토스페이먼츠 API 요청
2. "이 주문에 대해 QR 코드 만들어줘"
3. 토스페이먼츠 → QR 코드 URL 반환
4. 우리 프론트엔드 → URL을 QR 이미지로 변환해서 화면에 표시
5. 고객 → 휴대폰으로 QR 스캔
6. 자동으로 토스페이먼츠 결제 페이지 열림
7. 고객 → 결제 완료
8. 토스페이먼츠 → 우리 백엔드에 결제 완료 알림
```

---

## 🔧 테스트 계정 정보

### 토스페이먼츠 테스트 환경:
- **Client Key**: `test_ck_integration`
- **Secret Key**: `test_sk_integration_dummy_key_do_not_use_in_production`
- **API URL**: `https://sandbox-payment.tosspayments.com` (테스트 서버)

### 테스트 카드:
```
카드 번호: 1234-1234-1234-1234
유효기간: 03/25 (MM/YY)
CVC: 123
```

---

## 📊 데이터 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     프론트엔드 (Vue.js)                      │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  1. 상품 선택 페이지                                    │ │
│  │     - 아이스크림 종류 선택                              │ │
│  │     - 맛 선택                                          │ │
│  │     - 장바구니에 담기                                  │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  2. 결제 페이지                                        │ │
│  │     - 할인 적용                                        │ │
│  │     - "결제 완료" 버튼 클릭                            │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  3. QR 모달 (새로 추가!)                               │ │
│  │     - QR 코드 이미지 표시                              │ │
│  │     - "토스페이먼츠로 이동" 링크                        │ │
│  │     - 결제 금액 표시                                  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
              ↓ 주문 데이터 전송 (JSON)
┌─────────────────────────────────────────────────────────────┐
│                  백엔드 (Spring Boot)                        │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  1. OrderController (URL: /api/orders/checkout)        │ │
│  │     - 주문 데이터 받기                                 │ │
│  │     - OrderService 호출                                │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  2. OrderService                                       │ │
│  │     - 주문 정보 저장                                   │ │
│  │     - 주문 상세 저장                                   │ │
│  │     - 맛 정보 저장                                     │ │
│  │     - 결제 정보 생성                                   │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  3. PaymentController (URL: /api/payments/qr)          │ │
│  │     - QR 생성 요청 받기                                │ │
│  │     - PaymentService 호출                              │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  4. PaymentService (새로 추가!)                        │ │
│  │     - 토스페이먼츠 결제 URL 생성                        │ │
│  │     - QR 정보 반환                                     │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
              ↓ QR 정보 반환 (JSON)
┌─────────────────────────────────────────────────────────────┐
│                    H2 데이터베이스                           │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  테이블 1: orders (주문 정보)                          │ │
│  │  테이블 2: order_items (주문 상품)                     │ │
│  │  테이블 3: order_item_flavors (맛 정보)                │ │
│  │  테이블 4: payments (결제 정보) ← QR 저장              │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 요약

### 이 시스템이 하는 일:

1. **사용자가 상품 주문** → 장바구니에 담음
2. **결제 버튼 클릭** → 백엔드에 주문 저장
3. **백엔드** → 주문을 DB에 저장하고, 결제 URL 생성
4. **프론트엔드** → 결제 URL로 QR 코드 이미지 생성
5. **사용자** → 휴대폰 카메라로 QR 스캔
6. **자동으로** → 토스페이먼츠 결제 페이지 열림
7. **사용자가** → 결제 방법 선택 후 완료

### 주요 파일 3개만 기억하세요:

| 파일 | 역할 |
|------|------|
| `frontend/src/App.vue` | 사용자 화면 + QR 생성 로직 |
| `backend/controller/PaymentController.java` | QR 생성 API 제공 |
| `backend/service/PaymentService.java` | 토스페이먼츠 연동 로직 |

---

## 📚 용어 설명

| 용어 | 의미 |
|------|------|
| **API** | 프로그램들이 서로 통신하는 방법 |
| **REST API** | 인터넷을 통해 데이터를 주고받는 표준 방식 |
| **JSON** | 데이터를 텍스트로 표현하는 형식 |
| **QR 코드** | 카메라로 스캔할 수 있는 정보 코드 |
| **Spring Boot** | Java 웹 개발 프레임워크 |
| **Vue.js** | JavaScript 웹 프레임워크 |
| **H2 데이터베이스** | 메모리 기반 테스트용 데이터베이스 |
| **Vite** | 웹 개발 도구 (매우 빠름) |

---

**작성일**: 2026-07-10
**프로젝트**: 쿠키 주문 키오스크
**개발환경**: Vue.js + Spring Boot + H2 DB
