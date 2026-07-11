# 장바구니/주문/결제 기능 변경 이력 (feature/장바구니)

`feature/장바구니` 브랜치에서 작업한 일반사용자(고객) P0 플로우의 주요 구현 내용과,
이후 진행한 ponytail 코드 최적화 내역을 정리한 문서입니다.

## 1. 구현된 주요 기능

### 1.1 주문 플로우 (프론트: `frontend/src/views/kiosk/OrderView.vue`)

단계 전환 순서: `orderType → product → (container) → flavor → cart → customer`

| 단계 | 내용 |
|---|---|
| 매장/포장 선택 | 팝업으로 매장(DINE_IN)/포장(TAKEOUT) 선택 |
| 상품 선택 | 카테고리는 상단 토글 탭(아이스크림/커피)이고, 탭을 눌러도 화면 전환 없이 목록만 바뀜. 사이즈(레귤러/대용량) 구분 없이 8종 아이스크림이 한 화면에 전부 노출 |
| 용기 선택 | 컵/콘 둘 다 가능한 상품(싱글~더블레귤러)만 이 화면을 거치고, 컵만 되는 상품(파인트/쿼터/패밀리/하프갤런)은 건너뛰고 바로 맛 선택으로 진입 |
| 맛 선택 | 상품별 `selectableFlavorCount`만큼 선택. **사이즈 상관없이 모든 아이스크림에서 같은 맛 중복 선택 가능** (예: 더블주니어에서 바닐라를 2번 선택). 선택한 맛은 화면 하단 요약바에 `맛이름 x개수` 형태로 실시간 표시되고, `−` 버튼으로 하나씩 제거 가능 |
| 장바구니 확인 | 담은 항목 목록, 수정/삭제(삭제 시 확인창) |
| 포인트/결제 | 휴대폰 번호로 회원 조회 → 보유 등급/포인트 표시 → `-1000 / -100 / +100 / +1000 / 최대금액사용` 버튼으로 포인트 사용량 조정 → 결제 금액에서 실시간 차감 |

- 픽업 희망 일시(픽업 날짜/시간 선택) 기능은 요구사항 재검토 후 **완전히 제거**함 (프론트 UI, 카트 상태, 체크아웃 요청 바디, 백엔드 DTO/엔티티 배선까지 전부 삭제).

### 1.2 결제 (QR 시뮬레이션)

- `결제` 버튼 클릭 → `POST /api/orders/checkout` → `POST /api/payments/qr` 순으로 호출
- 실제 PG 연동 전이라, **결제 버튼을 누르면 체크아웃 요청에 사용된 JSON 데이터를 QR 팝업 안에 그대로 노출**해서 다른 결제 수단 연동 시 참고할 수 있게 함
- QR 팝업 우측 상단에 `×` 닫기 버튼 추가 — 클릭하면 폴링을 멈추고 팝업을 닫아 결제 버튼 화면으로 복귀
- `/pay/:token` 페이지에서 "결제 완료 처리"를 누르면 `POST /api/payments/{token}/confirm` 호출 → 프론트는 2초 간격으로 상태를 폴링하다가 `PAID`가 되면 3초 후 자동으로 장바구니를 비우고 광고 화면으로 복귀

### 1.3 포인트 적립/사용 규칙

- 등급별 적립률: FRIEND 3% / FAMILY 5% / VIP 8% (결제 확정 시 `final_amount` 기준으로 계산)
- **포인트를 사용한 결제건은 적립을 받지 않음** — `usedPoints > 0`이면 `earnedPoints = 0`으로 고정해 사용과 적립이 한 거래에서 중복되지 않도록 처리 (`PaymentService.confirm()`)
- 프론트의 "예상 적립" 표시도 동일한 규칙을 따르도록 `estimatedEarnedPoints`를 수정 (포인트 사용 중이면 0P로 표시)

### 1.4 장바구니 상태 유지

- `frontend/src/stores/cart.js` (Pinia) — 매장/포장 구분, 고객 번호, 사용 포인트, 담은 항목을 쿠키(JSON 직렬화) 하나로 저장
- 세션 쿠키(만료일 미지정)라서 브라우저/탭을 닫으면 사라짐 — 다음 고객이 이전 고객 장바구니를 이어받지 않도록 하기 위함

## 2. Ponytail 코드 최적화 (2026-07-11)

`ponytail:ponytail-audit`로 리포지토리 전체를 감사한 뒤 실제 구현된 코드(`kiosk/menu`, `kiosk/order`,
`kiosk/payment`, `kiosk/customer`, 프론트 카트/주문 화면)를 중심으로 죽은 코드와 안 쓰는 의존성을 제거했습니다.
`branch/`, `hq/`의 `package-info.java`만 있는 스캐폴딩 패키지는 CLAUDE.md에 명시된 6인 팀용 의도된 구조라 감사 대상에서 제외했습니다.

| # | 분류 | 내용 | 파일 |
|---|---|---|---|
| 1 | yagni (의존성 삭제) | `spring-boot-starter-validation` — 코드 전체에서 `@Valid`/`@NotNull`/`@NotBlank`/`jakarta.validation` 사용이 0건이라 완전히 미사용 상태였음. `pom.xml`에서 제거 | `backend/pom.xml` |
| 2 | delete (죽은 필드) | `OrderCheckoutRequest.pickupAt` 필드와 `OrderService`의 `.pickupAt(request.pickupAt())` 배선 제거. 픽업 일시 기능이 프론트에서 완전히 빠지면서 이 값은 항상 `null`로만 전달되고 있었음 | `OrderCheckoutRequest.java`, `OrderService.java` |
| 3 | delete (죽은 게터) | `cart.js`의 `totalAmountBeforeDiscount` 게터 — `amountBeforeDiscount`의 별칭인데 어디서도 참조되지 않음 | `frontend/src/stores/cart.js` |
| 4 | delete (죽은 필드) | `addItem`이 세팅하던 `requestNote` 기본 필드 — 화면/체크아웃 어디서도 읽힌 적 없음 | `frontend/src/stores/cart.js` |
| 5 | delete (죽은 플래그) | 카테고리 더미데이터의 `hasSizeTiers` 플래그 — 사이즈 단계 UI를 없앤 뒤 남은 잔재, 참조 0건 | `frontend/src/data/menuDummy.js` |

**결과**: 의존성 1개(`spring-boot-starter-validation`) 삭제, 약 20줄 정리. `mvn compile` 클린 확인 완료.

> 실제 구현부(`kiosk/*`)는 감사 전에도 불필요한 팩토리/인터페이스/래퍼 없이 이미 lean한 상태였고,
> 이번 최적화는 기능 변경 없이 죽은 코드/의존성만 제거한 것이라 동작 방식에는 영향이 없습니다.

### 참고: 백엔드 재기동 필요

IntelliJ devtools의 자동 재시작이 이번 변경(의존성 제거 포함)을 바로 반영하지 못하는 경우가 있어,
로컬에서 실행 중인 백엔드는 **수동으로 한 번 재시작**해야 `pom.xml` 변경사항(의존성 제거)까지 완전히 반영됩니다.
