# 🍪 쿠키 아이스크림 키오스크 - QR 결제 시스템

> Vue.js + Spring Boot + H2 DB로 만든 아이스크림 주문 키오스크 + 토스페이먼츠 QR 결제

---

## 📸 기능 소개

### ✨ 주요 기능

```
1. 상품 선택 & 맛 선택
   ├─ 카테고리별 상품 조회
   ├─ 맛 다중 선택
   ├─ 용기(컵/콘) 선택
   └─ 드라이아이스 옵션

2. 장바구니 관리
   ├─ 상품 추가/제거
   ├─ 수량 조절
   └─ 실시간 금액 계산

3. 할인/포인트 적용
   ├─ 쿠폰 할인
   ├─ 포인트 사용
   └─ 최종 금액 계산

4. 주문 저장
   ├─ 4개 테이블에 정규화 저장
   ├─ 주문 정보
   ├─ 주문 상품
   ├─ 상품별 맛
   └─ 결제 정보

5. ⭐ QR 결제 (새로 추가!)
   ├─ 결제 완료 시 QR 생성
   ├─ 휴대폰 카메라로 스캔
   ├─ 토스페이먼츠 결제 페이지 연동
   └─ 모바일 결제 완벽 지원
```

---

## 🏗️ 시스템 아키텍처

### 전체 구조

```
┌──────────────────────────────────────────────────────────────┐
│                      Front-end (Vue.js)                      │
│  - 사용자 인터페이스                                           │
│  - 상품 선택 및 카트 관리                                      │
│  - QR 코드 생성 및 표시                                        │
└──────────────────────────────────────────────────────────────┘
                              │
                    REST API (HTTP/JSON)
                              │
┌──────────────────────────────────────────────────────────────┐
│                  Back-end (Spring Boot)                       │
│  ┌──────────────────────────────────────────────────────────┤
│  │ Controllers:                                              │
│  │  - OrderController    /api/orders/checkout                │
│  │  - PaymentController  /api/payments/qr                    │
│  │                      /api/payments/confirm                │
│  └──────────────────────────────────────────────────────────┘
│  ┌──────────────────────────────────────────────────────────┤
│  │ Services:                                                 │
│  │  - OrderService       (주문 처리)                          │
│  │  - PaymentService     (QR 생성, 토스결제 연동) ⭐          │
│  │  - CatalogService     (상품 조회)                         │
│  └──────────────────────────────────────────────────────────┘
│  ┌──────────────────────────────────────────────────────────┤
│  │ Repositories (JPA):                                       │
│  │  - KioskOrderRepository                                   │
│  │  - OrderItemRepository                                    │
│  │  - OrderItemFlavorRepository                              │
│  │  - PaymentRepository                                      │
│  └──────────────────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────────┘
                              │
                         JPA/Hibernate
                              │
┌──────────────────────────────────────────────────────────────┐
│                   H2 In-Memory Database                       │
│  ┌──────────────────────────────────────────────────────────┤
│  │ orders            │ 주문 기본 정보                         │
│  │ order_items       │ 주문한 상품들                         │
│  │ order_item_flavors│ 각 상품의 맛 정보                     │
│  │ payments          │ 결제 정보 + QR 토큰 저장              │
│  └──────────────────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────────┘
                              │
                         (HTTP API)
                              │
┌──────────────────────────────────────────────────────────────┐
│                    토스페이먼츠 API                            │
│  - QR 코드 생성                                               │
│  - 결제 확인                                                  │
│  - 테스트 환경 지원                                            │
└──────────────────────────────────────────────────────────────┘
```

---

## 📁 프로젝트 구조

```
cookie/
├── backend/                              # Spring Boot 백엔드
│   ├── src/main/java/com/example/kiosksim/
│   │   ├── controller/
│   │   │   ├── OrderController.java
│   │   │   ├── PaymentController.java ⭐
│   │   │   └── CatalogController.java
│   │   ├── service/
│   │   │   ├── OrderService.java
│   │   │   ├── PaymentService.java ⭐
│   │   │   └── CatalogService.java
│   │   ├── domain/
│   │   │   ├── KioskOrder.java
│   │   │   ├── OrderItem.java
│   │   │   ├── OrderItemFlavor.java
│   │   │   └── Payment.java
│   │   ├── dto/
│   │   │   ├── OrderResponse.java
│   │   │   ├── PaymentQrRequest.java ⭐
│   │   │   ├── PaymentQrResponse.java ⭐
│   │   │   └── ... (기타 DTO)
│   │   ├── repository/
│   │   │   ├── KioskOrderRepository.java
│   │   │   ├── OrderItemRepository.java
│   │   │   ├── OrderItemFlavorRepository.java
│   │   │   └── PaymentRepository.java
│   │   └── KioskSimApplication.java
│   ├── src/main/resources/
│   │   └── application.properties ⭐ (토스 설정)
│   ├── pom.xml ⭐ (의존성 추가)
│   └── mvnw, mvnw.cmd
│
├── frontend/                             # Vue.js 프론트엔드
│   ├── src/
│   │   ├── App.vue ⭐ (QR 모달 추가)
│   │   ├── main.js
│   │   └── styles.css ⭐ (모달 스타일)
│   ├── package.json ⭐ (qrcode 라이브러리)
│   ├── vite.config.js
│   └── index.html
│
├── GUIDE_QR_PAYMENT.md ⭐ (상세 설명서)
├── QUICK_START.md ⭐ (빠른 시작 가이드)
└── README.md (이 파일)
```

⭐ = QR 결제 구현 시 추가/수정된 파일

---

## 🚀 시작하기

### 요구사항

- **Node.js** 18+
- **Java** 21+
- **Git**

### 설치 및 실행

#### 1️⃣ 프로젝트 클론
```bash
git clone <repository>
cd cookie
```

#### 2️⃣ 백엔드 실행
```bash
cd backend

# Windows
mvnw.cmd spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

✅ 백엔드 실행 완료:
```
Tomcat started on port 18080
```

#### 3️⃣ 프론트엔드 실행 (새 터미널)
```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

✅ 프론트엔드 실행 완료:
```
VITE v7.3.6 ready in 245 ms
➜  Local: http://localhost:5174/
```

#### 4️⃣ 브라우저에서 확인
```
http://localhost:5174/
```

---

## 📊 API 문서

### 주문 API

#### POST /api/orders/checkout
상품 주문 저장

**요청**:
```json
{
  "branchId": 1,
  "kioskId": 1,
  "orderType": "DINE_IN",
  "cartItems": [
    {
      "productId": 1,
      "quantity": 2,
      "flavors": [
        {"flavorId": 1},
        {"flavorId": 2}
      ]
    }
  ],
  "discount": {
    "couponDiscountAmount": 5000,
    "usedPoint": 10000
  }
}
```

**응답**:
```json
{
  "id": 1,
  "orderNo": "ORD-2026-07-10-ABC12345",
  "waitingNo": 125,
  "originalAmount": 50000,
  "discountAmount": 15000,
  "finalAmount": 35000,
  "orderType": "DINE_IN"
}
```

---

### 결제 API (⭐ 새로 추가)

#### POST /api/payments/qr
QR 코드 생성

**요청**:
```json
{
  "orderId": 1,
  "amount": 35000
}
```

**응답**:
```json
{
  "qrCode": "base64_encoded_image",
  "orderId": 1,
  "amount": 35000,
  "status": "PENDING",
  "expiresAt": "2026-07-10T10:35:00",
  "checkoutUrl": "https://sandbox-payment.tosspayments.com/login?orderId=..."
}
```

#### GET /api/payments/confirm
결제 확인

**요청**:
```
?paymentKey=toss_abc123&orderId=ORD-123-456&amount=35000
```

**응답**:
```json
{
  "paymentKey": "toss_abc123",
  "orderId": 1,
  "amount": 35000,
  "status": "DONE"
}
```

---

## 🗄️ 데이터베이스 스키마

### orders 테이블
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    branch_id BIGINT,
    kiosk_id BIGINT,
    order_no VARCHAR(255),
    order_type VARCHAR(255),        -- DINE_IN, TAKEOUT
    status VARCHAR(255),             -- PAID, DELIVERED, CANCELLED
    original_amount INTEGER,         -- 할인 전
    discount_amount INTEGER,         -- 할인액
    final_amount INTEGER,            -- 최종 금액
    used_point INTEGER,              -- 사용한 포인트
    waiting_no INTEGER,              -- 대기 번호
    ordered_at TIMESTAMP
);
```

### order_items 테이블
```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    product_id BIGINT,
    product_name_snapshot VARCHAR(255),
    quantity INTEGER,
    unit_price INTEGER,
    line_total INTEGER,
    container_type VARCHAR(255),    -- CUP, CONE, NONE
    spoon_count INTEGER,
    dry_ice_minutes INTEGER
);
```

### order_item_flavors 테이블
```sql
CREATE TABLE order_item_flavors (
    id BIGINT PRIMARY KEY,
    order_item_id BIGINT,
    flavor_id BIGINT,
    flavor_name_snapshot VARCHAR(255),
    select_order INTEGER,           -- 1번째 선택, 2번째 선택...
    quantity INTEGER
);
```

### payments 테이블
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    payment_method VARCHAR(255),    -- QR, CARD, TRANSFER
    status VARCHAR(255),             -- PENDING, DONE, FAILED
    qr_token VARCHAR(255),           -- QR 고유 토큰 ⭐
    qr_expires_at TIMESTAMP,         -- QR 만료 시간 ⭐
    paid_amount INTEGER,
    paid_at TIMESTAMP
);
```

---

## 🧪 테스트 환경

### 토스페이먼츠 테스트 계정

```properties
# application.properties
toss.payments.client.key=test_ck_integration
toss.payments.secret.key=test_sk_integration_dummy_key...
toss.payments.api.url=https://sandbox-payment.tosspayments.com
```

### 테스트 카드
```
카드 번호: 1234-1234-1234-1234
유효기간: 03/25
CVC: 123
```

---

## 📖 문서

- **상세 설명**: [GUIDE_QR_PAYMENT.md](./GUIDE_QR_PAYMENT.md) - 코딩 초보자도 이해할 수 있는 완전한 설명
- **빠른 시작**: [QUICK_START.md](./QUICK_START.md) - 5분 안에 이해하는 요약본

---

## 🔧 기술 스택

### 프론트엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| Vue.js | 3.5.17 | UI 프레임워크 |
| Vite | 7.0+ | 빌드 도구 |
| qrcode | 1.5.3 | QR 코드 생성 ⭐ |

### 백엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| Spring Boot | 3.5.3 | 웹 프레임워크 |
| Spring Data JPA | - | ORM |
| Hibernate | 6.6.18 | DB 매핑 |
| Tomcat | 10.1.42 | 웹 서버 |
| Gson | 2.13.1 | JSON 처리 ⭐ |
| Apache HttpClient | 5+ | HTTP 요청 ⭐ |

### 데이터베이스
| 기술 | 용도 |
|------|------|
| H2 Database | 메모리 기반 테스트 DB |

### 결제 (토스페이먼츠)
- QR 코드 결제 API
- 테스트 환경 지원

---

## 🎯 동작 흐름 (상세)

### 1단계: 사용자가 주문

```
사용자가 브라우저 접속 (localhost:5174)
    ↓
상품 클릭 → 맛 선택 → 장바구니에 담기
    ↓
할인/포인트 적용
    ↓
"결제 완료" 버튼 클릭
```

### 2단계: 백엔드에 주문 저장

```
POST /api/orders/checkout
    ↓
OrderController에서 요청 받음
    ↓
OrderService에서 처리:
  1. 주문 정보 생성
  2. 각 상품을 주문상품으로 저장
  3. 각 맛을 별도 레코드로 저장
  4. 결제 정보 생성
    ↓
H2 데이터베이스에 저장 (4개 테이블)
    ↓
프론트엔드에 주문 ID 반환
```

### 3단계: QR 코드 생성 ⭐

```
프론트엔드에서 POST /api/payments/qr 호출
    ↓
PaymentController에서 요청 받음
    ↓
PaymentService에서 처리:
  1. 고유한 주문 ID 생성
  2. 토스페이먼츠 결제 URL 생성
  3. QR 정보 반환
    ↓
프론트엔드에서 URL을 QR 이미지로 변환
    ↓
화면에 QR 이미지와 결제 정보 표시
```

### 4단계: 사용자가 QR 스캔

```
휴대폰 카메라로 QR 코드 스캔
    ↓
토스페이먼츠 결제 페이지 자동 열기
    ↓
결제 수단 선택 (카드, 계좌이체 등)
    ↓
결제 완료 또는 실패
    ↓
토스페이먼츠에서 우리 백엔드로 결과 전송
```

---

## 📝 주요 코드 예시

### 프론트엔드 (Vue.js)

**QR 생성 함수**:
```javascript
async function generatePaymentQr(orderId, amount) {
  const response = await fetch(`${API_BASE}/payments/qr`, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({ orderId, amount })
  });
  
  const qrData = await response.json();
  
  // QR 이미지 생성
  const qrImage = await QRCode.toDataURL(qrData.checkoutUrl);
  
  // 모달에 표시
  paymentQr.value = { ...qrData, qrCode: qrImage };
  showPaymentModal.value = true;
}
```

### 백엔드 (Spring Boot)

**PaymentService**:
```java
public PaymentQrResponse generateQrCode(Long orderId, Integer amount) {
    String orderId_str = "ORD-" + orderId + "-" + System.currentTimeMillis();
    String checkoutUrl = String.format(
        "https://sandbox-payment.tosspayments.com/login?orderId=%s&amount=%d",
        orderId_str, amount
    );
    
    return new PaymentQrResponse(
        "", orderId, amount, "PENDING",
        LocalDateTime.now().plusSeconds(300).toString(),
        checkoutUrl
    );
}
```

---

## 🐛 트러블슈팅

### 문제 1: "Port 18080 is already in use"

**해결책**:
```bash
# 포트 확인
netstat -ano | findstr :18080

# 프로세스 종료 (Windows)
taskkill /PID <PID> /F
```

### 문제 2: "Port 5174 is already in use"

**해결책**:
```bash
# 프로세스 확인
netstat -ano | findstr :5174

# 프로세스 종료
taskkill /PID <PID> /F
```

### 문제 3: npm install 오류

**해결책**:
```bash
# node_modules 삭제
rm -r node_modules

# package-lock.json 삭제
rm package-lock.json

# 재설치
npm install
```

---

## 📊 통계

- **백엔드 코드**: 31개 Java 파일
- **프론트엔드 코드**: Vue 1개 + CSS 1개
- **새로 추가된 코드**: 6개 파일
- **데이터베이스**: 4개 테이블

---

## 📞 지원

문제 발생 시:

1. `GUIDE_QR_PAYMENT.md` 읽어보기
2. `QUICK_START.md` 빠른 시작 가이드 확인
3. 터미널 로그 확인
4. 브라우저 개발자 도구 (F12) 확인

---

## 📄 라이선스

MIT License

---

## 👨‍💻 개발 환경

```
OS: Windows 11
IDE: VS Code + Spring Tools Suite
JDK: Java 21
Node.js: v18+
```

---

**마지막 업데이트**: 2026-07-10
**버전**: 1.0.0 (QR 결제 시스템 추가)
