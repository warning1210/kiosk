# Kiosk

아이스크림 프랜차이즈 키오스크 시스템 (지점 키오스크 주문 + 본사/지점 관리자 백오피스)

## 스택

- **Backend**: Spring Boot 3.3 (Java 21), Spring Data JPA, Maven
- **Frontend**: Vue 3 + Vite (JavaScript), Vue Router, Pinia, Axios
- **DB**: MySQL 8 (Docker Compose)

## 디렉터리 구조

```
Kiosk/
├── docker-compose.yml       # MySQL 컨테이너
├── db/init/01-schema.sql    # 스키마 DDL (컨테이너 최초 기동 시 자동 실행)
├── backend/                 # Spring Boot (Maven)
│   └── src/main/java/com/kiosk/domain/<table>/  # 테이블별 Entity + Repository
└── frontend/                # Vue 3 + Vite
    └── src/
        ├── router/          # 라우트 정의
        ├── api/             # axios 인스턴스
        └── views/           # kiosk(주문화면) / admin(관리자화면)
```

## 실행 방법

### 1. MySQL 기동

```bash
docker compose up -d
```

최초 기동 시 `db/init/01-schema.sql`이 자동 실행되어 스키마가 생성됩니다.
접속 정보: `kiosk` / `kiosk1234`, DB명 `kiosk`, 포트 `3307`(호스트 기준. 컨테이너 내부는 기본 3306이며,
로컬에 이미 다른 MySQL/컨테이너가 3306을 쓰고 있어 호스트 쪽만 3307로 매핑했습니다).

`docker compose up -d`는 MySQL과 함께 **Adminer**(웹 기반 DB 관리 툴)도 같이 띄웁니다.
브라우저에서 `http://localhost:8081` 접속 → System: `MySQL`, Server: `mysql`, Username: `kiosk`,
Password: `kiosk1234`, Database: `kiosk`로 로그인하면 설치 없이 테이블 데이터를 실시간으로 조회/수정할 수 있습니다.
개발 중 값을 넣고 바로 확인할 때, 또는 팀원과 같은 네트워크에서 공유 DB를 볼 때 편합니다
(팀원은 `localhost` 대신 호스트의 사설 IP로 `http://<호스트IP>:8081` 접속).

### 2. 백엔드 실행

```bash
cd backend
mvn spring-boot:run
```

`application.yml`은 `ddl-auto: validate`로 설정되어 있어, 엔티티가 `db/init/01-schema.sql`의
스키마와 일치하는지 기동 시 검증만 하고 스키마 자체는 변경하지 않습니다. 서버는 `:8080`에서 기동됩니다.

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

`:5173`에서 기동되며 `/api/*` 요청은 `vite.config.js`의 프록시 설정을 통해 백엔드(`:8080`)로 전달됩니다.

## 팀원과 DB 공유해서 접속하기

한 명(호스트)의 컴퓨터에서 띄운 MySQL 컨테이너를 팀원들이 함께 사용하는 방식입니다.
같은 사무실/집 Wi-Fi 등 **같은 사설 네트워크**에 있다는 전제입니다.

**호스트(도커를 띄운 사람)**

1. `docker compose up -d`로 MySQL을 기동합니다 (`docker-compose.yml`이 `3307:3306`으로 이미 호스트 전체 인터페이스에 포트를 열어둡니다).
2. 본인의 사설 IP를 확인합니다.
   - macOS: `ipconfig getifaddr en0` (Wi-Fi 기준, 유선이면 en1 등)
   - Windows: `ipconfig`에서 IPv4 주소 확인
3. OS 방화벽에서 3307 포트 인바운드를 허용합니다 (macOS는 최초 접속 시 허용 팝업이 뜨는 경우가 많고, Windows는 "고급 방화벽 설정"에서 인바운드 규칙 추가 필요).
4. 팀원에게 확인한 IP를 공유합니다 (예: `192.168.0.12`).

**팀원(접속하는 사람)**

- DB 클라이언트(DBeaver, MySQL Workbench, TablePlus, IntelliJ Database 등)로 접속: 호스트 `<호스트IP>`, 포트 `3307`, DB `kiosk`, 계정 `kiosk` / `kiosk1234`
- 백엔드(Spring Boot)를 본인 로컬에서 그 DB에 붙여 실행하려면, `application.yml`을 직접 고치지 말고 환경변수로 오버라이드하세요 (파일은 이미 `${DB_HOST:localhost}` 형태로 되어 있어 아무도 안 건드려도 됩니다):

  ```bash
  cd backend
  DB_HOST=192.168.0.12 DB_PORT=3307 mvn spring-boot:run
  ```

> ⚠️ 3307 포트를 공유기(라우터)에서 외부(인터넷)로 포트포워딩하는 방식은 권장하지 않습니다. 원격지 팀원과 붙어야 한다면 포트포워딩 대신 Tailscale/ngrok 같은 터널링을 쓰는 걸 권장합니다.

## 참고

- 백엔드는 25개 테이블 전체(기존 20개 + 다국어/지점별 상품/이벤트-지점-맛/감사로그 5개)에 대해
  JPA Entity + Repository만 생성된 상태이며, Service/Controller 계층과 인증/인가, 비즈니스 로직은 아직 구현되어 있지 않습니다.
- `order` 테이블은 MySQL 예약어라 `@Table(name = "\`order\`")`로 백틱 이스케이프 처리했습니다.
- `order.language`, `product_translation.language`, `flavor_translation.language` 컬럼은 DB ENUM 값(`ko`,`en`,`ja`,`zh`)과
  1:1 매핑을 위해 공용 `Language` enum(`com.kiosk.domain.common.Language`) 상수명도 소문자로 맞췄습니다.
