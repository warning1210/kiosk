# EC2 3대 + RDS + CI/CD 배포 과정과 배포 파일 해설

> 작성 기준: 2026-07-30  
> Backend 분석 기준: `summitpsy2/kiosk-backend` `main`, 커밋 `d3a1a91`  
> Frontend 분석 기준: `summitpsy2/kiosk-frontend` `main`, 커밋 `bc1840b`  
> 범위: 이번에 실제로 구축한 AWS 인프라, 수동 최초 배포, GitHub → GitLab → Manager Runner 자동 배포

이 문서의 **순수 배포 과정**에는 `curl`, `docker ps`, 로그 조회, DB 테이블 수 조회처럼 “잘됐는지 확인하는 과정”을 넣지 않았다. 단, 현재 `.gitlab-ci.yml` 안에 들어 있는 자동 상태 확인 코드는 배포 Job의 일부이므로 파일 해설에서는 설명한다.

비밀번호, GitLab 토큰, Runner 인증 토큰, SSH 개인키, `HQ_TOKEN_SECRET`의 실제 값은 기록하지 않는다.

빠르게 읽는 순서:

- 발표 준비: `1 → 2 → 3 → 20`
- 화면에 나온 핵심 파일 해설: `5 → 6 → 7 → 10 → 11 → 12`
- 화면에 없던 필수 파일 해설: `8 → 13 → 15 → 16`
- 빌드 지원 파일과 현재 한계: `9 → 14 → 18 → 19`

---

## 1. 먼저 답: 화면에 나온 파일들이 전부인가?

아니다. 화면에 나온 파일들은 **저장소 안의 핵심 배포 파일**이 맞지만, 실제 배포에는 EC2에만 만든 Compose·환경 파일, Runner 설정, SSH 키, GitHub/GitLab/AWS 설정도 함께 필요하다.

### 1-1. 화면에 나온 핵심 파일 7개

| 저장소 | 파일 | 어디에 쓰이는가 |
|---|---|---|
| Backend | `.github/workflows/mirror-to-gitlab.yml` | GitHub `main`을 Backend GitLab 저장소로 전달한다. |
| Backend | `.gitlab-ci.yml` | Manager Runner가 Backend EC2에 소스를 보내고 컨테이너를 교체한다. |
| Backend | `Dockerfile` | Maven으로 Spring Boot JAR를 빌드하고 Java 실행 이미지를 만든다. |
| Frontend | `.github/workflows/mirror-to-gitlab.yml` | GitHub `main`을 Frontend GitLab 저장소로 전달한다. |
| Frontend | `.gitlab-ci.yml` | Manager Runner가 Frontend EC2에 소스를 보내고 컨테이너를 교체한다. |
| Frontend | `Dockerfile` | Vue/Vite 정적 파일을 빌드하고 Nginx 실행 이미지를 만든다. |
| Frontend | `nginx.conf` | Vue 화면 제공, `/api`·`/uploads`의 Backend 전달, SPA 새로고침을 처리한다. |

### 1-3. 저장소 밖에서 실제로 만든 필수 파일

| 위치 | 파일·디렉터리 | 역할 |
|---|---|---|
| Backend EC2 | `/opt/kiosk/.env` | RDS 접속값과 Backend 비밀값을 보관한다. |
| Backend EC2 | `/opt/kiosk/docker-compose.backend.yml` | Backend 컨테이너의 실행 방식, 포트, 볼륨, 로그를 정의한다. |
| Backend EC2 | `/opt/kiosk/uploads/` | 컨테이너를 교체해도 업로드 파일을 유지한다. |
| Backend EC2 | `/opt/kiosk/src-backend/` | CI가 최신 Backend 소스를 동기화하는 경로이다. |
| Frontend EC2 | `/opt/kiosk/.env` | Backend EC2 사설 IP를 보관한다. |
| Frontend EC2 | `/opt/kiosk/docker-compose.frontend.yml` | Frontend 컨테이너 실행과 80번 포트를 정의한다. |
| Frontend EC2 | `/opt/kiosk/src-frontend/` | CI가 최신 Frontend 소스를 동기화하는 경로이다. |
| Manager EC2 | `/etc/gitlab-runner/config.toml` | 두 Project Runner의 등록 정보와 Shell Executor 설정을 보관한다. |
| Manager EC2 | `/home/gitlab-runner/.ssh/id_ed25519` | Runner가 Frontend·Backend EC2에 접속할 때 쓰는 개인키이다. |
| Manager EC2 | `/home/gitlab-runner/.ssh/id_ed25519.pub` | 대상 EC2에 등록하는 Runner 공개키이다. |
| Manager EC2 | `/home/gitlab-runner/.ssh/known_hosts` | SSH 대상 서버가 진짜 대상 서버인지 확인할 호스트키를 보관한다. |
| 대상 EC2 | `/home/ubuntu/.ssh/authorized_keys` | Manager Runner 공개키의 SSH 접속을 허용한다. |
| 사용자 PC | `C:\Users\summi\.ssh\config` | Manager 직접 접속과 Manager 경유 `ProxyJump` 접속을 정의한다. |
| 사용자 PC | `C:\Users\summi\.ssh\kiosk-seoul-key.pem` | EC2에 처음 관리자 권한으로 접속할 때 사용한다. |
| 사용자 PC·Backend EC2 | `kiosk-full-dump.sql` | 기존 팀 DB를 RDS로 최초 1회 복원하는 데 사용했다. |

### 1-4. 파일은 아니지만 반드시 필요한 설정

| 위치 | 설정 | 역할 |
|---|---|---|
| AWS | VPC·서브넷·보안 그룹 | 서버 위치와 계층별 통신 허용 범위를 정한다. |
| AWS | Frontend 탄력적 IP | 사용자가 접속하는 Frontend 주소를 고정한다. |
| AWS RDS | DB 서브넷 그룹 | RDS가 배치될 가용 영역과 서브넷을 정한다. |
| AWS RDS | DB 파라미터 그룹 | 서울 시간대와 `utf8mb4` 문자셋을 적용한다. |
| GitHub | Secret `GITLAB_TOKEN` | GitHub Actions가 GitLab에 push할 때 인증한다. |
| GitLab Backend | 변수 `BACKEND_PRIVATE_IP` | Runner가 배포할 Backend EC2 주소를 전달한다. |
| GitLab Frontend | 변수 `FRONTEND_PRIVATE_IP` | Runner가 배포할 Frontend EC2 주소를 전달한다. |
| GitLab | Project Runner 2개 | Backend Job과 Frontend Job을 Manager EC2에서 각각 받는다. |

### 1-5. 현재 없는 배포 구성

현재 두 저장소에는 다음 항목이 없다.

- 저장소에 커밋된 Docker Compose 파일
- 비밀값 이름만 안내하는 `.env.example`
- Flyway·Liquibase 같은 DB 마이그레이션 도구
- Terraform·CloudFormation 같은 AWS 인프라 코드
- Docker Registry에 이미지를 올리고 내려받는 과정
- 실패한 버전을 이전 이미지로 되돌리는 자동 롤백

즉, 현재 저장소만 새 서버에 clone한다고 완전히 복구되지는 않는다. 서버 전용 Compose 파일과 비밀값을 제외한 환경 예시 파일을 추후 저장소 또는 별도 인프라 저장소에서 관리하면 재현성이 좋아진다.

---

## 2. 완성된 구조와 역할

```text
개발자
  │ GitHub main push
  ▼
GitHub 저장소
  │ GitHub Actions: GitLab main으로 미러링
  ▼
GitLab 저장소
  │ Pipeline Job 전달
  ▼
Manager EC2 / GitLab Runner
  ├─ rsync + SSH ──▶ Frontend EC2 / Nginx 컨테이너
  └─ rsync + SSH ──▶ Backend EC2 / Spring Boot 컨테이너
                                      │
                                      └─ MySQL 3306 ──▶ RDS
```

| 역할 | 공개 IP | 사설 IP | 담당 작업 |
|---|---:|---:|---|
| Manager EC2 | `3.34.192.103` | `172.31.12.209` | GitLab Runner 실행, 소스 전달, 대상 EC2에 원격 배포 명령 실행 |
| Frontend EC2 | `3.34.173.143` | `172.31.11.138` | Vue 정적 파일 제공, API·업로드 요청을 Backend로 프록시 |
| Backend EC2 | `13.124.88.110` | `172.31.9.140` | Spring Boot API 실행, RDS 연결, 업로드 저장 |
| RDS MySQL | 공개 IP 없음 | 엔드포인트로 접속 | 운영 데이터 저장 |

외부 사용자는 `http://3.34.173.143`의 Frontend만 이용한다. Backend 8080은 Frontend 보안 그룹에서만, RDS 3306은 Backend 보안 그룹에서만 접근한다. Manager와 Backend의 공개 IP는 관리 편의를 위해 존재할 수 있지만 CI/CD는 사설 IP를 사용한다.

Manager와 Backend의 공개 IP는 탄력적 IP가 아니므로 인스턴스를 중지했다 다시 시작하면 바뀔 수 있다. Manager IP가 바뀌면 사용자 PC SSH 설정의 `kiosk-manager` `HostName`도 새 주소로 갱신해야 한다.

---

## 3. 순수 배포 과정

이 절은 이번에 실제로 진행한 **생성·설정·복원·배포 작업만** 정리한다.

### 3-0. 전체 순서만 먼저 보기

```text
1. VPC·서브넷·보안 그룹 구성
2. Manager·Frontend·Backend EC2 생성
3. Frontend에 탄력적 IP 연결
4. RDS MySQL·서브넷 그룹·파라미터 그룹 생성
5. SSH 경유 접속과 EC2 기본 프로그램 구성
6. 기존 DB 덤프를 RDS에 복원하고 앱 DB 사용자 생성
7. Backend Docker 이미지·Compose·환경 파일 구성
8. Frontend Docker 이미지·Compose·Nginx 환경 구성
9. Manager Runner 전용 SSH 키와 대상 EC2 권한 구성
10. GitLab Project Runner·CI/CD 변수 구성
11. GitHub 미러 워크플로와 GitLab 배포 Pipeline 구성
```

### 3-1. AWS 네트워크와 보안 그룹 구성

- 리전은 서울 `ap-northeast-2`를 사용했다.
- 기본 VPC `vpc-040a3910fa596fa53` (`172.31.0.0/16`)를 사용했다.
- EC2는 `subnet-08cf5e6bbabeea4ce` (`ap-northeast-2a`)에 생성했다.
- 키 페어 이름은 `kiosk-seoul-key`로 통일했다.
- 네 보안 그룹의 아웃바운드는 기본 허용 규칙을 유지했다.

| 보안 그룹 | 인바운드 규칙 |
|---|---|
| `kiosk-sg-manager` | SSH 22 ← 관리자 PC 공인 IP |
| `kiosk-sg-frontend` | HTTP 80 ← `0.0.0.0/0`, SSH 22 ← `kiosk-sg-manager` |
| `kiosk-sg-backend` | TCP 8080 ← `kiosk-sg-frontend`, SSH 22 ← `kiosk-sg-manager` |
| `kiosk-sg-rds` | MySQL 3306 ← `kiosk-sg-backend` |

### 3-2. EC2 3대 생성과 Frontend 주소 고정

- Manager, Frontend, Backend EC2를 같은 VPC에 생성했다.
- 각 역할에 맞는 보안 그룹을 연결했다.
- 기존 탄력적 IP `3.34.173.143`을 Frontend EC2에 연결했다.
- Frontend·Backend의 SSH는 Manager 보안 그룹에서만 허용했다.

### 3-3. RDS MySQL 생성

| 항목 | 실제 설정 |
|---|---|
| 엔진 | MySQL Community `8.4.10` |
| DB 식별자 | `kiosk-rds` |
| 초기 DB | `kiosk` |
| 마스터 사용자 | `kioskadmin` |
| 인스턴스 | `db.t4g.micro` |
| 스토리지 | gp3 20 GiB |
| 배포 | 단일 AZ |
| 퍼블릭 액세스 | 아니요 |
| 암호화 | 활성화, 기본 `aws/rds` KMS 키 |
| 자동 백업 보존 | 1일 |
| 보안 그룹 | `kiosk-sg-rds` |
| 엔드포인트 | `kiosk-rds.c7i0wus8a07b.ap-northeast-2.rds.amazonaws.com` |

DB 서브넷 그룹 `kiosk-db-subnet-group`에 다음 네 서브넷을 등록했다.

```text
subnet-08cf5e6bbabeea4ce
subnet-0157f003e97da93b4
subnet-09d2a7e8922c27acd
subnet-0ea954e37d9788984
```

MySQL Community, 패밀리 `mysql8.4`, 유형 `DB Parameter Group`으로 `kiosk-mysql84-db-params`를 만들고 다음 값을 지정했다.

```text
time_zone=Asia/Seoul
character_set_server=utf8mb4
collation_server=utf8mb4_unicode_ci
```

### 3-4. 사용자 PC SSH 구성

- 다운로드한 `kiosk-seoul-key.pem`을 `C:\Users\summi\.ssh\`로 옮겼다.
- Windows 파일 권한을 현재 사용자 읽기 전용으로 제한했다.
- `C:\Users\summi\.ssh\config`에 다음 접속 별칭을 만들었다.

```sshconfig
Host kiosk-manager
    HostName 3.34.192.103
    User ubuntu
    IdentityFile C:/Users/summi/.ssh/kiosk-seoul-key.pem
    IdentitiesOnly yes

Host kiosk-frontend
    HostName 172.31.11.138
    User ubuntu
    IdentityFile C:/Users/summi/.ssh/kiosk-seoul-key.pem
    IdentitiesOnly yes
    ProxyJump kiosk-manager

Host kiosk-backend
    HostName 172.31.9.140
    User ubuntu
    IdentityFile C:/Users/summi/.ssh/kiosk-seoul-key.pem
    IdentitiesOnly yes
    ProxyJump kiosk-manager
```

### 3-5. EC2 기본 프로그램과 작업 경로 구성

- Manager EC2: `git`, `rsync`, `curl`, `ca-certificates`를 설치했다.
- Frontend EC2: Docker, Docker Compose, `git`, `rsync`, `curl`을 설치했다.
- Backend EC2: Docker, Docker Compose, `git`, `rsync`, `curl`, MySQL Client를 설치했다.
- Frontend·Backend의 기준 경로를 `/opt/kiosk`로 만들었다.
- 빌드 중 메모리 부족을 줄이기 위해 Manager에는 2 GiB, Frontend·Backend에는 4 GiB swap을 구성했다.

Frontend·Backend의 기본 설치 형태는 다음과 같다.

```bash
sudo apt-get update -y
sudo apt-get install -y docker.io docker-compose-v2 git rsync curl
sudo usermod -aG docker ubuntu
sudo mkdir -p /opt/kiosk
sudo chown ubuntu:ubuntu /opt/kiosk
```

`docker` 그룹 변경은 다음 로그인부터 적용되므로 명령 실행 후 SSH에서 나갔다 다시 접속하거나 현재 세션에서 `newgrp docker`를 실행한다.

Backend에는 다음 패키지를 추가했다.

```bash
sudo apt-get install -y default-mysql-client
```

### 3-6. 기존 팀 DB를 RDS로 복원

로컬의 `C:\Users\summi\Downloads\kiosk-full-dump.sql`을 Backend EC2로 전송했다.

```powershell
scp "$env:USERPROFILE\Downloads\kiosk-full-dump.sql" kiosk-backend:/home/ubuntu/
```

Backend EC2에서 파일 권한을 제한하고 RDS의 `kiosk` DB로 입력했다.

```bash
chmod 600 ~/kiosk-full-dump.sql

mysql \
  --protocol=TCP \
  --default-character-set=utf8mb4 \
  -h kiosk-rds.c7i0wus8a07b.ap-northeast-2.rds.amazonaws.com \
  -P 3306 \
  -u kioskadmin \
  -p \
  kiosk < ~/kiosk-full-dump.sql
```

애플리케이션 전용 사용자 `kiosk`를 만들고 해당 DB의 읽기·쓰기 권한만 부여했다.

`APP_DB_PASSWORD`는 충분히 긴 값을 한 번 생성하거나 지정하고, 아래 SQL과 Backend `/opt/kiosk/.env`에 **같은 값**을 사용했다. 예를 들어 `openssl rand -hex 24`로 생성할 수 있다.

```sql
CREATE USER IF NOT EXISTS 'kiosk'@'%' IDENTIFIED BY '<APP_DB_PASSWORD>';
ALTER USER 'kiosk'@'%' IDENTIFIED BY '<APP_DB_PASSWORD>';
GRANT SELECT, INSERT, UPDATE, DELETE ON kiosk.* TO 'kiosk'@'%';
FLUSH PRIVILEGES;
```

덤프는 개인정보·암호 해시·토큰을 포함할 수 있으므로 Git에 올리지 않고, 복원이 끝난 Backend EC2 사본은 삭제 대상으로 관리한다.

### 3-7. Backend EC2 최초 배포

소스와 영구 업로드 경로를 만들었다.

```bash
cd /opt/kiosk
git clone https://github.com/summitpsy2/kiosk-backend.git src-backend
mkdir -p uploads
```

`/opt/kiosk/.env`와 `/opt/kiosk/docker-compose.backend.yml`을 작성했다. 실제 암호와 토큰은 `.env`에만 두고 권한을 `600`으로 제한했다.

```bash
chmod 600 /opt/kiosk/.env
cd /opt/kiosk
docker build -t kiosk-backend:latest ./src-backend
docker compose -f docker-compose.backend.yml --env-file .env up -d
```

### 3-8. Frontend EC2 최초 배포

소스를 배치했다.

```bash
cd /opt/kiosk
git clone https://github.com/summitpsy2/kiosk-frontend.git src-frontend
```

`/opt/kiosk/.env`에 `BACKEND_HOST=172.31.9.140`을 지정하고 `/opt/kiosk/docker-compose.frontend.yml`을 작성했다.

```bash
cd /opt/kiosk
docker build -t kiosk-frontend:latest ./src-frontend
docker compose -f docker-compose.frontend.yml --env-file .env up -d
```

### 3-9. Manager Runner의 대상 EC2 SSH 권한 구성

Frontend·Backend 최초 수동 배포 뒤 Manager EC2에 GitLab Runner를 설치했다.

```bash
curl -L \
  "https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh" \
  -o /tmp/gitlab-runner-repo.sh

sudo bash /tmp/gitlab-runner-repo.sh
sudo apt-get install -y gitlab-runner
```

Manager EC2의 `gitlab-runner` 계정에 전용 Ed25519 키를 생성했다.

```bash
sudo install -d -m 700 \
  -o gitlab-runner \
  -g gitlab-runner \
  /home/gitlab-runner/.ssh

sudo -u gitlab-runner -H ssh-keygen \
  -t ed25519 \
  -N "" \
  -C "gitlab-runner@kiosk-manager" \
  -f /home/gitlab-runner/.ssh/id_ed25519
```

- 공개키를 Backend·Frontend EC2의 `/home/ubuntu/.ssh/authorized_keys`에 등록했다.
- 두 대상 EC2의 SSH 호스트키를 Manager의 `/home/gitlab-runner/.ssh/known_hosts`에 등록했다.
- 공개키 등록에만 사용한 Manager의 `/home/ubuntu/kiosk-bootstrap.pem`은 삭제했다.

### 3-10. GitLab Project Runner와 CI/CD 변수 구성

Backend와 Frontend GitLab 프로젝트에 각각 Project Runner를 만들고 Manager EC2에 Shell Executor로 등록했다.

```text
Runner: kiosk-manager-backend
Runner: kiosk-manager-frontend
Tag: manager
Run untagged jobs: 해제
Protected: 활성화
Lock to current project: 활성화
Executor: shell
```

두 프로젝트에서 발급받은 Runner 토큰을 한 번씩 입력해 다음 형태로 등록했다.

```bash
read -s -p "Project Runner token 입력: " PROJECT_RUNNER_TOKEN
echo

sudo gitlab-runner register \
  --non-interactive \
  --url "https://gitlab.com/" \
  --token "$PROJECT_RUNNER_TOKEN" \
  --executor "shell" \
  --description "<kiosk-manager-backend 또는 kiosk-manager-frontend>"

unset PROJECT_RUNNER_TOKEN
```

GitLab 변수는 다음처럼 프로젝트별로 등록했다.

```text
Backend 프로젝트:  BACKEND_PRIVATE_IP=172.31.9.140
Frontend 프로젝트: FRONTEND_PRIVATE_IP=172.31.11.138
Environment: All
Protected: 활성화
Expand variable reference: 해제
```

DB 암호와 애플리케이션 비밀값은 GitLab에 중복 저장하지 않고 Backend EC2의 `/opt/kiosk/.env`에만 두었다.

두 Runner와 두 변수가 Protected이므로 GitLab `main`도 Protected branch로 유지했다.

### 3-11. GitHub → GitLab 미러와 자동 배포 구성

- GitHub `summitpsy2/kiosk-backend`를 GitLab `summitpsy/kiosk-backend`로 연결했다.
- GitHub `summitpsy2/kiosk-frontend`를 GitLab `summitpsy/kiosk-frontend`로 연결했다.
- 두 GitHub 저장소에 Repository Secret `GITLAB_TOKEN`을 등록했다.
- 각 저장소에 `.github/workflows/mirror-to-gitlab.yml`을 두어 GitHub `main`을 GitLab `main`으로 강제 미러링했다.
- 각 GitLab 저장소의 `.gitlab-ci.yml`이 Manager Runner를 통해 대상 EC2에 소스를 동기화하고 Docker 컨테이너를 재생성하도록 구성했다.
- Backend CI 변경을 커밋 `d3a1a91`, Frontend CI 변경을 커밋 `bc1840b`로 각각 GitHub `main`에 push했다.

현재처럼 GitLab이 GitHub와 같은 이력의 fast-forward 상태라면 `--force` 옵션이 있어도 별도 강제 push 허용 없이 진행될 수 있다. 앞으로 GitLab 이력이 GitHub와 갈라져 실제 강제 덮어쓰기가 필요해지면 GitLab 보호 브랜치 정책에서 미러용 주체의 force push를 허용해야 한다.

자동 배포의 한 문장 흐름은 다음과 같다.

```text
GitHub main push
→ GitHub Actions가 GitLab main으로 미러링
→ GitLab Pipeline 생성
→ Manager Project Runner가 Job 실행
→ rsync로 대상 EC2에 소스 전달
→ SSH로 대상 EC2에서 Docker 이미지 빌드
→ Docker Compose로 새 컨테이너 생성
```

---

## 4. 배포 파일을 읽기 전에 알아둘 원리

이하의 “줄별 주석본”은 이해를 위한 설명용 사본이다. 실제 원본에 주석을 허용하지 않는 JSON 파일이 있고, YAML의 여러 줄 셸 명령도 주석 위치에 따라 의미가 달라질 수 있으므로 **주석본 전체를 원본 파일에 그대로 덮어쓰지 않는다.**

### 4-1. GitHub와 GitLab의 역할이 다르다

- GitHub는 사람이 코드를 수정하고 보관하는 **원본 저장소**이다.
- GitLab은 그 코드를 받아 Pipeline을 실행하는 **배포용 미러 저장소**이다.
- `git push --force`를 사용하므로 GitLab에서 직접 수정한 커밋은 다음 미러 때 사라질 수 있다.

### 4-2. Manager와 대상 EC2의 역할이 다르다

- Manager Runner는 소스 전달과 SSH 명령 실행을 담당한다.
- 실제 Docker 이미지는 각각의 Frontend·Backend EC2에서 빌드된다.
- Docker Registry를 거치지 않는다.

### 4-3. 두 종류의 IP 변수를 혼동하면 안 된다

- `FRONTEND_PRIVATE_IP`: Manager가 **배포하러 접속할 Frontend EC2 주소**
- `BACKEND_PRIVATE_IP`: Manager가 **배포하러 접속할 Backend EC2 주소**
- `BACKEND_HOST`: Nginx가 **API를 전달할 Backend EC2 주소**

### 4-4. 저장소 파일과 서버 파일의 수명이 다르다

- `rsync`는 `/opt/kiosk/src-backend` 또는 `/opt/kiosk/src-frontend`만 갱신한다.
- 상위 `/opt/kiosk`의 `.env`, Compose 파일, `uploads`는 덮어쓰지 않는다.
- 그래서 재배포해도 암호와 업로드 파일은 유지된다.

---

## 5. GitHub → GitLab 미러 파일 상세 해설

아래 코드는 **설명용 주석본**이다. 실제 파일과 같은 명령에 이해를 돕는 주석만 추가했다.

### 5-1. Backend `.github/workflows/mirror-to-gitlab.yml`

**용도 한 줄:** Backend GitHub `main`의 현재 커밋과 전체 이력을 Backend GitLab `main`으로 전달한다.

```yaml
# GitHub Actions 화면에 표시할 워크플로 이름
name: Mirror to GitLab

# 워크플로 실행 조건
on:
  # GitHub에 push가 발생했을 때
  push:
    # main 브랜치 push에만 실행
    branches: [main]

# 실행할 Job 목록
jobs:
  # 내부 Job 이름
  mirror:
    # GitHub가 제공하는 임시 Ubuntu 실행기 사용
    runs-on: ubuntu-latest

    # Job 안에서 순서대로 실행할 단계
    steps:
      # GitHub 저장소 코드를 임시 실행기로 내려받음
      - uses: actions/checkout@v4
        # 최신 1개 커밋만이 아니라 전체 Git 이력을 가져옴
        with: { fetch-depth: 0 }

      # GitHub Actions 화면에 표시할 단계 이름
      - name: Push to GitLab
        # 다음 셸 명령을 실행
        run: |
          # GitHub의 현재 HEAD를 Backend GitLab main으로 강제 push
          # 인증값은 GitHub Secret GITLAB_TOKEN에서 읽으며 파일에는 저장하지 않음
          git push --force https://oauth2:${{ secrets.GITLAB_TOKEN }}@gitlab.com/summitpsy/kiosk-backend.git HEAD:main
```

동작 원리:

1. 개발자가 Backend GitHub `main`에 push한다.
2. GitHub가 임시 Ubuntu를 만들고 저장소 전체 이력을 checkout한다.
3. Secret의 토큰으로 Backend GitLab에 인증한다.
4. GitLab `main`을 GitHub의 현재 `HEAD`와 같게 만든다.
5. GitLab이 이 push를 받아 `.gitlab-ci.yml` Pipeline을 시작한다.

`--force`는 두 저장소를 확실히 같게 만들지만 GitLab에서 직접 만든 커밋을 덮어쓴다. 따라서 Backend 수정은 GitHub에서 해야 한다.

### 5-2. Frontend `.github/workflows/mirror-to-gitlab.yml`

**용도 한 줄:** Frontend GitHub `main`의 현재 커밋과 전체 이력을 Frontend GitLab `main`으로 전달한다.

```yaml
# GitHub Actions 화면에 표시할 워크플로 이름
name: Mirror to GitLab

# 워크플로 실행 조건
on:
  # GitHub에 push가 발생했을 때
  push:
    # main 브랜치 push에만 실행
    branches: [main]

# 실행할 Job 목록
jobs:
  # 내부 Job 이름
  mirror:
    # GitHub가 제공하는 임시 Ubuntu 실행기 사용
    runs-on: ubuntu-latest

    # Job 안에서 순서대로 실행할 단계
    steps:
      # GitHub 저장소 코드를 임시 실행기로 내려받음
      - uses: actions/checkout@v4
        # 전체 Git 이력을 가져옴
        with: { fetch-depth: 0 }

      # GitLab 전송 단계
      - name: Push to GitLab
        # 다음 셸 명령 실행
        run: |
          # 현재 HEAD를 Frontend GitLab main으로 강제 push
          # Backend 파일과 다른 핵심은 목적지 저장소가 kiosk-frontend라는 점
          git push --force https://oauth2:${{ secrets.GITLAB_TOKEN }}@gitlab.com/summitpsy/kiosk-frontend.git HEAD:main
```

Backend 미러 파일과 원리는 같고 목적지 GitLab 저장소만 다르다. 두 GitHub 저장소에 각각 `GITLAB_TOKEN` Secret이 있어야 한다.

---

## 6. Backend `.gitlab-ci.yml` 상세 해설

**용도 한 줄:** Manager EC2의 Backend Project Runner가 최신 소스를 Backend EC2로 보내고, 그 서버에서 새 Docker 이미지를 빌드해 컨테이너를 교체한다.

### 6-1. 줄별 주석본

```yaml
# Pipeline 단계 목록
stages:
  # 현재는 배포 단계 하나만 사용
  - deploy

# Backend 배포 Job 이름
deploy-backend:
  # 위에서 선언한 deploy 단계 소속
  stage: deploy

  # 이 Job을 받을 Runner의 태그 조건
  tags:
    # manager 태그를 가진 Runner만 실행 가능
    - manager

  # 운영 Backend 배포는 한 번에 하나만 실행
  # 연속 push로 두 배포가 동시에 서버를 바꾸는 일을 막음
  resource_group: kiosk-backend-production

  # Job 실행 조건
  rules:
    # 현재 브랜치가 GitLab 기본 브랜치인 경우에만 실행
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

  # 이 Job 내부에서 사용할 변수
  variables:
    # Manager EC2의 Runner 전용 SSH 개인키 경로
    SSH_KEY: /home/gitlab-runner/.ssh/id_ed25519
    # GitLab 변수와 ubuntu 계정을 합쳐 SSH 접속 대상 생성
    # 현재 값은 ubuntu@172.31.9.140 형태
    TARGET: ubuntu@$BACKEND_PRIVATE_IP

  # 서버를 변경하기 전에 필수 조건을 검사하는 명령
  before_script:
    # BACKEND_PRIVATE_IP가 빈 값이면 실패
    - test -n "$BACKEND_PRIVATE_IP"
    # Runner 계정이 SSH 개인키를 읽을 수 없으면 실패
    - test -r "$SSH_KEY"

  # 실제 배포 명령
  script:
    # 첫 번째 folded scalar: 아래 줄들을 하나의 rsync 명령으로 연결
    - >
      rsync -az --delete
      --exclude='.git'
      -e "ssh -i $SSH_KEY -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes"
      ./
      "$TARGET:/opt/kiosk/src-backend/"

    # 두 번째 folded scalar: 아래 줄들을 하나의 원격 SSH 명령으로 연결
    - >
      ssh -i "$SSH_KEY"
      -o IdentitiesOnly=yes
      -o StrictHostKeyChecking=yes
      "$TARGET"
      'set -e;
      cd /opt/kiosk;
      docker build -t kiosk-backend:latest ./src-backend;
      docker compose -f docker-compose.backend.yml --env-file .env up -d --force-recreate backend;
      for i in $(seq 1 60); do
        if curl -fsS http://127.0.0.1:8080/api/categories >/dev/null; then
          echo "Backend health check OK";
          docker image prune -f;
          exit 0;
        fi;
        sleep 2;
      done;
      echo "Backend health check failed";
      docker logs --tail 150 kiosk-backend;
      exit 1'
```

`>` 안에는 중간 주석을 넣지 않았다. 이 영역에 `#`를 끼워 넣으면 줄바꿈이 공백으로 접히면서 뒤 명령까지 셸 주석이 되어 배포가 실행되지 않을 수 있기 때문이다.

folded scalar 안의 줄별 의미:

| 원본 줄 | 해석 |
|---|---|
| `rsync -az --delete` | 파일 속성을 보존하고 압축해 전송하며 원본에 없는 대상 파일은 삭제한다. |
| `--exclude='.git'` | Git 내부 데이터는 전송·삭제 대상에서 제외한다. |
| `-e "ssh ..."` | 지정한 Runner 키만 쓰고 `known_hosts`와 일치하는 서버에 SSH한다. |
| `./` | GitLab Runner가 checkout한 현재 저장소를 원본으로 삼는다. |
| `"$TARGET:/opt/kiosk/src-backend/"` | Backend EC2의 소스 폴더를 목적지로 삼는다. |
| `ssh -i "$SSH_KEY"` | Runner 전용 개인키로 원격 접속한다. |
| `-o IdentitiesOnly=yes` | SSH Agent의 다른 키를 자동으로 시도하지 않는다. |
| `-o StrictHostKeyChecking=yes` | 등록된 호스트키와 다르면 접속을 거부한다. |
| `"$TARGET"` | GitLab 변수로 만든 Backend EC2 접속 주소이다. |
| `'set -e;` | 이후 원격 명령 하나가 실패하면 즉시 중단한다. |
| `cd /opt/kiosk;` | Compose와 `.env`가 있는 운영 폴더로 이동한다. |
| `docker build ...;` | 새 소스로 `kiosk-backend:latest` 이미지를 만든다. |
| `docker compose ...;` | 새 이미지와 서버 `.env`로 Backend 컨테이너를 강제 재생성한다. |
| `for i in $(seq 1 60); do` | 새 컨테이너 준비를 최대 60회 판정한다. |
| `if curl ...; then` | 서버 내부 API가 정상 HTTP 응답을 주는지 자동 판정한다. |
| `echo "Backend health check OK";` | 성공 메시지를 Pipeline 로그에 남긴다. |
| `docker image prune -f;` | 사용하지 않는 dangling 이미지를 정리한다. |
| `exit 0;` | 원격 명령과 Job을 성공으로 끝낸다. |
| `fi;` | 조건문을 닫는다. |
| `sleep 2;` | 준비 전이면 2초 기다린다. |
| `done;` | 반복문을 닫는다. |
| `echo "Backend health check failed";` | 제한 시간 초과 메시지를 남긴다. |
| `docker logs --tail 150 ...;` | 원인 확인용 최근 Backend 로그를 출력한다. |
| `exit 1'` | 원격 명령과 Pipeline Job을 실패로 끝낸다. |

### 6-2. 동작 원리

```text
GitLab checkout 경로
  │
  │ rsync
  ▼
Backend EC2 /opt/kiosk/src-backend
  │ docker build
  ▼
kiosk-backend:latest 이미지
  │ docker compose --force-recreate
  ▼
kiosk-backend 컨테이너
  │ /opt/kiosk/.env
  ▼
RDS 접속
```

- Manager EC2는 이미지를 빌드하지 않고 전달·지시만 한다.
- 이미지 빌드는 Backend EC2에서 일어나므로 Docker Registry가 필요 없다.
- 이미지 빌드 중에는 기존 컨테이너가 계속 실행된다.
- 새 이미지 빌드 후 컨테이너를 교체하는 순간에는 짧은 중단이 생길 수 있다.
- 자동 상태 확인이 실패해도 이전 컨테이너로 자동 롤백하지 않는다.
- 성공 시 dangling 이미지를 지우므로 수동 롤백용 이전 이미지가 남지 않을 수 있다.
- `rsync --delete`의 범위는 `/opt/kiosk/src-backend/`뿐이다. 상위 폴더의 `.env`, Compose, `uploads`는 삭제되지 않는다.
- `.git`은 동기화에서 제외되므로 소스 파일은 최신이어도 Backend EC2의 `git log`는 최초 clone 시점 커밋을 보여 줄 수 있다. 대상 EC2의 `git log`를 실제 배포 버전 증명으로 사용하면 안 된다.

---

## 7. Backend `Dockerfile` 상세 해설

**용도 한 줄:** Maven 빌드 환경에서 Spring Boot JAR를 만든 뒤, JRE만 있는 더 작은 실행 이미지에 JAR만 옮긴다.

### 7-1. 줄별 주석본

```dockerfile
# 첫 번째 단계는 소스를 JAR로 만드는 임시 빌드 단계
# Maven 3.9와 Java 21 JDK가 설치된 이미지를 build라는 이름으로 사용
FROM maven:3.9-eclipse-temurin-21 AS build

# 이후 명령을 실행할 컨테이너 내부 작업 폴더
WORKDIR /app

# 의존성 정의 파일을 소스보다 먼저 복사
COPY pom.xml .

# pom.xml에 정의된 의존성을 미리 내려받음
# pom.xml이 바뀌지 않았다면 Docker가 이 레이어를 캐시로 재사용
RUN mvn -B dependency:go-offline

# 실제 Java 소스를 빌드 컨테이너로 복사
COPY src ./src

# 테스트 실행은 생략하고 Spring Boot 실행 JAR 생성
RUN mvn -B -DskipTests package

# 두 번째 단계는 실제 운영 컨테이너 단계
# Maven과 전체 JDK 대신 Java 21 JRE만 있는 이미지를 사용
FROM eclipse-temurin:21-jre-jammy

# 실행 컨테이너의 작업 폴더
WORKDIR /app

# 첫 단계의 target 폴더에서 생성된 JAR만 가져와 app.jar로 저장
COPY --from=build /app/target/*.jar app.jar

# 이 이미지가 컨테이너 내부 8080을 사용한다는 메타데이터
# 실제 EC2 포트 연결은 Compose의 ports가 담당
EXPOSE 8080

# 컨테이너 시작 시 실행할 고정 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 7-2. 멀티 스테이지 빌드 원리

```text
maven:3.9 + JDK 21 + 소스 + 의존성
                │ mvn package
                ▼
             app.jar
                │ JAR만 복사
                ▼
Java 21 JRE + app.jar
```

최종 이미지에는 Maven 캐시와 전체 소스가 들어가지 않는다. 원본 Dockerfile 마지막 주석에는 “GitLab CI가 환경변수를 주입한다”고 적혀 있지만, 정확히는 **GitLab CI가 Backend EC2에서 Compose를 실행하고, 그 Compose가 서버의 `.env`를 컨테이너에 전달**한다.

현재 주의점:

- `-DskipTests`이므로 배포 빌드에서 테스트를 실행하지 않는다.
- Dockerfile에 `USER`가 없어 Java 프로세스는 컨테이너 기본 사용자인 root로 실행된다.
- Docker 자체 `HEALTHCHECK`는 없고 `.gitlab-ci.yml`의 `curl`만 사용한다.
- 베이스 이미지가 digest로 고정되지 않아 미래의 같은 태그가 다른 하위 이미지를 가리킬 수 있다.

---

## 8. Backend EC2 전용 실행 파일

이 절의 두 파일은 Backend 저장소에 없고 `/opt/kiosk`에만 존재한다.

### 8-1. `/opt/kiosk/docker-compose.backend.yml`

**용도 한 줄:** 이미 빌드된 `kiosk-backend:latest` 이미지를 어떤 환경·포트·볼륨·로그 정책으로 실행할지 정의한다.

```yaml
# 실행할 컨테이너 서비스 목록
services:
  # 서비스 이름
  # CI 명령 끝의 backend와 반드시 일치해야 함
  backend:
    # Backend EC2에서 직전에 빌드한 로컬 이미지 사용
    image: kiosk-backend:latest

    # docker ps와 로그 명령에서 보일 컨테이너 이름 고정
    container_name: kiosk-backend

    # 장애나 EC2 재부팅 후 자동 재시작
    # 사용자가 명시적으로 중지한 경우에는 다시 켜지 않음
    restart: unless-stopped

    # 작은 init 프로세스를 PID 1로 두어 자식 프로세스와 종료 신호를 정리
    init: true

    # 같은 폴더의 .env 각 값을 컨테이너 환경변수로 전달
    env_file:
      - .env

    # 비밀값이 아닌 추가 실행 환경
    environment:
      # Linux 컨테이너 시간대
      TZ: Asia/Seoul
      # Java 최소·최대 힙과 JVM 시간대
      JAVA_TOOL_OPTIONS: "-Xms128m -Xmx512m -Duser.timezone=Asia/Seoul"

    # EC2 8080 요청을 컨테이너 8080으로 연결
    # 외부 접근 제한은 Docker가 아니라 AWS 보안 그룹이 담당
    ports:
      - "8080:8080"

    # EC2의 uploads 폴더를 컨테이너 /app/uploads에 연결
    # 컨테이너를 삭제·재생성해도 업로드 파일은 EC2에 남음
    volumes:
      - ./uploads:/app/uploads

    # Docker의 기본 JSON 파일 로그 사용
    logging:
      driver: json-file
      options:
        # 로그 파일 하나의 최대 크기
        max-size: "10m"
        # 최근 로그 파일을 최대 3개 유지
        max-file: "3"
```

`ports: "8080:8080"`은 포트를 EC2에 열어 주는 설정이고, **누가 접근할 수 있는지**는 `kiosk-sg-backend`가 정한다. 현재는 Frontend 보안 그룹에서 온 요청만 8080으로 들어올 수 있다.

### 8-2. `/opt/kiosk/.env`

**용도 한 줄:** 저장소에 넣으면 안 되는 Backend 운영 접속값과 비밀값을 컨테이너에 전달한다.

```dotenv
# RDS DNS 엔드포인트
DB_HOST=kiosk-rds.c7i0wus8a07b.ap-northeast-2.rds.amazonaws.com

# RDS MySQL 포트
DB_PORT=3306

# 사용할 데이터베이스 이름
DB_NAME=kiosk

# 최소 권한으로 만든 애플리케이션 DB 사용자
DB_USERNAME=kiosk

# 실제 값은 문서와 Git에 기록하지 않음
DB_PASSWORD=<APP_DB_PASSWORD>

# 초대 링크·결제 복귀 주소 등의 기준이 되는 Frontend 공개 주소
APP_FRONTEND_URL=http://3.34.173.143

# SMTP 비밀값을 아직 넣지 않았으므로 실제 메일 발송 비활성화
MAIL_ENABLED=false

# Firebase Admin 자격증명 파일을 사용하지 않으므로 빈 값
FIREBASE_CREDENTIALS_PATH=

# 본사 관리자 토큰 서명에 쓰는 충분히 긴 무작위 비밀값
HQ_TOKEN_SECRET=<RANDOM_64_HEX_SECRET>

# Hibernate가 테이블을 생성·변경하지 않고 현재 스키마만 검증
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

실제 파일은 다음 원칙을 지킨다.

- 권한을 `600`으로 두어 `ubuntu` 사용자만 읽고 쓴다.
- Git에 커밋하지 않는다.
- 화면 캡처, Pipeline 로그, 발표 자료에 실제 암호·토큰을 표시하지 않는다.
- Compose의 `env_file`이 이 값을 컨테이너 환경변수로 전달한다.

---

## 10. Frontend `.gitlab-ci.yml` 상세 해설

**용도 한 줄:** Manager EC2의 Frontend Project Runner가 최신 소스를 Frontend EC2로 보내고, 그 서버에서 새 Nginx 이미지를 빌드해 컨테이너를 교체한다.

### 10-1. 줄별 주석본

```yaml
# Pipeline 단계 목록
stages:
  # 현재는 배포 단계 하나만 사용
  - deploy

# Frontend 배포 Job 이름
deploy-frontend:
  # deploy 단계 소속
  stage: deploy

  # 이 Job을 받을 Runner 태그
  tags:
    # manager 태그 Project Runner만 실행 가능
    - manager

  # Frontend 운영 배포는 한 번에 하나만 실행
  resource_group: kiosk-frontend-production

  # Job 실행 조건
  rules:
    # 현재 브랜치가 GitLab 기본 브랜치일 때만 실행
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'

  # Job 내부 변수
  variables:
    # Manager EC2의 Runner 전용 SSH 개인키
    SSH_KEY: /home/gitlab-runner/.ssh/id_ed25519
    # 현재는 ubuntu@172.31.11.138 형태의 배포 대상
    TARGET: ubuntu@$FRONTEND_PRIVATE_IP

  # 배포 전에 필수 조건 확인
  before_script:
    # FRONTEND_PRIVATE_IP가 비어 있으면 실패
    - test -n "$FRONTEND_PRIVATE_IP"
    # Runner가 SSH 키를 읽을 수 없으면 실패
    - test -r "$SSH_KEY"

  # 실제 배포 명령
  script:
    # 첫 번째 folded scalar: 아래 줄들을 하나의 rsync 명령으로 연결
    - >
      rsync -az --delete
      --exclude='.git'
      --exclude='node_modules'
      --exclude='dist'
      -e "ssh -i $SSH_KEY -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes"
      ./
      "$TARGET:/opt/kiosk/src-frontend/"

    # 두 번째 folded scalar: 아래 줄들을 하나의 원격 SSH 명령으로 연결
    - >
      ssh -i "$SSH_KEY"
      -o IdentitiesOnly=yes
      -o StrictHostKeyChecking=yes
      "$TARGET"
      'set -e;
      cd /opt/kiosk;
      docker build -t kiosk-frontend:latest ./src-frontend;
      docker compose -f docker-compose.frontend.yml --env-file .env up -d --force-recreate frontend;
      for i in $(seq 1 30); do
        if curl -fsS http://127.0.0.1/ >/dev/null &&
           curl -fsS http://127.0.0.1/api/categories >/dev/null &&
           curl -fsS http://127.0.0.1/branch/login >/dev/null; then
          echo "Frontend and API health checks OK";
          docker image prune -f;
          exit 0;
        fi;
        sleep 2;
      done;
      echo "Frontend health check failed";
      docker logs --tail 150 kiosk-frontend;
      exit 1'

  # GitLab Environment 화면에 보여 줄 메타데이터
  environment:
    # 환경 이름
    name: production
    # GitLab 화면의 운영 서비스 바로가기
    # 이 줄이 실제 AWS 네트워크를 설정하는 것은 아님
    url: http://3.34.173.143
```

`>` 안에 중간 주석을 넣지 않는 이유는 Backend 파일과 같다. YAML이 줄바꿈을 공백으로 접을 때 `#` 뒤의 실제 명령까지 셸 주석으로 처리될 수 있다.

folded scalar 안의 줄별 의미:

| 원본 줄 | 해석 |
|---|---|
| `rsync -az --delete` | 파일을 압축 동기화하고 원본에 없는 대상 파일을 삭제한다. |
| `--exclude='.git'` | Git 내부 데이터는 제외한다. |
| `--exclude='node_modules'` | 로컬 의존성 폴더는 보내지 않는다. |
| `--exclude='dist'` | 기존 정적 빌드 결과는 보내지 않는다. |
| `-e "ssh ..."` | Runner 키와 엄격한 호스트키 확인으로 SSH 전송한다. |
| `./` | GitLab Runner가 checkout한 저장소를 원본으로 삼는다. |
| `"$TARGET:/opt/kiosk/src-frontend/"` | Frontend EC2 소스 폴더를 목적지로 삼는다. |
| `ssh -i "$SSH_KEY"` | Runner 전용 개인키로 원격 접속한다. |
| `-o IdentitiesOnly=yes` | 다른 SSH 키를 자동으로 시도하지 않는다. |
| `-o StrictHostKeyChecking=yes` | 등록된 호스트키와 다르면 접속을 거부한다. |
| `"$TARGET"` | GitLab 변수로 만든 Frontend EC2 접속 주소이다. |
| `'set -e;` | 원격 명령 하나가 실패하면 즉시 중단한다. |
| `cd /opt/kiosk;` | Compose와 `.env`가 있는 운영 폴더로 이동한다. |
| `docker build ...;` | 새 소스로 `kiosk-frontend:latest` 이미지를 만든다. |
| `docker compose ...;` | 새 이미지와 서버 `.env`로 Frontend 컨테이너를 재생성한다. |
| `for i in $(seq 1 30); do` | 준비 상태를 최대 30회 판정한다. |
| 첫 번째 `curl` | Nginx 정적 첫 화면을 판정한다. |
| 두 번째 `curl` | Nginx에서 Backend로 이어지는 API 프록시를 판정한다. |
| 세 번째 `curl` | Vue Router history 경로의 SPA fallback을 판정한다. |
| `echo "Frontend and API health checks OK";` | 세 요청 성공 메시지를 남긴다. |
| `docker image prune -f;` | 사용하지 않는 dangling 이미지를 정리한다. |
| `exit 0;` | 원격 명령과 Job을 성공으로 끝낸다. |
| `fi;` | 조건문을 닫는다. |
| `sleep 2;` | 준비 전이면 2초 기다린다. |
| `done;` | 반복문을 닫는다. |
| `echo "Frontend health check failed";` | 제한 시간 초과 메시지를 남긴다. |
| `docker logs --tail 150 ...;` | 원인 확인용 최근 Nginx 로그를 출력한다. |
| `exit 1'` | 원격 명령과 Pipeline Job을 실패로 끝낸다. |

### 10-2. 동작 원리

```text
GitLab checkout 경로
  │
  │ rsync
  ▼
Frontend EC2 /opt/kiosk/src-frontend
  │ docker build
  ▼
kiosk-frontend:latest 이미지
  │ docker compose --force-recreate
  ▼
kiosk-frontend 컨테이너
  │ BACKEND_HOST 환경변수 치환
  ▼
Nginx 정적 서비스 + Backend 프록시
```

- `FRONTEND_PRIVATE_IP`는 Runner가 배포하러 갈 서버 주소다.
- `BACKEND_HOST`는 Nginx가 API를 넘길 서버 주소다.
- `rsync --delete` 범위는 `/opt/kiosk/src-frontend/`뿐이므로 상위의 `.env`와 Compose 파일은 유지된다.
- `.git`은 동기화하지 않으므로 Frontend EC2의 `git log` 역시 실제 배포 소스 버전과 다를 수 있다.
- 새 컨테이너 실패 시 이전 버전 자동 롤백은 없다.
- 성공 시 dangling 이미지를 지우므로 즉시 롤백할 이전 이미지가 남지 않을 수 있다.

---

## 11. Frontend `Dockerfile` 상세 해설

**용도 한 줄:** Node/Vite로 Vue 정적 파일을 만든 뒤, Nginx만 있는 작은 실행 이미지에 결과물과 프록시 설정을 넣는다.

### 11-1. 줄별 주석본

```dockerfile
# 첫 번째 단계는 Vue 소스를 정적 파일로 만드는 임시 빌드 단계
# Node.js 22 이미지를 build라는 이름으로 사용
FROM node:22 AS build

# 이후 명령의 컨테이너 내부 기준 폴더
WORKDIR /app

# package.json과 package-lock.json을 먼저 복사
# 소스만 바뀌면 npm 설치 레이어를 재사용할 수 있음
COPY package*.json ./

# package-lock.json에 고정된 정확한 버전으로 의존성 설치
RUN npm ci

# .dockerignore에서 제외하지 않은 나머지 프로젝트 소스 복사
COPY . .

# package.json의 build 스크립트, 즉 vite build 실행
# 결과는 /app/dist에 생성
RUN npm run build

# 두 번째 단계는 실제 운영 실행 단계
# 가벼운 Nginx Alpine 이미지를 사용
FROM nginx:alpine

# 첫 단계에서 만든 dist 정적 파일만 Nginx 웹 루트로 복사
COPY --from=build /app/dist /usr/share/nginx/html

# nginx.conf를 공식 Nginx 이미지의 환경변수 치환 템플릿 위치로 복사
COPY nginx.conf /etc/nginx/templates/default.conf.template

# 컨테이너 내부 80번 포트를 사용한다는 메타데이터
# 실제 EC2 포트 연결은 Compose가 담당
EXPOSE 80
```

### 11-2. 멀티 스테이지 빌드 원리

```text
node:22 + npm + Vue 원본
          │ npm ci
          │ npm run build
          ▼
       dist 정적 파일
          │ dist만 복사
          ▼
nginx:alpine + 정적 파일 + nginx 템플릿
```

최종 컨테이너에는 Node, npm, `node_modules`, Vue 원본이 필요 없다. Nginx는 완성된 HTML·CSS·JavaScript 파일만 제공한다.

현재 주의점:

- `node:22`, `nginx:alpine`이 정확한 patch 버전이나 digest로 고정되지 않았다.
- Docker 자체 `HEALTHCHECK`는 없다.
- 실제 포트 공개는 `EXPOSE 80`이 아니라 Compose의 `"80:80"`이 담당한다.

---

## 12. Frontend `nginx.conf` 상세 해설

**용도 한 줄:** 정적 Vue 화면을 제공하면서 `/api`와 `/uploads`만 Backend EC2로 보내고, Vue Router 새로고침을 처리한다.

### 12-1. 줄별 주석본

```nginx
# 아래 BACKEND_HOST는 컨테이너가 시작될 때 Backend EC2 사설 IP로 바뀜
# 공식 Nginx Docker 이미지가 /etc/nginx/templates/*.template를 읽음
# 시작 스크립트의 envsubst가 결과를 /etc/nginx/conf.d에 생성
# 별도 치환 스크립트를 직접 만들 필요가 없음
# $host, $remote_addr 같은 Nginx 자체 변수는 그대로 보존됨

# 하나의 가상 웹 서버 설정 시작
server {
  # 컨테이너 80번 포트에서 HTTP 요청 수신
  listen 80;

  # 특정 도메인이 아니어도 모든 Host 요청 처리
  server_name _;

  # Vue 빌드 결과가 있는 Nginx 웹 루트
  root /usr/share/nginx/html;

  # 기본 문서 이름
  index index.html;

  # /api/로 시작하는 요청 처리
  location /api/ {
    # URI를 유지한 채 Backend EC2 8080으로 전달
    proxy_pass http://${BACKEND_HOST}:8080;

    # 사용자가 요청한 Host 헤더를 Backend에도 전달
    proxy_set_header Host $host;

    # 실제 접속자의 IP를 Backend에 전달
    proxy_set_header X-Real-IP $remote_addr;

    # 여러 프록시를 거친 IP 목록을 누적해 전달
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    # 원래 요청이 HTTP인지 HTTPS인지 전달
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  # /uploads/로 시작하는 이미지·파일 요청 처리
  location /uploads/ {
    # 업로드 파일도 Backend EC2 8080에서 가져옴
    proxy_pass http://${BACKEND_HOST}:8080;

    # 원래 Host 헤더 전달
    proxy_set_header Host $host;
  }

  # 위 규칙에 걸리지 않은 나머지 프론트 요청
  location / {
    # 실제 파일이 있으면 그 파일을 제공
    # 폴더가 있으면 그 폴더를 사용
    # 둘 다 없으면 index.html 반환
    try_files $uri $uri/ /index.html;
  }
# server 블록 종료
}
```

### 12-2. `BACKEND_HOST`가 실제 IP로 바뀌는 과정

```text
Frontend EC2 /opt/kiosk/.env
BACKEND_HOST=172.31.9.140
        │
        ▼
Docker Compose environment
        │
        ▼
Nginx 컨테이너 환경변수
        │
        ▼
/etc/nginx/templates/default.conf.template
        │ envsubst
        ▼
/etc/nginx/conf.d/default.conf
        │
        ▼
proxy_pass http://172.31.9.140:8080
```

### 12-3. 요청별 처리

| 사용자가 요청한 주소 | Nginx 처리 |
|---|---|
| `/`, `/assets/...` | `/usr/share/nginx/html`의 정적 파일 제공 |
| `/api/categories` | `172.31.9.140:8080/api/categories`로 전달 |
| `/uploads/image.png` | `172.31.9.140:8080/uploads/image.png`로 전달 |
| `/branch/login` 새로고침 | 실제 파일이 없으므로 `index.html`을 주고 Vue Router가 화면 선택 |

브라우저는 Backend 사설 IP를 알 필요가 없다. 같은 Frontend 주소의 `/api`를 호출하고 Nginx가 내부망에서 Backend로 전달한다.

---

## 13. Frontend EC2 전용 실행 파일

이 절의 두 파일은 Frontend 저장소에 없고 `/opt/kiosk`에만 존재한다.

### 13-1. `/opt/kiosk/docker-compose.frontend.yml`

**용도 한 줄:** `kiosk-frontend:latest` 이미지를 80번 포트로 실행하고 Nginx에 Backend 사설 IP를 전달한다.

```yaml
# 실행할 컨테이너 서비스 목록
services:
  # 서비스 이름
  # CI 명령 끝의 frontend와 일치해야 함
  frontend:
    # Frontend EC2에서 직전에 빌드한 로컬 이미지
    image: kiosk-frontend:latest

    # 실제 컨테이너 이름 고정
    container_name: kiosk-frontend

    # 장애나 EC2 재부팅 후 자동 재시작
    # 사용자가 명시적으로 중지한 경우는 제외
    restart: unless-stopped

    # 자식 프로세스와 종료 신호를 정리할 작은 init 사용
    init: true

    # 컨테이너에 전달할 환경변수
    environment:
      # Compose가 /opt/kiosk/.env에서 BACKEND_HOST를 읽어 Nginx에 전달
      BACKEND_HOST: ${BACKEND_HOST}

    # Frontend EC2 80번과 Nginx 컨테이너 80번 연결
    ports:
      - "80:80"

    # Docker JSON 로그 크기 제한
    logging:
      driver: json-file
      options:
        # 로그 파일 하나 최대 10MB
        max-size: "10m"
        # 최대 3개 파일 유지
        max-file: "3"
```

### 13-2. `/opt/kiosk/.env`

**용도 한 줄:** Nginx가 API와 업로드 요청을 전달할 Backend EC2 사설 IP를 저장한다.

```dotenv
# Backend EC2 사설 IP
# 공개 IP를 사용하지 않으므로 인터넷을 거치지 않고 같은 VPC 내부에서 통신
BACKEND_HOST=172.31.9.140
```

Frontend `.env`에는 현재 비밀번호가 없지만 운영 환경값이므로 서버에서 관리한다. 이 `BACKEND_HOST`와 GitLab 변수 `FRONTEND_PRIVATE_IP`는 서로 다른 목적이다.

---

## 15. Manager Runner와 SSH 관련 파일

### 15-1. Manager `/etc/gitlab-runner/config.toml`

**용도 한 줄:** GitLab Runner 서비스가 어떤 GitLab Project와 연결되고 어떤 방식으로 Job을 실행할지 저장한다.

이 파일은 `gitlab-runner register`가 자동 생성하며 Runner 인증 토큰이 들어 있다. 실제 파일을 복사하거나 공개하지 않는다. 구조를 이해하기 위한 비밀값 제거 예시는 다음과 같다.

```toml
# 동시에 실행할 수 있는 전체 Job 수
concurrent = <설정값>

# GitLab에 새 Job이 있는지 확인하는 주기 관련 설정
check_interval = 0

# 첫 번째 Project Runner 설정 시작
[[runners]]
  # GitLab과 Manager에서 보이는 Backend Runner 이름
  name = "kiosk-manager-backend"

  # 연결할 GitLab 서버
  url = "https://gitlab.com/"

  # GitLab이 부여한 내부 Runner ID
  id = <BACKEND_RUNNER_ID>

  # Runner 인증 토큰; 절대 공개하거나 Git에 저장하면 안 됨
  token = "<REDACTED>"

  # Job을 Linux 셸에서 직접 실행
  executor = "shell"

# 두 번째 Project Runner 설정 시작
[[runners]]
  # Frontend Runner 이름
  name = "kiosk-manager-frontend"

  # 연결할 GitLab 서버
  url = "https://gitlab.com/"

  # Frontend Runner 내부 ID
  id = <FRONTEND_RUNNER_ID>

  # Frontend Runner 인증 토큰
  token = "<REDACTED>"

  # Job을 Manager EC2의 셸에서 실행
  executor = "shell"
```

Shell Executor의 의미:

- `.gitlab-ci.yml`의 `rsync`, `ssh`, `test` 명령이 Manager EC2에서 `gitlab-runner` 사용자 권한으로 실행된다.
- 그래서 Manager에는 GitLab Runner, `rsync`, OpenSSH Client, 기본 셸 도구가 필요하다.
- 현재 `curl` 상태 판정은 SSH 따옴표 안에서 실행되므로 `curl`은 Frontend·Backend 대상 EC2에 필요하다. Manager에도 설치했지만 현재 CI 명령의 필수 조건은 아니다.
- 실제 `docker build` 명령은 SSH 따옴표 안에 있으므로 대상 Frontend·Backend EC2에서 실행된다.

### 15-2. Manager Runner SSH 키

| 파일 | 공개 여부 | 역할 |
|---|---|---|
| `/home/gitlab-runner/.ssh/id_ed25519` | 비공개 | Manager Runner가 대상 EC2에 자신을 증명한다. |
| `/home/gitlab-runner/.ssh/id_ed25519.pub` | 공개 가능 | Frontend·Backend의 `authorized_keys`에 등록한다. |
| `/home/gitlab-runner/.ssh/known_hosts` | 보통 비밀 아님 | 접속할 서버의 공개 호스트키를 저장해 위장 서버 접속을 막는다. |
| 대상의 `~/.ssh/authorized_keys` | 공개키 목록 | 어떤 키 소유자가 `ubuntu`로 로그인할 수 있는지 정한다. |

인증 원리:

```text
Manager의 개인키 id_ed25519
          │ 서명
          ▼
대상 EC2의 authorized_keys에 있는 공개키
          │ 검증
          ▼
암호 없이 SSH 허용

대상 EC2가 제시한 호스트키
          │ 비교
          ▼
Manager의 known_hosts
          │ 일치
          ▼
정상 서버로 판단
```

개인키 권한은 `600`, `.ssh` 폴더 권한은 `700`으로 관리한다. CI 파일의 `StrictHostKeyChecking=yes` 때문에 `known_hosts`에 없는 새 서버에는 자동 배포가 접속하지 않는다.

### 15-3. 사용자 PC `C:\Users\summi\.ssh\config`

**용도 한 줄:** 사람이 짧은 별칭으로 Manager에 접속하고, Frontend·Backend는 Manager를 경유해 접속하게 한다.

```sshconfig
# ssh kiosk-manager 명령의 설정
Host kiosk-manager
    # Manager 공개 IP
    HostName 3.34.192.103
    # Ubuntu EC2 기본 사용자
    User ubuntu
    # 최초 관리자용 EC2 개인키
    IdentityFile C:/Users/summi/.ssh/kiosk-seoul-key.pem
    # 다른 키를 자동으로 시도하지 않음
    IdentitiesOnly yes

# ssh kiosk-frontend 명령의 설정
Host kiosk-frontend
    # Frontend 사설 IP
    HostName 172.31.11.138
    User ubuntu
    IdentityFile C:/Users/summi/.ssh/kiosk-seoul-key.pem
    IdentitiesOnly yes
    # 먼저 Manager에 접속한 뒤 내부망 Frontend로 이어서 접속
    ProxyJump kiosk-manager

# ssh kiosk-backend 명령의 설정
Host kiosk-backend
    # Backend 사설 IP
    HostName 172.31.9.140
    User ubuntu
    IdentityFile C:/Users/summi/.ssh/kiosk-seoul-key.pem
    IdentitiesOnly yes
    # 먼저 Manager에 접속한 뒤 내부망 Backend로 이어서 접속
    ProxyJump kiosk-manager
```

이 파일은 사람의 관리 접속용이다. GitLab Runner 자동 배포는 이 PC 파일을 쓰지 않고 Manager의 `id_ed25519`를 직접 사용한다.

---

## 16. GitHub·GitLab 설정값 해설

### 16-1. GitHub Secret `GITLAB_TOKEN`

```text
Settings
→ Secrets and variables
→ Actions
→ Repository secrets
→ GITLAB_TOKEN
```

- Backend·Frontend GitHub 저장소에 각각 등록한다.
- 미러 워크플로가 GitLab에 push할 때만 사용한다.
- `${{ secrets.GITLAB_TOKEN }}`으로 참조하며 파일에 실제 값을 쓰지 않는다.
- 토큰에는 대상 GitLab 저장소에 push할 최소 권한만 부여한다.

### 16-2. GitLab Backend 변수

```text
Key: BACKEND_PRIVATE_IP
Value: 172.31.9.140
Type: Variable
Environment: All
Protected: 활성화
Expand variable reference: 해제
```

이 값은 Manager Runner가 **배포하러 접속할 Backend EC2 주소**이다. RDS 주소나 사용자 브라우저 주소가 아니다.

### 16-3. GitLab Frontend 변수

```text
Key: FRONTEND_PRIVATE_IP
Value: 172.31.11.138
Type: Variable
Environment: All
Protected: 활성화
Expand variable reference: 해제
```

이 값은 Manager Runner가 **배포하러 접속할 Frontend EC2 주소**이다. Nginx의 Backend 대상은 Frontend EC2 `.env`의 `BACKEND_HOST`가 따로 담당한다.

### 16-4. Protected Runner와 Protected 변수

- Runner가 Protected이면 보호된 브랜치·태그의 Job만 받는다.
- 변수가 Protected이면 보호된 브랜치·태그의 Pipeline에서만 값이 전달된다.
- 현재 자동 배포 대상 `main`도 GitLab에서 보호 브랜치로 유지해야 한다.

---

## 17. 파일들이 실제로 연결되는 전체 순서

### 17-1. Backend

| 순서 | 파일·설정 | 다음 단계에 전달하는 것 |
|---:|---|---|
| 1 | GitHub Backend 소스 | `main` 커밋 |
| 2 | `.github/workflows/mirror-to-gitlab.yml` | GitLab Backend `main` |
| 3 | GitLab의 `.gitlab-ci.yml` | Manager의 Backend Project Runner Job |
| 4 | `BACKEND_PRIVATE_IP` + Runner SSH 키 | Backend EC2 접속 대상과 인증 |
| 5 | `rsync` | `/opt/kiosk/src-backend` 최신 소스 |
| 6 | Backend `Dockerfile` + `pom.xml` + `src/**` | `kiosk-backend:latest` 이미지 |
| 7 | `docker-compose.backend.yml` + `.env` | 실행 중인 `kiosk-backend` 컨테이너 |
| 8 | `application.yml` + 환경변수 | RDS 연결, 8080 API, 업로드·외부 서비스 설정 |
| 9 | RDS 보안 그룹과 엔드포인트 | `kiosk` DB 데이터 |

### 17-2. Frontend

| 순서 | 파일·설정 | 다음 단계에 전달하는 것 |
|---:|---|---|
| 1 | GitHub Frontend 소스 | `main` 커밋 |
| 2 | `.github/workflows/mirror-to-gitlab.yml` | GitLab Frontend `main` |
| 3 | GitLab의 `.gitlab-ci.yml` | Manager의 Frontend Project Runner Job |
| 4 | `FRONTEND_PRIVATE_IP` + Runner SSH 키 | Frontend EC2 접속 대상과 인증 |
| 5 | `rsync` | `/opt/kiosk/src-frontend` 최신 소스 |
| 6 | `Dockerfile` + `package*.json` + Vue 소스 | `kiosk-frontend:latest` 이미지 |
| 7 | `docker-compose.frontend.yml` + `.env` | `BACKEND_HOST`가 있는 Nginx 컨테이너 |
| 8 | `nginx.conf` | 정적 화면, API·업로드 프록시, SPA fallback |
| 9 | Frontend 탄력적 IP와 보안 그룹 | 사용자의 HTTP 80 접속 |

---

## 18. 비밀값과 파일 보관 기준

| 항목 | 저장 위치 | Git 커밋 | 공개 |
|---|---|---:|---:|
| GitLab Access Token | GitHub Secret `GITLAB_TOKEN` | 금지 | 금지 |
| GitLab Runner 인증 토큰 | Manager `/etc/gitlab-runner/config.toml` | 금지 | 금지 |
| Runner SSH 개인키 | Manager `/home/gitlab-runner/.ssh/id_ed25519` | 금지 | 금지 |
| Runner SSH 공개키 | Manager `.pub`, 대상 `authorized_keys` | 보통 불필요 | 가능 |
| EC2 최초 접속 PEM | 사용자 PC `.ssh` | 금지 | 금지 |
| RDS 마스터 암호 | 안전한 비밀번호 관리자 | 금지 | 금지 |
| 애플리케이션 DB 암호 | Backend `/opt/kiosk/.env` | 금지 | 금지 |
| `HQ_TOKEN_SECRET` | Backend `/opt/kiosk/.env` | 금지 | 금지 |
| Firebase 서비스 계정 JSON | 서버 비밀 경로·Secret 저장소 | 금지 | 금지 |
| `kiosk-full-dump.sql` | 제한된 로컬 백업 위치 | 금지 | 금지 |
| Frontend `BACKEND_HOST` | Frontend `/opt/kiosk/.env` | 선택 | 사설 IP이므로 불필요한 공개 지양 |
| Compose 파일 | 각 대상 EC2 `/opt/kiosk` | 비밀 제거 후 가능 | 가능 |

덤프 파일은 줄별 해설 대상이 아니다. SQL 구조뿐 아니라 팀 데이터가 들어 있는 민감한 운영 자료이므로 내용은 문서에 복사하지 않고 “RDS 최초 복원 입력 파일”로만 관리한다.

---

## 19. 현재 방식의 특성과 한계

### 현재 방식의 장점

- 외부 사용자는 Frontend 80번만 접근한다.
- Backend와 RDS는 보안 그룹 참조 방식으로 계층별 격리된다.
- Manager 한 대에서 두 프로젝트의 배포를 통제한다.
- GitHub를 원본으로 유지하면서 GitLab Runner를 CI/CD에 활용한다.
- 서버 `.env`와 업로드 폴더는 소스 재배포에 덮어쓰이지 않는다.
- Frontend와 Backend를 서로 독립적으로 배포할 수 있다.

### 현재 반드시 알아야 할 한계

- Pipeline은 소스 테스트를 실행하지 않는다.
- 새 컨테이너 실패 시 이전 버전으로 자동 롤백하지 않는다.
- 컨테이너 교체 순간 짧은 중단이 생길 수 있다.
- 운영 EC2에서 직접 빌드하므로 서버 CPU·메모리를 사용한다.
- 두 Project Runner가 같은 Manager의 `gitlab-runner` OS 계정과 같은 SSH 개인키를 공유하므로 프로젝트 간 OS·키 수준의 강한 격리는 아니다.
- GitHub → GitLab이 강제 push라 GitLab 직접 수정 커밋이 덮어써진다.
- Compose와 `.env` 예시가 저장소에 없어 새 서버의 완전 자동 복원이 어렵다.
- AWS 네트워크와 RDS가 콘솔 수동 설정이라 인프라 재생성 자동화가 없다.
- Backend `application.yml`에 예전 DB 기본 접속값이 남아 있다.
- Backend `.dockerignore`에서 `.git` 등 일부 불필요 파일을 아직 제외하지 않는다.
- `pom.xml`에 Mail 의존성이 중복돼 있다.
- 저장소 DB SQL과 현재 RDS 덤프 스키마가 일치하지 않는다.
- Firebase Admin, SMTP 메일, 실제 프린터, Toss 라이브 결제는 현재 기본 배포에서 활성화하지 않았다.
- 현재 공개 서비스는 HTTP이고 도메인·TLS 인증서·HTTPS 구성이 없다.

### 다음 개선 우선순위

1. Backend 저장소의 기존 DB 기본 암호를 폐기하고 환경변수를 필수화한다.
2. 두 Compose 파일의 비밀값 없는 사본과 `.env.example`을 버전 관리한다.
3. Pipeline에 Backend 테스트와 Frontend 빌드 검사를 배포 전 단계로 추가한다.
4. 이미지에 커밋 SHA 태그를 붙이고 실패 시 이전 태그로 롤백하게 한다.
5. Flyway 또는 Liquibase로 DB 스키마 변경 이력을 관리한다.
6. 도메인, HTTPS, 인증서 자동 갱신을 구성한다.
7. 장기적으로 Terraform·CloudFormation으로 AWS 인프라를 코드화한다.

---

## 20. 발표용 최종 요약

이번 배포는 기존의 “EC2 한 대에 모든 서비스를 실행”하는 방식에서 역할을 분리했다.

```text
Manager EC2 = CI/CD 명령 통제
Frontend EC2 = Vue 정적 화면과 Nginx 프록시
Backend EC2 = Spring Boot API
RDS = 관리형 MySQL 데이터베이스
```

사용자는 Frontend에만 접속하고, Frontend만 Backend에, Backend만 RDS에 접근한다. 개발자가 GitHub `main`에 push하면 GitHub Actions가 GitLab으로 코드를 미러링하고, Manager EC2의 GitLab Runner가 대상 EC2에 소스를 전달한 뒤 Docker 이미지와 컨테이너를 교체한다.

핵심 실행 흐름은 다음 한 줄로 정리할 수 있다.

```text
GitHub push → GitLab mirror → Manager Runner → rsync/SSH → 대상 EC2 Docker build → Compose 재생성
```
