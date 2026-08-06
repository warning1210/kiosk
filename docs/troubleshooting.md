# 트러블슈팅 로그

발생한 문제, 원인 분석, 조치 내용을 시간순으로 기록한다.

---

## 2026-08-06: 지점 대시보드/주문관리(`/branch/orders`) 응답 지연

### 증상
지점 대시보드 진입 시 로딩이 체감상 매우 오래 걸림.

### 원인 (N+1 쿼리, 2중 증폭)
`BranchOrderService.getBranchOrders()`가 주문 목록을 가져온 뒤, **주문 1건당 결제 정보를 최대 2번**(필터링 1번 + 응답 조립 1번) 개별 조회했다(`paymentRepository.findByOrder_OrderId(...)`).

여기에 더해, `PaymentRepository`의 결제 조회 매퍼가 아래처럼 매 건마다 연결된 주문을 **서브쿼리로 통째로 재조회**하도록 짜여 있었다:

```xml
<association property="order" column="order_id" select="com.kiosk.domain.order.OrderRepository.selectById"/>
```

`OrderRepository.selectById` 자체도 그 주문의 `order_item`을 또 서브쿼리로 조회하므로, 결제 조회 1번 = 실제 쿼리 3번(결제 + 주문 재조회 + 주문상품 재조회)으로 증폭됐다. 이 재조회된 주문 데이터는 `getBranchOrders()`에서 결제수단(`payment.getPaymentMethod()`) 외에는 전혀 쓰이지 않는 낭비였다.

게다가 DB가 원격 공유 서버(`3.35.218.189`)라 각 쿼리마다 네트워크 왕복 지연이 매번 붙었다.

### 조치
결제수단만 필요한 이 화면을 위해 **주문 ID 목록을 한 번에 IN 절로 벌크 조회**하는 전용 쿼리를 추가하고(`PaymentRepository.findMethodsByOrderIds`), 주문별 개별 조회를 전부 제거했다. 이 벌크 쿼리의 resultMap은 서브쿼리 없이 같은 행의 `order_id`만 매핑해서 주문 재조회 자체가 발생하지 않는다.

- 커밋 대상: `backend/src/main/resources/mapper/payment/PaymentRepository.xml`, `backend/src/main/java/com/kiosk/domain/payment/PaymentRepository.java`, `backend/src/main/java/com/kiosk/branch/order/service/BranchOrderService.java`

### 응답속도 개선 수치화

측정 방법: 실제 공유 DB(`3.35.218.189`)의 지점 1(`branch_id=1`) 실데이터 기준으로 코드 로직을 그대로 대입해 쿼리 횟수를 정확히 계산하고, 같은 DB에 대한 실측 쿼리 왕복시간(같은 커넥션으로 300회 순차 조회, 평균 **8.64ms/쿼리**)을 곱해 추정했다. (엔드투엔드 실측치가 아니라 "쿼리 횟수 × 실측 왕복시간" 기반 추정치임을 명시.)

| 항목 | 값 |
|---|---|
| 대상 주문 건수 (`branch_id=1`, 전체) | 294건 (COMPLETED 283 · PENDING_PAYMENT 9 · PAID 2) |
| PENDING_PAYMENT 중 결제수단 | 전부 QR (CASH 0건 → 필터 통과 285건) |
| **수정 전 쿼리 수** | 1(목록) + 9×3(필터 단계 결제조회) + 285×3(응답조립 결제조회) = **883회** |
| **수정 후 쿼리 수** | 1(목록) + 1(벌크 결제조회) = **2회** |
| 쿼리 수 감소 | 883 → 2 (**99.8% 감소**, 881회 제거) |
| 실측 쿼리당 왕복시간 | 8.64ms (같은 DB, 300회 순차 조회 평균) |
| **추정 응답시간** | 883 × 8.64ms ≈ **7,630ms(7.6초)** → 2 × 8.64ms ≈ **17ms** |

### 추가 수정 (같은 날, 후속) — 결제 수정 후에도 "약 7초"가 그대로 보고됨

위 결제 N+1을 고친 뒤에도 대시보드가 여전히 ~7초 걸린다는 보고가 들어왔다. 이 수치가 **수정 전 추정치(7.6초)와 거의 일치**해서, 우선 IntelliJ가 새 빌드를 실제로 반영했는지부터 의심할 대상이지만(이 세션 내내 반복된 문제), 그와 별개로 "남은 최적화 여지"로 적어뒀던 것과 대시보드가 동시에 부르는 `/branch/inventory`를 마저 확인해서 실제로 존재하는 N+1 2개를 더 찾아 고쳤다.

**1) 주문 목록 조회 자체의 order_item N+1** — `OrderRepository`의 `findByBranchIdAndOrderStatusIn...` 두 메서드가 쓰는 resultMap에 `order_item`을 행마다 서브쿼리로 채우는 `<collection>`이 걸려 있어(`OrderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc`), 주문 목록을 가져오는 시점 자체에서 **주문 건수만큼**(294건 기준 294회) 추가 쿼리가 발생했다. 이 두 메서드는 `getBranchOrders()`에서만 쓰이는 걸 확인하고, orderItems 없는 전용 `listMap` resultMap으로 분리 + 메뉴 요약에 필요한 주문상품은 `findByOrder_OrderIdIn`으로 한 번에 벌크 조회하도록 바꿨다. `OrderCleanupService`가 쓰는 `findByOrderStatusInAndCreatedAtBefore`도 주문상품을 안 쓰는 걸 확인하고 같이 `listMap`으로 옮겼다(매분 도는 배치라 여기도 낭비였음).

**2) 재고 조회의 맛별 존재확인 N+1** — `BranchInventoryService.getInventory()`가 호출될 때마다(대시보드 로드마다) 내부에서 `initializeMissingInventory()`가 판매중인 맛(31개) 하나하나에 대해 "이 지점에 이 맛 재고가 이미 있는지" 개별 쿼리로 확인했다. 지점 재고를 한 번에 벌크로 읽어 메모리에서 비교하도록 바꿨다(31회 → 1회).

- 커밋 대상 추가: `backend/src/main/resources/mapper/order/OrderRepository.xml`, `backend/src/main/resources/mapper/order/OrderItemRepository.xml`, `backend/src/main/java/com/kiosk/domain/order/OrderItemRepository.java`, `backend/src/main/java/com/kiosk/branch/inventory/service/BranchInventoryService.java`

### 대시보드 전체(주문+재고+공지) 기준 최종 수치

| 단계 | 쿼리 수 | 비고 |
|---|---|---|
| 최초 (결제 N+1 미수정) | 주문 ~883 + 재고 ~32 + 공지 ~2 = **~917회** | |
| 결제만 수정 (이번 절 "조치") | 주문 ~296(order_item N+1 남음) + 재고 ~32 + 공지 ~2 = **~330회** | 사용자가 "여전히 7초"라 보고한 시점 - 이 추정(~2.85초)보다 실제가 더 컸다는 건 새 빌드가 아직 반영 안 됐을 가능성을 시사 |
| 전부 수정 (이번 절 "추가 수정") | 주문 3 + 재고 2 + 공지 2 = **7회** | |
| 추정 응답시간(쿼리수×8.64ms) | 917회 ≈ 7.9초 → 330회 ≈ 2.85초 → **7회 ≈ 60ms** | |

**확인 필요**: 이번에도 IntelliJ 재빌드/재시작이 실제로 새 코드를 반영했는지 확인 후 다시 측정할 것 - "결제만 수정" 단계 추정치(2.85초)와 실제 보고값(7초)의 괴리가 그 가능성을 가리킨다.
