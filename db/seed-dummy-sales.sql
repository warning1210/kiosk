-- ============================================================
-- seed-dummy-sales.sql : 본점 통계(/admin/reports) 확인용 더미 데이터
-- ------------------------------------------------------------
-- 01-schema.sql / 02-seed.sql 처럼 초기 볼륨 생성 시 자동 실행되는 파일이 아니다.
-- 이미 데이터가 있는 DB에 직접 실행한다:
--   docker exec -i kiosk-mysql mysql -ukiosk -pkiosk1234 kiosk < db/seed-dummy-sales.sql
--
-- 목적: 지점 4곳 추가(기존 강남점 포함 5곳) + 최근 60일 주문 500건 +
--       이번달 재고 신청 60건을 랜덤 생성해서 HqDashboardService가 집계하는
--       이번달 매출/성장률/지점별 순위/맛별 판매·신청 TOP5가 실제로 채워지게 한다.
--
-- 정리하고 싶으면(재실행 전 등) 아래를 먼저 실행:
--   DELETE FROM stock_request_item WHERE stock_request_id IN (SELECT stock_request_id FROM stock_request WHERE request_number LIKE 'DUMMY-%');
--   DELETE FROM stock_request WHERE request_number LIKE 'DUMMY-%';
--   DELETE FROM order_item_flavor WHERE order_item_id IN (SELECT order_item_id FROM order_item WHERE order_id IN (SELECT order_id FROM `order` WHERE order_number LIKE 'DUMMY-%'));
--   DELETE FROM order_item WHERE order_id IN (SELECT order_id FROM `order` WHERE order_number LIKE 'DUMMY-%');
--   DELETE FROM `order` WHERE order_number LIKE 'DUMMY-%';
--   DELETE FROM admin WHERE login_id LIKE 'dummy-manager%';
--   DELETE FROM branch WHERE branch_name LIKE '베스킨라빈스 %' AND branch_id > 1;
-- ============================================================

-- ── 지점 4곳 추가 (기존 강남점=1과 합쳐 5곳) ──────────────
INSERT INTO `branch`
  (`branch_name`, `region`, `address`, `phone`, `operation_status`, `is_busy`, `kiosk_status`, `created_at`, `updated_at`)
VALUES
  ('베스킨라빈스 홍대점', '서울', '서울시 마포구 양화로 100', '02-000-0001', 'ACTIVE', 0, 'ACTIVE', NOW(), NOW()),
  ('베스킨라빈스 해운대점', '부산', '부산시 해운대구 해운대로 50', '051-000-0002', 'ACTIVE', 0, 'ACTIVE', NOW(), NOW()),
  ('베스킨라빈스 유성점', '대전', '대전시 유성구 대학로 30', '042-000-0003', 'ACTIVE', 0, 'ACTIVE', NOW(), NOW()),
  ('베스킨라빈스 수완점', '광주', '광주시 광산구 수완로 20', '062-000-0004', 'ACTIVE', 0, 'ACTIVE', NOW(), NOW());

-- ── 지점별 매니저 계정 (stock_request.requester_admin_id 채우는 용도) ──
-- 로그인 테스트 용도가 아니라서 password_hash는 실제 로그인 불가능한 값으로 둔다.
INSERT INTO `admin` (`branch_id`, `login_id`, `password_hash`, `name`, `role`, `account_status`, `created_at`, `updated_at`)
SELECT branch_id, CONCAT('dummy-manager', branch_id), 'DUMMY_NOT_A_REAL_HASH', CONCAT(branch_name, ' 매니저'), 'BRANCH_MANAGER', 'ACTIVE', NOW(), NOW()
FROM `branch`
WHERE branch_id NOT IN (SELECT DISTINCT branch_id FROM `admin` WHERE branch_id IS NOT NULL);

-- ── 최근 60일 주문 500건 랜덤 생성 (지점 1~5, 상품/맛은 02-seed.sql 값 재사용) ──
DELIMITER $$
CREATE PROCEDURE seed_dummy_orders()
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE b_id BIGINT;
  DECLARE order_created DATETIME;
  DECLARE p_id BIGINT;
  DECLARE p_name VARCHAR(100);
  DECLARE p_price INT;
  DECLARE flavor_cnt INT;
  DECLARE qty INT;
  DECLARE line_total INT;
  DECLARE status_roll DECIMAL(3,2);
  DECLARE o_status VARCHAR(20);
  DECLARE new_order_id BIGINT;
  DECLARE new_item_id BIGINT;
  DECLARE f INT;
  DECLARE flavor_pick BIGINT;

  WHILE i < 500 DO
    -- branch_id가 항상 1..N으로 연속이라는 보장이 없어(재실행으로 생긴 gap 등) 실제 존재하는 값에서 뽑는다.
    SELECT branch_id INTO b_id FROM branch ORDER BY RAND() LIMIT 1;
    SET order_created = NOW() - INTERVAL FLOOR(RAND() * 60) DAY - INTERVAL FLOOR(RAND() * 24) HOUR;

    IF RAND() < 0.7 THEN
      SET p_id = 101 + FLOOR(RAND() * 8);
    ELSE
      SET p_id = 201 + FLOOR(RAND() * 4);
    END IF;
    SELECT product_name, base_price, selectable_flavor_count INTO p_name, p_price, flavor_cnt FROM product WHERE product_id = p_id;

    SET qty = 1 + FLOOR(RAND() * 2);
    SET line_total = p_price * qty;

    SET status_roll = RAND();
    SET o_status = CASE
      WHEN status_roll < 0.70 THEN 'COMPLETED'
      WHEN status_roll < 0.80 THEN 'PAID'
      WHEN status_roll < 0.85 THEN 'MAKING'
      WHEN status_roll < 0.90 THEN 'READY'
      WHEN status_roll < 0.98 THEN 'CANCELLED'
      ELSE 'PENDING_PAYMENT'
    END;

    INSERT INTO `order`
      (branch_id, order_number, order_type, order_status, language, is_easy_mode,
       used_points, earned_points, amount_before_discount, discount_amount, final_amount,
       created_at, order_completed_at)
    VALUES
      (b_id, CONCAT('DUMMY-ORDER-', LPAD(i, 5, '0')),
       IF(RAND() < 0.5, 'DINE_IN', 'TAKEOUT'), o_status, 'ko', 0,
       0, FLOOR(line_total * 0.01), line_total, 0, line_total,
       order_created, IF(o_status = 'COMPLETED', order_created, NULL));
    SET new_order_id = LAST_INSERT_ID();

    INSERT INTO order_item
      (order_id, product_id, product_name_snapshot, unit_price_snapshot, quantity, item_total, container_type, spoon_count, created_at)
    VALUES
      (new_order_id, p_id, p_name, p_price, qty, line_total, IF(p_id < 200, 'CUP', 'NONE'), 1, order_created);
    SET new_item_id = LAST_INSERT_ID();

    IF flavor_cnt > 0 THEN
      SET f = 0;
      WHILE f < flavor_cnt DO
        -- RAND()를 WHERE 절 안에서 직접 비교하면 스캔되는 행마다 다시 평가돼
        -- 한 문장에서 0개 또는 여러 개가 매칭될 수 있다(누락/중복키 원인) - 변수로 먼저 고정한다.
        SET flavor_pick = 1 + FLOOR(RAND() * 10);
        INSERT INTO order_item_flavor (order_item_id, flavor_id, flavor_name_snapshot, selection_order, quantity, created_at)
        SELECT new_item_id, flavor_id, flavor_name, f + 1, 1, order_created
        FROM flavor WHERE flavor_id = flavor_pick;
        SET f = f + 1;
      END WHILE;
    END IF;

    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

CALL seed_dummy_orders();
DROP PROCEDURE seed_dummy_orders;

-- ── 이번달 재고 신청 60건 랜덤 생성 (지점별 맛별 신청 순위 채우는 용도) ──
DELIMITER $$
CREATE PROCEDURE seed_dummy_stock_requests()
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE b_id BIGINT;
  DECLARE req_admin BIGINT;
  DECLARE req_created DATETIME;
  DECLARE new_req_id BIGINT;
  DECLARE flavor_cnt INT;
  DECLARE f INT;

  WHILE i < 60 DO
    SELECT branch_id INTO b_id FROM branch ORDER BY RAND() LIMIT 1;
    SELECT admin_id INTO req_admin FROM admin WHERE branch_id = b_id LIMIT 1;
    SET req_created = NOW() - INTERVAL FLOOR(RAND() * 3) DAY;

    INSERT INTO stock_request
      (request_number, branch_id, requester_admin_id, request_type, request_status, urgency, requested_at, created_at, updated_at)
    VALUES
      (CONCAT('DUMMY-REQ-', LPAD(i, 5, '0')), b_id, req_admin, 'RESTOCK', 'PENDING', 'NORMAL', req_created, req_created, req_created);
    SET new_req_id = LAST_INSERT_ID();

    SET flavor_cnt = 1 + FLOOR(RAND() * 3);
    SET f = 0;
    WHILE f < flavor_cnt DO
      INSERT INTO stock_request_item (stock_request_id, flavor_id, requested_quantity, created_at)
      VALUES (new_req_id, 1 + FLOOR(RAND() * 10), 5 + FLOOR(RAND() * 20), req_created);
      SET f = f + 1;
    END WHILE;

    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;

CALL seed_dummy_stock_requests();
DROP PROCEDURE seed_dummy_stock_requests;
