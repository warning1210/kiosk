package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 로그아웃한 토큰이 실제로 죽는지 확인한다.
 *
 * <p>이 기능이 없던 시절의 문제: 로그아웃은 프론트에서 localStorage를 지우는 것이 전부였고,
 * 서버가 발급한 토큰은 12시간 동안 그대로 유효했다. 로그아웃 전에 토큰을 복사해둔 사람은
 * (매장 공용 PC, 퇴사자, 유출된 액세스 로그) 계속 그 계정으로 API를 호출할 수 있었다.
 *
 * <p>AdminTokenService의 세대 카운터가 그 구멍을 막는다. 아래 테스트가 "로그아웃 전에는 통하고
 * 로그아웃 후에는 통하지 않는다"를 한 시나리오 안에서 같은 토큰으로 보여준다.
 */
class TokenRevocationTest {

    private final AdminTokenService service = new AdminTokenService("test-secret-for-hmac-signing-32-bytes");

    /** 핵심 시나리오. 매장 공용 PC에서 로그인한 뒤 토큰이 복사됐고, 그 다음 로그아웃한 상황. */
    @Test
    void tokenStopsWorkingAfterLogout() {
        // 1. 로그인 - 이 문자열이 공격자에게 복사됐다고 치자.
        String stolenToken = service.issue(1L);

        // 2. BEFORE: 로그아웃 전에는 당연히 통한다.
        assertThat(service.verify(stolenToken)).isEqualTo(1L);

        // 3. 로그아웃 - LogoutController가 부르는 그 메서드.
        service.revokeAll(1L);

        // 4. AFTER: 서명도 멀쩡하고 만료도 안 됐지만 세대가 죽어서 거부된다.
        assertThat(service.verify(stolenToken)).isNull();
    }

    /** 무효화는 그 계정의 "이전 토큰"만 죽인다 - 다시 로그인하면 정상 사용할 수 있어야 한다. */
    @Test
    void reloginAfterLogout_worksAgain() {
        service.revokeAll(1L);

        assertThat(service.verify(service.issue(1L))).isEqualTo(1L);
    }

    /** 한 사람의 로그아웃이 다른 관리자의 토큰까지 죽이면 안 된다. */
    @Test
    void logoutDoesNotAffectOtherAdmins() {
        String otherToken = service.issue(2L);

        service.revokeAll(1L);

        assertThat(service.verify(otherToken)).isEqualTo(2L);
    }

    /** 로그아웃은 발급해둔 SSE 스트림 티켓도 같이 죽여야 한다. */
    @Test
    void streamTicketAlsoDiesOnLogout() {
        String ticket = service.issueStreamTicket(1L);
        assertThat(service.verifyStreamTicket(ticket)).isEqualTo(1L);

        service.revokeAll(1L);

        assertThat(service.verifyStreamTicket(ticket)).isNull();
    }

    /** 여러 번 로그아웃해도 세대가 정상적으로 계속 올라간다. */
    @Test
    void repeatedLogout_keepsInvalidatingNewTokens() {
        String first = service.issue(1L);
        service.revokeAll(1L);

        String second = service.issue(1L);
        service.revokeAll(1L);

        assertThat(service.verify(first)).isNull();
        assertThat(service.verify(second)).isNull();
        assertThat(service.verify(service.issue(1L))).isEqualTo(1L);
    }
}
