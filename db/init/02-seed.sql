-- 재고 신청 기능 로컬 개발/검증용 시드 데이터.
-- 로그인 기능이 아직 없어 실제 계정을 만들 수 없으므로, 프론트의 "관리자 선택" 화면과
-- 백엔드 API를 실제로 구동해보기 위한 최소한의 지점/관리자/상품/재고 데이터를 채운다.
--
-- 멱등하게 작성됨(WHERE NOT EXISTS 가드) -- 여러 번 실행해도 중복 데이터가 생기지 않는다.
-- 로컬 docker-compose 신규 볼륨에는 자동 실행되고, 팀 공용 원격 DB에는 자동 반영되지 않으므로
-- 필요할 때 수동으로 한 번 실행한다 (README 참고).

-- 지점 (Figma 목업에 등장하는 지점명과 맞춤)
INSERT INTO branch (branch_name, region, address, phone, email, manager_name, operation_status, opening_date, kiosk_code, kiosk_status, created_at, updated_at)
SELECT '강남점', '서울', '서울특별시 강남구 테헤란로 123', '02-1111-2222', 'gangnam@example.com', '변혁진', 'ACTIVE', CURDATE(), 'KIOSK-GANGNAM-01', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch WHERE branch_name = '강남점');

INSERT INTO branch (branch_name, region, address, phone, email, manager_name, operation_status, opening_date, kiosk_code, kiosk_status, created_at, updated_at)
SELECT '잠실점', '서울', '서울특별시 송파구 올림픽로 300', '02-3333-4444', 'jamsil@example.com', '김지점', 'ACTIVE', CURDATE(), 'KIOSK-JAMSIL-01', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch WHERE branch_name = '잠실점');

INSERT INTO branch (branch_name, region, address, phone, email, manager_name, operation_status, opening_date, kiosk_code, kiosk_status, created_at, updated_at)
SELECT '부산해운대점', '부산', '부산광역시 해운대구 해운대해변로 264', '051-5555-6666', 'haeundae@example.com', '이지점', 'ACTIVE', CURDATE(), 'KIOSK-HAEUNDAE-01', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch WHERE branch_name = '부산해운대점');

SELECT branch_id INTO @branch_gangnam FROM branch WHERE branch_name = '강남점' LIMIT 1;
SELECT branch_id INTO @branch_jamsil FROM branch WHERE branch_name = '잠실점' LIMIT 1;
SELECT branch_id INTO @branch_haeundae FROM branch WHERE branch_name = '부산해운대점' LIMIT 1;

-- 카테고리
INSERT INTO category (category_name, display_order, is_visible, created_at, updated_at)
SELECT '아이스크림', 1, 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = '아이스크림');
INSERT INTO category (category_name, display_order, is_visible, created_at, updated_at)
SELECT '커피', 2, 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = '커피');
INSERT INTO category (category_name, display_order, is_visible, created_at, updated_at)
SELECT '음료', 3, 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = '음료');
INSERT INTO category (category_name, display_order, is_visible, created_at, updated_at)
SELECT '아이스크림 케이크', 4, 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = '아이스크림 케이크');

SELECT category_id INTO @cat_icecream FROM category WHERE category_name = '아이스크림' LIMIT 1;

-- 맛(플레이버) -- 전부 '아이스크림' 카테고리
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '엄마는 외계인', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '엄마는 외계인');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '민트초코칩', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '민트초코칩');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '초콜릿 무스', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '초콜릿 무스');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '사랑에 빠진 딸기', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '사랑에 빠진 딸기');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '아몬드 봉봉', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '아몬드 봉봉');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '레인보우 스프링클', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '레인보우 스프링클');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '망고 샤베트', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '망고 샤베트');
INSERT INTO flavor (category_id, flavor_name, sale_status, is_visible, created_at, updated_at)
SELECT @cat_icecream, '바닐라', 'ON_SALE', 1, NOW(), NOW() FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flavor WHERE flavor_name = '바닐라');

SELECT flavor_id INTO @flavor_alien FROM flavor WHERE flavor_name = '엄마는 외계인' LIMIT 1;
SELECT flavor_id INTO @flavor_mintchoco FROM flavor WHERE flavor_name = '민트초코칩' LIMIT 1;
SELECT flavor_id INTO @flavor_mousse FROM flavor WHERE flavor_name = '초콜릿 무스' LIMIT 1;
SELECT flavor_id INTO @flavor_strawberry FROM flavor WHERE flavor_name = '사랑에 빠진 딸기' LIMIT 1;
SELECT flavor_id INTO @flavor_almond FROM flavor WHERE flavor_name = '아몬드 봉봉' LIMIT 1;
SELECT flavor_id INTO @flavor_rainbow FROM flavor WHERE flavor_name = '레인보우 스프링클' LIMIT 1;
SELECT flavor_id INTO @flavor_mango FROM flavor WHERE flavor_name = '망고 샤베트' LIMIT 1;
SELECT flavor_id INTO @flavor_vanilla FROM flavor WHERE flavor_name = '바닐라' LIMIT 1;

-- 관리자 (실제 로그인은 없음 -- password_hash는 검증되지 않는 placeholder)
INSERT INTO admin (branch_id, login_id, password_hash, name, role, account_status, created_at, updated_at)
SELECT NULL, 'dev_super1', 'DEV_NO_AUTH_PLACEHOLDER', '슈퍼관리자', 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admin WHERE login_id = 'dev_super1');

INSERT INTO admin (branch_id, login_id, password_hash, name, role, account_status, created_at, updated_at)
SELECT NULL, 'dev_hq1', 'DEV_NO_AUTH_PLACEHOLDER', '본사 관리자', 'HQ_ADMIN', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admin WHERE login_id = 'dev_hq1');

INSERT INTO admin (branch_id, login_id, password_hash, name, role, account_status, created_at, updated_at)
SELECT @branch_gangnam, 'dev_branch1', 'DEV_NO_AUTH_PLACEHOLDER', '변혁진', 'BRANCH_MANAGER', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admin WHERE login_id = 'dev_branch1');

INSERT INTO admin (branch_id, login_id, password_hash, name, role, account_status, created_at, updated_at)
SELECT @branch_jamsil, 'dev_branch2', 'DEV_NO_AUTH_PLACEHOLDER', '김지점', 'BRANCH_MANAGER', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admin WHERE login_id = 'dev_branch2');

INSERT INTO admin (branch_id, login_id, password_hash, name, role, account_status, created_at, updated_at)
SELECT @branch_haeundae, 'dev_branch3', 'DEV_NO_AUTH_PLACEHOLDER', '이지점', 'BRANCH_MANAGER', 'ACTIVE', NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM admin WHERE login_id = 'dev_branch3');

-- 지점별 재고 (강남점: 품절/부족/정상이 섞이도록, 나머지 지점은 넉넉하게)
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_mintchoco, 0, 20, 'SOLD_OUT', 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_mintchoco);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_strawberry, 4, 20, 'LOW', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_strawberry);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_vanilla, 6, 15, 'LOW', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_vanilla);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_mousse, 9, 20, 'LOW', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_mousse);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_mango, 32, 15, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_mango);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_rainbow, 48, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_rainbow);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_alien, 25, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_alien);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_gangnam, @flavor_almond, 18, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_gangnam AND flavor_id = @flavor_almond);

INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_jamsil, @flavor_alien, 30, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_jamsil AND flavor_id = @flavor_alien);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_jamsil, @flavor_mintchoco, 22, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_jamsil AND flavor_id = @flavor_mintchoco);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_jamsil, @flavor_mousse, 20, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_jamsil AND flavor_id = @flavor_mousse);

INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_haeundae, @flavor_almond, 15, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_haeundae AND flavor_id = @flavor_almond);
INSERT INTO branch_inventory (branch_id, flavor_id, current_quantity, safety_quantity, inventory_status, is_kiosk_visible, updated_at)
SELECT @branch_haeundae, @flavor_strawberry, 12, 10, 'NORMAL', 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM branch_inventory WHERE branch_id = @branch_haeundae AND flavor_id = @flavor_strawberry);
