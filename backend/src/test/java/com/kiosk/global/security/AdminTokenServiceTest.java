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
 * 공격자가 시도할 만한 것(내용 바꿔치기, 서명 변조, 다른 키로 위조, 만료 무시, 세대 되돌리기)을
 * 그대로 재현해 전부 거부되는지 본다.
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
        String[] parts = parts(service.issue(42L));
        String tampered = encode("999:" + parts[1] + ":" + parts[2] + ":" + parts[3]); // 서명은 그대로 두고 ID만 교체

        assertThat(service.verify(tampered)).isNull();
    }

    // 공격 시나리오 2: 만료 시각을 미래로 늘려 만료를 무력화한다.
    @Test
    void tokenWithExtendedExpiry_isRejected() {
        String[] parts = parts(service.issue(42L));
        long farFuture = Long.parseLong(parts[2]) + 60 * 60 * 24 * 365;
        String tampered = encode(parts[0] + ":" + parts[1] + ":" + farFuture + ":" + parts[3]);

        assertThat(service.verify(tampered)).isNull();
    }

    // 공격 시나리오 3: 서명 부분만 임의의 값으로 바꿔 검증을 통과시키려 한다.
    @Test
    void tokenWithForgedSignature_isRejected() {
        String[] parts = parts(service.issue(42L));
        String tampered = encode(parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + "forged-signature");

        assertThat(service.verify(tampered)).isNull();
    }

    // 공격 시나리오 4: 키를 모르는 상태에서 다른 키로 서명해 통째로 만들어낸다.
    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        AdminTokenService attacker = new AdminTokenService("another-secret-that-is-32-bytes-long!");
        String forged = attacker.issue(42L);

        assertThat(service.verify(forged)).isNull();
    }

    // 공격 시나리오 5: 로그아웃으로 무효화된 토큰의 세대 번호만 되돌려 되살리려 한다.
    // 세대도 서명 대상이라 값을 건드리는 순간 서명이 깨진다.
    @Test
    void tokenWithDowngradedVersion_isRejected() {
        service.revokeAll(42L);
        String[] parts = parts(service.issue(42L)); // 세대 1로 발급됨
        String downgraded = encode(parts[0] + ":0:" + parts[2] + ":" + parts[3]);

        assertThat(service.verify(downgraded)).isNull();
    }

    // 공격 시나리오 6: 토큰 형식 자체를 벗어난 값으로 예외를 유발해 검증을 우회한다.
    @Test
    void malformedToken_isRejectedWithoutThrowing() {
        assertThat(service.verify("not-a-token")).isNull();
        assertThat(service.verify("")).isNull();
        assertThat(service.verify(encode("1:2"))).isNull(); // 서명 부분이 없는 형식
    }

    // 세대 도입 이전 형식(adminId:만료:서명)은 조각 수가 달라 그대로 거부된다.
    // 배포 시점에 전원 재로그인이 필요하다는 뜻이므로, 의도된 동작임을 테스트로 못박아 둔다.
    @Test
    void legacyThreePartToken_isRejected() {
        assertThat(service.verify(encode("42:1900000000:some-old-signature"))).isNull();
    }

    // 서명 키가 약하면 위조가 쉬워지므로, 애초에 기동을 막는다.
    @Test
    void shortSecret_isRejectedAtConstruction() {
        assertThatThrownBy(() -> new AdminTokenService("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    // SSE 스트림 티켓: 정상 발급-검증 왕복이 되는지 확인.
    @Test
    void issuedStreamTicket_resolvesToSameAdminId() {
        String ticket = service.issueStreamTicket(42L);

        assertThat(service.verifyStreamTicket(ticket)).isEqualTo(42L);
    }

    // 스트림 티켓은 접두사가 달라 일반 토큰 형식이 아니다 - 유출돼도 일반 API 인증에는 못 쓴다.
    @Test
    void streamTicket_isRejectedByRegularVerify() {
        String ticket = service.issueStreamTicket(42L);

        assertThat(service.verify(ticket)).isNull();
    }

    // 반대 방향: 12시간짜리 풀 권한 로그인 토큰으로 스트림 엔드포인트를 열 수 없다.
    @Test
    void regularToken_isRejectedByStreamTicketVerify() {
        String token = service.issue(42L);

        assertThat(service.verifyStreamTicket(token)).isNull();
    }

    // 만료 시각을 늘려 1분 제한을 무력화하려는 시도도 서명 대상이라 거부된다.
    @Test
    void streamTicketWithExtendedExpiry_isRejected() {
        String[] parts = parts(service.issueStreamTicket(42L));
        long farFuture = Long.parseLong(parts[3]) + 60 * 60 * 24;
        String tampered = encode(parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + farFuture + ":" + parts[4]);

        assertThat(service.verifyStreamTicket(tampered)).isNull();
    }

    @Test
    void malformedStreamTicket_isRejectedWithoutThrowing() {
        assertThat(service.verifyStreamTicket("not-a-ticket")).isNull();
        assertThat(service.verifyStreamTicket("")).isNull();
    }

    private static String[] parts(String token) {
        return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8).split(":");
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
