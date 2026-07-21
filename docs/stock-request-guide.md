# 재고 신청 기능 — 구현 내용 및 테스트 방법

- 브랜치: `feature/stock-request-port` (develop3.0 기반)
- 출처: `feature/stock-request` 브랜치 코드를 develop3.0 구조·인증에 맞춰 이식
- 범위: **백엔드 API만**. 화면(프론트엔드)은 아직 없음

---

## 1. 무엇이 구현됐나

### 완성된 업무 흐름

```
[지점] 재고 신청 등록            PENDING
   ↓
[본사] 승인                      PREPARING        (또는 반려 → REJECTED)
   ↓
[본사] 배송 등록(운송장 입력)     SHIPPING
   ↓
[지점] 수령 확인                 DELIVERED
   ↓
[지점] 재고화면에서 입고 처리      CLOSED + 재고 증가   ← 기존에 이미 있던 기능
```

지점은 본사가 아직 손대지 않은 신청(PENDING)을 스스로 **취소**(→ CLOSED)할 수도 있습니다.

### 지점 API — `/api/branch/stock-requests`

| 메서드 | 경로 | 하는 일 | 요구사항 |
|---|---|---|---|
| POST | `/api/branch/stock-requests` | 재고 신청 등록 (201 반환) | BR-001 |
| GET | `/api/branch/stock-requests` | 내 지점 신청 목록 (페이지·상태필터) | BR-002 |
| PATCH | `/api/branch/stock-requests/{id}/cancel` | 신청 취소 (204, PENDING만 가능) | BR-002 |
| PATCH | `/api/branch/stock-requests/{id}/confirm-receipt` | 수령 확인 (SHIPPING만 가능) | BR-003, BR-010 |

### 본사 API — `/api/hq/stock-requests`

| 메서드 | 경로 | 하는 일 | 요구사항 |
|---|---|---|---|
| GET | `/api/hq/stock-requests` | 전 지점 신청 검색 (상태·지점·기간·키워드) | HQ-001 |
| GET | `/api/hq/stock-requests/summary` | 상태별 건수 요약 | HQ-004 |
| PATCH | `/api/hq/stock-requests/{id}/approve` | 승인 (PENDING만 가능) | HQ-002 |
| PATCH | `/api/hq/stock-requests/{id}/reject` | 반려 (사유 필수) | HQ-003 |
| PATCH | `/api/hq/stock-requests/{id}/ship` | 배송 등록 (PREPARING만 가능) | HQ-013 |

### 함께 적용된 안전장치

- **다른 지점 데이터 차단**: 지점 ID를 클라이언트에서 받지 않고 로그인 토큰에서 서버가 직접 판별. 남의 지점 신청 번호를 넣으면 403.
- **동시 처리 방지**: 상태를 바꾸는 모든 API는 DB 행을 잠근(`SELECT ... FOR UPDATE`) 뒤 상태를 확인. 두 명이 동시에 승인/취소해도 한쪽만 성공.
- **상태 전이 검사**: 예를 들어 이미 승인된 건은 다시 승인 불가(409 반환).
- **중복·오입력 차단**: 같은 맛 두 줄 신청, 존재하지 않는 맛 ID, 수량 0 이하, 반려 사유 누락 → 모두 400.
- **검증 실패 응답 통일**: 프론트가 항상 `{"message": "..."}` 형태만 읽으면 되도록 기존 예외 핸들러에 통합.

### 이식하면서 원본과 다르게 한 것

| 항목 | 원본 브랜치 | 이식본 | 이유 |
|---|---|---|---|
| 인증 | `X-Admin-Id` 헤더 (임시 스캐폴딩) | `Authorization: Bearer <토큰>` | develop3.0의 실제 로그인 사용 |
| 수령 확인 시 재고 | 신청 수량을 재고에 **그대로** 더함 | 재고는 건드리지 않고 상태만 변경 | 원본대로면 **2통 입고가 2g으로** 들어감. develop3.0은 재고를 그램으로 관리하고 `수량 × 3000g`으로 환산하므로, 환산 로직이 있는 기존 입고 처리에 재고 반영을 맡김 (이중 증가 방지) |
| 예외 핸들러 | `ApiExceptionHandler` 새 클래스 | 기존 `GlobalExceptionHandler`에 통합 | 두 개가 같은 예외를 처리해 충돌 |
| 재고 조회 API | 자체 `BranchInventoryController` | 이식 안 함 | develop3.0 것과 경로가 겹쳐 **서버가 아예 기동 실패**. 기존 것이 기능도 더 많음 |

> **수량 단위**: 신청 수량은 **통(tub) 개수**입니다. 1통 = 3,000g.

---

## 2. 테스트 준비

### 서버 실행

```bash
# 백엔드 (:8080)
cd backend && mvn spring-boot:run

# 프론트엔드 (:5173) — 이번 기능은 화면이 없어서 API 테스트에는 불필요
cd frontend && npm run dev
```

### 테스트 계정 현황 (공유 DB 기준)

| 계정 | 역할 | 소속 | DB 로그인 | 비고 |
|---|---|---|---|---|
| `admin` / `admin1234` | 지점장 | 강남점(1) | ✅ 가능 | **지점 테스트용** |
| `hadmin` | 본사 | - | ✅ 가능 | **본사 테스트용** (비밀번호는 팀에서 확인) |
| `hq_admin_test` | 본사 | - | ❌ 불가 | 비밀번호 해시가 `N/A`로 들어가 있어 로그인 안 됨 |
| `gurwlsl0903`, `user1` | 지점장 | 5, 10 | ❌ 불가 | Firebase 전용 계정 (`FIREBASE$...` 마커) |

---

## 3. 테스트 절차 (curl)

### STEP 0. 토큰 발급

```bash
# 지점 토큰
BRANCH_TOKEN=$(curl -s -X POST http://localhost:8080/api/branch-auth/db-login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"admin","password":"admin1234"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "지점 토큰: ${BRANCH_TOKEN:0:20}..."

# 본사 토큰 (비밀번호를 실제 값으로 바꿔주세요)
HQ_TOKEN=$(curl -s -X POST http://localhost:8080/api/hq-auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId":"hadmin","password":"실제비밀번호"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "본사 토큰: ${HQ_TOKEN:0:20}..."
```

### STEP 1. [지점] 재고 신청 등록

```bash
curl -s -X POST http://localhost:8080/api/branch/stock-requests \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requestReason": "주말 대비 재고 확보",
    "urgency": "HIGH",
    "items": [
      { "flavorId": 16, "requestedQuantity": 2 },
      { "flavorId": 13, "requestedQuantity": 1 }
    ]
  }'
```

- `urgency`: `LOW` / `NORMAL` / `HIGH` (생략 시 NORMAL)
- 맛 목록은 `curl -s http://localhost:8080/api/flavors` 로 확인
- **응답의 `stockRequestId`를 메모** → 이후 단계에서 사용

기대: HTTP 201, `requestStatus: "PENDING"`, `requestNumber: "REQ-20260721-{id}"`

### STEP 2. [지점] 내 신청 목록 조회

```bash
# 전체
curl -s -H "Authorization: Bearer $BRANCH_TOKEN" \
  "http://localhost:8080/api/branch/stock-requests?page=0&size=10"

# 대기중인 것만
curl -s -H "Authorization: Bearer $BRANCH_TOKEN" \
  "http://localhost:8080/api/branch/stock-requests?status=PENDING"
```

### STEP 3. [본사] 신청 목록·요약 조회

```bash
# 요약 (상태별 건수)
curl -s -H "Authorization: Bearer $HQ_TOKEN" \
  http://localhost:8080/api/hq/stock-requests/summary

# 전 지점 목록
curl -s -H "Authorization: Bearer $HQ_TOKEN" \
  "http://localhost:8080/api/hq/stock-requests?page=0&size=10"

# 검색 조건 조합 (전부 선택사항)
curl -s -H "Authorization: Bearer $HQ_TOKEN" \
  "http://localhost:8080/api/hq/stock-requests?status=PENDING&branchId=1&keyword=요거트"
curl -s -H "Authorization: Bearer $HQ_TOKEN" \
  "http://localhost:8080/api/hq/stock-requests?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59"
```

- `keyword`는 **신청번호 / 지점명 / 맛 이름**을 한 번에 검색

### STEP 4. [본사] 승인

```bash
ID=여기에_STEP1의_stockRequestId
curl -s -X PATCH -H "Authorization: Bearer $HQ_TOKEN" \
  http://localhost:8080/api/hq/stock-requests/$ID/approve
```

기대: `requestStatus: "PREPARING"`, 각 품목의 `approvedQuantity`가 신청 수량으로 채워짐

### STEP 5. [본사] 배송 등록

```bash
curl -s -X PATCH http://localhost:8080/api/hq/stock-requests/$ID/ship \
  -H "Authorization: Bearer $HQ_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "trackingNumber": "1234-5678-9012",
    "courierName": "CJ대한통운",
    "driverName": "김기사",
    "estimatedArrivalAt": "2026-07-24T14:00:00"
  }'
```

기대: `requestStatus: "SHIPPING"`, 운송 정보가 응답에 반영

### STEP 6. [지점] 수령 확인

```bash
curl -s -X PATCH -H "Authorization: Bearer $BRANCH_TOKEN" \
  http://localhost:8080/api/branch/stock-requests/$ID/confirm-receipt
```

기대: `requestStatus: "DELIVERED"`, `deliveredAt` 채워짐
(재고 수량은 아직 안 늘어남 — 다음 단계에서 반영)

### STEP 7. [지점] 재고 반영 확인 (기존 기능)

```bash
# 재고 목록에서 해당 맛의 inventoryId 확인
curl -s -H "Authorization: Bearer $BRANCH_TOKEN" \
  http://localhost:8080/api/branch/inventory

# 입고 처리 → 재고 증가 (통 × 3000g)
curl -s -X POST -H "Authorization: Bearer $BRANCH_TOKEN" \
  http://localhost:8080/api/branch/inventory/{inventoryId}/receive
```

---

## 4. 예외 상황 테스트 (중요)

이 API들이 **잘못된 요청을 제대로 막는지** 확인하는 항목입니다.

| # | 테스트 | 명령 | 기대 결과 |
|---|---|---|---|
| 1 | 토큰 없이 호출 | `curl -s http://localhost:8080/api/branch/stock-requests` | 401 `{"message":"로그인이 필요합니다."}` |
| 2 | 지점 토큰으로 본사 API 호출 | `curl -s -H "Authorization: Bearer $BRANCH_TOKEN" http://localhost:8080/api/hq/stock-requests` | 403 (지점 계정은 본사 기능 사용 불가) |
| 3 | 같은 맛 중복 신청 | items에 `flavorId:16`을 두 줄 | 400 `같은 상품을 중복해서 신청할 수 없습니다` |
| 4 | 없는 맛 ID | `flavorId: 999999` | 400 `존재하지 않는 상품이 포함되어 있습니다` |
| 5 | 수량 0 | `requestedQuantity: 0` | 400 `신청 수량은 1개 이상이어야 합니다` |
| 6 | 빈 신청서 | `items: []` | 400 `신청할 상품을 1개 이상 선택해주세요` |
| 7 | 반려 사유 없이 반려 | `-d '{"rejectionReason":""}'` | 400 `반려 사유를 입력해주세요` |
| 8 | 이미 승인된 건 재승인 | STEP4를 두 번 실행 | 409 `대기중인 신청만 승인할 수 있습니다` |
| 9 | 승인 안 된 건 배송 등록 | PENDING 상태에서 STEP5 | 409 `배송 준비중인 신청만 배송 등록할 수 있습니다` |
| 10 | 배송 안 된 건 수령 확인 | PENDING 상태에서 STEP6 | 409 `배송중인 신청만 수령 확인할 수 있습니다` |
| 11 | **다른 지점 신청 조작** | 강남점 토큰으로 다른 지점의 신청 ID를 cancel | 403 `다른 지점의 신청 건은 처리할 수 없습니다` |
| 12 | 없는 신청 ID | `/api/branch/stock-requests/999999/cancel` | 404 `신청 내역을 찾을 수 없습니다` |

**11번이 가장 중요합니다** — 주소창의 숫자만 바꿔서 남의 지점 데이터를 건드리는 문제(IDOR)를 막는지 확인하는 테스트입니다.

### 반려 흐름 테스트

```bash
# 새 신청을 하나 더 만든 뒤 (STEP 1 반복)
curl -s -X PATCH http://localhost:8080/api/hq/stock-requests/$ID2/reject \
  -H "Authorization: Bearer $HQ_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"rejectionReason":"현재 본사 재고 부족으로 다음 주 재신청 바랍니다"}'
```

기대: `requestStatus: "REJECTED"`, `rejectionReason`이 지점 조회 시에도 보임

### 취소 흐름 테스트

```bash
# PENDING 상태의 신청을 지점이 취소
curl -s -X PATCH -H "Authorization: Bearer $BRANCH_TOKEN" \
  http://localhost:8080/api/branch/stock-requests/$ID3/cancel -w "\n[%{http_code}]\n"
```

기대: 204 (본문 없음). 이후 조회하면 `CLOSED`

---

## 5. DB에서 직접 확인하기

```bash
mysql -h 13.125.213.12 -P 3307 -u kiosk -pkiosk6578 kiosk -e "
SELECT stock_request_id, request_number, request_status, urgency,
       tracking_number, requested_at, delivered_at
FROM stock_request ORDER BY stock_request_id DESC LIMIT 10;"

# 품목까지 함께
mysql -h 13.125.213.12 -P 3307 -u kiosk -pkiosk6578 kiosk -e "
SELECT sr.request_number, sr.request_status, f.flavor_name,
       i.requested_quantity, i.approved_quantity
FROM stock_request sr
JOIN stock_request_item i ON i.stock_request_id = sr.stock_request_id
JOIN flavor f ON f.flavor_id = i.flavor_id
ORDER BY sr.stock_request_id DESC LIMIT 20;"
```

> ⚠️ 이 DB는 **팀 공유 DB**입니다. 테스트하면 팀원에게도 그 데이터가 보입니다.
> 신청번호에 `TEST` 같은 표시를 남기거나, 테스트 후 정리하는 편이 좋습니다.

---

## 6. 아직 안 된 것

| 항목 | 상태 |
|---|---|
| **프론트엔드 화면 전체** | `/admin/stock-requests`는 여전히 빈 자리표시자(`AdminComingSoonView`). 지점 사이드바의 "입고 신청 현황"도 `href="#"` 링크 |
| 지점 신청 등록 화면 | 없음 (API만 존재) |
| 본사 승인/반려/배송 화면 | 없음 (API만 존재) |
| 배송 지연 모니터링 (HQ-015) | 미구현 |
| 배송 상태 변경 이력 (HQ-014) | 미구현 (현재는 최종 상태만 저장) |
| 알림 (BR-011 배송 지연 알림 등) | 미구현 — FCM 발송 코드가 프로젝트에 없음 |

> 원본 `feature/stock-request` 브랜치에는 Vue 화면이 있지만, 로그인 대신 "관리자를 직접 고르는 임시 화면"을 전제로 만들어져 있어 그대로는 못 씁니다. 실제 로그인 기준으로 다시 만들어야 합니다.

---

## 7. 검증 완료 상태

- `mvn compile` 통과
- 서버 정상 기동 (`Started KioskApplication`)
- 신규 엔드포인트 9개 전부 매핑 등록 확인, 미인증 시 401 정상
- 기존 API 회귀 없음 (`/api/products` 200, `/api/branch/inventory` 401)
- 지점 로그인 → 신청 목록 조회 **실제 동작 확인**
  (develop3.0의 자동 발주 기능이 만든 신청 `AUTO-260720161157651`이 새 API로 정상 조회됨)

> 쓰기 계열(신청 생성/승인/배송)은 공유 DB에 데이터를 남기므로 자동 검증하지 않았습니다.
> 위 STEP 1~7을 순서대로 실행하면 전체 흐름을 직접 확인할 수 있습니다.
