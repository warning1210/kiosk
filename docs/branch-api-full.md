## 지점 시스템 API 전체 정리

| 화면 | 기능 | url | 파라메타 | 결과값 | 클래스 (백엔드) | vue(프론트엔드) | 수정 사항 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 로그인 | DB 폴백 로그인 | POST /api/branch-auth/db-login | id, pw | Long : adminId<br>Long : branchId<br>String : branchName<br>String : managerName<br>String : token | BranchFallbackLoginService | BranchLoginView | |
| 로그인 | Firebase 로그인 | POST /api/branch-auth/firebase-session | idToken | Long : adminId<br>Long : branchId<br>String : branchName<br>String : managerName | BranchAuthService | BranchLoginView | 로그인 아이디에 @ 있으면 이 경로로 감 (db-login과 양자택일) |
| 로그인 | 초대 토큰 조회 | GET /api/branch-auth/invites/{token} | token (path) | ApplicationResponse 전체 (신청자 정보) | BranchAuthService | BranchJoinView | 본사가 지점 개설 승인 후 보내는 이메일 링크로만 진입 |
| 로그인 | 지점 가입 | POST /api/branch-auth/join | token, loginId, password, managerName, branchName, phone, address, businessNumber | Long : adminId<br>Long : branchId<br>String : branchName<br>String : managerName | BranchAuthService | BranchJoinView | |
| 로그인 | 로그인아이디→이메일 조회 | GET /api/branch-auth/login-identity/{loginId} | loginId (path) | String : email | BranchAuthService | 미사용 | 프론트 어디서도 호출 안 함, 죽은 코드로 보임 |
| 공통(사이드바) | 온라인 상태 유지 | POST /api/branch/presence/heartbeat | 없음 | 200 (body 없음) | BranchAccessService | BranchSidebar | 5초마다 자동 호출, 모든 지점 화면에 공통 적용 |
| 공통(사이드바) | 직원 호출 상태 조회 | GET /api/branch/staff-call | 없음 | Boolean : called<br>datetime : calledAt | BranchStaffCallService | BranchSidebar (useStaffCallAlert) | |
| 공통(사이드바) | 직원 호출 확인(끄기) | DELETE /api/branch/staff-call | 없음 | 200 (body 없음) | BranchStaffCallService | BranchSidebar (useStaffCallAlert) | |
| 대시보드 | 주문 | GET /api/branch/orders | 없음 | Long : orderId<br>String : orderNumber<br>Integer : waitingNumber<br>long : elapsedMinutes<br>String(enum) : orderType<br>String : menuSummary<br>String(enum) : status<br>datetime : createdAt<br>Integer : finalAmount | BranchOrderService | DashboardView | 대시보드에서는 현재 처리해야할 주문만 보이면 되므로, 전체 주문 데이터를 가져오는 요청은 데이터베이스가 아파함..ㅠㅠ 수정해야함. onMounted() 사용해서 타이머(2초) 간격으로 계속해서 대시보드에 필요한 전체 데이터를 갱신하면서 가져오기 때문에 서버에 부하가 걸려 onMounted가 아닌 다른 코드로 처리할 필요가 있을 수 있음. 백엔드에서 필터링된 주문 데이터만 전송 하게 변경 |
| 대시보드 | 재고현황 | GET /api/branch/inventory | 없음 | Long : inventoryId<br>Long : flavorId<br>String : flavorName<br>String : imageUrl<br>Integer : remainingGrams<br>String(enum) : status<br>String(enum) : requestStatus<br>datetime : expectedArrivalAt<br>datetime : updatedAt | BranchInventoryService | DashboardView | |
| 대시보드 | 재고배송현황 | GET /api/branch/inventory (재고현황과 동일 URL) | 없음 | 위 재고현황과 동일 응답, requestStatus 있는 것만 필터 | BranchInventoryService | DashboardView | 현재 위 재고현황과 같은 url을 재사용 중임 (별도 엔드포인트 없음). 실제로는 /api/branch/stock-requests 쪽 데이터(배송번호/담당자/도착예정일)가 더 정확한데 안 쓰고 있음 → url 분리 + 정확한 배송정보 연동 필요 |
| 대시보드 | 공지사항 | GET /api/branch/notices | 없음 | String(enum) : noticeType<br>Long : id<br>String : title<br>String : content<br>String : imageUrl<br>datetime : postedAt<br>datetime : endAt | BranchNoticeService | DashboardView | |
| 주문 관리 | 주문 현황 | GET /api/branch/orders | 없음 | (대시보드 주문과 동일 응답) | BranchOrderService | OrdersView | 대시보드와 동일한 이슈(전체 조회 후 프론트 필터) |
| 주문 관리 | 주문상태변경 | PATCH /api/branch/orders/{orderId}/status | String(enum) : status<br>String : cancelReason | 200 (body 없음) | BranchOrderService | OrdersView | |
| 주문 관리 | 바빠요 버튼 | PATCH /api/branch/status | Boolean : isBusy<br>Integer : estimatedWaitMinutes | Boolean : isBusy<br>Integer : estimatedWaitMinutes | BranchStatusService | OrdersView | |
| 주문 내역 | 주문 내역 | GET /api/branch/orders | 없음 | (대시보드 주문과 동일 응답) | BranchOrderService | OrderListView | |
| 주문 내역 | 달력 - 날짜 필터 | GET /api/branch/orders?date= | Date : date | (대시보드 주문과 동일 응답) | BranchOrderService | OrderListView | |
| 주문 내역 | 달력 - 선택가능 날짜 목록 | GET /api/branch/orders/dates | 없음 | String(date) 배열 | BranchOrderService | OrderListView | 달력에 "이 날짜엔 주문 있음" 표시용, 위 날짜 필터랑 세트로 같이 씀 |
| 주문 내역 | 상세 내역 | GET /api/branch/orders/{orderId} | Long : orderId | Long : orderId<br>String : orderNumber<br>Integer : waitingNumber<br>String(enum) : orderType<br>String(enum) : status<br>datetime : createdAt<br>Integer : amountBeforeDiscount<br>Integer : discountAmount<br>Integer : finalAmount<br>payment{}<br>items[] | BranchOrderService | OrderListView | |
| 주문 내역 | 영수증 출력 | 없음 | - | - | - | OrderListView | 버튼에 @click 자체가 없음 - 미구현 |
| 주문 내역 | 결제 취소 | 없음 | - | - | - | OrderListView | 버튼에 @click 자체가 없음 - 미구현 (다른 화면의 주문상태변경 API와는 별개) |
| 상품 관리 | 상품 리스트 | GET /api/branch/products | 없음 | Long : productId<br>String : categoryName<br>String : productName<br>Integer : basePrice<br>String : imageUrl<br>Boolean : isVisible | BranchProductService | BranchProductView | |
| 상품 관리 | 상품 노출 | PATCH /api/branch/products/{productId}/visibility | Long : productId<br>Boolean : isVisible | Long : productId<br>String : categoryName<br>String : productName<br>Integer : basePrice<br>String : imageUrl<br>Boolean : isVisible | BranchProductService | BranchProductView | |
| 재고 관리 | 재고 리스트 | GET /api/branch/inventory | 없음 | (대시보드 재고현황과 동일 응답) | BranchInventoryService | InventoryView | |
| 재고 관리 | 재고 조정 | PATCH /api/branch/inventory/{inventoryId} | Long : inventoryId<br>String : type<br>Integer : grams | 204 (body 없음) — 변경값(DB): Integer:changeQuantity / Integer:quantityAfter / String(enum):transactionType | BranchInventoryService | InventoryView | |
| 재고 관리 | 재고 신청 | POST /api/branch/stock-requests | String : requestReason<br>String(enum) : urgency<br>List : items | Long : stockRequestId<br>String : requestNumber<br>Long : branchId<br>String : branchName<br>String : requesterAdminName<br>String(enum) : requestStatus<br>String(enum) : urgency<br>String : requestReason<br>String : rejectionReason<br>datetime : requestedAt<br>String : processedAdminName<br>datetime : processedAt<br>String : shipmentNumber<br>String : driverName<br>datetime : estimatedArrivalAt<br>datetime : shippedAt<br>datetime : deliveredAt<br>List&lt;StockRequestItemResponse&gt; : items | BranchStockRequestService | InventoryView | |
| 재고 관리 | 배송 완료(입고) 확인 | POST /api/branch/inventory/{inventoryId}/receive | Long : inventoryId | 204 (body 없음) — 변경값(DB): Integer:changeQuantity / Integer:quantityAfter / String(enum):transactionType / String(enum):requestStatus | BranchInventoryService | InventoryView | |
| 입고 신청 현황 | 신청 리스트 | GET /api/branch/stock-requests | 없음 | (재고 신청과 동일 응답) | BranchStockRequestService | StockRequestsView | |
| 입고 신청 현황 | 신청 취소 | PATCH /api/branch/stock-requests/{id}/cancel | Long : stockRequestId | 204 (body 없음) | BranchStockRequestService | StockRequestsView | |
| 입고 신청 현황 | 수령 확인 | PATCH /api/branch/stock-requests/{id}/confirm-receipt | Long : stockRequestId | StockRequestResponse (상태값 CLOSED로 업데이트, 나머지는 신청 리스트와 동일) | BranchStockRequestService | StockRequestsView | |
| 이벤트 관리 | 이벤트 리스트 | GET /api/branch/events | 없음 | Long : eventId<br>String : eventName<br>String(enum) : eventType<br>String(enum) : benefitType<br>BigDecimal : discountRate<br>Integer : discountAmount<br>datetime : startAt<br>datetime : endAt<br>Long : selectedFlavorId<br>String : selectedFlavorName<br>List&lt;FlavorOptionResponse&gt; : flavorOptions | BranchEventService | EventsView | |
| 이벤트 관리 | 이벤트 적용 | POST /api/branch/events/{eventId}/flavor | Long : eventId<br>Long : flavorId | (이벤트 리스트와 동일 응답 객체) | BranchEventService | EventsView | DB 변경값: Event:event / Branch:branch / Flavor:flavor / datetime:createdAt (event_branch_flavor 테이블에 새로 insert) |
| 판매 통계 | 매출 내역 | GET /api/branch/sales | 없음 | summary: {Long:revenue, Long:order_count, Long:average_amount}<br>flavors: [{String:label, Long:quantity}]<br>sizes: [{String:label, Long:quantity}]<br>hourly: [{String:label, Long:quantity}]<br>monthly: [{String:label, Long:quantity, Long:revenue}] | BranchSalesService | SalesView | 추후에 통계 화면 재설계해야함. 응답이 고정 DTO가 아니라 JdbcTemplate로 직접 집계한 Map<String,Object>임 |
| 채팅 | 메시지 목록 | GET /api/branch/chat/messages | 없음 | Long : chatMessageId<br>Long : senderAdminId<br>String : senderName<br>Boolean : fromHq<br>String : messageContent<br>datetime : createdAt | BranchChatService | ChatView | |
| 채팅 | 메시지 전송 | POST /api/branch/chat/messages | String : content | (메시지 목록과 동일 객체, 201 Created) | BranchChatService | ChatView | |
| 공지사항 | 공지사항 상세 | GET /api/branch/notices (전용 엔드포인트 없음) | 없음 | String(enum) : noticeType<br>Long : id<br>String : title<br>String : content<br>String : imageUrl<br>datetime : postedAt<br>datetime : endAt | BranchNoticeService | NoticeDetailView | 상세 페이지 하나 보려고 전체 공지 목록을 매번 다시 불러옴 - 대시보드/주문관리와 같은 패턴 |

---

## curl 테스트

<details>
<summary><b>mac</b></summary>

<details>
<summary>0) 로그인 (공통, 제일 먼저 실행)</summary>

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/branch-auth/db-login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"admin1234"}' | jq -r '.token')
echo "TOKEN: $TOKEN"
```

</details>

<details>
<summary>공통(사이드바)</summary>

```bash
curl -s -X POST http://localhost:8080/api/branch/presence/heartbeat -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/branch/staff-call -H "Authorization: Bearer $TOKEN" | jq
curl -s -X DELETE http://localhost:8080/api/branch/staff-call -H "Authorization: Bearer $TOKEN"
```

</details>

<details>
<summary>대시보드</summary>

```bash
curl -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer $TOKEN" | jq
curl -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer $TOKEN" | jq
curl -s http://localhost:8080/api/branch/notices -H "Authorization: Bearer $TOKEN" | jq
```

</details>

<details>
<summary>주문 관리</summary>

```bash
curl -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer $TOKEN" | jq

curl -s -X PATCH http://localhost:8080/api/branch/orders/1/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"MAKING"}' | jq

curl -s -X PATCH http://localhost:8080/api/branch/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"isBusy":true,"estimatedWaitMinutes":15}' | jq
```

</details>

<details>
<summary>주문 내역</summary>

```bash
curl -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer $TOKEN" | jq
curl -s "http://localhost:8080/api/branch/orders?date=2026-07-22" -H "Authorization: Bearer $TOKEN" | jq
curl -s http://localhost:8080/api/branch/orders/dates -H "Authorization: Bearer $TOKEN" | jq
curl -s http://localhost:8080/api/branch/orders/1 -H "Authorization: Bearer $TOKEN" | jq
# 영수증 출력 / 결제 취소 → 미구현, curl 테스트 대상 없음
```

</details>

<details>
<summary>상품 관리</summary>

```bash
curl -s http://localhost:8080/api/branch/products -H "Authorization: Bearer $TOKEN" | jq

curl -s -X PATCH http://localhost:8080/api/branch/products/1/visibility \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"isVisible":false}' | jq
```

</details>

<details>
<summary>재고 관리</summary>

```bash
curl -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer $TOKEN" | jq

curl -s -X PATCH http://localhost:8080/api/branch/inventory/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"IN","grams":500}' | jq

curl -s -X POST http://localhost:8080/api/branch/stock-requests \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"requestReason":"재고 부족","urgency":"NORMAL","items":[{"flavorId":1,"requestedQuantity":3}]}' | jq

curl -s -X POST http://localhost:8080/api/branch/inventory/1/receive -H "Authorization: Bearer $TOKEN" | jq
```

</details>

<details>
<summary>입고 신청 현황</summary>

```bash
curl -s "http://localhost:8080/api/branch/stock-requests?page=0&size=20" -H "Authorization: Bearer $TOKEN" | jq
curl -s -X PATCH http://localhost:8080/api/branch/stock-requests/1/cancel -H "Authorization: Bearer $TOKEN"
curl -s -X PATCH http://localhost:8080/api/branch/stock-requests/1/confirm-receipt -H "Authorization: Bearer $TOKEN" | jq
```

</details>

<details>
<summary>이벤트 관리</summary>

```bash
curl -s http://localhost:8080/api/branch/events -H "Authorization: Bearer $TOKEN" | jq

curl -s -X POST http://localhost:8080/api/branch/events/1/flavor \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"flavorId":1}' | jq
```

</details>

<details>
<summary>판매 통계</summary>

```bash
curl -s http://localhost:8080/api/branch/sales -H "Authorization: Bearer $TOKEN" | jq
```

</details>

<details>
<summary>채팅</summary>

```bash
curl -s http://localhost:8080/api/branch/chat/messages -H "Authorization: Bearer $TOKEN" | jq

curl -s -X POST http://localhost:8080/api/branch/chat/messages \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"테스트 메시지"}' | jq
```

</details>

</details>

<details>
<summary><b>windows</b></summary>

<details>
<summary>jq 설치 (한 번만)</summary>

```bat
winget install jqlang.jq --source winget
```

</details>

<details>
<summary>0) 로그인 (공통, 제일 먼저 실행)</summary>

```bat
for /f "delims=" %A in ('curl.exe -s -X POST http://localhost:8080/api/branch-auth/db-login -H "Content-Type: application/json" -d "{\"loginId\":\"admin\",\"password\":\"admin1234\"}" ^| jq -r ".token"') do set TOKEN=%A
echo TOKEN: %TOKEN%
```

</details>

<details>
<summary>공통(사이드바)</summary>

```bat
curl.exe -s -X POST http://localhost:8080/api/branch/presence/heartbeat -H "Authorization: Bearer %TOKEN%"
curl.exe -s http://localhost:8080/api/branch/staff-call -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s -X DELETE http://localhost:8080/api/branch/staff-call -H "Authorization: Bearer %TOKEN%"
```

</details>

<details>
<summary>대시보드</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s http://localhost:8080/api/branch/notices -H "Authorization: Bearer %TOKEN%" | jq
```

</details>

<details>
<summary>주문 관리</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer %TOKEN%" | jq

curl.exe -s -X PATCH http://localhost:8080/api/branch/orders/1/status -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"status\":\"MAKING\"}" | jq

curl.exe -s -X PATCH http://localhost:8080/api/branch/status -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"isBusy\":true,\"estimatedWaitMinutes\":15}" | jq
```

</details>

<details>
<summary>주문 내역</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s "http://localhost:8080/api/branch/orders?date=2026-07-22" -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s http://localhost:8080/api/branch/orders/dates -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s http://localhost:8080/api/branch/orders/1 -H "Authorization: Bearer %TOKEN%" | jq
:: 영수증 출력 / 결제 취소 → 미구현, curl 테스트 대상 없음
```

</details>

<details>
<summary>상품 관리</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/products -H "Authorization: Bearer %TOKEN%" | jq

curl.exe -s -X PATCH http://localhost:8080/api/branch/products/1/visibility -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"isVisible\":false}" | jq
```

</details>

<details>
<summary>재고 관리</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer %TOKEN%" | jq

curl.exe -s -X PATCH http://localhost:8080/api/branch/inventory/1 -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"type\":\"IN\",\"grams\":500}" | jq

curl.exe -s -X POST http://localhost:8080/api/branch/stock-requests -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"requestReason\":\"재고 부족\",\"urgency\":\"NORMAL\",\"items\":[{\"flavorId\":1,\"requestedQuantity\":3}]}" | jq

curl.exe -s -X POST http://localhost:8080/api/branch/inventory/1/receive -H "Authorization: Bearer %TOKEN%" | jq
```

</details>

<details>
<summary>입고 신청 현황</summary>

```bat
curl.exe -s "http://localhost:8080/api/branch/stock-requests?page=0&size=20" -H "Authorization: Bearer %TOKEN%" | jq
curl.exe -s -X PATCH http://localhost:8080/api/branch/stock-requests/1/cancel -H "Authorization: Bearer %TOKEN%"
curl.exe -s -X PATCH http://localhost:8080/api/branch/stock-requests/1/confirm-receipt -H "Authorization: Bearer %TOKEN%" | jq
```

</details>

<details>
<summary>이벤트 관리</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/events -H "Authorization: Bearer %TOKEN%" | jq

curl.exe -s -X POST http://localhost:8080/api/branch/events/1/flavor -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"flavorId\":1}" | jq
```

</details>

<details>
<summary>판매 통계</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/sales -H "Authorization: Bearer %TOKEN%" | jq
```

</details>

<details>
<summary>채팅</summary>

```bat
curl.exe -s http://localhost:8080/api/branch/chat/messages -H "Authorization: Bearer %TOKEN%" | jq

curl.exe -s -X POST http://localhost:8080/api/branch/chat/messages -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"content\":\"테스트 메시지\"}" | jq
```

</details>

</details>
