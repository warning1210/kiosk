# Ice Cream Kiosk · 아이스크림 키오스크 통합 관리 시스템

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

## 서비스 개요

무인 아이스크림 매장의 **고객 주문 → 지점 운영 → 본사 관리**로 이어지는 흐름을 하나의 시스템으로 통합했습니다.

| 이용 주체 | 설명 | 인증 |
| --- | --- | --- |
| **고객 (Kiosk)** | 매장 키오스크에서 비로그인으로 주문·결제 | 없음 (지점 `kiosk_code`로 범위 지정) |
| **지점 관리자 (Branch)** | 자기 매장 주문 처리·재고 신청·매출 조회 | Firebase Auth + 역할(`BRANCH_MANAGER`) |
| **본사 관리자 (HQ)** | 전 지점 재고 승인·배송·이벤트·통계 관리 | 역할(`SUPER_ADMIN` / `HQ_ADMIN`) |

---

## 주요 기능

### 고객 · 키오스크 주문
- 상품·용기(컵/콘)·**맛 선택**(상품별 선택 가능 개수 관리) → 장바구니 → 결제
- **Toss Payments QR 결제** — 화면 QR을 휴대폰으로 스캔, 결제 완료 자동 감지
- 포인트 적립·사용, 쿠폰 적용, 영수증 출력
- **다국어 4종**(한국어·English·日本語·中文) + **쉬운 모드**(글자 확대·단계 단순화)
- 직원 호출

### 지점 관리자
- 실시간 주문 접수·완료 처리, 주문 취소·환불
- 카테고리별 **재고 관리·부족 재고 본사 신청**, 배송 수령 확인
- 매출 통계(막대·꺾은선), 상품 노출 on/off
- 본사와 1:1 채팅, 공지 확인, "바빠요" 상태 표시

### 본사 관리자
- 전 지점 **재고 신청 승인/반려**, 배송 상태 추적
- 지점 개설 신청·계정 관리(이메일 초대), 상품·카테고리·맛 관리
- 이벤트·쿠폰 발행, 공지 발송, 통합 매출 통계
- 감사 로그(audit log)

---

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Frontend** | Vue 3 · Vite · Vue Router · Pinia · Axios |
| **Backend** | Java 21 · Spring Boot 3.3 · Spring Data JPA · Spring Security · Spring Mail |
| **Database** | MySQL 8 (25개 테이블, `ddl-auto: validate`) |
| **인증 / 결제 / 알림** | Firebase Auth · Toss Payments · Gmail SMTP |
| **Infra / 배포** | Docker · Nginx · AWS EC2 (ap-northeast-2) |

---

## 프로젝트 구조

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
└── docs/                          # API · 요구사항 문서
```

---

<p align="center">
  <b>🍦 대우능력개발원 · AI 에이전트 클라우드 · 보안 코딩 개발자 양성 과정 팀 프로젝트</b>
</p>
