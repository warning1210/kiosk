# macOS

## 0) 로그인해서 토큰 발급 (공통, 제일 먼저 1번 실행)
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/branch-auth/db-login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"admin1234"}' | jq -r '.token')
echo "TOKEN: $TOKEN"
```

## 대시보드
```bash
# 주문 전체 조회 → 화면에서 신규/처리중/완료 카운트, 오늘 매출, "실시간 주문" 리스트 계산에 씀
curl -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer $TOKEN" | jq

# 재고 전체 조회 → "재고 부족 알림"(remainingGrams<=3000)이랑 "재고 배송 현황"(requestStatus 있는 것) 둘 다 이 응답 하나로 만듦
curl -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer $TOKEN" | jq

# 공지+이벤트 통합 조회 → 상단 공지 배너에 씀
curl -s http://localhost:8080/api/branch/notices -H "Authorization: Bearer $TOKEN" | jq
```

## 주문 관리
```bash
# 주문 현황 조회 → 화면에 뜨는 주문 카드 목록
curl -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer $TOKEN" | jq

# 주문 상태 변경 → "조리 시작"/"준비 완료"/"수령 완료" 버튼 눌렀을 때 (orderId는 실제 값으로 교체)
curl -s -X PATCH http://localhost:8080/api/branch/orders/1/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"status":"MAKING"}' | jq

# 바빠요 버튼 → 매장 혼잡 표시 켜고 대기시간 입력
curl -s -X PATCH http://localhost:8080/api/branch/status \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"isBusy":true,"estimatedWaitMinutes":15}' | jq
```

## 주문 내역
```bash
# 주문 내역 조회 → 리스트 화면
curl -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer $TOKEN" | jq

# 달력에서 날짜 선택 → 그 날짜 주문만 필터
curl -s "http://localhost:8080/api/branch/orders?date=2026-07-22" -H "Authorization: Bearer $TOKEN" | jq

# 리스트에서 주문 클릭 → 상세 내역(결제정보/항목) 팝업 (orderId는 실제 값으로 교체)
curl -s http://localhost:8080/api/branch/orders/1 -H "Authorization: Bearer $TOKEN" | jq

# 영수증 출력 / 결제 취소 버튼 → 프론트에 @click 자체가 없어서 curl로 테스트할 API가 없음 (미구현)
```

## 상품 관리
```bash
# 상품 리스트 조회 → 화면에 뜨는 상품 카드 + 노출 스위치 상태
curl -s http://localhost:8080/api/branch/products -H "Authorization: Bearer $TOKEN" | jq

# 노출 스위치 토글 → 우리 지점 키오스크 메뉴에서 이 상품 숨김/노출 (productId는 실제 값으로 교체)
curl -s -X PATCH http://localhost:8080/api/branch/products/1/visibility \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"isVisible":false}' | jq
```

## 재고 관리
```bash
# 재고 리스트 조회 → 맛별 잔량/상태 카드
curl -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer $TOKEN" | jq

# 재고 수동 조정 → "입고 처리"/"실사 보정" 버튼 눌러서 수량 직접 입력 (inventoryId는 실제 값으로 교체)
curl -s -X PATCH http://localhost:8080/api/branch/inventory/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"IN","grams":500}' | jq
```

## 입고 신청 현황
```bash
# 신청 상태 목록 → 우리 지점이 본사에 올린 재고 신청 전체 진행 상황(대기/승인/배송중/완료)
curl -s "http://localhost:8080/api/branch/stock-requests?page=0&size=20" -H "Authorization: Bearer $TOKEN" | jq

# 입고 확정 버튼 → 본사가 배송완료(DELIVERED) 처리한 건을 지점이 "실제로 받았다"고 확인, 그때 재고량 반영됨 (inventoryId는 실제 값으로 교체)
curl -s -X POST http://localhost:8080/api/branch/inventory/1/receive -H "Authorization: Bearer $TOKEN" | jq
```

---

# Windows cmd

```bat
:: jq 설치 (한 번만)
winget install jqlang.jq --source winget
```

## 0) 로그인해서 토큰 발급 (공통, 제일 먼저 1번 실행)
```bat
for /f "delims=" %A in ('curl.exe -s -X POST http://localhost:8080/api/branch-auth/db-login -H "Content-Type: application/json" -d "{\"loginId\":\"admin\",\"password\":\"admin1234\"}" ^| jq -r ".token"') do set TOKEN=%A
echo TOKEN: %TOKEN%
```

## 대시보드
```bat
:: 주문 전체 조회 → 신규/처리중/완료 카운트, 오늘 매출, "실시간 주문" 리스트 계산에 씀
curl.exe -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer %TOKEN%" | jq

:: 재고 전체 조회 → "재고 부족 알림"이랑 "재고 배송 현황" 둘 다 이 응답 하나로 만듦
curl.exe -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer %TOKEN%" | jq

:: 공지+이벤트 통합 조회 → 상단 공지 배너
curl.exe -s http://localhost:8080/api/branch/notices -H "Authorization: Bearer %TOKEN%" | jq
```

## 주문 관리
```bat
:: 주문 현황 조회 → 주문 카드 목록
curl.exe -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer %TOKEN%" | jq

:: 주문 상태 변경 → 조리시작/준비완료/수령완료 버튼
curl.exe -s -X PATCH http://localhost:8080/api/branch/orders/1/status -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"status\":\"MAKING\"}" | jq

:: 바빠요 버튼 → 매장 혼잡 표시 + 대기시간
curl.exe -s -X PATCH http://localhost:8080/api/branch/status -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"isBusy\":true,\"estimatedWaitMinutes\":15}" | jq
```

## 주문 내역
```bat
:: 주문 내역 조회
curl.exe -s http://localhost:8080/api/branch/orders -H "Authorization: Bearer %TOKEN%" | jq

:: 달력에서 날짜 선택 → 그 날짜만 필터
curl.exe -s "http://localhost:8080/api/branch/orders?date=2026-07-22" -H "Authorization: Bearer %TOKEN%" | jq

:: 리스트 클릭 → 상세 내역(결제정보/항목) 팝업
curl.exe -s http://localhost:8080/api/branch/orders/1 -H "Authorization: Bearer %TOKEN%" | jq

:: 영수증 출력 / 결제 취소 → 프론트에 @click 자체가 없어서 curl로 테스트할 API가 없음 (미구현)
```

## 상품 관리
```bat
:: 상품 리스트 조회 → 상품 카드 + 노출 스위치 상태
curl.exe -s http://localhost:8080/api/branch/products -H "Authorization: Bearer %TOKEN%" | jq

:: 노출 스위치 토글 → 우리 지점 키오스크 메뉴에서 숨김/노출
curl.exe -s -X PATCH http://localhost:8080/api/branch/products/1/visibility -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"isVisible\":false}" | jq
```

## 재고 관리
```bat
:: 재고 리스트 조회 → 맛별 잔량/상태 카드
curl.exe -s http://localhost:8080/api/branch/inventory -H "Authorization: Bearer %TOKEN%" | jq

:: 재고 수동 조정 → 입고처리/실사보정 버튼
curl.exe -s -X PATCH http://localhost:8080/api/branch/inventory/1 -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"type\":\"IN\",\"grams\":500}" | jq
```

## 입고 신청 현황
```bat
:: 신청 상태 목록 → 우리 지점이 본사에 올린 재고 신청 진행 상황
curl.exe -s "http://localhost:8080/api/branch/stock-requests?page=0&size=20" -H "Authorization: Bearer %TOKEN%" | jq

:: 입고 확정 버튼 → 본사가 배송완료 처리한 건을 지점이 수령 확인, 그때 재고량 반영
curl.exe -s -X POST http://localhost:8080/api/branch/inventory/1/receive -H "Authorization: Bearer %TOKEN%" | jq
```
