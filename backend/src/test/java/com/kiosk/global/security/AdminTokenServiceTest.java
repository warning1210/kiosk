package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * 관리자 토큰이 "위조·재사용 시도"에 실제로 버티는지 확인하는 테스트.
 *
 * <p>정상 동작만 확인하면 공격 시나리오가 검증되지 않은 채로 남는다. 여기서는 토큰을 손에 넣은
 * 공격자가 시도할 만한 것(내용 바꿔치기, 서명 변조, 다른 키로 위조, 만료 무시)을 그대로 재현해
 * 전부 거부되는지 본다.
 */
class AdminTokenServiceTest {

    private static final String SECRET = "test-secret-for-hmac-signing-32-bytes";

    private final AdminTokenService service = new AdminTokenService(SECRET);

    @Test
    void issuedToken_resolvesToSameAdminId() {
        String token = service.issue(42L);

        assertThat(service.verify(token)).isEqualTo(42L);
    }

    // 공격 시나리오 1: 자기 토큰의 adminId만 다른 값으로 바꿔 다른 관리자로 행세한다.
    @Test
    void tokenWithTamperedAdminId_isRejected() {
        String raw = decode(service.issue(42L));
        String[] parts = raw.split(":");
        String tampered = encode("999:" + parts[1] + ":" + parts[2]); // 서명은 그대로 두고 ID만 교체

        assertThat(service.verify(tampered)).isNull();
    }

    // 공격 시나리오 2: 만료 시각을 미래로 늘려 만료를 무력화한다.
    @Test
    void tokenWithExtendedExpiry_isRejected() {
        String raw = decode(service.issue(42L));
        String[] parts = raw.split(":");
        long farFuture = Long.parseLong(parts[1]) + 60 * 60 * 24 * 365;
        String tampered = encode(parts[0] + ":" + farFuture + ":" + parts[2]);

        assertThat(service.verify(tampered)).isNull();
    }

    // 공격 시나리오 3: 서명 부분만 임의의 값으로 바꿔 검증을 통과시키려 한다.
    @Test
    void tokenWithForgedSignature_isRejected() {
        String raw = decode(service.issue(42L));
        String[] parts = raw.split(":");
        String tampered = encode(parts[0] + ":" + parts[1] + ":" + "forged-signature");

        assertThat(service.verify(tampered)).isNull();
    }

    // 공격 시나리오 4: 키를 모르는 상태에서 다른 키로 서명해 통째로 만들어낸다.
    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        AdminTokenService attacker = new AdminTokenService("another-secret-that-is-32-bytes-long!");
        String forged = attacker.issue(42L);

        assertThat(service.verify(forged)).isNull();
    }

    // 공격 시나리오 5: 토큰 형식 자체를 벗어난 값으로 예외를 유발해 검증을 우회한다.
    @Test
    void malformedToken_isRejectedWithoutThrowing() {
        assertThat(service.verify("not-a-token")).isNull();
        assertThat(service.verify("")).isNull();
        assertThat(service.verify(encode("1:2"))).isNull(); // 서명 부분이 없는 형식
    }

    // 서명 키가 약하면 위조가 쉬워지므로, 애초에 기동을 막는다.
    @Test
    void shortSecret_isRejectedAtConstruction() {
        assertThatThrownBy(() -> new AdminTokenService("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static String decode(String token) {
        return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
