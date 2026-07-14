package com.kiosk.hq.auth.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // jakarta.transaction 대신 스프링의 @Transactional 권장

import com.kiosk.hq.auth.repository.RegistrationToken;
import com.kiosk.hq.auth.repository.RegistrationTokenRepository; // 레포지토리 인터페이스 임포트 필요

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    // 엔티ti가 아닌 Repository 인터페이스를 주입받아야 합니다.
    private final RegistrationTokenRepository tokenRepository;

    // 1. URL 생성용 토큰 발급
    @Transactional // DB에 Insert가 발생하므로 트랜잭션 처리를 해주는 것이 안전합니다.
    public String createRegistrationLink() {
        // java.util.UUID를 사용해 랜덤값 생성
        String randomToken = UUID.randomUUID().toString().replace("-", ""); 
        
        RegistrationToken tokenEntity = new RegistrationToken(randomToken, 3); // 3시간 설정
        tokenRepository.save(tokenEntity);
        
        // 프론트엔드(Vue) 서버 주소에 맞게 포트나 도메인을 변경해 주세요.
        return "http://localhost:5173/register?token=" + randomToken; 
    }

    // 2. 토큰 유효성 검증
    @Transactional(readOnly = true) // 단순 조회 검증이므로 readOnly = true가 성능상 유리합니다.
    public boolean validateToken(String token) {
        // java.util.Optional 타입 명시
        Optional<RegistrationToken> oToken = tokenRepository.findByToken(token);
        
        if (oToken.isEmpty()) {
            return false; // 토큰이 없음
        }
        
        RegistrationToken tokenEntity = oToken.get();
        
        // 엔티티 내부에 구현된 만료 여부 메서드 활용
        if (tokenEntity.isExpired() || tokenEntity.isUsed()) {
            return false; // 만료되었거나 이미 사용됨
        }
        
        return true;
    }
}