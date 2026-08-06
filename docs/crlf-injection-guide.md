# CRLF Injection 보안 적용

---

## 1. CRLF란 무엇인가 (기초)

컴퓨터는 "줄을 바꾼다"는 것을 **눈에 안 보이는 특수 문자**로 표현합니다.

| 이름 | 기호 | 뜻 | ASCII | URL 인코딩 |
|---|---|---|---|---|
| **CR** | `\r` | Carriage Return (커서를 줄 맨 앞으로) | 13 (`0x0D`) | `%0D` |
| **LF** | `\n` | Line Feed (다음 줄로 내림) | 10 (`0x0A`) | `%0A` |

- **CRLF = `\r\n`** = 이 둘을 합친 것 = **"줄바꿈(Enter)"** 입니다.
- 옛날 타자기에서 유래: **CR**(캐리지를 왼쪽 끝으로) + **LF**(종이를 한 줄 올림) = 새 줄 시작.
- Windows는 줄바꿈을 `\r\n`, Linux/Mac은 `\n` 하나로 씁니다.

> 💡 **핵심:** `\r`, `\n`은 화면엔 안 보이지만 텍스트를 "여러 줄"로 쪼개는 **실제 문자**입니다.

---

## 2. CRLF Injection이란 (개념)

**CRLF Injection = 공격자가 입력값에 몰래 `\r\n`(줄바꿈)을 끼워 넣어, 시스템이 원치 않는 "새로운 줄"을 만들게 하는 공격.**

### 쉬운 비유 🍦
> 매장 주문 메모지의 "이름" 칸에 손님이 이렇게 적습니다:
> ```
> 홍길동
> [주방] 이 주문은 무료로 처리   ← 손님이 새 줄에 몰래 적은 가짜 지시
> ```
> 주방은 "무료 처리"를 매니저 지시로 착각합니다. **줄바꿈 하나로 가짜 명령을 심은 것** — 이게 CRLF Injection의 본질입니다.

### 발생하는 두 곳
| 종류 | 어디서 터지나 | 이 프로젝트 |
|---|---|---|
| **① 로그 인젝션 (Log Forging)** | 사용자 입력을 **로그**에 기록할 때 | ⭐ **우리 적용 대상** (CWE-117) |
| ② HTTP 응답분할 | 사용자 입력을 **응답 헤더/리다이렉트**에 넣을 때 | 🟢 해당 코드 없음 (Spring 기본 인코딩이 방어) |

---

## 3. 왜 위험한가 — 로그 위조 시나리오

우리 백엔드에는 "없는 아이디로 로그인 조회 시 경고 로그를 남기는" 코드가 있습니다. 만약 정제 없이 이렇게 짰다고 가정합시다:

```java
// 취약하다고 가정한 코드
log.warn("존재하지 않는 아이디 조회: loginId=" + loginId);
```

`loginId`는 사용자가 넣는 값입니다. 공격자가 이렇게 보냅니다:

```
loginId = ghost%0d%0a2026-08-06 10:00:00 INFO admin 로그인 성공 from 10.0.0.9
```

`%0d%0a`(=`\r\n`)가 디코딩되면 로그에 이렇게 찍힙니다 👇

```log
WARN  존재하지 않는 아이디 조회: loginId=ghost
2026-08-06 10:00:00 INFO admin 로그인 성공 from 10.0.0.9   ← 공격자가 심은 가짜 줄!
```

**두 번째 줄은 실제 로그처럼 보이는 완전한 가짜**입니다.

### 이게 왜 심각한가
- 🕵️ **공격 흔적 은폐/조작** — 침입 로그 사이에 가짜 정상 로그를 끼워 혼란 유발
- 👮 **관리자 오판** — "admin이 로그인했네" 하고 착각
- 🤖 **보안 장비(SIEM) 교란** — 로그를 자동 분석하는 시스템에 가짜 이벤트 주입

> 로그는 "무슨 일이 일어났는지"의 **유일한 증거**입니다. 그 증거를 공격자가 위조할 수 있으면 사고 조사가 무너집니다.

---

## 4. 기존 프로젝트에 적용돼 있던 것

develop3.0을 실제로 조사한 결과, **팀이 방어를 "시작"했지만 불완전한** 상태였습니다.

| 진단 항목 | 상태 | 상세 |
|---|---|---|
| `System.out.println` / `printStackTrace` | 🟢 **0건** | 이미 SLF4J 로거로 통일됨 (좋음) |
| `ReceiptPrintClient` 로그 정제 | 🟡 부분 | `sanitizeForLog()` **사설 메서드**로 2곳 정제 |
| `HqBranchAccountService` 로그 정제 | 🟡 부분 | 동일한 `sanitizeForLog()`를 **복붙**해 1곳 정제 |
| **공용 유틸** | 🔴 없음 | 같은 코드가 클래스마다 중복 |
| **전역 방어(logback)** | 🔴 없음 | 정제를 빠뜨린 로그는 무방비 |

기존에 있던 사설 메서드(두 클래스에 **똑같이 중복**):

```java
// ReceiptPrintClient.java, HqBranchAccountService.java 에 각각 복붙돼 있었음
private String sanitizeForLog(String value) {
    if (value == null) return null;
    return value.replace("\r", "").replace("\n", "");
}
```

**문제점**
1. **중복** — 같은 코드가 두 클래스에 흩어져 유지보수 어려움
2. **누락 위험** — 새 로그 추가 시 이 메서드를 깜빡하면 그대로 취약
3. **전역 안전망 없음** — 스프링/마이바티스 등 **프레임워크 로그**는 정제 안 됨

---

## 5. 이번에 적용한 방식 (쉽게 짠 2중 방어)

**"흩어진 중복 → 공용 유틸 1개로 통합 + 전역 안전망 추가"** 방향으로 최대한 **쉽고 읽기 편하게** 구성했습니다.

```
      [1차 방어] 호출 지점                        [2차 방어] 전역
 log.warn(..., CrlfUtils.forLog(입력))   →   logback %replace 로 모든 로그 한 번 더 정제
     (팀 기존 코드와 똑같이 간단)                 (누락·프레임워크 로그까지 커버)
```

### 왜 "쉬운" 코드인가
- 공용 유틸 `CrlfUtils.forLog()`의 알맹이는 **딱 한 줄**: `value.replace("\r", "").replace("\n", "")` — 기존 팀 코드와 동일한 방식이라 새로 배울 게 없음.
- 정규식·복잡한 API 없음. **누구나 바로 이해**.
- 별도 패키지를 새로 만들지 않고 기존 `com.kiosk.global.security` 안에 둠.
- 전역 방어는 **설정 파일 1개**(`logback-spring.xml`)에 핵심 한 줄.

### 변경 요약

| 구분 | 파일 | 내용 |
|---|---|---|
| 🆕 신규 | `global/security/CrlfUtils.java` | 공용 정제 유틸 (`forLog()`) |
| 🆕 신규 | `resources/logback-spring.xml` | 전역 CRLF 방어 + 콘솔 설정 |
| ♻️ 수정 | `kiosk/receipt/ReceiptPrintClient.java` | 사설 메서드 → `CrlfUtils.forLog()` |
| ♻️ 수정 | `hq/branch/service/HqBranchAccountService.java` | 사설 메서드 → `CrlfUtils.forLog()` |
| ➕ 수정 | `branch/auth/service/BranchAuthService.java` | `@Slf4j` + 미검증 `loginId` 정제 감사 로그 |

> 전체 소스는 [§8 전체 수정 코드 모음](#8-전체-수정-코드-모음)에 정리했습니다.

---

## 6. 적용 전 vs 적용 후 (핵심)

> **가정:** 이 보안사항을 적용하지 **않았을** 때(Before)와 적용한 **후**(After)의 차이.

### 공격 입력 (동일)
```
loginId = ghost\r\n2026-08-06 10:00:00 INFO admin 로그인 성공 from 10.0.0.9
```

### ❌ Before — 미적용 (취약)
```log
WARN  존재하지 않는 아이디 조회: loginId=ghost
2026-08-06 10:00:00 INFO admin 로그인 성공 from 10.0.0.9
```
→ 줄바꿈이 그대로 반영되어 **가짜 로그 줄 삽입 성공**. 관리자·보안장비가 속음.

### ✅ After — 적용 (안전)
```log
WARN  존재하지 않는 아이디 조회: loginId=ghost2026-08-06 10:00:00 INFO admin 로그인 성공 from 10.0.0.9
```
→ `\r\n`이 제거되어 **한 줄로 유지**. 공격자가 새 줄을 못 만듦. **위조 실패.**

### 무엇이 달라지나 — 요약표

| 항목 | Before (미적용) | After (적용) |
|---|---|---|
| 로그에 `\r\n` 주입 | ✅ 가짜 줄 생성 성공 | ❌ 개행 제거, 한 줄 유지 |
| 감사 로그 신뢰성 | 🔴 위조 가능 | 🟢 위조 불가 |
| 방어 범위 | 정제한 일부 로그만 | **모든 로그**(호출지점+전역) |
| 코드 유지보수 | 클래스마다 중복 | 공용 유틸 1곳 |

### 실제 실행 결과 (JDK 21로 컴파일·실행 확인)
```
---- 정제 없이 (취약) ----
WARN 존재하지 않는 아이디 조회: loginId=ghost
2026-08-06 10:00:00 INFO  [http] admin 로그인 성공 from 10.0.0.9   ← 가짜 줄!

---- CrlfUtils.forLog() 적용 후 (안전) ----
WARN 존재하지 않는 아이디 조회: loginId=ghost2026-08-06 10:00:00 INFO ...  ← 한 줄, 위조 불가
검증: CR/LF 제거됨 = true   [PASS]
```

---

## 7. 검증 방법

### curl 재현 (앱 실행 중)
```bash
# 존재하지 않는 아이디 조회 → 정제된 감사 로그가 남는지 확인
curl "http://localhost:8080/api/branch-auth/login-identity/ghosttest"
```
로그에 `존재하지 않는 지점 로그인 아이디 조회 시도: loginId=ghosttest` 가 **한 줄로** 남으면 정상.

> **참고(방어 계층):** URL **경로**에 `%0d%0a`를 직접 넣으면 Tomcat이 400으로 먼저 차단합니다(1차 방어선). 하지만 **요청 본문·쿼리·외부 서비스 응답**(예: 프린터 응답)에는 CRLF가 들어올 수 있고, 그 경로를 `CrlfUtils` + `logback %replace`가 막습니다.

> 위 [§6 실제 실행 결과](#6-적용-전-vs-적용-후-핵심)는 `CrlfUtils`를 JDK 21로 직접 컴파일·실행해 CRLF 제거를 확인한 결과입니다.

---

## 8. 전체 수정 코드 모음

이번에 develop3.0에 적용한 **모든 코드**를 파일별로 정리합니다.

### 8.1 🆕 신규 — `CrlfUtils.java` (공용 유틸)
**경로:** `backend/src/main/java/com/kiosk/global/security/CrlfUtils.java`

```java
package com.kiosk.global.security;

/**
 * CRLF(줄바꿈) 로그 위조 방지 유틸리티.
 *
 * <p>사용자 입력이나 외부 시스템 응답을 로그로 남기기 전에 개행 문자(CR {@code \r}, LF {@code \n})를
 * 제거한다. 개행이 그대로 로그에 들어가면 공격자가 "가짜 로그 줄"을 삽입(로그 위조, CWE-117)할 수 있다.
 *
 * <p>사용 예:
 * <pre>{@code
 * log.warn("로그인 실패: id={}", CrlfUtils.forLog(loginId));
 * }</pre>
 */
public final class CrlfUtils {

    private CrlfUtils() {
        // 유틸리티 클래스 - 인스턴스화 금지
    }

    /**
     * 로그로 남기기 전에 개행 문자(\r, \n)를 제거한다. null 안전.
     *
     * @param value 신뢰할 수 없는 원본 문자열(로그 대상)
     * @return 개행이 제거된 안전한 문자열, 입력이 {@code null} 이면 {@code null}
     */
    public static String forLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "").replace("\n", "");
    }
}
```

### 8.2 🆕 신규 — `logback-spring.xml` (전역 방어)
**경로:** `backend/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  CRLF(로그 위조, CWE-117) 전역 방어 설정.

  로그 메시지(%m)에 들어 있는 개행 문자(\r, \n)를 공백으로 치환한다.
  호출 지점의 CrlfUtils.forLog() 와 함께 이중(2중)으로 막으며,
  이 설정은 우리 코드뿐 아니라 스프링/마이바티스 등 프레임워크 로그까지 모두 보호한다.

  참고: 파일명을 logback-spring.xml 로 두면 스프링부트가 이 설정을 자동으로 읽는다.
-->
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <charset>UTF-8</charset>
            <!-- 핵심: %replace(%m){'개행', ' '} 로 메시지 안의 CR/LF 를 공백으로 바꾼다. -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %replace(%m){'[\r\n]+', ' '}%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

### 8.3 ♻️ 수정 — `ReceiptPrintClient.java`
**경로:** `backend/src/main/java/com/kiosk/kiosk/receipt/ReceiptPrintClient.java`
프린터 서비스 응답 본문(외부 입력)을 로그로 남기는 부분. **사설 `sanitizeForLog` 제거 → 공용 `CrlfUtils.forLog()`**.

**Before**
```java
import com.fasterxml.jackson.databind.ObjectMapper;
// ...
            if (!success) {
                log.warn("영수증 출력 실패 응답: status={}, body={}",
                        response.statusCode(), sanitizeForLog(response.body()));
            }
        } catch (Exception e) {
            log.warn("프린터 서비스에 연결하지 못했습니다 ({}). ...", sanitizeForLog(printerUrl));
        }
    }

    // ↓ 클래스마다 중복돼 있던 사설 메서드
    private String sanitizeForLog(String value) {
        if (value == null) return null;
        return value.replace("\r", "").replace("\n", "");
    }
}
```

**After**
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiosk.global.security.CrlfUtils;   // ← 추가
// ...
            if (!success) {
                // 프린터 서비스 응답 본문은 외부(신뢰 불가) 입력이므로 로그 위조(CRLF)를 막기 위해 정제한다.
                log.warn("영수증 출력 실패 응답: status={}, body={}",
                        response.statusCode(), CrlfUtils.forLog(response.body()));
            }
        } catch (Exception e) {
            log.warn("프린터 서비스에 연결하지 못했습니다 ({}). 영수증은 화면으로만 표시됩니다.",
                    CrlfUtils.forLog(printerUrl));
        }
    }
    // 사설 sanitizeForLog 메서드 삭제 (공용 유틸로 대체)
}
```

### 8.4 ♻️ 수정 — `HqBranchAccountService.java`
**경로:** `backend/src/main/java/com/kiosk/hq/branch/service/HqBranchAccountService.java`
Firebase 사용자 삭제 실패 시 이메일을 로그로 남기는 부분. **사설 메서드 제거 → 공용 유틸**.

**Before**
```java
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import com.kiosk.hq.branch.dto.HqBranchAccountResponse;
// ...
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase 사용자 삭제 실패: {}", sanitizeForLog(email));
            return;
        }
// ...
    // ↓ 또 하나의 중복 사설 메서드
    private String sanitizeForLog(String value) {
        if (value == null) return null;
        return value.replace("\r", "").replace("\n", "");
    }
}
```

**After**
```java
import com.kiosk.domain.branchapplication.BranchApplicationRepository;
import com.kiosk.global.security.CrlfUtils;   // ← 추가
import com.kiosk.hq.branch.dto.HqBranchAccountResponse;
// ...
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase 사용자 삭제 실패: {}", CrlfUtils.forLog(email));
            return;
        }
// ...
    // 사설 sanitizeForLog 메서드 삭제 (공용 유틸로 대체)
}
```

### 8.5 ➕ 수정 — `BranchAuthService.java`
**경로:** `backend/src/main/java/com/kiosk/branch/auth/service/BranchAuthService.java`
검증 가능한 실사례. 미검증 `loginId`(PathVariable) 조회 실패 시 **정제된 감사 로그**를 남긴다.

**Before**
```java
import com.kiosk.global.security.TurnstileVerifier;
import lombok.RequiredArgsConstructor;
// ...
@Service
@RequiredArgsConstructor
@Transactional
public class BranchAuthService {
// ...
    public LoginIdentityResponse loginIdentity(String loginId) {
        Admin admin = adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 아이디입니다."));
```

**After**
```java
import com.kiosk.global.security.CrlfUtils;        // ← 추가
import com.kiosk.global.security.TurnstileVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;                  // ← 추가
// ...
@Service
@Slf4j                                             // ← 추가
@RequiredArgsConstructor
@Transactional
public class BranchAuthService {
// ...
    public LoginIdentityResponse loginIdentity(String loginId) {
        Admin admin = adminRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    // 존재하지 않는 아이디 조회는 계정 탐색(enumeration) 시도일 수 있어 감사 로그로 남긴다.
                    // loginId는 검증되지 않은 사용자 입력(PathVariable)이므로 CrlfUtils로 개행을 제거해 로그 위조를 막는다.
                    log.warn("존재하지 않는 지점 로그인 아이디 조회 시도: loginId={}", CrlfUtils.forLog(loginId));
                    return new IllegalArgumentException("등록되지 않은 아이디입니다.");
                });
```

---