# 고객 전화번호 저장 방식 — 해시 + 마스킹 설계

고객 전화번호를 DB에 평문으로 저장하지 않기 위한 설계와 실제 코드 흐름을 정리한다.

## 왜 이렇게 만들었나

처음엔 AES-256-GCM 암호화(복호화 가능) → 그다음 HMAC-SHA256(비밀키로 조회용 해시 생성) 순서로 두 번 설계를 바꿨고, 최종적으로 **키가 아예 없는 SHA-256 해시**로 정착했다.

- AES 방식은 복호화 키를 팀원 전체가 똑같이 맞춰야 하는 운영 부담이 있어서 제외 — 애초에 원본을 화면에 다시 보여줄 일이 없었다(본점 쿠폰 화면도 "이 쿠폰이 누구 것인지 구분"만 하면 됨, 마스킹 값으로 충분).
- HMAC 방식(키 있는 해시)은 그다음으로 채택했는데, 여기서도 "팀원끼리 `MOBILE_NUMBER_HASH_SECRET` 값을 맞춰야 서버가 뜬다"는 설정 부담이 남아있었다.
- 이건 팀 내부용 작은 프로젝트라 "이런 기능을 넣었다"는 것 자체가 목적이지, DB 유출까지 가정한 엄격한 방어가 요구사항은 아니라고 판단해서 최종적으로 **키 없는 SHA-256**으로 단순화했다. 설정값이 아예 없어서 서버가 별도 환경변수 없이 바로 뜬다.

그래서 지금은 **복호화가 아예 불가능한 단방향 해시**로 조회용 컬럼을 만들고, 화면 표시가 필요한 곳은 **키 없는 마스킹 문자열**을 별도 컬럼에 저장하는 방식이다. "원본이 필요 없다면 애초에 원본을 복원할 수 있는 구조 자체를 두지 않는다"가 핵심.

> **트레이드오프를 분명히 알고 선택한 것**: 전화번호는 `010` + 8자리라 경우의 수가 약 1억 개뿐이라, 키 없는 SHA-256은 DB가 통째로 유출되면 공격자가 1억 개를 미리 다 해시 계산해두는 데 몇 초~몇 분이면 끝나서 사실상 평문 유출과 다를 바 없어진다. 프로덕션급 개인정보 보호가 필요한 서비스라면 이 방식은 부적절하고 HMAC(키 있는 해시)을 다시 써야 한다. 지금은 "팀 내부 학습/데모 프로젝트에서 설정 부담 없이 평문 저장만 피한다"는 좁은 목표에 맞춘 선택.

## 컬럼 구성

`db/init/01-schema.sql`의 `customer` 테이블:

```sql
CREATE TABLE `customer` (
  `customer_id` bigint PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `mobile_number_hash` char(64) UNIQUE NOT NULL,     -- 조회 전용
  `mobile_number_masked` varchar(20) NOT NULL,       -- 화면 표시용
  ...
);
```

평문 전화번호를 저장하는 컬럼은 아예 없다. 대신 같은 원본에서 파생된 두 값을 각각 다른 용도로 저장한다. SHA-256 출력도 32바이트(64자 hex)라 HMAC-SHA256 때와 컬럼 크기가 동일해서, 방식을 바꾸면서도 스키마 변경은 필요 없었다.

| 컬럼 | 생성 방법 | 용도 | 원본 복원 |
|---|---|---|---|
| `mobile_number_hash` | SHA-256 (키 없음) | `WHERE` 절 조회 (같은 번호 재방문 고객 식별) | 이론상 가능(위 트레이드오프 참고), 실제로는 안 함 |
| `mobile_number_masked` | 앞 3자리 + `****` + 뒤 4자리 (키 불필요) | 관리자 화면 표시 | 불가능 (애초에 가운데 4자리가 사라짐) |

## `MobileNumberCrypto.java` 코드 해설

`backend/src/main/java/com/kiosk/global/security/MobileNumberCrypto.java`

```java
public class MobileNumberCrypto {

    private static final Pattern MOBILE_NUMBER_PATTERN = Pattern.compile("^\\d{11}$");

    private MobileNumberCrypto() {
    }

    public static String hash(String plainMobileNumber) {
        requireValidFormat(plainMobileNumber);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(plainMobileNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("전화번호 해시 생성에 실패했습니다.", e);
        }
    }
```

- 스프링 빈이 아니다(`@Component` 없음) — 키/설정값 등 인스턴스 상태가 전혀 없어서 그냥 정적 유틸리티 클래스로 뒀다. 생성자는 `private`으로 막아서 인스턴스화 자체를 방지.
- `hash()`는 `MessageDigest.getInstance("SHA-256")`으로 전화번호를 해시해서 64자 hex 문자열(32바이트)로 반환한다. `char(64)` 컬럼과 정확히 맞아떨어짐.
- **같은 입력 → 항상 같은 출력** (결정적)이라서 "이 번호로 조회하면 같은 고객이 나온다"가 보장됨. 위 트레이드오프에서 설명했듯, 이 결정성과 좁은 입력 공간이 합쳐지면 DB 유출 시 역산 위험이 있다는 게 이 설계의 알려진 한계.

```java
    public static String mask(String plainMobileNumber) {
        requireValidFormat(plainMobileNumber);
        return plainMobileNumber.substring(0, 3) + "****" + plainMobileNumber.substring(7);
    }

    private static void requireValidFormat(String mobileNumber) {
        if (mobileNumber == null || !MOBILE_NUMBER_PATTERN.matcher(mobileNumber).matches()) {
            throw new IllegalArgumentException("전화번호는 숫자 11자리여야 합니다.");
        }
    }
}
```

- `01012345678` (11자) → `010` + `****` + `5678` = `010****5678`.
- **`hash()`/`mask()` 둘 다 진입 시점에 `requireValidFormat()`으로 정확히 숫자 11자리인지 검증하고, 아니면 `IllegalArgumentException`을 던진다.** 이 앱은 키오스크 UI(`PHONE_DIGITS = 11`)를 거쳐야만 전화번호를 입력할 수 있어서 형식이 항상 고정이라는 전제가 성립하는데, 백엔드 API를 직접 호출(Postman 등)하면 그 전제를 우회할 수 있으므로 서버 경계에서도 같은 규칙을 강제한다. 검증을 `hash()`/`mask()` 공통 헬퍼 하나로 두면 두 메서드의 호출자(`OrderService`, `CustomerService`) 전부가 자동으로 보호된다.
- `IllegalArgumentException`은 `GlobalExceptionHandler`가 이미 400 Bad Request(`{"message": "잘못된 요청입니다."}`)로 변환해주므로, 이 검증을 위한 별도 예외 처리 코드가 컨트롤러/서비스 쪽에는 필요 없다.
- 검증이 길이를 11로 못박아 보장하기 때문에, `mask()` 내부는 가변 길이 계산 없이 `prefix(3) + "****" + suffix(4)` 고정 조합으로 단순함.
- 마스킹은 가운데 구간을 아예 버리는 방식이라 이 값만으로는 원본 번호를 복원할 수 없다.

## 실제 사용 흐름

### 1) 키오스크 주문 시 고객 식별 — `OrderService.checkout()`

`backend/src/main/java/com/kiosk/kiosk/order/service/OrderService.java:69-78`

```java
if (request.customerMobileNumber() != null && !request.customerMobileNumber().isBlank()) {
    String rawMobileNumber = request.customerMobileNumber();
    String mobileNumberHash = MobileNumberCrypto.hash(rawMobileNumber);
    customer = customerRepository.findByMobileNumberHash(mobileNumberHash)
            .orElseGet(() -> customerRepository.save(
                    Customer.builder()
                            .mobileNumberHash(mobileNumberHash)
                            .mobileNumberMasked(MobileNumberCrypto.mask(rawMobileNumber))
                            .build()));
}
```

흐름:
1. 사용자가 키오스크에서 전화번호(`rawMobileNumber`)를 입력한다.
2. `hash()`로 조회용 해시를 만들고, 이 해시로 기존 고객인지 찾는다(`findByMobileNumberHash`).
3. 없으면 신규 고객으로 즉시 등록 — 이때 `mobileNumberHash`(조회용)와 `mobileNumberMasked`(표시용) 둘 다 저장하고, **평문(`rawMobileNumber`)은 이 메서드 스코프를 벗어나면 어디에도 남지 않는다.** DB는 물론 로그에도 안 찍힘.
4. 이후 이 메서드가 리턴되고 나면, 서버 어디에도 평문 전화번호가 남아있지 않다 — 애초에 저장을 안 했으니 유출될 컬럼 자체가 없는 것. (단, 위 트레이드오프처럼 저장된 해시 자체는 DB 유출 시 역산 가능하다는 점은 별개.)

### 2) 본점 쿠폰 화면에서 고객 식별 — `HqCouponResponse.from()`

`backend/src/main/java/com/kiosk/hq/coupon/dto/HqCouponResponse.java:21-35`

```java
coupon.getCustomer() != null ? coupon.getCustomer().getMobileNumberMasked() : null,
```

본점 관리자가 "이 쿠폰이 어느 고객 것인지" 구분해야 할 때, 저장된 `mobileNumberMasked`(`010****5678`)를 그대로 응답에 실어 보낸다. 복호화 로직 자체가 없으므로 `MobileNumberCrypto`를 이 클래스에 주입할 필요도 없다.

### 3) 키오스크에서 내 쿠폰/포인트 조회 — `CustomerService.getByMobileNumber()`

`backend/src/main/java/com/kiosk/kiosk/customer/service/CustomerService.java:28-29`

```java
public CustomerResponse getByMobileNumber(String mobileNumber) {
    String mobileNumberHash = MobileNumberCrypto.hash(mobileNumber);
    Customer customer = customerRepository.findByMobileNumberHash(mobileNumberHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 회원이 아닙니다."));
    ...
```

`hash()`를 호출하는 두 번째(마지막) 지점. `OrderService.checkout()`과 마찬가지로 `hash()` 내부의 `requireValidFormat()` 검증을 그대로 물려받는다.

## 설정값

없음. `MobileNumberCrypto`가 정적 유틸리티(키/설정 없음)라 `.env`/`application.yml`에 넣어야 하는 값이 없다 — 과거 `MOBILE_NUMBER_HASH_SECRET`, 그 이전 `MOBILE_NUMBER_ENC_SECRET` 둘 다 코드에서 제거하면서 설정 파일에서도 삭제했다.

## 테스트

`backend/src/test/java/com/kiosk/global/security/MobileNumberCryptoTest.java`

- `hash_isDeterministicForSameInput`: 같은 번호 → 같은 해시 (조회가 실제로 동작하려면 필수 성질)
- `hash_differsForDifferentInput`: 다른 번호 → 다른 해시
- `hash_withWrongLength_throwsIllegalArgument`: 11자리가 아니면 즉시 예외
- `mask_keepsFirstThreeAndLastFourDigits`: `01012345678` → `010****5678`
- `mask_withWrongLength_throwsIllegalArgument`: 7자리 등 11자리가 아니면 즉시 예외
- `mask_withNonDigits_throwsIllegalArgument`: `010-1234-5678`처럼 숫자가 아닌 문자가 섞이면 즉시 예외

## 한계 / 주의할 점

- **키 없는 해시의 본질적 한계 (가장 중요)**: DB가 유출되면, 전화번호 전체 스페이스(`010` + 8자리 ≈ 1억 개)가 크지 않아 공격자가 오프라인으로 SHA-256을 1억 번 계산해서 `mobile_number_hash` 값 전부를 원본 전화번호로 역산할 수 있다(GPU 기준 수 초~수 분). 즉 "DB 덤프만으로는 전화번호를 못 얻는다"는 보장은 **없다** — 이 프로젝트 규모/목적상 감수하기로 한 트레이드오프다. 실사용 서비스로 확장한다면 HMAC(키 있는 해시)이나 Argon2/bcrypt 같은 느린 해시로 다시 바꿔야 한다.
- **마스킹 값의 재사용 위험 없음**: `mobile_number_masked`는 여러 고객이 같은 값을 가질 수 있다(뒤 4자리가 같으면 마스킹 결과도 같음). 이 컬럼은 UNIQUE가 아니고 조회에도 안 쓰이므로 문제없음 — 조회는 전부 `mobile_number_hash`(UNIQUE) 기준.
- **11자리 고정 전제**: `requireValidFormat()`이 `010` + 8자리(총 11자리 숫자)만 허용한다. 만약 나중에 011/016 같은 구형 번호(10자리)나 해외 번호를 받아야 하는 요구사항이 생기면 이 정규식(`^\d{11}$`)부터 완화해야 한다 — 지금은 키오스크 키패드가 애초에 010 11자리만 입력 가능하게 만들어져 있어서 이 전제가 유효함.
