# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

아이스크림 프랜차이즈 키오스크 시스템 — 지점 키오스크 주문 화면 + 본사/지점 관리자 백오피스.

- **Backend**: Spring Boot 3.3 (Java 21), MyBatis, Maven — `backend/`
- **Frontend**: Vue 3 + Vite (JavaScript), Vue Router, Pinia, Axios — `frontend/`
- **DB**: MySQL 8 via Docker Compose — `db/init/01-schema.sql`

## Commands

### DB (MySQL + Adminer via Docker)
```bash
docker compose up -d          # starts mysql (host :3307 -> container :3306) + adminer (:8081)
```
Requires a `.env` at the repo root with `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` (see `.env.example`; `.env` is gitignored, never commit real credentials). `db/init/01-schema.sql` only runs automatically on first boot of an empty volume — schema changes to an existing volume require a manual `ALTER`/re-import, not a container restart.

### Backend
```bash
cd backend
mvn spring-boot:run           # starts on :8080
mvn compile
mvn test                      # no tests exist yet; this is the standard entrypoint once added
mvn -Dtest=ClassName test      # single test class
```
To point at a non-default DB host (shared team DB, cloud dev server, etc.), override via env vars rather than editing `application.yml`:
```bash
DB_HOST=<host> DB_PORT=<port> DB_USERNAME=<user> DB_PASSWORD=<pass> mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev                    # :5173, proxies /api/* to :8080 (see vite.config.js)
npm run build
```

## Architecture

### Schema is the source of truth
`db/init/01-schema.sql` is the single source of truth for the DB schema (25 tables). MyBatis never creates or validates the schema. Any schema change must be made in `01-schema.sql` first, then reflected in the corresponding mapper SQL/result mapping and domain POJO.

Two schema quirks to know about:
- `order` is a MySQL reserved word, so `Order.java` maps it via `@Table(name = "\`order\`")`.
- `order.language`, `product_translation.language`, `flavor_translation.language` are DB `ENUM('ko','en','ja','zh')`. They map to the shared `com.kiosk.domain.common.Language` enum, whose constants are deliberately **lowercase** (`ko`, `en`, ...) so `EnumType.STRING` (which persists via `name()`) lines up with the DB values.

### Backend package layout: two layers
```
com.kiosk.domain.<table>/   POJO + MyBatis Mapper, one package per DB table (shared by every actor)
com.kiosk.global/            cross-cutting: config, security, exception, response
com.kiosk.kiosk.<feature>/   unauthenticated in-store kiosk ordering flow (Controller/Service)
com.kiosk.branch.<feature>/  branch-manager backoffice (Controller/Service)
com.kiosk.hq.<feature>/      HQ-admin backoffice (Controller/Service)
```
`domain/*` contains shared POJOs and MyBatis Mapper interfaces for all 25 tables. Mapper SQL is implemented with annotations or XML under `backend/src/main/resources/mapper`. Services must call explicit insert/update methods; there is no persistence-context dirty checking.

The `admin` table's `role` column (`SUPER_ADMIN` / `HQ_ADMIN` / `BRANCH_MANAGER`) is what distinguishes the `branch` vs `hq` actor at auth time; kiosk endpoints are unauthenticated and scope by `branch.kiosk_code` instead.

### Frontend structure
```
src/router/   route definitions (kiosk vs admin routes)
src/api/      axios instance (http.js, baseURL '/api')
src/views/kiosk/   in-store ordering screens
src/views/admin/   branch/HQ admin screens
```

### Local dev DB access
The `README.md` documents connecting to a teammate's locally-hosted MySQL (via `DB_HOST`/`DB_PORT` env overrides) as well as a shared cloud dev DB. Whichever host is in use, never hardcode credentials in `application.yml` — it already reads `DB_HOST`/`DB_PORT`/`DB_USERNAME`/`DB_PASSWORD`/`DB_NAME` from the environment with `localhost`/`3307`/`kiosk`/`kiosk1234`/`kiosk` as fallback defaults for pure local dev.
