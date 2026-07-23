# 맛 할인 판단/계산 로직 (요약)

**흐름**: 맛 목록 조회 시점에 백엔드가 할인 여부를 미리 판단 → 프론트는 뱃지로 표시만 → 장바구니 담을 때 화면용 금액 계산 → 결제 시 서버가 독립적으로 재계산해 확정.

| 단계 | API / 위치 | 역할 |
|---|---|---|
| 1. 맛 목록 조회 | `GET /api/flavors` (`MenuController` → `MenuService` → `KioskFlavorDiscountService`) | 진행 중인 이벤트(`MONTHLY_FLAVOR`/`HQ_FLAVOR_DISCOUNT`/`FLAVOR_DISCOUNT`)를 맛별로 매칭해 `discountType`/`discountRate`/`discountAmount` 실어서 응답 |
| 2. 뱃지 표시 | `frontend/src/views/kiosk/steps/FlavorStep.vue` | 서버가 내려준 `discountType` 값이 있으면 뱃지만 그림 (재판단 없음) |
| 3. 장바구니 담기 | `frontend/src/stores/orderFlow.js` (`resolveFlavorDiscount`) | 선택한 맛들 중 할인 최대값 하나만 적용해 화면 표시 금액 계산 |
| 4. 결제 확정 | `POST /api/orders/checkout` (`OrderController` → `OrderService.checkout`) | 서버가 처음부터 다시 계산, 프론트 금액은 참고만 하고 신뢰하지 않음 |

**핵심**: 할인 여부/금액 판단은 항상 백엔드 기준이고, 프론트 계산은 서버 로직을 미러링한 "표시용"일 뿐 — 최종 금액은 결제 시 서버가 다시 확정한다.
