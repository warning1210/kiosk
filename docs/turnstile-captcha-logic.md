# 지점 로그인 Cloudflare Turnstile 캡차 로직 (요약)

**흐름**: 로그인 화면 진입 시 위젯을 명시적으로 렌더링 → 사용자가 위젯 체크 → 발급된 토큰을 로그인 요청에 실어 전송 → 백엔드가 실제 인증(비밀번호/Firebase 토큰 검증) 전에 Cloudflare에 그 토큰을 재검증 → 통과해야 로그인 진행.

| 단계 | API / 위치 | 역할 |
|---|---|---|
| 1. 위젯 렌더링 | `frontend/src/views/branch/BranchLoginView.vue` (`renderTurnstile`) | `onMounted`마다 `turnstile.render()`를 직접 호출. `data-sitekey` implicit 렌더링은 스크립트 로드 시 페이지를 한 번만 스캔해서, SPA 라우팅으로 이 화면에 재진입하면 위젯이 안 뜨는 문제가 있어 명시적 렌더링으로 교체함 |
| 2. 토큰 발급 | Cloudflare 위젯 (자체 UI, 우리 쪽 커스텀 버튼 없음) | 사용자가 위젯을 통과하면 `callback`으로 `turnstileToken`을 받음. 토큰 없으면 "로그인" 버튼 비활성화 |
| 3. 로그인 요청 | `loginAsHq` / `loginWithFirebase` / `loginWithDb` (같은 파일) | `POST /hq-auth/login`, `/branch-auth/firebase-session`, `/branch-auth/db-login` 요청 body에 `turnstileToken`을 같이 전송 |
| 4. 서버 재검증 | `HqAuthService.login`, `BranchFallbackLoginService.login`, `BranchAuthService.firebaseSession` (각 서비스 최상단) | 비밀번호/Firebase 토큰을 확인하기 **전에** `TurnstileVerifier.verify(token)`부터 호출 |
| 5. Cloudflare 검증 | `com.kiosk.global.security.TurnstileVerifier` | `POST https://challenges.cloudflare.com/turnstile/v0/siteverify`에 `secret`(서버 전용 키) + `response`(토큰)를 보내 `success` 여부 확인 |
| 6. 실패 처리 | 위 3개 서비스 + 프론트 `login()`의 catch 블록 | 서버가 `IllegalArgumentException`을 던지면 `GlobalExceptionHandler`가 400 `{message}`로 응답 → 프론트는 위젯을 `reset()`하고 토큰을 비워 재시도 가능하게 함 (Turnstile 토큰은 1회용) |

## 핵심
- **캡차 통과 여부는 항상 백엔드(Cloudflare siteverify) 기준**이고, 프론트의 버튼 비활성화는 UX용일 뿐 실제 방어선이 아니다. `curl`로 로그인 API를 직접 호출해도 서버가 토큰을 재검증하기 때문에 캡차 없이는 통과할 수 없다.
- `turnstile.secret-key`(`TURNSTILE_SECRET_KEY` 환경변수)가 비어있으면 `TurnstileVerifier`가 즉시 `IllegalStateException`을 던진다 — **fail-closed**: 설정을 안 하면 조용히 뚫리는 대신 로그인 자체가 막힌다.
- 시크릿 키는 프론트 sitekey(`BranchLoginView.vue`의 `TURNSTILE_SITE_KEY`)와 Cloudflare 대시보드에서 짝을 이루는 값이라, 둘이 안 맞으면(다른 위젯 것을 넣으면) 항상 검증 실패로 처리된다.

## 관련 파일
- `backend/src/main/java/com/kiosk/global/security/TurnstileVerifier.java`
- `backend/src/test/java/com/kiosk/global/security/TurnstileVerifierTest.java`
- `frontend/src/views/branch/BranchLoginView.vue`
- `backend/src/main/resources/application.yml` (`turnstile.secret-key`), `.env.example` (`TURNSTILE_SECRET_KEY`)
