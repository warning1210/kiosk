-- ============================================================
-- 02-seed.sql  :  개발/시연용 기본 데이터
-- ------------------------------------------------------------
-- 01-schema.sql 로 테이블이 만들어진 "직후" 자동으로 실행된다
-- (docker-compose 가 db/init 폴더의 .sql 을 파일 이름 순서대로 실행함).
--
-- 목적: 키오스크 주문 흐름을 실제로 끝까지 돌려보려면 DB 에
--       지점 1곳 + 상품 + 맛이 최소한 들어 있어야 한다.
--       (주문 결제 단계에서 백엔드가 productId 로 상품을 다시 조회하기 때문)
--
-- 주의: 이 파일은 "비어 있는 DB 를 처음 만들 때" 한 번만 자동 실행된다.
--       이미 데이터가 들어있는 DB 에 다시 넣고 싶으면 아래처럼 직접 실행한다.
--         docker exec -i kiosk-mysql mysql -ukiosk -pkiosk1234 kiosk < db/init/02-seed.sql
--       (INSERT ... 앞에 있는 값들이 이미 있으면 중복 오류가 날 수 있으니,
--        재실행이 필요하면 각 테이블을 먼저 비우고 넣는 것을 권장)
-- ============================================================

-- ── 지점 ──────────────────────────────────────────────
-- 지금은 키오스크 1대 = 지점 1곳(branch_id = 1) 으로 가정한다.
-- (프론트 orderFlow.js 의 checkoutPayload 에서 branchId: 1 로 고정해 보냄)
INSERT INTO `branch`
  (`branch_id`, `branch_name`, `region`, `address`, `phone`, `operation_status`,
   `is_busy`, `kiosk_status`, `created_at`, `updated_at`)
VALUES
  (1, '베스킨라빈스 강남점', '서울', '서울시 강남구 테헤란로 1', '02-000-0000', 'ACTIVE',
   0, 'ACTIVE', NOW(), NOW());

-- ── 카테고리 ──────────────────────────────────────────
INSERT INTO `category`
  (`category_id`, `category_name`, `display_order`, `is_visible`, `created_at`, `updated_at`)
VALUES
  (1, '아이스크림', 1, 1, NOW(), NOW()),
  (2, '커피',       2, 1, NOW(), NOW());

-- ── 상품(사이즈) ─────────────────────────────────────
-- product_id 는 프론트 더미데이터(frontend/src/data/menuDummy.js)의 값과
-- "똑같이" 맞춰서 넣는다. 그래야 화면에서 고른 상품이 결제 때 DB 에서 조회된다.
--
-- selectable_flavor_count : 고를 수 있는 맛 개수 (싱글=1, 파인트=3, 쿼터=4 ...)
-- container_policy        : NONE(용기없음) / CUP_ONLY(컵만) / CUP_OR_CONE(컵·콘 둘다)
-- is_large                : 대용량(패밀리/하프갤런) 여부
INSERT INTO `product`
  (`product_id`, `category_id`, `product_name`, `base_price`,
   `requires_flavor_selection`, `selectable_flavor_count`, `container_policy`,
   `is_large`, `is_new`, `sale_status`, `is_visible`, `created_at`, `updated_at`)
VALUES
  -- 아이스크림
  (101, 1, '싱글레귤러',  4200, 1, 1, 'CUP_OR_CONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (102, 1, '싱글킹',      4700, 1, 1, 'CUP_OR_CONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (103, 1, '더블주니어',  6500, 1, 2, 'CUP_OR_CONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (104, 1, '더블레귤러',  7500, 1, 2, 'CUP_OR_CONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (105, 1, '파인트',     12000, 1, 3, 'CUP_ONLY',    0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (106, 1, '쿼터',       21000, 1, 4, 'CUP_ONLY',    0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (107, 1, '패밀리',     28000, 1, 5, 'CUP_ONLY',    1, 0, 'ON_SALE', 1, NOW(), NOW()),
  (108, 1, '하프갤런',   32000, 1, 5, 'CUP_ONLY',    1, 0, 'ON_SALE', 1, NOW(), NOW()),
  -- 커피 (맛 선택 없음)
  (201, 2, '아메리카노',  3000, 0, 0, 'NONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (202, 2, '카페라떼',    3800, 0, 0, 'NONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (203, 2, '바닐라라떼',  4200, 0, 0, 'NONE', 0, 0, 'ON_SALE', 1, NOW(), NOW()),
  (204, 2, '카페모카',    4500, 0, 0, 'NONE', 0, 0, 'ON_SALE', 1, NOW(), NOW());

-- ── 맛 ────────────────────────────────────────────────
-- 프론트는 맛 목록을 실제 API(GET /api/flavors)로 가져온다.
-- MenuService 가 is_visible=1 AND sale_status='ON_SALE' 인 맛만 내려주므로
-- 아래 조건을 맞춰 넣는다.
INSERT INTO `flavor`
  (`flavor_id`, `category_id`, `flavor_name`, `sale_status`, `is_visible`, `created_at`, `updated_at`)
VALUES
  (1,  1, '초코',           'ON_SALE', 1, NOW(), NOW()),
  (2,  1, '바닐라',         'ON_SALE', 1, NOW(), NOW()),
  (3,  1, '딸기',           'ON_SALE', 1, NOW(), NOW()),
  (4,  1, '민트초코',       'ON_SALE', 1, NOW(), NOW()),
  (5,  1, '엄마는 외계인',  'ON_SALE', 1, NOW(), NOW()),
  (6,  1, '뉴욕치즈케이크', 'ON_SALE', 1, NOW(), NOW()),
  (7,  1, '체리쥬빌레',     'ON_SALE', 1, NOW(), NOW()),
  (8,  1, '아몬드봉봉',     'ON_SALE', 1, NOW(), NOW()),
  (9,  1, '슈팅스타',       'ON_SALE', 1, NOW(), NOW()),
  (10, 1, '레인보우샤베트', 'ON_SALE', 1, NOW(), NOW());

-- ── 테스트 고객 ──────────────────────────────────────
-- 포인트 사용/적립 흐름을 눈으로 확인하려고 미리 1명 넣어둔다.
-- (없어도 됨 — 결제 때 새 번호를 넣으면 FRIEND 등급으로 자동 가입된다)
INSERT INTO `customer`
  (`customer_id`, `mobile_number`, `point_balance`, `grade`, `created_at`, `updated_at`)
VALUES
  (1, '01012345678', 5000, 'FAMILY', NOW(), NOW());
