package com.kiosk.hq.auth.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    // 토큰 문자열로 엔티티를 찾는 메서드가 필요합니다.
    Optional<RegistrationToken> findByToken(String token);
}
