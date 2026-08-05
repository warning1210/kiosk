# 🍦 Ice Cream Kiosk · 아이스크림 프랜차이즈 통합 관리 시스템

> 무인 매장 **키오스크 주문**부터 **지점 운영**, **본사 관리**까지 하나로 연결한 **클라우드 기반 통합 플랫폼**

![Vue.js](https://img.shields.io/badge/Vue.js_3-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-007396?style=flat-square&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![GitLab CI](https://img.shields.io/badge/GitLab_CI/CD-FC6D26?style=flat-square&logo=gitlab&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase_Auth-FFCA28?style=flat-square&logo=firebase&logoColor=black)

---

## 🌐 서비스 개요 (3-Actor)

무인 아이스크림 매장의 **고객 주문 → 지점 운영 → 본사 관리**로 이어지는 흐름을 하나의 시스템으로 통합했습니다.

| 이용 주체 | 설명 | 인증 |
| --- | --- | --- |
| 🧑 **고객 (Kiosk)** | 매장 키오스크에서 비로그인으로 주문·결제 | 없음 (지점 `kiosk_code`로 범위 지정) |
| 🏪 **지점 관리자 (Branch)** | 자기 매장 주문 처리·재고 신청·매출 조회 | Firebase Auth + 역할(`BRANCH_MANAGER`) |
| 🏢 **본사 관리자 (HQ)** | 전 지점 재고 승인·배송·이벤트·통계 관리 | 역할(`SUPER_ADMIN` / `HQ_ADMIN`) |

---

## ✨ 주요 기능

### 🧑 고객 · 키오스크 주문
- 🛒 상품·용기(컵/콘)·**맛 선택**(상품별 선택 가능 개수 관리) → 장바구니 → 결제
- 💳 **Toss Payments QR 결제** — 화면 QR을 휴대폰으로 스캔, 결제 완료 자동 감지
- 🎁 포인트 적립·사용, 쿠폰 적용, 영수증 출력
- 🌏 **다국어 4종**(한국어·English·日本語·中文) + **쉬운 모드**(글자 확대·단계 단순화)
- 🔔 직원 호출

### 🏪 지점 관리자 (Branch Backoffice)
- 📦 실시간 주문 접수·완료 처리, 주문 취소·환불
- 📊 카테고리별 **재고 관리·부족 재고 본사 신청**, 배송 수령 확인
- 📈 매출 통계(막대·꺾은선), 상품 노출 on/off
- 💬 본사와 1:1 채팅, 공지 확인, "바빠요" 상태 표시

### 🏢 본사 관리자 (HQ Backoffice)
- ✅ 전 지점 **재고 신청 승인/반려**, 배송 상태 추적
- 🏬 지점 개설 신청·계정 관리(이메일 초대), 상품·카테고리·맛 관리
- 🎉 이벤트·쿠폰 발행, 공지 발송, 통합 매출 통계
- 📝 감사 로그(audit log)

### 🔗 핵심 연동 로직
```
주문 발생 → 재고 자동 차감 → 재고 0 시 키오스크에서 자동 제외 → 관리자 알림
재고 신청 → 본사 승인 → 배송 → 지점 수령 확인 시 재고 자동 증가
```

---

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Frontend** | Vue 3 · Vite · Vue Router · Pinia · Axios |
| **Backend** | Java 21 · Spring Boot 3.3 · MyBatis · Spring Security · Spring Mail |
| **Database** | MySQL 8 (25개 테이블, SQL 스키마 직접 관리) |
| **인증 / 결제 / 알림** | Firebase Auth · Toss Payments · Gmail SMTP |
| **Infra / 배포** | Docker · Nginx · AWS EC2 (ap-northeast-2) |
| **CI/CD / 협업** | GitLab CI/CD (GitHub 미러) · GitLab Container Registry · Git |

---

## 🏗️ 시스템 아키텍처

```
                  git push            mirror              ┌─ build & push (경로 기반: 바뀐 경로의 job만 실행)
   개발자 ──▶ GitHub ──────▶ GitLab CI/CD ─────────────────┤
                                                          └─ deploy ▼
   ┌──────────────────────────────  AWS (ap-northeast-2)  ──────────────────────────────┐
   │                                                                                     │
   │   ┌───────────────────────┐    /api (사설망)     ┌───────────────────────┐          │
   │   │ Frontend EC2          │───────────────────▶ │ Backend EC2           │          │
   │   │ Nginx + Vue 빌드 :80  │                      │ Spring Boot :8080     │          │
   │   └───────────┬───────────┘                      └───────────┬───────────┘          │
   │               │ 🌐 외부 공개                        🔒 사설망 │ JDBC                │
   └───────────────┼───────────────────────────────────────────────┼─────────────────────┘
                   │ https :80                                       ▼
              ┌────┴────┐                                   ┌────────────────┐
              │ 사용자  │                                   │ DB 서버 (MySQL)│ ← 스키마 수동 반영
              └─────────┘                                   └────────────────┘
```

- **경로 기반 배포** — `backend/`, `frontend/`, `db/` 중 실제로 바뀐 경로의 job만 실행 (`rules: changes:`)
- **비용 최적화** — backend/frontend EC2는 평소 stop, 배포 시 자동 start 후 새 공인 IP 조회 (Elastic IP 미사용)
- **DB는 자동 배포 제외** — 스키마 자동 반영은 위험하므로 `db/` 변경 시 알림만 발생, `01-schema.sql` 수동 반영

---

## 🚀 배포 / CI/CD

| 구분 | 대상 | 포트 | 설명 |
| --- | --- | :---: | --- |
| **Frontend** | AWS EC2 (Nginx) | `:80` | Vue 빌드 정적 서빙 + `/api` 리버스 프록시 · 🌐 외부 공개 |
| **Backend** | AWS EC2 (Docker) | `:8080` | Spring Boot 단일 컨테이너 · 🔒 VPC 사설망만 접근 |
| **Database** | 별도 MySQL 서버 | `:3306` | 스키마 수동 반영 (CI/CD 자동 배포 제외) |
| **CI/CD** | GitLab (GitHub 미러) | — | 경로 기반 빌드·배포, Container Registry로 이미지 관리 |

---

## 📁 프로젝트 구조

```
kiosk/
├── backend/                       # Spring Boot 3.3 (Java 21, Maven)
│   ├── Dockerfile
│   └── src/main/java/com/kiosk/
│       ├── domain/<table>/        # 25개 테이블 Entity + Repository (전 Actor 공용)
│       ├── global/                # config · security · exception · response
│       ├── kiosk/<feature>/       # 고객 키오스크 (비로그인 주문·결제)
│       ├── branch/<feature>/      # 지점 관리자 백오피스
│       └── hq/<feature>/          # 본사 관리자 백오피스
├── frontend/                      # Vue 3 + Vite
│   ├── Dockerfile · nginx.conf
│   └── src/
│       ├── router/  api/          # 라우트 정의 · axios 인스턴스
│       └── views/{kiosk,branch,admin}/
├── db/init/01-schema.sql          # 스키마 단일 기준 (Single Source of Truth)
├── docker-compose.yml             # 로컬 개발용 MySQL + Adminer
├── .gitlab-ci.yml                 # CI/CD 파이프라인
└── docs/                          # API · 요구사항 문서
```

> **스키마가 기준(Single Source of Truth)**: `db/init/01-schema.sql`이 유일한 기준입니다. 스키마 변경은 SQL을 먼저 수정한 뒤 해당 MyBatis Mapper SQL과 result mapping에 반영합니다.

---

## ⚡ 실행 방법 (로컬)

### 1. DB 기동 (MySQL + Adminer)
```bash
docker compose up -d      # MySQL(host :3307 → container :3306) + Adminer(:8081)
```
- 루트에 `.env` 필요 (`MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD`, `.env.example` 참고 · `.env`는 커밋 금지)
- 최초 기동 시 `db/init/01-schema.sql`이 자동 실행되어 스키마 생성
- Adminer: `http://localhost:8081` (Server `mysql`, User `kiosk`, DB `kiosk`)

### 2. 백엔드
```bash
cd backend
mvn spring-boot:run       # :8080
```
> DB 접속 정보는 `application.yml`을 직접 고치지 말고 환경변수로 오버라이드:
> ```bash
> DB_HOST=<host> DB_PORT=<port> DB_USERNAME=<user> DB_PASSWORD=<pass> mvn spring-boot:run
> ```

### 3. 프론트엔드
```bash
cd frontend
npm install
npm run dev               # :5173, /api/* → :8080 프록시 (vite.config.js)
```

---

## 📚 문서

| 문서 | 내용 |
| --- | --- |
| [`docs/requirements-priority.md`](docs/requirements-priority.md) | 요구사항 및 우선순위 |
| [`docs/branch-api-full.md`](docs/branch-api-full.md) | 지점 API 명세 |
| [`docs/branch-api-curl-tests.md`](docs/branch-api-curl-tests.md) | 지점 API curl 테스트 |
| [`docs/stock-request-guide.md`](docs/stock-request-guide.md) | 재고 신청 흐름 가이드 |
| [`docs/flavor-discount-logic.md`](docs/flavor-discount-logic.md) | 맛 선택·할인 로직 |
| [`docs/cart-order-payment-changes.md`](docs/cart-order-payment-changes.md) | 장바구니·주문·결제 변경 이력 |

---
<p align="center">
  <b>🍦 대우능력개발원 · AI 에이전트 클라우드 · 보안 코딩 개발자 양성 과정 팀 프로젝트</b>
</p>
