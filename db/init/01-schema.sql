CREATE TABLE `branch` (
  `branch_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_name` varchar(100) NOT NULL,
  `region` varchar(50),
  `address` varchar(255) NOT NULL,
  `phone` varchar(20),
  `email` varchar(255),
  `manager_name` varchar(50),
  `operation_status` ENUM ('ACTIVE', 'PENDING', 'SUSPENDED', 'CLOSED') NOT NULL DEFAULT 'PENDING',
  `opening_date` date,
  `is_busy` tinyint NOT NULL DEFAULT 0,
  `estimated_wait_minutes` tinyint,
  `kiosk_code` varchar(50),
  `kiosk_status` ENUM ('ACTIVE', 'INACTIVE', 'MAINTENANCE') NOT NULL DEFAULT 'ACTIVE',
  `kiosk_last_access_at` datetime,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `admin` (
  `admin_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_id` bigint,
  `login_id` varchar(50) UNIQUE NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20),
  `email` varchar(255),
  `role` ENUM ('SUPER_ADMIN', 'HQ_ADMIN', 'BRANCH_MANAGER') NOT NULL,
  `account_status` ENUM ('ACTIVE', 'PENDING', 'SUSPENDED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
  `invite_token` varchar(255),
  `invite_expires_at` datetime,
  `inviter_admin_id` bigint,
  `last_login_at` datetime,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `branch_application` (
  `branch_application_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_name` varchar(100),
  `manager_name` varchar(50),
  `phone` varchar(20),
  `email` varchar(255),
  `address` varchar(255),
  `business_number` varchar(20),
  `invite_token` varchar(255) UNIQUE COMMENT '본점이 발급하는 지점 가입 URL 토큰',
  `invite_expires_at` datetime,
  `issued_by_admin_id` bigint COMMENT '가입 URL을 발급한 본점 관리자',
  `approval_status` ENUM ('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  `rejection_reason` varchar(500),
  `processed_admin_id` bigint,
  `processed_at` datetime,
  `approved_branch_id` bigint,
  `applied_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `category` (
  `category_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `category_name` varchar(50) NOT NULL COMMENT '아이스크림, 커피, 아이스케이크, 신제품 등',
  `display_order` int NOT NULL DEFAULT 0,
  `is_visible` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `product` (
  `product_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `category_id` bigint,
  `product_name` varchar(100) NOT NULL COMMENT '싱글레귤러, 싱글킹, 파인트, 쿼터 등',
  `base_price` int NOT NULL DEFAULT 0,
  `image_url` varchar(500),
  `description` varchar(500),
  `requires_flavor_selection` tinyint NOT NULL DEFAULT 0,
  `selectable_flavor_count` tinyint NOT NULL DEFAULT 0 COMMENT '싱글=1, 파인트=3, 쿼터=4',
  `container_policy` ENUM ('NONE', 'CUP_ONLY', 'CUP_OR_CONE') NOT NULL DEFAULT 'NONE',
  `is_large` tinyint NOT NULL DEFAULT 0,
  `is_new` tinyint NOT NULL DEFAULT 0,
  `release_date` date,
  `sale_status` ENUM ('ON_SALE', 'SOLD_OUT', 'DISCONTINUED') NOT NULL DEFAULT 'ON_SALE',
  `is_visible` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `flavor` (
  `flavor_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `category_id` bigint COMMENT '재고 화면의 카테고리별 그룹핑용',
  `flavor_name` varchar(100) NOT NULL COMMENT '초코, 바닐라, 민트, 딸기, 엄마는 외계인 등',
  `image_url` varchar(500),
  `description` varchar(500),
  `allergy_info` varchar(500),
  `sale_status` ENUM ('ON_SALE', 'SOLD_OUT', 'DISCONTINUED') NOT NULL DEFAULT 'ON_SALE',
  `is_visible` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `branch_inventory` (
  `branch_inventory_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `flavor_id` bigint NOT NULL COMMENT '재고는 맛 단위로 추적',
  `current_quantity` int NOT NULL DEFAULT 0,
  `safety_quantity` int NOT NULL DEFAULT 0,
  `inventory_status` ENUM ('NORMAL', 'LOW', 'SOLD_OUT') NOT NULL DEFAULT 'NORMAL',
  `is_kiosk_visible` tinyint NOT NULL DEFAULT 1,
  `is_branch_recommended` tinyint NOT NULL DEFAULT 0,
  `display_order` int NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `customer` (
  `customer_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `mobile_number` varchar(20) UNIQUE NOT NULL,
  `point_balance` int NOT NULL DEFAULT 0,
  `grade` ENUM ('FRIEND', 'FAMILY', 'VIP') NOT NULL DEFAULT 'FRIEND',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `order` (
  `order_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `kiosk_code` varchar(50),
  `customer_id` bigint,
  `order_number` varchar(30) UNIQUE NOT NULL,
  `waiting_number` int,
  `order_type` ENUM ('DINE_IN', 'TAKEOUT') NOT NULL,
  `pickup_at` datetime COMMENT '포장(TAKEOUT) 주문의 픽업 희망 일시',
  `order_status` ENUM ('PENDING_PAYMENT', 'PAID', 'MAKING', 'READY', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING_PAYMENT',
  `language` ENUM ('ko', 'en', 'ja', 'zh') NOT NULL DEFAULT 'ko',
  `is_easy_mode` tinyint NOT NULL DEFAULT 0,
  `used_points` int NOT NULL DEFAULT 0,
  `earned_points` int NOT NULL DEFAULT 0,
  `amount_before_discount` int NOT NULL,
  `discount_amount` int NOT NULL DEFAULT 0,
  `final_amount` int NOT NULL,
  `cancellation_reason` varchar(500),
  `created_at` datetime NOT NULL COMMENT '주문 생성(접수) 시각 - 대기열 정렬 기준',
  `order_completed_at` datetime
);

CREATE TABLE `payment` (
  `payment_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `order_id` bigint UNIQUE NOT NULL,
  `payment_method` ENUM ('QR', 'CARD', 'CASH', 'POINT', 'EASY_PAY') NOT NULL DEFAULT 'QR',
  `payment_status` ENUM ('QR_CREATED', 'PAID', 'FAILED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'QR_CREATED',
  `qr_token` varchar(255) UNIQUE,
  `qr_expires_at` datetime,
  `requested_amount` int NOT NULL,
  `paid_amount` int NOT NULL DEFAULT 0,
  `approval_number` varchar(100),
  `failure_reason` varchar(500),
  `paid_at` datetime,
  `payment_key` varchar(200) COMMENT '토스페이먼츠가 발급하는 결제 키'
);

CREATE TABLE `order_item` (
  `order_item_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL COMMENT '어떤 제품(사이즈)을 주문했는지',
  `product_name_snapshot` varchar(100) NOT NULL COMMENT '스냅샷',
  `unit_price_snapshot` int NOT NULL COMMENT '스냅샷',
  `quantity` int NOT NULL,
  `item_total` int NOT NULL,
  `container_type` ENUM ('CUP', 'CONE', 'WAFFLE_CONE', 'NONE') NOT NULL DEFAULT 'NONE' COMMENT 'CUP / CONE / WAFFLE_CONE / NONE',
  `spoon_count` tinyint NOT NULL DEFAULT 0,
  `dry_ice_minutes` tinyint,
  `request_note` varchar(500),
  `created_at` datetime NOT NULL
);

CREATE TABLE `order_item_flavor` (
  `order_item_flavor_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `order_item_id` bigint NOT NULL,
  `flavor_id` bigint NOT NULL COMMENT '어떤 맛을 선택했는지',
  `flavor_name_snapshot` varchar(100) NOT NULL COMMENT '스냅샷',
  `selection_order` tinyint NOT NULL,
  `quantity` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL
);

CREATE TABLE `stock_request` (
  `stock_request_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `request_number` varchar(30) UNIQUE NOT NULL,
  `branch_id` bigint NOT NULL,
  `requester_admin_id` bigint NOT NULL,
  `request_type` ENUM ('RESTOCK', 'ADJUSTMENT') NOT NULL DEFAULT 'RESTOCK' COMMENT 'RESTOCK=신규 입고 신청, ADJUSTMENT=실사 불일치 조정 요청',
  `request_status` ENUM ('PENDING', 'APPROVED', 'REJECTED', 'PREPARING', 'SHIPPING', 'DELIVERED', 'CLOSED') NOT NULL DEFAULT 'PENDING',
  `request_reason` varchar(500),
  `rejection_reason` varchar(500),
  `urgency` ENUM ('LOW', 'NORMAL', 'HIGH') NOT NULL DEFAULT 'NORMAL',
  `requested_at` datetime NOT NULL,
  `processed_admin_id` bigint,
  `processed_at` datetime,
  `shipment_number` varchar(30),
  `tracking_number` varchar(100),
  `courier_name` varchar(100),
  `driver_name` varchar(50),
  `estimated_arrival_at` datetime,
  `shipped_at` datetime,
  `delivered_at` datetime,
  `receipt_confirmed_admin_id` bigint,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `stock_request_item` (
  `stock_request_item_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `stock_request_id` bigint NOT NULL,
  `flavor_id` bigint NOT NULL COMMENT '신청 대상 맛',
  `requested_quantity` int NOT NULL,
  `approved_quantity` int,
  `created_at` datetime NOT NULL
);

CREATE TABLE `inventory_transaction` (
  `inventory_transaction_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `branch_inventory_id` bigint NOT NULL,
  `flavor_id` bigint NOT NULL,
  `transaction_type` ENUM ('IN', 'OUT', 'ADJUST', 'ORDER', 'REQUEST_RECEIVED', 'DISPOSAL') NOT NULL,
  `change_quantity` int NOT NULL,
  `quantity_after` int NOT NULL,
  `reason` varchar(500),
  `order_id` bigint,
  `stock_request_id` bigint,
  `processed_admin_id` bigint,
  `transaction_at` datetime NOT NULL
);

CREATE TABLE `event` (
  `event_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `event_name` varchar(100) NOT NULL,
  `event_type` ENUM ('COUPON', 'FLAVOR_DISCOUNT') NOT NULL COMMENT 'COUPON=본점 쿠폰 배부, FLAVOR_DISCOUNT=특정 상품(맛) 할인 - 어느 맛에 붙일지는 지점이 선택(event_branch_flavor)',
  `benefit_type` ENUM ('COUPON', 'DISCOUNT_RATE', 'DISCOUNT_AMOUNT') NOT NULL DEFAULT 'COUPON',
  `image_url` varchar(500),
  `description` text,
  `discount_rate` decimal(5,2),
  `discount_amount` int,
  `size_up_from_product_id` bigint COMMENT '사이즈업 전 제품',
  `size_up_to_product_id` bigint COMMENT '사이즈업 후 제품',
  `additional_payment` int,
  `target_json` json COMMENT '적용 대상 (지점/맛/고객 등)',
  `start_at` datetime NOT NULL,
  `end_at` datetime NOT NULL,
  `status` ENUM ('DRAFT', 'SCHEDULED', 'ACTIVE', 'ENDED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
  `creator_admin_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `coupon` (
  `coupon_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `coupon_name` varchar(100) NOT NULL,
  `qr_token` varchar(255) UNIQUE NOT NULL,
  `discount_type` ENUM ('RATE', 'AMOUNT') NOT NULL DEFAULT 'AMOUNT',
  `discount_rate` decimal(5,2) COMMENT 'discount_type=RATE일 때 사용 (%)',
  `discount_amount` int COMMENT 'discount_type=AMOUNT일 때 사용 (원)',
  `coupon_status` ENUM ('AVAILABLE', 'USED', 'EXPIRED') NOT NULL DEFAULT 'AVAILABLE',
  `used_order_id` bigint,
  `customer_id` bigint COMMENT '쿠폰 발급 대상 고객',
  `event_id` bigint COMMENT '발급 캠페인(이벤트) 연결 - 캠페인과 무관한 단독 발급이면 NULL',
  `expires_at` datetime NOT NULL
);

CREATE TABLE `notice` (
  `notice_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `title` varchar(150) NOT NULL,
  `content` text NOT NULL,
  `image_url` varchar(500),
  `status` ENUM ('DRAFT', 'PUBLISHED', 'HIDDEN') NOT NULL DEFAULT 'DRAFT',
  `author_admin_id` bigint NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `chat_room` (
  `chat_room_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `consultation_status` ENUM ('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN',
  `started_at` datetime NOT NULL,
  `ended_at` datetime,
  `assigned_admin_id` bigint
);

CREATE TABLE `chat_message` (
  `chat_message_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `chat_room_id` bigint NOT NULL,
  `sender_admin_id` bigint NOT NULL,
  `message_content` text NOT NULL,
  `read_at` datetime,
  `created_at` datetime NOT NULL
);

CREATE TABLE `product_translation` (
  `product_translation_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `language` ENUM ('ko', 'en', 'ja', 'zh') NOT NULL,
  `product_name` varchar(100) NOT NULL,
  `description` varchar(500),
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `flavor_translation` (
  `flavor_translation_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `flavor_id` bigint NOT NULL,
  `language` ENUM ('ko', 'en', 'ja', 'zh') NOT NULL,
  `flavor_name` varchar(100) NOT NULL,
  `description` varchar(500),
  `allergy_info` varchar(500),
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `branch_product` (
  `branch_product_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `branch_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `is_visible` tinyint NOT NULL DEFAULT 1,
  `sale_status` ENUM ('ON_SALE', 'SOLD_OUT', 'DISCONTINUED') NOT NULL DEFAULT 'ON_SALE',
  `display_order` int NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL
);

CREATE TABLE `event_branch_flavor` (
  `event_branch_flavor_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `branch_id` bigint NOT NULL,
  `flavor_id` bigint NOT NULL COMMENT '해당 지점이 이벤트 적용 대상으로 선택한 맛',
  `created_at` datetime NOT NULL
);

CREATE TABLE `audit_log` (
  `audit_log_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(50) NOT NULL COMMENT '예: BRANCH, STOCK_REQUEST',
  `entity_id` bigint NOT NULL,
  `admin_id` bigint,
  `action` varchar(50) NOT NULL COMMENT '예: STATUS_CHANGE, APPROVE, REJECT, UPDATE',
  `field_name` varchar(100),
  `old_value` varchar(500),
  `new_value` varchar(500),
  `reason` varchar(500),
  `changed_at` datetime NOT NULL
);

CREATE UNIQUE INDEX `uq_order_item_flavor_selection_order` ON `order_item_flavor` (`order_item_id`, `selection_order`);
CREATE UNIQUE INDEX `uq_product_translation_product_language` ON `product_translation` (`product_id`, `language`);
CREATE UNIQUE INDEX `uq_flavor_translation_flavor_language` ON `flavor_translation` (`flavor_id`, `language`);
CREATE UNIQUE INDEX `uq_branch_product` ON `branch_product` (`branch_id`, `product_id`);
CREATE UNIQUE INDEX `uq_event_branch_flavor` ON `event_branch_flavor` (`event_id`, `branch_id`, `flavor_id`);

ALTER TABLE `branch` COMMENT = '프랜차이즈 지점 정보 및 키오스크 상태';
ALTER TABLE `admin` COMMENT = '본사/지점 관리자 계정';
ALTER TABLE `branch_application` COMMENT = '지점 가입 신청 (지점→본점)';
ALTER TABLE `category` COMMENT = '메뉴 카테고리';
ALTER TABLE `product` COMMENT = '제품(사이즈) 정보 - 가격과 맛 선택 개수를 가짐';
ALTER TABLE `flavor` COMMENT = '맛(플레이버) 정보 - 재고 추적 대상';
ALTER TABLE `branch_inventory` COMMENT = '지점별 맛 재고 관리';
ALTER TABLE `customer` COMMENT = '고객 정보 (전화번호 기반, 등급별 포인트 적립률)';
ALTER TABLE `order` COMMENT = '키오스크 주문';
ALTER TABLE `payment` COMMENT = '결제 정보 (주문:결제 = 1:1)';
ALTER TABLE `order_item` COMMENT = '주문 내 개별 상품 상세';
ALTER TABLE `order_item_flavor` COMMENT = '주문상세 내 선택 맛 (1상품 → N맛)';
ALTER TABLE `stock_request` COMMENT = '지점→본점 재고 신청';
ALTER TABLE `stock_request_item` COMMENT = '재고 신청 내 개별 맛 목록 및 수량';
ALTER TABLE `inventory_transaction` COMMENT = '재고 변동 이력 (주문차감, 입고, 조정, 폐기 등)';
ALTER TABLE `event` COMMENT = '이벤트/프로모션 (이달의맛, 사이즈업, 할인, 쿠폰 등)';
ALTER TABLE `coupon` COMMENT = '쿠폰 (QR 스캔 방식)';
ALTER TABLE `notice` COMMENT = '본사→지점 공지사항';
ALTER TABLE `chat_room` COMMENT = '지점↔본사 채팅 상담방';
ALTER TABLE `chat_message` COMMENT = '채팅 메시지';
ALTER TABLE `product_translation` COMMENT = '제품명 다국어 번역';
ALTER TABLE `flavor_translation` COMMENT = '맛 이름 다국어 번역';
ALTER TABLE `branch_product` COMMENT = '지점별 상품(사이즈) 노출/판매상태 오버라이드';
ALTER TABLE `event_branch_flavor` COMMENT = '금액 할인 등 이벤트에 지점이 선택한 적용 맛';
ALTER TABLE `audit_log` COMMENT = '관리자 액션/상태변경 감사 로그';

ALTER TABLE `admin` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `admin` ADD FOREIGN KEY (`inviter_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `branch_application` ADD FOREIGN KEY (`processed_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `branch_application` ADD FOREIGN KEY (`approved_branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `branch_application` ADD FOREIGN KEY (`issued_by_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `product` ADD FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`);
ALTER TABLE `flavor` ADD FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`);
ALTER TABLE `branch_inventory` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `branch_inventory` ADD FOREIGN KEY (`flavor_id`) REFERENCES `flavor` (`flavor_id`);
ALTER TABLE `order` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `order` ADD FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`);
ALTER TABLE `payment` ADD FOREIGN KEY (`order_id`) REFERENCES `order` (`order_id`);
ALTER TABLE `order_item` ADD FOREIGN KEY (`order_id`) REFERENCES `order` (`order_id`);
ALTER TABLE `order_item` ADD FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);
ALTER TABLE `order_item_flavor` ADD FOREIGN KEY (`order_item_id`) REFERENCES `order_item` (`order_item_id`);
ALTER TABLE `order_item_flavor` ADD FOREIGN KEY (`flavor_id`) REFERENCES `flavor` (`flavor_id`);
ALTER TABLE `stock_request` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `stock_request` ADD FOREIGN KEY (`requester_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `stock_request` ADD FOREIGN KEY (`processed_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `stock_request` ADD FOREIGN KEY (`receipt_confirmed_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `stock_request_item` ADD FOREIGN KEY (`stock_request_id`) REFERENCES `stock_request` (`stock_request_id`);
ALTER TABLE `stock_request_item` ADD FOREIGN KEY (`flavor_id`) REFERENCES `flavor` (`flavor_id`);
ALTER TABLE `inventory_transaction` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `inventory_transaction` ADD FOREIGN KEY (`branch_inventory_id`) REFERENCES `branch_inventory` (`branch_inventory_id`);
ALTER TABLE `inventory_transaction` ADD FOREIGN KEY (`flavor_id`) REFERENCES `flavor` (`flavor_id`);
ALTER TABLE `inventory_transaction` ADD FOREIGN KEY (`order_id`) REFERENCES `order` (`order_id`);
ALTER TABLE `inventory_transaction` ADD FOREIGN KEY (`stock_request_id`) REFERENCES `stock_request` (`stock_request_id`);
ALTER TABLE `inventory_transaction` ADD FOREIGN KEY (`processed_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `event` ADD FOREIGN KEY (`size_up_from_product_id`) REFERENCES `product` (`product_id`);
ALTER TABLE `event` ADD FOREIGN KEY (`size_up_to_product_id`) REFERENCES `product` (`product_id`);
ALTER TABLE `event` ADD FOREIGN KEY (`creator_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `coupon` ADD FOREIGN KEY (`used_order_id`) REFERENCES `order` (`order_id`);
ALTER TABLE `coupon` ADD FOREIGN KEY (`customer_id`) REFERENCES `customer` (`customer_id`);
ALTER TABLE `coupon` ADD FOREIGN KEY (`event_id`) REFERENCES `event` (`event_id`);
ALTER TABLE `notice` ADD FOREIGN KEY (`author_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `chat_room` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `chat_room` ADD FOREIGN KEY (`assigned_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `chat_message` ADD FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`chat_room_id`);
ALTER TABLE `chat_message` ADD FOREIGN KEY (`sender_admin_id`) REFERENCES `admin` (`admin_id`);
ALTER TABLE `product_translation` ADD FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);
ALTER TABLE `flavor_translation` ADD FOREIGN KEY (`flavor_id`) REFERENCES `flavor` (`flavor_id`);
ALTER TABLE `branch_product` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `branch_product` ADD FOREIGN KEY (`product_id`) REFERENCES `product` (`product_id`);
ALTER TABLE `event_branch_flavor` ADD FOREIGN KEY (`event_id`) REFERENCES `event` (`event_id`);
ALTER TABLE `event_branch_flavor` ADD FOREIGN KEY (`branch_id`) REFERENCES `branch` (`branch_id`);
ALTER TABLE `event_branch_flavor` ADD FOREIGN KEY (`flavor_id`) REFERENCES `flavor` (`flavor_id`);
ALTER TABLE `audit_log` ADD FOREIGN KEY (`admin_id`) REFERENCES `admin` (`admin_id`);
