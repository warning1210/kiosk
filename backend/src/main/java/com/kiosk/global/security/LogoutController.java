package com.kiosk.global.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 지금까지 로그아웃은 프론트에서 localStorage를 지우는 것이 전부였다 - 서버가 발급한 토큰은
// 그대로 12시간 살아있어서, 로그아웃 전에 토큰을 복사해둔 사람은 계속 쓸 수 있었다.
// 여기서 두 가지를 한다: (1) 토큰 세대를 올려 기존 토큰을 전부 즉시 무효화하고,
// (2) httpOnly 쿠키를 지운다. 쿠키는 JS가 못 지우므로 서버가 지워주는 수밖에 없다.
//
// 본점/지점(폴백) 둘 다 같은 AdminTokenService 토큰을 쓰므로 엔드포인트도 하나로 공유한다.
// Firebase로 로그인한 지점은 프론트의 signOut()이 이미 처리하므로 여기 해당 없음.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LogoutController {

    private final AdminTokenService adminTokenService;
    private final AuthCookie authCookie;

    // 이미 만료됐거나 위조된 토큰으로 온 요청도 쿠키는 지워준다 - 그래야 죽은 쿠키가 브라우저에
    // 남아 계속 401을 맞는 상황이 안 생긴다. 무효화 성공 여부는 응답으로 구분해주지 않는다
    // (구분해주면 "이 토큰이 유효한가"를 알려주는 판별기가 되어버린다).
    // Authorization 헤더는 AuthCookieFilter가 쿠키에서 채워준 값이다.
    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                       HttpServletResponse response) {
        authCookie.clear(response);
        if (authorization == null || !authorization.startsWith("Bearer ")) return;

        Long adminId = adminTokenService.verify(authorization.substring(7));
        if (adminId != null) adminTokenService.revokeAll(adminId);
    }
}
