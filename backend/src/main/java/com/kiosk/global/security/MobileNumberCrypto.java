package com.kiosk.global.security;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 고객 전화번호를 평문으로 저장하지 않기 위한 해시 유틸리티.
 *
 * hash(): 조회 전용(WHERE 절 매칭). HMAC-SHA256이라 같은 입력이면 항상 같은 출력이 나오지만,
 * 해시에서 원래 번호를 복원할 수는 없다 - 원본이 필요한 화면(쿠폰 관리 등)은 복호화 대신
 * mask()로 만든 마스킹 값(예: 010****5678)을 별도 컬럼에 저장해 보여준다. 역방향 비밀키를
 * 팀원 전체에 배포/동기화해야 하는 부담을 없애기 위한 선택 - mask()는 키가 필요 없다.
 */
@Component
public class MobileNumberCrypto {

    private static final String HMAC_ALGO = "HmacSHA256";

    private final SecretKeySpec hmacKey;

    public MobileNumberCrypto(@Value("${mobile-number.hash-secret}") String hashSecret) {
        requireStrongSecret("MOBILE_NUMBER_HASH_SECRET", hashSecret);
        this.hmacKey = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
    }

    private static void requireStrongSecret(String envVarName, String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(envVarName + "은 32바이트 이상의 값으로 설정해야 합니다.");
        }
    }

    public String hash(String plainMobileNumber) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(hmacKey);
            byte[] result = mac.doFinal(plainMobileNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("전화번호 해시 생성에 실패했습니다.", e);
        }
    }

    /** 앞 3자리·뒤 4자리만 남기고 가운데를 가린다 (010****5678). 키가 필요 없는 순수 문자열 변환. */
    public static String mask(String plainMobileNumber) {
        int len = plainMobileNumber.length();
        if (len <= 7) {
            return "*".repeat(len);
        }
        String prefix = plainMobileNumber.substring(0, 3);
        String suffix = plainMobileNumber.substring(len - 4);
        return prefix + "*".repeat(len - 7) + suffix;
    }
}
